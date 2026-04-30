# 业务 Agent 接入开发指南

本文面向业务 Agent 接入方开发者，说明业务 Agent 如何接入 Agent 网关、申请资源令牌 `TR`，并携带 `TR` 调用 MCP 网关。

业务 Agent 有两种接入模式：

- **完整对接**：接入统一登录、授权、`TR` 获取、MCP 调用。
- **不对接登录系统**：业务 Agent 已有自己的登录态，只接入授权、`TR` 获取、MCP 调用。

## 1. 接入模式总览

| 模式 | 适用场景 | 需要接入 |
| --- | --- | --- |
| 完整对接 | 业务 Agent 没有自己的登录系统，或希望统一走 Agent 网关 + IDaaS 登录 | 登录接口、授权接口、`TR` 交换、MCP 调用 |
| 不对接登录系统 | 业务 Agent 已有自己的登录态，只需要使用统一授权和 MCP 权限校验 | 授权接口、`TR` 交换、MCP 调用 |

### 1.1 完整对接需要集成的接口

| 阶段 | 接口 | 调用方 | 是否必须 |
| --- | --- | --- | --- |
| 发起登录 | `GET /gw/auth/login` | 浏览器 | 是 |
| 登录结果交换 | `POST /gw/auth/ticket/exchange` | 业务 Agent 后端 | 是 |
| 申请 `TR` | `POST /gw/token/resource-token` | 业务 Agent 后端 | 是 |
| 授权跳转 | `GET /gw/auth/authorize?request_id=...` | 浏览器 | 是 |
| `TR` 结果交换 | `POST /gw/token/result/exchange` | 业务 Agent 后端 | 是 |
| 调用 MCP | MCP 网关工具调用接口 | 业务 Agent 后端 | 是 |

### 1.2 不对接登录系统需要集成的接口

| 阶段 | 接口 | 调用方 | 是否必须 |
| --- | --- | --- | --- |
| 申请 `TR` | `POST /gw/token/resource-token` | 业务 Agent 后端 | 是 |
| 授权跳转 | `GET /gw/auth/authorize?request_id=...` | 浏览器 | 是 |
| `TR` 结果交换 | `POST /gw/token/result/exchange` | 业务 Agent 后端 | 是 |
| 调用 MCP | MCP 网关工具调用接口 | 业务 Agent 后端 | 是 |

不对接登录系统时，不需要接入：

```text
GET /gw/auth/login
POST /gw/auth/ticket/exchange
```

但业务 Agent 必须自己维护可信的本地登录态，并在授权申请时传 `subject_hint`。

## 2. 接入前准备

业务 Agent 团队需要准备这些信息：

```text
agent_id: 2d513fbfee9b4cfe96722060bc7f1b9d
appId: bad1bc45aa10401f8618faa47d607a01
enterprise: 11111111111111111111111111111111
return_url: http://localhost:18082/agent
allowedReturnHosts:
  - localhost
  - your-agent.huawei.com
required_tools:
  - mcp:contract-server/get_contract
  - mcp:invoice-server/query_invoices
```

前置条件：

- Agent 已在管理面注册。
- `return_url` 所属域名已加入 `allowedReturnHosts`。
- MCP 网关已上报 tool 与权限点映射。
- Agent 网关与 IDaaS/IAM 的委托关系已由注册系统完成。
- 业务 Agent 后端可以访问 Agent 网关和 MCP 网关。

业务 Agent 不需要：

- 直连 IDaaS/IAM。
- 自己申请 `Tc/T1/TR`。
- 自己构造权限点 code。
- 在浏览器 URL 中接收用户信息或令牌。

## 3. 完整对接流程

完整对接适用于业务 Agent 没有自己的登录系统，或希望统一走 Agent 网关 + IDaaS 登录的场景。

### 3.1 首次进入业务 Agent

业务 Agent 检查本地是否有 `site_session`。

如果没有 `site_session`，浏览器跳转到 Agent 网关：

```http
GET https://agent-gateway.example.com/gw/auth/login?agent_id=2d513fbfee9b4cfe96722060bc7f1b9d&return_url=http%3A%2F%2Flocalhost%3A18082%2Fagent&state=st_login_001
```

