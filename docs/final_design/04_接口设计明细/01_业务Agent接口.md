# 业务 Agent 接口

## 1. 模块职责

业务 Agent 负责：

- 提供用户入口页面。
- 建立本地 `site_session`。
- 判断当前请求需要哪些 `MCP tool`。
- 按需向 Agent 网关申请 `TR`。
- 使用一次性票据从 Agent 网关换取用户信息或 `TR`。
- 带 `TR` 调用 `MCP 网关`。

业务 Agent 不负责：

- 直连 `IDaaS / IAM`。
- 自己处理 OAuth2 callback。
- 自己构造权限点 code。
- 自己申请 `Tc / T1 / TR`。
- 在浏览器 URL 中接收用户信息或令牌。

## 2. 本地状态模型

### 2.1 `site_session`

```text
site_session_id -> {
  user_id,
  username,
  created_at
}
```

`site_session` 是业务 Agent 自己的网站登录态。它由业务 Agent 后端在成功交换 `ticketST` 后创建。

### 2.2 `tr_cache`

```text
key:   site_session_id + agent_id
value: current_tr, covered_tools, coveredPermissionPointCodes, expires_at
```

`TR` 是否复用由业务 Agent 本地缓存决定，Agent 网关不再维护“当前用户已有 TR 覆盖范围”。

## 3. 接口清单

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/agent` | 页面入口；也承接网关登录或授权完成后的回跳 |
| `GET` | `/agent/session` | 查询当前站点会话是否有效 |
| `POST` | `/agent/logout` | 退出登录，清理本地会话与 Cookie |
| `POST` | `/chat/send` | 处理用户消息；按需向网关申请 `TR` |

## 4. `GET /agent`

### 4.1 用途

- 浏览器首次进入业务 Agent。
- 网关 base 登录完成后，通过 `return_url` 携带 `ticketST` 回到这里。
- 网关授权完成后，通过 `return_url` 携带 `token_result_ticket` 回到这里。

### 4.2 请求示例：首次进入

```http
GET /agent
Cookie: site_session_id=site_789
```

### 4.3 请求示例：base 登录完成回跳

```http
GET /agent?ticketST=st_login_001&state=st_login_page_001
```

### 4.4 请求示例：授权完成回跳

```http
GET /agent?token_result_ticket=trt_001&request_id=req_001&state=st_auth_001
Cookie: site_session_id=site_789
```

### 4.5 处理规则

#### 情况 A：已有本地站点登录态，且没有一次性票据

- 通过 `site_session_id` 找到本地 `site_session`。
- 直接返回业务页面或聊天页。

#### 情况 B：收到 `ticketST`

- 业务 Agent 后端调用 `POST /gw/auth/ticket/exchange`。
- 换取用户信息。
- 创建本地 `site_session`。
- 设置 `site_session_id` Cookie。
- 302 到业务页面，并清理 URL 中的一次性票据。

#### 情况 C：收到 `token_result_ticket`

- 业务 Agent 后端调用 `POST /gw/token/result/exchange`。
- 换取 `TR` 和 `agency_user`。
- 校验 `TR.agency_user` 与当前 `site_session` 用户一致。
- 写入本地 `tr_cache`。
- 前端根据 `state` 恢复原消息并继续调用 `/chat/send`。

#### 情况 D：没有本地站点登录态，也没有 `ticketST`

- 302 跳转到 Agent 网关登录入口：
  - `/gw/auth/login?agent_id=...&return_url=...&state=...`

### 4.6 响应示例：创建本地站点会话

```http
HTTP/1.1 302 Found
Set-Cookie: site_session_id=site_789; Path=/; HttpOnly; SameSite=Lax
Location: /agent.html?state=st_login_page_001
```

## 5. `GET /agent/session`

### 5.1 用途

前端页面在加载时主动检查当前站点会话是否仍然有效。

### 5.2 请求示例

```http
GET /agent/session
Cookie: site_session_id=site_789
```

### 5.3 成功响应示例

```json
{
  "userId": "z01062668",
  "username": "张三"
}
```

### 5.4 未登录响应示例

```http
HTTP/1.1 401 Unauthorized
```

## 6. `POST /agent/logout`

### 6.1 用途

- 清理业务 Agent 本地 `site_session`。
- 清理业务 Agent 本地 `TR` 缓存。
- 清理浏览器上的 `site_session_id`。

是否同时退出 IDaaS，由业务产品决定。当前主流程只要求退出业务 Agent 本地登录态。

### 6.2 请求示例

```http
POST /agent/logout
Cookie: site_session_id=site_789
```

### 6.3 响应示例

```http
HTTP/1.1 204 No Content
Set-Cookie: site_session_id=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax
```

## 7. `POST /chat/send`

### 7.1 用途

处理用户消息，并按需：

1. 判断需要哪些 `MCP tool`。
2. 判断本地 `TR` 是否可复用。
3. 必要时向 Agent 网关申请 `TR`。
4. 带 `TR` 调用 `MCP 网关`。

### 7.2 请求示例

```json
{
  "message": "帮我看一下这个合同"
}
```

### 7.3 处理规则

#### 情况 A：本地 `TR` 可复用

- 直接带 `TR` 调用 `MCP 网关`。
- 返回答案。

#### 情况 B：需要先去网关补充授权

- 调 Agent 网关：
  - `POST /gw/token/resource-token`
- 如果网关返回 `redirect_url`：
  - 前端把当前消息保存到 `sessionStorage`。
  - 浏览器跳转到 `redirect_url`。

#### 情况 C：授权完成后回到业务 Agent

- 前端从 `sessionStorage` 读取原消息。
- 后端已用 `token_result_ticket` 换取 `TR` 并写入 `tr_cache`。
- 再次调用 `/chat/send` 时命中本地 `TR`。

### 7.4 成功响应示例

```json
{
  "status": "answer",
  "source": "gateway",
  "answer": "已完成授权并成功查询到合同摘要。合同编号 HT-2026-001，当前状态为审批中。"
}
```

### 7.5 跳转响应示例

```json
{
  "status": "redirect",
  "redirectUrl": "https://agent-gateway.huawei.com/gw/auth/authorize?request_id=req_001",
  "state": "st_auth_001"
}
```

### 7.6 无权限响应示例

```json
{
  "code": "REQUEST_FAILED",
  "message": "当前用户无权使用该 Agent 的此项功能：ERP 合同的可读权限"
}
```
