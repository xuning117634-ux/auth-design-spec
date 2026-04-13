# 第三阶段：引入 Agent 网关，统一对接 IDaaS / IAM

---

## 1. 核心分工变化

| 职责 | phase2 归属 | phase3 归属 |
|---|---|---|
| OAuth2 redirect / callback | 业务 Agent | **Agent 网关** |
| code 换 Tc | 业务 Agent | **Agent 网关** |
| 申请 T1 | 业务 Agent | **Agent 网关** |
| 用 Tc + T1 合成 TR | 业务 Agent | **Agent 网关** |
| Tc / T1 / TR 生命周期管理 | 业务 Agent | **Agent 网关** |
| Agent 注册与身份管理 | 无 | **Agent 网关** |
| 站点登录态（site_session） | 业务 Agent | 业务 Agent（不变） |
| 判断当前请求需要什么 scope | 业务 Agent | 业务 Agent（不变） |
| TR 本地缓存 | 业务 Agent | 业务 Agent（不变） |
| 携带 TR 调用资源服务 | 业务 Agent | 业务 Agent（不变） |

**一句话总结**：网关负责 IDaaS / IAM 的全部编排；业务 Agent 只做"发起跳转 → 问网关要 TR → 缓存 TR → 带 TR 调资源"。

**Agent 不需要理解"授权"概念**，只知道"问网关要 TR，网关给了就用，网关让跳转就跳"。

---

## 2. 总体架构图

```mermaid
flowchart LR
    U["用户浏览器"]
    A["业务 Agent<br/>前端 + 瘦后端"]
    GW["Agent 网关<br/>统一认证授权编排"]
    I["IDaaS"]
    IAM["IAM<br/>T1 / TR"]
    R["资源服务<br/>API / MCP"]

    U <--> A
    U <--> GW
    A <--> GW
    GW <--> I
    GW <--> IAM
    A --> R
```

- 浏览器 OAuth2 重定向经过网关，callback URL 全部指向网关
- 业务 Agent 与 IDaaS / IAM 之间无直接连线
- 业务 Agent 拿到 TR 后本地缓存，直接调用资源服务（网关不代理资源流量）

---

## 3. 网关内部关键概念

### 3.1 Agent Registry（Agent 注册表）

每个业务 Agent 接入前在网关注册（一次性配置）：

```text
agent_id              → agt_business_001
agent_name            → 业务数据助手
agent_service_account → svc_ai_business_agent
principal             → com.huawei.business.agent
allowed_return_hosts  → [business-agent.huawei.com]
```

网关用注册表中的 `agent_service_account` / `principal` / `agent_id` 向 IAM 申请 T1，防止业务 Agent 伪造身份。`allowed_return_hosts` 用于白名单校验 `return_url`，防 open redirect。

### 3.2 gw_session（网关侧会话）

base 登录成功后由网关创建：

```text
gw_session_id → user_id, username, 创建时间
```

同时向浏览器种 `gw_session_id` cookie（网关域，HttpOnly + Secure），用于后续业务授权时识别用户，无需业务 Agent 传递。

### 3.3 gw_session_token（网关颁发给业务 Agent 的凭证）

base 登录完成后，网关通过 `return_url` 参数将其直接返回给业务 Agent：

```text
gw_session_token → 对应网关侧的 gw_session_id（不透明引用，不含敏感信息）
```

业务 Agent 将其存入站点 session，用于后续调用 `/gw/token/resource-token` 时标识用户。

### 3.4 gw_auth_context（网关侧授权上下文，预留）

业务授权成功后由网关创建，当前阶段 TR 直接发给业务 Agent，此结构为后续 TR 刷新预留：

```text
key:   gw_session_id + agent_id
value: Tc, T1, TR, consented_scopes, 过期时间
```

### 3.5 pending_auth_transaction（临时状态，用后即删）

OAuth2 重定向过程中暂存：

```text
gw_state → agent_id, scope, return_url, gw_session_id, outer_state
```

---

## 4. 主时序图

### 4.1 base 登录阶段

