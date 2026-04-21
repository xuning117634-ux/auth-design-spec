# MCP 网关接口

## 1. 模块职责

MCP 网关负责：

1. 接收业务 Agent 带来的 `TR`
2. 按当前待调用工具反查所需权限点
3. 校验这些权限点是否已经在 `TR` 中
4. 查询这些权限点对应的 Agent 策略
5. 再按 `TR` 中权限点反查工具集合
6. 校验当前请求工具是否包含在该工具集合中
7. 路由到具体 `MCP 服务`

## 2. 对外接口清单

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/mcp/v1/tools/invoke` | 调用指定 MCP 工具 |

## 3. `POST /mcp/v1/tools/invoke`

### 3.1 请求头示例

```http
POST /mcp/v1/tools/invoke
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...<TR>...
Content-Type: application/json
```

### 3.2 请求体示例

```json
{
  "agent_id": "agt_business_001",
  "tool_id": "mcp:contract-server/get_contract",
  "arguments": {
    "contract_no": "HT-2026-001"
  }
}
```

### 3.3 处理规则

1. 解析 `TR`
2. 读取：
   - `agent_id`
   - `agency_user.user_id`
   - `authorizedPermissionPoints`
3. 调策略中心 `resolve-by-tools`
4. 得到当前 `tool_id` 所需的 `permissionPointCodes`
5. 校验这些权限点是否都已经包含在 `TR.authorizedPermissionPoints[].code` 中
6. 查询当前 `agent_id + permissionPointCodes` 的 Agent 策略
7. 执行判定：
   - `DENY` 优先
   - 无策略默认允许
   - 有 `PERMIT` 时必须命中至少一条
8. 再以 `TR.authorizedPermissionPoints[].code` 调策略中心 `resolve-by-codes`
9. 拿到 `TR` 对应的工具集合
10. 校验当前 `tool_id` 是否包含在该工具集合中
11. 判定通过后，路由到具体 `MCP 服务`

### 3.4 成功响应示例

```json
{
  "status": "SUCCESS",
  "tool_id": "mcp:contract-server/get_contract",
  "output": {
    "contractNo": "HT-2026-001",
    "summary": "该合同当前状态为审批中，签约主体为华为技术有限公司。"
  }
}
```

### 3.5 失败响应示例：`TR` 范围不足

```json
{
  "code": "FORBIDDEN",
  "message": "当前请求缺少所需的用户授权：ERP 合同的可读权限"
}
```

### 3.6 失败响应示例：命中 Agent 策略拒绝

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无权使用该 Agent 的此项功能：ERP 合同的可读权限"
}
```

## 4. 与策略中心的协作

MCP 网关运行时至少会调用以下接口：

- `POST /internal/v1/permission-points/resolve-by-tools`
  - 用于把当前待调用工具反查成所需权限点
- `POST /internal/v1/agent-strategies/query`
  - 用于获取当前 Agent 对当前工具所需权限点的策略
- `POST /internal/v1/permission-points/resolve-by-codes`
  - 用于把 `TR` 中权限点反查成工具集合

## 5. 当前版本结论

- MCP 网关运行时先按当前待调用工具反查所需权限点
- 再对这些权限点做 Agent 策略判断
- 然后再按 `TR` 中权限点反查工具集合
- 只要当前请求工具不在该工具集合内，当前请求直接失败
