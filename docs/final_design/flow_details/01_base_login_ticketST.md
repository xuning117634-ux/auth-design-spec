# base 登录与 ticketST

## 1. 目标

让业务 Agent 在不直连 IDaaS、不在浏览器 URL 暴露用户信息的前提下，拿到可信登录用户信息并创建自己的 `site_session`。

## 2. 流程

```mermaid
sequenceDiagram
    participant 用户 as 用户浏览器
    participant Agent as 业务 Agent
    participant GW as Agent 网关
    participant IDaaS as IDaaS

    用户->>Agent: GET /agent
    Agent->>Agent: 检查 site_session
    Agent-->>用户: 302 /gw/auth/login
    用户->>GW: GET /gw/auth/login
    GW->>GW: 创建 pending_base_login
    GW-->>用户: 302 IDaaS /authorize?scope=base
    用户->>IDaaS: GET /oauth2/authorize
    IDaaS-->>用户: 展示登录页，或已有登录态则快速回调
    用户->>IDaaS: 提交登录信息（如需要）
    IDaaS-->>用户: 302 /gw/auth/base/callback?code&state
    用户->>GW: GET /gw/auth/base/callback?code&state
    GW->>IDaaS: POST /oauth2/token
    IDaaS-->>GW: 返回用户信息
    GW->>GW: 生成 ticketST
    GW-->>用户: 302 return_url?ticketST&state
    用户->>Agent: GET /agent?ticketST&state
    Agent->>GW: POST /gw/auth/ticket/exchange
    GW-->>Agent: 返回用户信息
    Agent->>Agent: 创建 site_session
```

## 3. 关键数据

`pending_base_login`：

```text
gw_state -> agent_id, return_url, outer_state, expires_at
```

`ticketST`：

```text
ticketST -> agent_id, user_info, used, expires_at
```

## 4. 关键约束

- `ticketST` 单次使用。
- `ticketST` 短 TTL。
- `ticketST` 绑定 `agent_id`。
- 回跳业务 Agent 时只带 `ticketST + state`。
- 用户信息只通过后端交换接口返回，不进入浏览器 URL。
- 业务 Agent 成功交换后创建自己的 `site_session`。