参数说明：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent ID |
| `return_url` | 是 | 登录完成后回跳业务 Agent 的地址 |
| `state` | 是 | 业务 Agent 自己生成的状态值 |

Agent 网关会跳转到 IDaaS：

```http
HTTP/1.1 302 Found
Location: https://uniportal-dev.huawei.com/saaslogin1/oauth2/agent/authorize?response_type=code&client_id=Agent_xxx&agent_id=2d513fbfee9b4cfe96722060bc7f1b9d&scope=base.profile&redirect_uri=...
```

IDaaS 登录完成后，浏览器最终回到业务 Agent：

```http
GET http://localhost:18082/agent?ticketST=st_abc123&state=st_login_001
```

### 3.2 用 `ticketST` 换用户信息

业务 Agent 后端调用：

```http
POST https://agent-gateway.example.com/gw/auth/ticket/exchange
Content-Type: application/json
```

请求体：

```json
{
  "agent_id": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "ticketST": "st_abc123"
}
```

成功响应：

```json
{
  "user": {
    "userId": "Y30037812",
    "uuid": "uuid~eTMwMDM3ODEy",
    "username": "杨程义aa"
  },
  "expiresIn": 60
}
```

业务 Agent 处理要求：

- 用返回的用户信息创建自己的 `site_session`。
- 设置自己的登录 Cookie，例如 `site_session_id=site_xxx`。
- `ticketST` 只能使用一次，不能由前端直接交换。
- URL 中不要保留 `ticketST`，交换成功后应跳转到干净页面。

## 4. 不对接登录系统流程

如果业务 Agent 已经有自己的登录系统，可以跳过 Agent 网关 base 登录。

业务 Agent 自己负责：

- 维护本地登录态。
- 确认当前用户可信。
- 在申请 `TR` 时传入 `subject_hint`。
- 换回 `TR` 后校验 `TR` 用户与当前登录用户一致。

不对接登录系统时，接入流程从“申请 `TR`”开始。

## 5. 申请 TR

完整对接和不对接登录系统都需要调用该接口。

### 5.1 接口

```http
POST https://agent-gateway.example.com/gw/token/resource-token
Content-Type: application/json
```

### 5.2 完整对接模式请求体

```json
{
  "agent_id": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "required_tools": [
    "mcp:contract-server/get_contract"
  ],
  "return_url": "http://localhost:18082/agent",
  "state": "st_auth_001",
  "subject_hint": {
    "userId": "Y30037812"
  }
}
```

### 5.3 不对接登录系统请求体

