# 业务授权与 token_result_ticket

## 1. 目标

让业务 Agent 在本地没有可复用 `TR` 时，通过 Agent 网关完成用户授权和 `TR` 获取，同时避免 `TR` 出现在浏览器 URL 中。

## 2. 流程

```mermaid
sequenceDiagram
    participant 用户 as 用户浏览器
    participant Agent as 业务 Agent
    participant GW as Agent 网关
    participant PC as 策略中心
    participant IDaaS as IDaaS
    participant IAM as IAM

    用户->>Agent: POST /chat/send
    Agent->>Agent: 判断 required_tools，检查 tr_cache
    Agent->>GW: POST /gw/token/resource-token
    GW->>PC: resolve-by-tools(required_tools)
    PC-->>GW: requiredPermissionPointCodes
    GW->>GW: 创建 pending_auth_transaction
    GW-->>Agent: redirect_url + request_id
    Agent-->>用户: 跳转 redirect_url
    用户->>GW: GET /gw/auth/authorize?request_id
    GW-->>用户: 302 IDaaS /authorize
    用户->>IDaaS: GET /oauth2/authorize
    IDaaS-->>用户: 首次非 base 授权通常展示授权页
    用户->>IDaaS: 勾选并确认授权
    IDaaS-->>用户: 302 /gw/auth/consent/callback?code&state
    用户->>GW: GET /gw/auth/consent/callback?code&state
    GW->>IDaaS: code 换 Tc
    GW->>IAM: 申请 T1
    GW->>IAM: 申请 TR
    GW->>GW: 生成 token_result_ticket
    GW-->>用户: 302 return_url?token_result_ticket&request_id&state
    用户->>Agent: GET /agent?token_result_ticket&request_id&state
    Agent->>GW: POST /gw/token/result/exchange
    GW-->>Agent: 返回 TR
    Agent->>Agent: 校验用户一致，写入 tr_cache
```

## 3. 关键数据

`pending_auth_transaction`：

```text
request_id -> agent_id, required_tools, requiredPermissionPointCodes, return_url, outer_state, subject_hint, expires_at
```

`token_result_ticket`：

```text
token_result_ticket -> request_id, agent_id, tr, agency_user, consented_scopes, used, expires_at
```

## 4. 关键约束

- `POST /gw/token/resource-token` 不接收长期网关登录凭证。
- `POST /gw/token/resource-token` 不直接返回 `TR`。
- `subject_hint` 只作为 IDaaS 登录提示。
- `token_result_ticket` 单次使用、短 TTL、绑定 `agent_id` 和 `request_id`。
- 业务 Agent 换回 `TR` 后，必须校验 `TR.agency_user` 与当前 `site_session` 用户一致。
- `TR` 复用由业务 Agent 本地 `tr_cache` 决定。