```mermaid
sequenceDiagram
    title Phase3：base 登录（网关完成后直接返回 gw_session_token）
    participant 用户 as 用户浏览器
    participant Agent as 业务 Agent
    participant GW as Agent 网关
    participant IDaaS as IDaaS

    用户->>Agent: GET /agent
    Agent->>Agent: 检查 site_session_id

    alt 没有站点登录态
        Agent-->>用户: 302 → GW /gw/auth/login?agent_id=agt_001&return_url=https://agent/agent&state=xyz

        用户->>GW: GET /gw/auth/login?agent_id=agt_001&return_url=https://agent/agent&state=xyz
        GW->>GW: 校验 agent_id 在注册表，校验 return_url host 合法
        GW->>GW: 记录 pending_state → {agent_id, return_url, outer_state=xyz}
        GW-->>用户: 302 → IDaaS /authorize?scope=base&client_id=gw_client&redirect_uri=https://gw/auth/base/callback&state=gw_state_001

        用户->>IDaaS: GET /authorize?scope=base&...
        IDaaS->>IDaaS: 处理登录（展示登录页、校验身份）
        IDaaS->>IDaaS: 登录成功，生成 authorization code
        IDaaS-->>用户: 302 → GW /gw/auth/base/callback?code=code_base&state=gw_state_001

        用户->>GW: GET /gw/auth/base/callback?code=code_base&state=gw_state_001
        GW->>GW: 校验 state，取出 {agent_id, return_url, outer_state}
        GW->>IDaaS: POST /oauth2/token（code 换基础登录结果）
        IDaaS-->>GW: 返回 base 结果（user_id、username 等）
        GW->>GW: 创建 gw_session，生成 gw_session_token
        GW->>GW: Set-Cookie: gw_session_id=xxx（网关域，HttpOnly）

        Note over GW,用户: 直接把 gw_session_token 和用户信息附在 return_url 上
        GW-->>用户: 302 → https://agent/agent?gw_session_token=xxx&user_id=z01062668&username=张建国&state=xyz

        用户->>Agent: GET /agent?gw_session_token=xxx&user_id=z01062668&username=张建国&state=xyz
        Agent->>Agent: 校验 state
        Agent->>Agent: 创建 site_session，存储 gw_session_token 和用户信息
        Agent-->>用户: Set-Cookie: site_session_id=site_001
        Agent-->>用户: 302 → /agent（清除 URL 参数）

        用户->>Agent: GET /agent
        Agent-->>用户: 返回聊天页
    else 已有站点登录态
        Agent-->>用户: 直接返回聊天页
    end
```

**说明**：

- 网关在 base 登录完成后，直接将 `gw_session_token` 和用户信息附在 `return_url` 上跳回
- 业务 Agent 在已有的 `/agent` handler 里检测 `gw_session_token` 参数，存入 session 后重定向到干净 URL
- 不需要专用 callback 接口，不需要额外的 exchange API 调用

### 4.2 业务授权阶段 + 获取 TR

