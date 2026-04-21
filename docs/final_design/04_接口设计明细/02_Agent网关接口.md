# Agent 网关接口

## 1. 模块职责

Agent 网关负责：

- 统一处理浏览器登录/授权跳转
- 统一处理 OAuth2 callback
- 统一编排 `code -> Tc -> T1 -> TR`
- 把 `required_tools` 翻译成 `requiredPermissionPointCodes`
- 维护 `gw_session`、`gw_auth_context`、临时授权事务

## 2. 接口分类

| 分类 | 接口 |
| --- | --- |
| 浏览器登录入口 | `GET /gw/auth/login` |
| base 登录回调 | `GET /gw/auth/base/callback` |
| 业务 Agent 申请 `TR` | `POST /gw/token/resource-token` |
| 浏览器业务授权入口 | `GET /gw/auth/authorize` |
| 业务授权回调 | `GET /gw/auth/consent/callback` |

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

1. 校验 `agent_id`
2. 校验 `return_url` 是否满足 `allowed_return_hosts`
3. 创建 `pending_base_login`
4. 302 跳转到 IDaaS `/authorize?scope=base`

### 3.5 响应示例

```http
HTTP/1.1 302 Found
Location: https://idaas.huawei.com/oauth2/authorize?response_type=code&client_id=gw_client_001&redirect_uri=https%3A%2F%2Fagent-gateway.huawei.com%2Fgw%2Fauth%2Fbase%2Fcallback&scope=base&state=gwst_001
```

## 4. `GET /gw/auth/base/callback`

### 4.1 用途

承接 IDaaS base 登录成功回调。

### 4.2 请求示例

```http
GET /gw/auth/base/callback?code=code_base_001&state=gwst_001
```

### 4.3 处理规则

1. 通过 `state` 找到 `pending_base_login`
2. 用 `code` 调 IDaaS `/oauth2/token`
3. 创建网关侧 `gw_session`
4. 生成 `gw_session_token`
5. 302 回业务 Agent 的 `return_url`

### 4.4 响应示例

```http
HTTP/1.1 302 Found
Set-Cookie: gw_session_id=gws_123; Path=/; HttpOnly; SameSite=Lax
Location: https://business-agent.huawei.com/agent?gw_session_token=gwst_abc_xyz&user_id=z01062668&username=%E5%BC%A0%E4%B8%89&state=st_login_001
```

### 4.5 说明

- `gw_session_id`：网关自己的会话 Cookie
- `gw_session_token`：发给业务 Agent 的不透明桥接凭证

## 5. `POST /gw/token/resource-token`

### 5.1 用途

业务 Agent 向网关申请当前请求所需 `TR`。

### 5.2 请求头示例

```http
POST /gw/token/resource-token
Authorization: Bearer gwst_abc_xyz
Content-Type: application/json
```

### 5.3 请求体示例

```json
{
  "agent_id": "agt_business_001",
  "required_tools": [
    "mcp:contract-server/get_contract"
  ],
  "return_url": "https://business-agent.huawei.com/agent.html",
  "state": "st_auth_001"
}
```

### 5.4 请求字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `agent_id` | 是 | 当前业务 Agent 标识 |
| `required_tools` | 是 | 本次请求需要访问的工具集合 |
| `return_url` | 是 | 如需浏览器补充授权，完成后回到哪里 |
| `state` | 是 | 业务 Agent 自己用于恢复前端上下文的状态值 |

### 5.5 处理规则

1. 通过 `gw_session_token` 找回网关侧 `gw_session`
2. 校验 `agent_id`
3. 校验 `return_url`
4. 调策略中心：
   - `POST /internal/v1/permission-points/resolve-by-tools`
5. 得到 `requiredPermissionPointCodes`
6. 检查当前 `gw_auth_context` 是否已有有效 `TR`

补充约束：

- `resolve-by-tools` 的请求体只包含 `requiredTools`
- 该接口不带 `agentId`
- 工具与权限点的映射关系不带 `agent` 维度

### 5.6 成功响应示例：直接返回 `TR`

```json
{
  "status": "TOKEN_READY",
  "access_token": "eyJhbGciOiJIUzI1NiJ9...<TR>...",
  "expires_in": 3600
}
```

### 5.7 成功响应示例：需要浏览器补充授权

```json
{
  "status": "REDIRECT_REQUIRED",
  "request_id": "req_001",
  "redirect_url": "https://agent-gateway.huawei.com/gw/auth/authorize?request_id=req_001"
}
```

### 5.8 失败响应示例

```json
{
  "code": "REQUEST_FAILED",
  "message": "当前 required_tools 无法解析成合法权限点"
}
```

## 6. `GET /gw/auth/authorize`

### 6.1 用途

浏览器根据 `redirect_url` 进入网关，正式开始本次业务授权。

### 6.2 请求示例

```http
GET /gw/auth/authorize?request_id=req_001
Cookie: gw_session_id=gws_123
```

### 6.3 处理规则

1. 通过 `request_id` 找到 `pending_auth_transaction`
2. 找到本次缺失的 `requiredPermissionPointCodes`
3. 302 跳转到 IDaaS `/authorize`

### 6.4 响应示例

```http
HTTP/1.1 302 Found
Location: https://idaas.huawei.com/oauth2/authorize?response_type=code&client_id=gw_client_001&redirect_uri=https%3A%2F%2Fagent-gateway.huawei.com%2Fgw%2Fauth%2Fconsent%2Fcallback&scope=erp%3Acontract%3Ar&state=gwst_auth_001
```

## 7. `GET /gw/auth/consent/callback`

### 7.1 用途

承接 IDaaS 业务授权回调，完成 `Tc / T1 / TR` 编排。

### 7.2 请求示例

```http
GET /gw/auth/consent/callback?code=code_consent_001&state=gwst_auth_001
```

### 7.3 处理规则

1. 用 `state` 找到 `request_id`
2. 用 `code` 向 IDaaS 换取 `Tc`
3. 向 IAM 申请 `T1`
4. 用 `Tc + T1` 申请 `TR`
5. 更新 `gw_auth_context`
6. 302 回 `return_url`

### 7.4 响应示例

```http
HTTP/1.1 302 Found
Location: https://business-agent.huawei.com/agent.html?state=st_auth_001
```

### 7.5 说明

授权完成回到业务 Agent 页面后：

- 业务 Agent 通过前端 `state` 恢复原消息
- 再次调用 `POST /gw/token/resource-token`
- 此时网关直接返回可用 `TR`
