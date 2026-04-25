# Agent 网关接口

## 1. 模块职责

Agent 网关负责：

- 统一处理浏览器登录/授权跳转。
- 统一处理 OAuth2 callback。
- 统一编排 `code -> Tc -> T1 -> TR`。
- 把 `required_tools` 翻译成 `requiredPermissionPointCodes`。
- 维护短期事务和一次性结果凭据。

Agent 网关不负责：

- 维护长期用户登录态。
- 判断业务 Agent 本地是否已有可复用 `TR`。
- 在浏览器 URL 中返回 `Tc`、`TR` 或用户信息。

## 2. 接口分类

| 分类 | 接口 |
| --- | --- |
| 浏览器登录入口 | `GET /gw/auth/login` |
| base 登录回调 | `GET /gw/auth/base/callback` |
| 登录结果交换 | `POST /gw/auth/ticket/exchange` |
| 业务 Agent 申请 `TR` | `POST /gw/token/resource-token` |
| 浏览器业务授权入口 | `GET /gw/auth/authorize` |
| 业务授权回调 | `GET /gw/auth/consent/callback` |
| 授权结果交换 | `POST /gw/token/result/exchange` |

## 3. `GET /gw/auth/login`

### 3.1 用途

浏览器发起 base 登录流程。

### 3.2 请求示例

```http
GET /gw/auth/login?agent_id=agt_business_001&return_url=https%3A%2F%2Fbusiness-agent.huawei.com%2Fagent&state=st_login_001
```

### 3.3 请求参数

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent 标识 |
| `return_url` | 是 | 登录完成后回到业务 Agent 的地址 |
| `state` | 是 | 业务 Agent 透传的外部状态值 |

### 3.4 处理规则

1. 校验 `agent_id`。
2. 校验 `return_url` 是否满足 `allowed_return_hosts`。
3. 创建 `pending_base_login`。
4. 302 跳转到 IDaaS `/authorize?scope=base`。

### 3.5 响应示例

```http
HTTP/1.1 302 Found
Location: https://idaas.huawei.com/oauth2/authorize?response_type=code&client_id=gw_client_001&redirect_uri=https%3A%2F%2Fagent-gateway.huawei.com%2Fgw%2Fauth%2Fbase%2Fcallback&scope=base&state=gwst_login_001
```

## 4. `GET /gw/auth/base/callback`

### 4.1 用途

承接 IDaaS base 登录成功回调，生成一次性 `ticketST`。

### 4.2 请求示例

```http
GET /gw/auth/base/callback?code=code_base_001&state=gwst_login_001
```

### 4.3 处理规则

1. 通过 `state` 找到 `pending_base_login`。
2. 用 `code` 调 IDaaS `/oauth2/token`。
3. 从 IDaaS 返回结果中取得可信用户信息。
4. 创建一次性 `ticketST`。
5. 删除或标记完成 `pending_base_login`。
6. 302 回业务 Agent 的 `return_url`。

### 4.4 响应示例

```http
HTTP/1.1 302 Found
Location: https://business-agent.huawei.com/agent?ticketST=st_login_001&state=st_login_001
```

### 4.5 说明

- `ticketST` 只是一张一次性取件票据。
- `ticketST` 不等同于用户登录态，也不等同于资源访问令牌。
- 用户信息不放入浏览器 URL。

## 5. `POST /gw/auth/ticket/exchange`

### 5.1 用途

业务 Agent 后端用 `ticketST` 换取基础登录用户信息。

### 5.2 请求示例

```http
POST /gw/auth/ticket/exchange
Content-Type: application/json
```

```json
{
  "agent_id": "agt_business_001",
  "ticketST": "st_login_001"
}
```

### 5.3 请求字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent 标识 |
| `ticketST` | 是 | base 登录回跳携带的一次性票据 |

### 5.4 处理规则

1. 校验 `ticketST` 存在、未过期、未使用。
2. 校验 `ticketST` 绑定的 `agent_id` 与请求一致。
3. 将 `ticketST` 标记为已使用。
4. 返回用户信息。

### 5.5 成功响应示例

```json
{
  "user": {
    "userId": "z01062668",
    "uuid": "uuid~1234556",
    "username": "张三"
  },
  "expiresIn": 60
}
```

### 5.6 失败响应示例

```json
{
  "code": "TICKET_INVALID",
  "message": "ticketST 不存在、已过期或已被使用"
}
```

## 6. `POST /gw/token/resource-token`

### 6.1 用途

业务 Agent 向网关申请当前请求所需 `TR`。该接口只创建授权事务并返回浏览器授权地址，不负责复用历史 `TR`。

### 6.2 请求示例

```http
POST /gw/token/resource-token
Content-Type: application/json
```

```json
{
  "agent_id": "agt_business_001",
  "required_tools": [
    "mcp:contract-server/get_contract"
  ],
  "return_url": "https://business-agent.huawei.com/agent",
  "state": "st_auth_001",
  "subject_hint": {
    "userId": "z01062668"
  }
}
```

### 6.3 请求字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent 标识 |
| `required_tools` | 是 | 本次请求需要访问的工具集合 |
| `return_url` | 是 | 授权完成后回到业务 Agent 的地址 |
| `state` | 是 | 业务 Agent 用于恢复前端上下文的状态值 |
| `subject_hint` | 否 | 当前业务站点用户提示，只能传给 IDaaS 辅助定位用户，不作为可信身份 |