```mermaid
sequenceDiagram
    title Phase3：业务授权 + TR（Agent 不关心授权，只问网关要 TR）
    participant 用户 as 用户浏览器
    participant Agent as 业务 Agent
    participant GW as Agent 网关
    participant IDaaS as IDaaS
    participant IAM as IAM
    participant 资源服务 as 资源服务 / MCP

    用户->>Agent: POST /chat/send（"分析12月财报"）
    Agent->>Agent: 判断需要 scope=report.read
    Agent->>Agent: 检查本地 TR 缓存

    alt 本地有有效 TR 且 scope 覆盖
        Agent->>资源服务: 带 TR 调资源
        资源服务-->>Agent: 返回数据
        Agent-->>用户: 返回答案

    else 本地无 TR / 过期 / scope 不够
        Note over Agent,GW: Agent 不关心为什么没 TR，直接问网关要
        Agent->>GW: GET /gw/token/resource-token?agent_id=agt_001&scope=report.read（Bearer gw_session_token）
        GW->>GW: 检查 gw_auth_context 中是否有满足该 scope 的有效 TR

        alt 网关有有效 TR（已存在 / 可用 Tc+T1 刷新）
            GW-->>Agent: 200 {access_token: "<TR JWT>", expires_in: 3600}
            Agent->>Agent: 写入本地 TR 缓存
            Agent->>资源服务: 带 TR 调资源
            资源服务-->>Agent: 返回数据
            Agent-->>用户: 返回答案

        else 网关也没有，需要用户参与授权
            Note over GW: 网关构造完整的授权跳转 URL，包含 return_url
            GW-->>Agent: 200 {status: "redirect", redirect_url: "https://gw/auth/authorize?agent_id=agt_001&scope=report.read&return_url=https://agent/chat&state=abc"}

            Note over Agent: Agent 不需要理解"授权"，只需透传 redirect_url
            Agent-->>用户: 200 {status: "redirect", redirect_url: "..."}

            Note over 用户: 前端存消息到 sessionStorage，跳转到网关给的 URL
            用户->>用户: sessionStorage.set("pending_message", "分析12月财报")
            用户->>GW: GET /gw/auth/authorize?agent_id=agt_001&scope=report.read&return_url=https://agent/chat&state=abc

            GW->>GW: 读取网关域 gw_session_id cookie → 确认用户身份
            GW->>GW: 记录 pending_state → {agent_id, scope, return_url, gw_session_id, outer_state=abc}
            GW-->>用户: 302 → IDaaS /authorize?scope=report.read&client_id=gw_client&redirect_uri=https://gw/auth/consent/callback&state=gw_state_002

            用户->>IDaaS: GET /authorize?scope=report.read&...
            IDaaS->>IDaaS: 检查已有登录态，处理业务授权
            IDaaS->>IDaaS: 授权成功，生成 authorization code
            IDaaS-->>用户: 302 → GW /gw/auth/consent/callback?code=code_tc&state=gw_state_002

            用户->>GW: GET /gw/auth/consent/callback?code=code_tc&state=gw_state_002
            GW->>GW: 校验 state，取出 {agent_id, scope, return_url, gw_session_id}
            GW->>IDaaS: POST /oauth2/token（code 换 Tc）
            IDaaS-->>GW: 返回 Tc
            GW->>GW: 从 Agent Registry 取 agent_service_account / principal / agent_id
            GW->>IAM: POST /iam/projects/{proxy_project_id}/assume_agent_token（申请 T1）
            IAM-->>GW: 返回 T1
            GW->>IAM: POST /iam/auth/resource-token（Tc + T1 → TR）
            IAM-->>GW: 返回 TR
            GW->>GW: 写入 gw_auth_context（预留，供后续 TR 刷新使用）

            Note over GW,用户: 网关直接跳回 return_url，无附加参数
            GW-->>用户: 302 → https://agent/chat?state=abc

            Note over 用户,Agent: 前端检测到已回跳（state 匹配），从 sessionStorage 恢复消息并重发
            用户->>Agent: POST /chat/send（从 sessionStorage 恢复 "分析12月财报"）
            Agent->>Agent: 检查本地 TR 缓存（仍为空）
            Agent->>GW: GET /gw/token/resource-token?agent_id=agt_001&scope=report.read（Bearer gw_session_token）
            GW->>GW: 从 gw_auth_context 取刚写入的 TR
            GW-->>Agent: 200 {access_token: "<TR JWT>", expires_in: 3600}
            Agent->>Agent: 写入本地 TR 缓存
            Agent->>资源服务: 带 TR 调资源
            资源服务-->>Agent: 返回数据
            Agent-->>用户: 返回答案
        end
    end
```

**说明**：

- **Agent 不需要理解"授权"概念**，只知道"问网关要 TR"
- 网关返回 `200 {status: "redirect", redirect_url}`（不是 403），Agent 只需透传给前端
- `redirect_url` 完全由网关构造，Agent 不需要知道 `/gw/auth/authorize` 接口存在，也不需要知道如何构造授权 URL
- 前端在跳转前将消息存入 `sessionStorage`，授权回来后自动恢复重发，用户感知连贯
- 业务授权完成后，网关跳回原聊天页（`return_url`），无附加参数，Agent 重调 `resource-token` 即可拿到 TR
- Agent 拿到 TR 后写入本地缓存，后续请求直接复用

---

## 5. 接口清单

### 5.1 业务 Agent 视角：phase2 vs phase3

| 业务 Agent 需要做的事 | phase2（直连） | phase3（接入网关） |
|---|---|---|
| 专用 callback 接口 | 2 个 | **0 个** |
| 需要理解的概念 | OAuth2 + IDaaS + IAM + Tc + T1 + TR + 授权 | **只有 TR + redirect_url** |
| 向外部换 token 次数 | IDaaS×2 + IAM×2 | **1 次**（`/gw/token/resource-token`，且按需调用） |
| 本地维护的令牌 | Tc + T1 + TR | **只有 TR**（Agent 不接触 Tc / T1） |
| 状态维护 | site_session + pending_auth + agent_security_context | **site_session（仅 gw_session_token）** |

