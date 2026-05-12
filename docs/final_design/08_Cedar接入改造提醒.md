# Cedar 接入改造提醒

本文不是当前版本的实施方案，而是给后续接入 Cedar 策略引擎时使用的改造提醒。当前阶段策略中心仍可以先用自研规则执行器，但数据模型、接口和管理页面应尽量保持 Cedar-friendly，避免未来重做。

## 1. 核心判断

如果未来要引入 Cedar，不建议让管理员直接写 Cedar 表达式。推荐路径是：

```text
管理页面表单
  -> 结构化 JSON IR
  -> 当前自研执行器
  -> 未来 Cedar 编译器
  -> Cedar Policy / Schema / Entities / Request
```

也就是说，真正稳定的不是 Cedar 文本本身，而是策略中心自己的结构化 JSON 中间层。

## 2. PARC 到 Cedar 的映射

策略中心通用权限模型采用 `Principal + Action + Resource + Context`。它与 Cedar 的授权请求模型天然接近。

| 策略中心字段 | Cedar 概念 | 示例 |
| --- | --- | --- |
| `principal` | principal | `User::"Y30037812"` |
| `action` | action | `Action::"permissionPoint.use"` |
| `resource` | resource | `PermissionPoint::"erp:contract:r"` |
| `context` | context | `{ "agentId": "contract_agent_001" }` |

运行时请求可以保持为：

```json
{
  "principal": {
    "type": "USER",
    "id": "Y30037812"
  },
  "action": "permissionPoint.use",
  "resource": {
    "type": "PERMISSION_POINT",
    "id": "erp:contract:r"
  },
  "context": {
    "agentId": "contract_agent_001",
    "enterprise": "11111111111111111111111111111111"
  }
}
```

未来 Cedar 执行器只需要把它转换成 Cedar request，不需要改变业务 Web、MCP 网关或管理页面的调用心智。

## 3. 建议保留的 JSON IR

建议把“默认裁决模式”和“策略规则”拆开保存。

### AuthzTarget

`AuthzTarget` 表达一个管控目标，也就是某个 Agent 场景下，对某个资源执行某个动作。

```json
{
  "targetId": "target_contract_agent_permission_erp_contract_r",
  "enterprise": "11111111111111111111111111111111",
  "action": "permissionPoint.use",
  "resource": {
    "type": "PERMISSION_POINT",
    "id": "erp:contract:r"
  },
  "context": {
    "agentId": "contract_agent_001"
  },
  "accessMode": "OPEN",
  "status": "ACTIVE"
}
```

`accessMode` 只表示未命中策略时的默认裁决：

- `OPEN`：未命中策略时默认允许。
- `RESTRICTED`：未命中策略时默认拒绝。

### AuthzPolicy

`AuthzPolicy` 表达某个管控目标下的一条黑名单或白名单规则。

```json
{
  "policyId": "policy_deny_y30037812_contract_r",
  "targetId": "target_contract_agent_permission_erp_contract_r",
  "principal": {
    "type": "USER",
    "id": "Y30037812"
  },
  "effect": "DENY",
  "condition": null,
  "status": "ACTIVE"
}
```

如果要表达“外包不能访问合同助手”，可以使用条件：

```json
{
  "policyId": "policy_deny_wx_contract_agent",
  "targetId": "target_contract_agent_access",
  "principal": {
    "type": "ANY"
  },
  "effect": "DENY",
  "condition": {
    "field": "principal.employeeType",
    "operator": "equals",
    "values": ["WX"]
  },
  "status": "ACTIVE"
}
```

`principal.employeeType` 不由业务方传入，应由策略中心通过 `UserProfileResolver` 查询人员画像后构造成内部属性。

## 4. Cedar 编译示例

### DENY 策略

上面的“用户 Y30037812 不允许在合同助手下使用 ERP 合同权限点”，未来可以编译成类似 Cedar forbid 策略：

```cedar
forbid(
  principal == User::"Y30037812",
  action == Action::"permissionPoint.use",
  resource == PermissionPoint::"erp:contract:r"
)
when {
  context.agentId == "contract_agent_001"
};
```

### PERMIT 策略

“财务组允许访问发票助手”可以编译成类似：

```cedar
permit(
  principal in UserGroup::"finance_group",
  action == Action::"agent.access",
  resource == Agent::"invoice_agent_001"
)
when {
  context.agentId == "invoice_agent_001"
};
```

这里的 `principal in UserGroup::"finance_group"` 依赖 Cedar entities 表达用户和用户组的关系。

## 5. OPEN / RESTRICTED 与 Cedar 默认拒绝