```json
{
  "agent_id": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "required_tools": [
    "mcp:contract-server/get_contract",
    "mcp:invoice-server/query_invoices"
  ],
  "return_url": "https://your-agent.huawei.com/oauth/agent-callback",
  "state": "st_auth_20260430_001",
  "subject_hint": {
    "userId": "Y30037812"
  }
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent ID |
| `required_tools` | 是 | 本次业务请求需要使用的 MCP tool |
| `return_url` | 是 | 授权完成后回跳业务 Agent 的地址 |
| `state` | 是 | 业务 Agent 自己生成的状态值，用于恢复前端上下文 |
| `subject_hint` | 否 | 当前用户提示信息，只用于辅助 IDaaS 定位用户，不是可信身份来源 |

成功响应：

```json
{
  "status": "REDIRECT_REQUIRED",
  "request_id": "req_001",
  "redirect_url": "https://agent-gateway.example.com/gw/auth/authorize?request_id=req_001"
}
```

业务 Agent 处理要求：

- 前端保存当前用户问题或业务上下文到 `sessionStorage`。
- 浏览器跳转到 `redirect_url`。
- 不要把 `TR`、用户信息或内部令牌放到浏览器 URL。

## 6. 授权跳转

业务 Agent 收到 `redirect_url` 后，让浏览器跳转：

```http
GET https://agent-gateway.example.com/gw/auth/authorize?request_id=req_001
```

Agent 网关会跳转到 IDaaS 授权页：

```http
HTTP/1.1 302 Found
Location: https://uniportal-dev.huawei.com/saaslogin1/oauth2/agent/authorize?response_type=code&client_id=Agent_xxx&agent_id=2d513fbfee9b4cfe96722060bc7f1b9d&scope=erp%3Acontract%3Ar&redirect_uri=...
```

授权完成后，浏览器最终回到业务 Agent：

```http
GET http://localhost:18082/agent?token_result_ticket=trt_abc123&request_id=req_001&state=st_auth_001
```

业务 Agent 处理要求：

- 前端不要读取 `TR`。
- 后端用 `token_result_ticket` 换 `TR`。
- `state` 用于恢复授权前的用户上下文。

## 7. 用 `token_result_ticket` 换 TR

业务 Agent 后端调用：

```http
POST https://agent-gateway.example.com/gw/token/result/exchange
Content-Type: application/json
```

请求体：

```json
{
  "agent_id": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "request_id": "req_001",
  "token_result_ticket": "trt_abc123"
}
```

成功响应：

```json
{
  "status": "TOKEN_READY",
  "request_id": "req_001",
  "access_token": "eyJhbGciOiJSUzUxMiJ9...<TR>...",
  "expires_in": 86399,
  "agency_user": {
    "user_id": "Y30037812",
    "global_user_id": "Y30037812",
    "username": "杨程义aa"
  },
  "consented_scopes": [
    "erp:contract:r"
  ]
}
```

`TR` payload 关键字段示例：

```json
{
  "iss": "iam",
  "sub": "Agent_2d513fbfee9b4cfe96722060bc7f1b9d",
  "aud": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "scope": "erp:contract:r",
  "agency_user": {
    "idp": "idaas",
    "consented_scopes": [
      "erp:contract:r"
    ],
    "user": "{\"uid\":\"Y30037812\",\"uuid\":\"uuid~eTMwMDM3ODEy\",\"displayNameCn\":\"杨程义aa\",\"tenantId\":\"11111111111111111111111111111111\"}"
  }
}
```

业务 Agent 处理要求：

- 校验 `agency_user.user_id` 或 `TR.agency_user.user.uid` 与当前 `site_session` 用户一致。
- 将 `access_token` 作为 `TR` 存入本地 `tr_cache`。
- 使用 `expires_in` 或 `TR` 的 `exp` 控制缓存有效期。
- `consented_scopes` 是权限点 code 字符串数组。
- `scope` 是预留字段，不作为运行时鉴权依据。

## 8. 调用 MCP 网关

业务 Agent 后端调用 MCP 网关时，应携带当前 `TR`。

请求示例：

```json
{
  "agent_id": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "tool_id": "mcp:contract-server/get_contract",
  "tr": "eyJhbGciOiJSUzUxMiJ9...<TR>...",
  "input": {
    "contractNo": "HT-2026-001"
  }
}
```

成功响应示例：

```json
{
  "status": "SUCCESS",
  "tool_id": "mcp:contract-server/get_contract",
  "data": {
    "contractNo": "HT-2026-001",
    "contractName": "ERP 合同演示数据",
    "status": "审批中"
  }
}
```

无权限响应示例：

```json
{
  "status": "FORBIDDEN",
  "code": "PERMISSION_DENIED",
  "message": "当前用户无权使用该 Agent 的此项功能"
}
```

MCP 网关运行时会校验：

- `TR.aud` 是否匹配当前业务 Agent。
- 当前工具所需权限点是否在 `TR.agency_user.consented_scopes` 中。
- 当前用户是否通过 Agent 策略。
- 当前工具是否属于 `TR` 权限点可反查出的工具集合。

## 9. 业务 Agent 本地状态建议

### 9.1 `site_session`

```json
{
  "siteSessionId": "site_001",
  "userId": "Y30037812",
  "username": "杨程义aa",
  "createdAt": "2026-04-30T10:00:00+08:00"
}
```

`site_session` 是业务 Agent 自己的登录态，Agent 网关不维护业务 Agent 的长期登录态。

### 9.2 `tr_cache`

```json
{
  "siteSessionId": "site_001",
  "agentId": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "currentTr": "eyJhbGciOiJSUzUxMiJ9...",
  "coveredTools": [
    "mcp:contract-server/get_contract"
  ],
  "coveredPermissionPointCodes": [
    "erp:contract:r"
  ],
  "expiresAt": "2026-05-01T10:00:00+08:00"
}
```

`TR` 是否复用由业务 Agent 自己判断。Agent 网关不保存业务 Agent 的历史 `TR` 覆盖范围。

## 10. 前端处理建议

授权前保存上下文：

```javascript
sessionStorage.setItem("pending:" + state, JSON.stringify({
  message: "帮我看一下 ERP 合同",
  createdAt: Date.now()
}));
window.location.href = redirectUrl;
```

授权回来恢复上下文：

```javascript
const params = new URLSearchParams(window.location.search);
const state = params.get("state");
const pending = sessionStorage.getItem("pending:" + state);