### 5.2 业务 Agent ↔ Agent 网关（仅 1 个 API）

| 接口 | 方法 | 调用时机 | 说明 |
|---|---|---|---|
| `/gw/token/resource-token` | GET | 本地 TR 缺失 / 过期 / scope 不够时 | Header: `Authorization: Bearer gw_session_token`；Query: `agent_id`, `scope`；返回 TR 或 redirect 指令 |

**响应格式**：

成功返回 TR：
```json
{
  "access_token": "<TR JWT>",
  "expires_in": 3600
}
```

需要用户授权：
```json
{
  "status": "redirect",
  "redirect_url": "https://gw/auth/authorize?agent_id=agt_001&scope=report.read&return_url=https://agent/chat&state=abc"
}
```

### 5.3 浏览器 ↔ Agent 网关（302 经过，网关自有接口）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/gw/auth/login` | GET | 发起 base 登录，网关 302 到 IDaaS |
| `/gw/auth/base/callback` | GET | IDaaS base 登录回调（redirect_uri 指向这里） |
| `/gw/auth/authorize` | GET | 发起业务授权，网关 302 到 IDaaS |
| `/gw/auth/consent/callback` | GET | IDaaS 业务授权回调（redirect_uri 指向这里） |

### 5.4 Agent 网关 ↔ IDaaS（调用方从业务 Agent 变为网关，接口不变）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/authorize` | GET | 统一授权入口（scope=base 或业务 scope） |
| `/oauth2/token` | POST | code 换 token（base 结果或 Tc） |

### 5.5 Agent 网关 ↔ IAM（调用方从业务 Agent 变为网关，接口不变）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/iam/projects/{proxy_project_id}/assume_agent_token` | POST | 申请 T1（使用注册表中的 Agent 身份） |
| `/iam/auth/resource-token` | POST | 用 Tc + T1 申请 TR |

### 5.6 业务 Agent ↔ 资源服务（完全不变）

资源服务只接受 TR，与 phase2 完全一致。

---

## 6. 网关侧状态模型

```text
┌─────────────────────────────────────────────────────┐
│  agent_registry（静态配置，注册时写入）              │
│  agent_id → agent_name, agent_service_account,      │
│             principal, allowed_return_hosts          │
├─────────────────────────────────────────────────────┤
│  gw_session（base 登录后创建）                       │
│  gw_session_id → user_id, username, 创建时间         │
│  （同时以 cookie 形式种在网关域）                    │
├─────────────────────────────────────────────────────┤
│  gw_auth_context（业务授权后写入，预留供 TR 刷新）   │
│  (gw_session_id + agent_id) → Tc, T1, TR,           │
│                               consented_scopes,     │
│                               过期时间               │
├─────────────────────────────────────────────────────┤
│  pending_auth_transaction（临时，用后即删）           │
│  gw_state → agent_id, scope, return_url,            │
│             gw_session_id, outer_state               │
└─────────────────────────────────────────────────────┘
```

业务 Agent 侧状态极简：

```text
site_session: site_session_id → gw_session_token, user_id, username
tr_cache:     (agent_id + scope) → TR JWT, expires_at
```

---

## 7. 对比 phase2 的变化总结

| 维度 | phase2 | phase3 |
|---|---|---|
| IDaaS redirect_uri 指向 | 业务 Agent | Agent 网关 |
| code 换 Tc | 业务 Agent | Agent 网关 |
| T1 / TR 合成 | 业务 Agent | Agent 网关 |
| 业务 Agent 专用 callback 接口 | 2 个 | **0 个** |
| 业务 Agent 对外换 token 次数 | 4 次 | **按需调用 1 个 API** |
| 业务 Agent 维护的令牌 | Tc + T1 + TR | **仅 TR（本地缓存）** |
| 业务 Agent 需要理解的概念 | OAuth2 + 授权 + Tc/T1/TR | **只有 TR + redirect_url** |
| 授权失败时网关响应 | N/A（Agent 自己判断） | **返回 redirect_url，Agent 透传** |
| 新 Agent 接入成本 | 实现完整 OAuth2 + IAM 对接 | 注册到网关 + 页面加几行判断 + 调 1 个 API |
| 三令牌模型 | Tc / T1 / TR | Tc / T1 / TR（不变） |
| 资源服务认什么 | 只认 TR | 只认 TR（不变） |