Cedar 自身是默认拒绝模型：没有命中 `permit` 时会拒绝。我们的 `OPEN` 模式表示“没有命中策略时默认允许”，因此未来接入 Cedar 时要特别处理。

推荐做法：

- `RESTRICTED`：不注入默认 `permit`，保持 Cedar 默认拒绝。
- `OPEN`：由策略中心在执行前注入一条虚拟默认 `permit`，但不把它作为管理员创建的真实策略保存。
- `DENY`：仍编译成 `forbid`，并保持优先级最高。

执行顺序可以理解为：

```text
1. 编译并加载真实 DENY / PERMIT 策略。
2. 如果目标是 OPEN，执行层追加虚拟默认 permit。
3. Cedar 评估请求。
4. 如果 forbid 命中，最终拒绝。
```

这样可以保留现有 `OPEN / RESTRICTED` 的产品语义，同时兼容 Cedar 的默认拒绝模型。

## 6. 建议的数据库预留字段

短期不一定要一次性建成通用表，但未来通用化时建议至少预留这些字段。

### 管控目标表

```text
pc_authz_target
- target_id
- enterprise
- action
- resource_type
- resource_id
- context_agent_id
- access_mode
- status
- created_at
- updated_at
```

### 策略规则表

```text
pc_authz_policy
- policy_id
- target_id
- effect
- principal_type
- principal_id
- condition_json
- status
- compiled_policy_text
- compiled_policy_version
- created_at
- updated_at
```

其中：

- `condition_json` 保存策略中心自己的条件结构。
- `compiled_policy_text` 可在未来保存编译后的 Cedar 文本，便于审计和灰度。
- `compiled_policy_version` 用于标记编译器版本，方便回滚和兼容。

## 7. 建议的执行器架构

未来可以把策略判断抽象成一个接口：

```text
PolicyEngine
  -> InternalPolicyEngine
  -> CedarPolicyEngine
```

配套组件：

```text
JsonPolicyCompiler：把策略中心 JSON IR 编译成 Cedar policy。
JsonRequestMapper：把 batch-check 请求转成 Cedar request。
JsonEntityBuilder：根据 UserProfileResolver、Agent、Skill、权限点关系构造 Cedar entities。
PolicyDecisionLogger：记录输入、命中策略、最终裁决和引擎版本。
```

这样可以做到：

- 当前阶段使用 `InternalPolicyEngine`。
- 灰度阶段同时执行 Internal 和 Cedar，只比较结果，不影响线上裁决。
- 稳定后通过配置切换到 `CedarPolicyEngine`。

## 8. 接入 Cedar 的分阶段路线

### 阶段 1：统一 JSON IR

先把管理面和接口统一到 `AuthzTarget + AuthzPolicy + PARC request`，由自研执行器执行。

### 阶段 2：只生成 Cedar 文本

新增编译器，把 JSON IR 编译成 Cedar 文本，但只用于日志、审计和人工检查，不参与线上裁决。

### 阶段 3：双引擎影子运行

线上请求同时跑自研执行器和 Cedar 执行器：

```text
真实结果：InternalPolicyEngine
影子结果：CedarPolicyEngine
```

如果两边结果不一致，记录差异，不影响用户请求。

### 阶段 4：切换 Cedar 执行

差异率稳定后，通过配置切换主执行器。保留自研执行器作为回退。

## 9. 风险提醒

- 不要把 Cedar 语法直接暴露给普通管理员，否则配置门槛会明显升高。
- 不要让业务 Web 或业务 Agent 传用户属性，用户画像应由策略中心内部解析。
- 不要把 MCP tool 作为管理员主要配置对象，短期仍通过权限点管理工具访问。
- 不要把 `OPEN` 编译成真实持久化策略，否则后续很难区分管理员创建的白名单和系统默认允许。
- Cedar 的实体关系很重要，用户组、角色、Agent、Skill、权限点之间的关系需要稳定构造。
- Cedar 默认拒绝，如果忘记为 `OPEN` 目标注入虚拟默认允许，会导致现有开放型资源被误拒绝。

## 10. 当前结论

短期最重要的不是马上接入 Cedar，而是先把策略中心的数据形态收敛成 Cedar-friendly：

- 对外判断请求使用 `Principal + Action + Resource + Context`。
- 管理面先选 Agent，自动带入 `context.agentId`。
- `OPEN / RESTRICTED` 是管控目标的默认裁决，不是单条策略字段。
- 策略规则只表达 `PERMIT / DENY`。
- 用户画像由策略中心内部查询。

只要这些口径稳定，未来从自研执行器切到 Cedar 执行器就是“增加编译和执行层”，而不是重做整个产品模型。