if (pending) {
  sessionStorage.removeItem("pending:" + state);
  const context = JSON.parse(pending);
  // 重新调用业务后端，例如 POST /chat/send
}
```

## 11. 退出登录说明

业务 Agent 退出登录至少要清理：

```text
site_session
site_session_id Cookie
tr_cache
sessionStorage 中未完成的授权上下文
```

注意：

- 清理业务 Agent Cookie 不等于退出 IDaaS。
- 如果 IDaaS Cookie 仍存在，下一次登录可能会静默完成。
- 如果要切换 IDaaS 用户，需要额外接入 IDaaS logout 或强制登录参数。

## 12. 常见问题

### 12.1 为什么不能把 `TR` 放到 URL？

`TR` 是资源访问令牌，放在 URL 中容易被浏览器历史、代理日志、服务端访问日志或监控系统记录。业务 Agent 必须通过后端接口交换并保存 `TR`。

### 12.2 为什么需要 `ticketST` 和 `token_result_ticket`？

它们是一次性取件凭据，用来避免浏览器 URL 直接暴露用户信息、`Tc` 或 `TR`。业务 Agent 后端拿到票据后，再向 Agent 网关交换真实结果。

### 12.3 退出业务 Agent 后为什么可能没有重新出现 IDaaS 登录页？

业务 Agent 退出只清理自己的 `site_session` 和 `TR` 缓存，不等于退出 IDaaS。只要 IDaaS Cookie 仍有效，下一次登录可能会静默完成。

### 12.4 不对接登录系统时，`subject_hint` 是可信身份吗？

不是。`subject_hint` 只用于辅助 IDaaS 定位用户。最终用户身份以 IDaaS/IAM 令牌中的用户信息为准，业务 Agent 换回 `TR` 后仍需校验用户一致性。

### 12.5 业务 Agent 是否需要理解权限点？

业务 Agent 不需要自己构造权限点 code。业务 Agent 只需要知道本次请求需要哪些 `required_tools`。Agent 网关会把 tool 解析为权限点并发起授权。

## 13. 联调验收清单

- 首次进入业务 Agent，无 `site_session` 时能跳转 Agent 网关登录。
- 登录完成后业务 Agent 能收到 `ticketST`，并后端换取用户信息。
- URL 中不出现用户信息、`Tc`、`TR`。
- 用户发起受保护工具请求时，业务 Agent 能提交 `required_tools`。
- 无 `TR` 时能收到 `redirect_url` 并跳转授权。
- 授权完成后业务 Agent 能用 `token_result_ticket` 换回 `TR`。
- `TR.agency_user.consented_scopes` 是字符串数组。
- 业务 Agent 能缓存 `TR`，并在二次同类请求中复用。
- MCP 网关返回无权限时，业务 Agent 能给用户明确提示。
- 退出登录后，业务 Agent 本地 `site_session` 和 `tr_cache` 被清理。

## 14. 接入方实现边界

业务 Agent 需要做：

- 维护自己的 `site_session`。
- 判断用户请求需要哪些 `required_tools`。
- 按需向 Agent 网关申请 `TR`。
- 处理一次性票据交换。
- 缓存和复用 `TR`。
- 带 `TR` 调用 MCP 网关。

业务 Agent 不需要做：

- 直连 IDaaS/IAM。
- 自己申请 `Tc/T1/TR`。
- 自己执行 Agent 策略判断。
- 自己维护工具与权限点映射。
- 在浏览器 URL 中接收或传递令牌。