---

## 8. 安全要点

1. **return_url host 白名单** — 网关根据 `allowed_return_hosts` 校验 return_url，防止 open redirect
2. **gw_session_id cookie 仅网关域，HttpOnly + Secure** — 浏览器无法通过 JS 读取
3. **gw_session_token 通过 HTTPS 传输** — 在 return_url 参数中传输，全程 HTTPS，Agent 收到后立即重定向到干净 URL
4. **网关是 IDaaS 的唯一 OAuth2 Client** — 所有 Agent 共用网关 client_id，网关用 agent_id 区分上下文
5. **T1 身份来自注册表，不由业务 Agent 传入** — 防止伪造

---

## 9. 简化总图

```mermaid
sequenceDiagram
    participant 用户
    participant 业务Agent
    participant Agent网关
    participant IDaaS
    participant IAM
    participant 资源服务

    用户->>业务Agent: 打开页面
    业务Agent->>Agent网关: 302 到 /gw/auth/login
    Agent网关->>IDaaS: 302 到 /authorize(scope=base)
    IDaaS-->>Agent网关: 回调 code
    Agent网关->>IDaaS: 用 code 换基础登录结果
    IDaaS-->>Agent网关: 返回 base 结果
    Agent网关->>Agent网关: 创建 gw_session
    Agent网关-->>业务Agent: 302 回 return_url（附带 gw_session_token）
    业务Agent->>业务Agent: 建立站点登录态

    用户->>业务Agent: 发起资源型对话
    业务Agent->>Agent网关: GET /gw/token/resource-token（Bearer gw_session_token）
    Agent网关-->>业务Agent: 200 {status: redirect, redirect_url}

    Note over 业务Agent: Agent 不理解授权，只透传 redirect_url
    业务Agent-->>用户: 返回 redirect_url
    用户->>Agent网关: 跳转到 redirect_url

    Agent网关->>IDaaS: 302 到 /authorize(scope=业务权限)
    IDaaS-->>Agent网关: 回调 code
    Agent网关->>IDaaS: 用 code 换 Tc
    IDaaS-->>Agent网关: 返回 Tc
    Agent网关->>IAM: 用 Agent 注册身份申请 T1
    IAM-->>Agent网关: 返回 T1
    Agent网关->>IAM: 用 Tc + T1 申请 TR
    IAM-->>Agent网关: 返回 TR
    Agent网关-->>用户: 302 回业务 Agent 页面

    用户->>业务Agent: 重发请求（sessionStorage 恢复）
    业务Agent->>Agent网关: GET /gw/token/resource-token
    Agent网关-->>业务Agent: 200 {access_token: TR}
    业务Agent->>业务Agent: 缓存 TR
    业务Agent->>资源服务: 带 TR 调资源
    资源服务-->>业务Agent: 返回结果
    业务Agent-->>用户: 返回答案
```

这张图用于快速理解主线，重点只有五句：

- 第一次打开页面时，网关代为完成基础登录，Agent 只收一个 `gw_session_token`。
- 第一次真正访问业务资源时，Agent 问网关要 TR，网关说"需要跳转"，Agent 透传给前端。
- 网关代为完成全部授权流程（IDaaS 授权 → 换 Tc → 申请 T1 → 合成 TR），Agent 全程不参与。
- 授权完成后 Agent 再问一次网关，直接拿到 TR，缓存后调资源。
- **业务 Agent 不需要理解 OAuth2、不需要理解授权、不接触 Tc / T1，只知道 TR 和 redirect_url。**

---

## 10. 当前阶段建议

- 先用 1 个业务 Agent 跑通全流程（base 登录 → 业务授权 → 获取 TR → 调资源）
- scope 粒度继续保持粗粒度（`base` / `report` / `invoice`）
- TR 刷新：`gw_auth_context` 已预留，后续实现时网关用存储的 Tc + T1 自动续期，对 Agent 透明
- 增量授权（scope 追加而非重新全量授权）作为后续优化项