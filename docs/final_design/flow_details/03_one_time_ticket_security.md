# 一次性凭据安全约束

## 1. 为什么需要一次性凭据

浏览器回跳 URL 会进入浏览器历史、代理日志、前端异常日志和 Referer 风险范围，因此不能把 `Tc`、`TR`、用户信息放在 URL 中。一次性凭据只是一张短期取件票，真正的用户信息和 `TR` 只通过业务 Agent 后端与 Agent 网关之间的服务端接口交换。

## 2. ticketST

`ticketST` 用于 base 登录结果交付：

```text
ticketST -> agent_id, authorization_code, client_id, redirect_uri, used, expires_at
```

`ticketST` 实际绑定的是 base callback 带回的 `authorization code`，用户信息在交换接口里再获取。

安全要求：

- 只允许交换一次。
- 过期后不可交换。
- 必须绑定 `agent_id`。
- 必须绑定原始 `return_url` 所属白名单。
- 交换成功后立即标记为 used。
- 不允许返回 `Tc` 或 `TR`。
- `/gw/auth/base/callback` 不立即用 `code` 换用户信息。
- `/gw/auth/ticket/exchange` 才执行 `code + client_id + client_secret + 可选 redirect_uri -> Tc access_token -> IDaaS 用户信息`。
- 获取用户信息接口只传 `access_token`。

## 3. token_result_ticket

`token_result_ticket` 用于授权结果交付：

```text
token_result_ticket -> request_id, agent_id, tr, agency_user, consented_scopes, used, expires_at
```

安全要求：

- 只允许交换一次。
- 过期后不可交换。
- 必须绑定 `agent_id`。
- 必须绑定 `request_id`。
- 交换成功后立即标记为 used。
- 业务 Agent 必须校验 `TR.agency_user` 与当前 `site_session` 用户一致。

## 4. 不可信输入

业务 Agent 传给网关的 `subject_hint`、浏览器 URL 中的 `state`、前端恢复消息都不是可信身份来源。

可信身份来源只有：

- base 登录阶段：IDaaS `code -> token` 返回的用户信息。
- 业务授权阶段：`Tc` 和 `TR.agency_user` 中的用户信息。

## 5. 失败处理

- 票据不存在：返回 `TICKET_INVALID`。
- 票据过期：返回 `TICKET_EXPIRED`。
- 票据已使用：返回 `TICKET_USED`。
- `agent_id` 不匹配：返回 `AGENT_MISMATCH`。
- `request_id` 不匹配：返回 `REQUEST_MISMATCH`。
- 换回的用户与当前 `site_session` 不一致：业务 Agent 清理本地会话并重新走登录。