### 6.4 处理规则

1. 校验 `agent_id`。
2. 校验 `return_url` 是否满足 `allowed_return_hosts`。
3. 调策略中心 `POST /internal/v1/permission-points/resolve-by-tools`。
4. 得到 `requiredPermissionPointCodes`。
5. 创建 `pending_auth_transaction`。
6. 返回 `redirect_url + request_id`。

补充约束：

- `resolve-by-tools` 的请求体只包含 `requiredTools`。
- 该接口不带 `agentId`。
- 工具与权限点的映射关系不带 `agent` 维度。
- 该接口不接收长期网关登录凭证。
- 该接口不直接返回 `TR`。

### 6.5 成功响应示例

```json
{
  "status": "REDIRECT_REQUIRED",
  "request_id": "req_001",
  "redirect_url": "https://agent-gateway.huawei.com/gw/auth/authorize?request_id=req_001"
}
```

### 6.6 失败响应示例

```json
{
  "code": "REQUEST_FAILED",
  "message": "当前 required_tools 无法解析成合法权限点"
}
```

## 7. `GET /gw/auth/authorize`

### 7.1 用途

浏览器根据 `redirect_url` 进入网关，正式开始本次业务授权。

### 7.2 请求示例

```http
GET /gw/auth/authorize?request_id=req_001
```

### 7.3 处理规则

1. 通过 `request_id` 找到 `pending_auth_transaction`。
2. 找到本次需要申请的 `requiredPermissionPointCodes`。
3. 将 `subject_hint` 作为 `login_hint` 或等价参数传给 IDaaS。
4. 创建内部 `gw_state -> request_id` 映射。
5. 302 跳转到 IDaaS `/authorize`。
6. 浏览器实际访问 IDaaS 授权地址。
7. 首次非 base 授权通常由 IDaaS 展示授权页，用户确认后再 302 回网关 callback。

### 7.4 响应示例

```http
HTTP/1.1 302 Found
Location: https://idaas.huawei.com/oauth2/authorize?response_type=code&client_id=gw_client_001&redirect_uri=https%3A%2F%2Fagent-gateway.huawei.com%2Fgw%2Fauth%2Fconsent%2Fcallback&scope=erp%3Acontract%3Ar&state=gwst_auth_001&login_hint=z01062668
```

## 8. `GET /gw/auth/consent/callback`

### 8.1 用途

承接 IDaaS 业务授权回调，完成 `Tc / T1 / TR` 编排，并生成 `token_result_ticket`。

### 8.2 请求示例

```http
GET /gw/auth/consent/callback?code=code_consent_001&state=gwst_auth_001
```

### 8.3 处理规则

1. 用 `state` 找到 `request_id`。
2. 用 `request_id` 找到 `pending_auth_transaction`。
3. 用 `code` 向 IDaaS 换取 `Tc`。
4. 向 IAM 申请 `T1`。
5. 用 `Tc + T1` 申请 `TR`。
6. 创建一次性 `token_result_ticket`。
7. 删除或标记完成 `pending_auth_transaction`。
8. 302 回业务 Agent 的 `return_url`。

### 8.4 响应示例

```http
HTTP/1.1 302 Found
Location: https://business-agent.huawei.com/agent?token_result_ticket=trt_001&request_id=req_001&state=st_auth_001
```

## 9. `POST /gw/token/result/exchange`

### 9.1 用途

业务 Agent 后端用 `token_result_ticket` 换取授权完成后的 `TR`。

### 9.2 请求示例

```http
POST /gw/token/result/exchange
Content-Type: application/json
```

```json
{
  "agent_id": "agt_business_001",
  "request_id": "req_001",
  "token_result_ticket": "trt_001"
}
```

### 9.3 请求字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent 标识 |
| `request_id` | 是 | 本次授权事务号 |
| `token_result_ticket` | 是 | 授权完成回跳携带的一次性取件凭据 |

### 9.4 处理规则

1. 校验 `token_result_ticket` 存在、未过期、未使用。
2. 校验 `token_result_ticket` 绑定的 `request_id` 与请求一致。
3. 校验 `token_result_ticket` 绑定的 `agent_id` 与请求一致。
4. 将 `token_result_ticket` 标记为已使用。
5. 返回 `TR`、过期时间、授权用户信息和权限点。

### 9.5 成功响应示例

```json
{
  "status": "TOKEN_READY",
  "request_id": "req_001",
  "access_token": "eyJhbGciOiJSUzUxMiJ9...<TR>...",
  "expires_in": 86399,
  "agency_user": {
    "user_id": "uuid~1234556",
    "global_user_id": "z01062668"
  },
  "consented_scopes": [
    {
      "code": "erp:contract:r",
      "displayNameZh": "ERP 合同的可读权限"
    }
  ]
}
```

### 9.6 失败响应示例

```json
{
  "code": "TOKEN_RESULT_TICKET_INVALID",
  "message": "token_result_ticket 不存在、已过期或已被使用"
}
```

## 10. 一次性凭据统一约束

- `ticketST` 和 `token_result_ticket` 都必须单次使用。
- TTL 建议 1 到 5 分钟。
- 票据必须绑定 `agent_id`。
- 票据必须绑定原始 `return_url` 所属白名单。
- 交换接口必须由业务 Agent 后端调用，不允许前端直接读取结果。
- 业务 Agent 换回 `TR` 后，必须校验 `TR.agency_user` 与当前 `site_session` 用户一致。
