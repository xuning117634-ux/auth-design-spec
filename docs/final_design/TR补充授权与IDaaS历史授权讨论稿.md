# TR 补充授权与 IDaaS 历史授权讨论稿

本文是跨部门讨论稿，用于对齐 TR 补充授权、IDaaS 历史授权记录和本地 TR 缓存的建议方案。本文不作为最终正式口径，讨论确认后再同步正式设计文档和代码实现。

## 1. 核心结论

- `TR` 仍然是短期资源访问令牌，不设计成长期令牌。
- IDaaS 应保存“历史授权账本”，但不保存 `TR`。
- 补充授权时，建议申请“当前有效授权范围 + 本次新增所需范围”的并集。
- 用户授权页只需要突出展示新增权限点，但最终返回的 `Tc.consented_scopes` 应覆盖本次完整可用范围。
- 业务 Agent 本地仍只缓存一个当前最佳 `TR`，新 `TR` 校验通过后整体替换旧 `TR`。

用一个例子说明：

```text
当前有效 TR 覆盖权限点：1、2、3
本次调用 MCP 工具需要权限点：3、4、5
本次建议申请目标范围：1、2、3、4、5
IDaaS 页面重点提示新增权限点：4、5
最终返回的 Tc/TR 覆盖范围：1、2、3、4、5
```

这里的“全量”不是 Agent 订阅的全部权限点，而是“当前有效 `TR` 已覆盖范围”和“本次调用所需范围”的并集。

## 2. 为什么这样设计

### 2.1 避免增量 TR 覆盖旧能力

如果当前 `TR` 覆盖 `1、2、3`，本次只申请增量 `4、5`，那么新 `TR` 只覆盖 `4、5`。业务 Agent 如果用新 `TR` 替换旧 `TR`，就会丢掉 `1、2、3` 的能力。

这会导致一个很别扭的问题：用户刚刚授权过的能力，在补充授权后反而不可用了。

### 2.2 避免多 TR 缓存复杂化

另一种做法是旧 `TR` 保留 `1、2、3`，新 `TR` 只保存 `4、5`，业务 Agent 本地缓存多个 `TR`。

这种方案会带来明显复杂度：

- MCP 调用时要判断该带哪个 `TR`。
- 一个请求如果同时需要 `3、4、5`，可能需要合并多个 `TR` 的授权范围。
- MCP 网关当前运行时模型是校验单个 `TR`，多 `TR` 会改变调用和鉴权模型。
- 多 `TR` 的过期、撤销、刷新和调试成本更高。

因此，当前阶段更建议保持“一个会话下一个当前最佳 `TR`”。

### 2.3 符合当前 MCP 运行时模型

当前 MCP 网关运行时只信当前请求携带的一个短期 `TR`，并基于这个 `TR` 做三类判断：

- 当前工具所需权限点是否在 `TR.agency_user.consented_scopes` 中。
- 当前用户是否通过这些权限点对应的 Agent 策略。
- 当前工具是否属于 `TR` 权限点可反查出的工具集合。

如果新 `TR` 覆盖完整并集，MCP 网关不需要理解历史授权、不需要合并多个令牌，也不需要改运行时鉴权模型。

### 2.4 IDaaS 最适合保存历史授权账本

历史授权本质上是用户对某个 Agent 使用某些权限点的长期同意记录。它不应该放在 Agent 网关，也不应该放在业务 Agent。

更合理的位置是 IDaaS：

- IDaaS 能识别当前登录用户。
- IDaaS 负责展示授权页和收集用户确认。
- IDaaS 能判断哪些权限点历史已经授权，哪些是本次新增。
- IDaaS 能提供统一的用户授权管理和撤销入口。

这样可以让 Agent 网关继续保持轻量：只负责跳转编排、`code -> Tc -> T1 -> TR` 编排和一次性票据交付。

### 2.5 长期授权账本和短期 TR 分离

长期授权记录用于改善用户体验，表示用户曾经同意某个 Agent 使用某些权限点。

`TR` 用于运行时访问资源，仍然必须是短期令牌。

二者分离后，可以同时满足两个目标：

- 用户体验：已授权过的权限点不反复打扰用户。
- 安全边界：真正访问资源时仍使用短期 `TR`，过期后必须重新申请。

## 3. TR 补充授权建议流程

### 3.1 业务 Agent 本地判断

业务 Agent 本地维护 `tr_cache`，缓存维度建议保持：

```text
site_session_id + agent_id
```

缓存内容至少包括：

```text
current_tr
covered_tools
covered_permission_point_codes
expires_at
```

当用户请求调用 MCP 工具时：

- 如果当前 `TR` 未过期，且 `covered_tools` 覆盖本次 `required_tools`，直接复用当前 `TR`。
- 如果当前 `TR` 过期，则不再复用旧授权范围，只按本次 `required_tools` 重新申请。
- 如果当前 `TR` 未过期但覆盖不足，则按并集申请补充授权。

### 3.2 并集申请口径

当前有效 `TR` 覆盖 `1、2、3`，本次需要 `3、4、5` 时，业务 Agent 应计算：

```text
desired_scope = current_valid_tr_scope ∪ current_required_scope
```

如果业务 Agent 不直接理解权限点 code，也可以在工具维度做并集：

```text
desired_tools = cached.covered_tools ∪ current_required_tools
```

然后将 `desired_tools` 传给 Agent 网关，由 Agent 网关继续负责解析成权限点集合。

### 3.3 IDaaS 授权页展示

Agent 网关跳转 IDaaS authorize 时，scope 表达本次目标完整范围，例如 `1、2、3、4、5`。

IDaaS 根据历史授权账本判断：

```text
已授权：1、2、3
新增待确认：4、5
```

页面展示上，建议重点展示新增权限点 `4、5`，避免用户误以为 `1、2、3` 又要重复授权。

### 3.4 返回 Tc 和兑换 TR

用户确认后，IDaaS 返回授权 `code`。Agent 网关用 `code` 换取 `Tc`。

建议 `Tc.consented_scopes` 覆盖本次完整可用范围：

```text
1、2、3、4、5
```

Agent 网关再用 `Tc + T1` 向 IAM 申请新的短期 `TR`。最终 `TR.agency_user.consented_scopes` 也应覆盖 `1、2、3、4、5`。

### 3.5 业务 Agent 替换缓存

业务 Agent 用 `token_result_ticket` 换回 `TR` 后：

- 校验 `TR` 用户与当前 `site_session` 用户一致。
- 校验新 `TR` 至少覆盖本次调用需要的工具或权限点。
- 校验通过后，用新 `TR` 替换旧 `TR`。
- 如果新 `TR` 不覆盖本次调用需要的权限，不覆盖旧缓存，并返回授权不足。

## 4. IDaaS 历史授权账本建议

### 4.1 IDaaS 保存什么

IDaaS 不保存 `TR`，只保存用户授权记录。

推荐授权账本 key：

```text
tenant/user_id + client_id + principal_appid/agent_id
```

推荐授权账本 value：

```text
permissionPointCodes
granted_at
updated_at
grant_source
grant_status
optional_expires_at
```

其中 `permissionPointCodes` 是用户已经授权给该 Agent 的权限点 code 集合。

### 4.2 IDaaS 如何判断历史授权范围

IDaaS 在 `/authorize` 收到本次申请 scope 后，按当前登录用户查询授权账本。

假设本次请求：

```text
requested_scopes = 1、2、3、4、5
saved_grants = 1、2、3
```

则计算：

```text
already_granted = requested_scopes ∩ saved_grants = 1、2、3
new_scopes = requested_scopes - saved_grants = 4、5
```

如果 `new_scopes` 为空，表示本次申请范围历史都已授权，可以不展示授权页，直接返回 `code`。

如果 `new_scopes` 不为空，则展示新增权限点给用户确认。用户确认后，IDaaS 将授权账本更新为并集：

```text
saved_grants = saved_grants ∪ new_scopes
```

### 4.3 生命周期建议

推荐默认生命周期：

```text
长期有效，直到撤销
```

撤销来源包括：

- 用户在统一授权管理页主动撤销。
- 管理员撤销某用户、某部门或某 Agent 的授权。
- Agent 下架、停用或解除委托关系。
- 权限点被删除、停用或风险等级变化后要求重新确认。
- 用户离职、冻结、租户状态异常。

可选增强：

- 高风险权限点支持固定有效期，例如 90 天、180 天或 365 天后重新确认。
- 普通低风险权限点长期有效，减少重复打扰。
- 授权记录支持审计，记录授权时间、授权来源、撤销时间和撤销来源。

### 4.4 与 IDaaS 登录态的关系

IDaaS 登录态和历史授权账本是两类状态：

- 登录态表示“当前浏览器是否已经登录 IDaaS”。
- 历史授权账本表示“该用户是否曾经同意某个 Agent 使用某些权限点”。

用户退出业务 Agent 只清理业务 Agent 自己的 `site_session` 和 `tr_cache`，不等于退出 IDaaS，也不等于撤销 IDaaS 历史授权。

如果 IDaaS 登录态仍有效，下一次可以静默登录。如果历史授权账本也覆盖本次 scope，授权页也可以静默跳过。

## 5. 讨论场景

### 5.1 首次申请权限点 1、2、3

```text
requested_scopes = 1、2、3
saved_grants = 空
new_scopes = 1、2、3
```

IDaaS 展示 `1、2、3`，用户确认后保存 `1、2、3`，最终返回覆盖 `1、2、3` 的 `Tc/TR`。

### 5.2 已有 1、2、3，本次申请 1、2、3、4、5

```text
requested_scopes = 1、2、3、4、5
saved_grants = 1、2、3
new_scopes = 4、5
```

IDaaS 只突出展示新增 `4、5`。用户确认后保存 `1、2、3、4、5`，最终返回覆盖 `1、2、3、4、5` 的 `Tc/TR`。

### 5.3 已有 1、2、3、4、5，本次只需要 3、4、5

```text
requested_scopes = 3、4、5
saved_grants = 1、2、3、4、5
new_scopes = 空
```

IDaaS 可以静默返回 `code`。最终返回的 `Tc/TR` 至少覆盖本次申请范围 `3、4、5`。

如果业务 Agent 希望继续保留当前会话下的完整能力，应在发起申请前传入并集范围，而不是只传 `3、4、5`。

### 5.4 用户拒绝新增 4、5

如果用户拒绝新增权限点 `4、5`，IDaaS 不应返回覆盖 `4、5` 的 `Tc/TR`。

业务 Agent 不应覆盖旧 `TR`，本次调用返回授权不足。旧 `TR` 如果仍未过期，仍可继续用于它原本覆盖的 `1、2、3`。

### 5.5 权限点 2 被撤销或下架

如果权限点 `2` 被管理员撤销、停用或下架，IDaaS 历史授权账本中对应授权应被移除或标记失效。

后续即使用户历史授权过 `2`，也不能继续静默返回覆盖 `2` 的 `Tc/TR`。

## 6. 建议对齐问题

和 IDaaS、IAM、业务接入方讨论时，建议优先确认以下问题：

- IDaaS 是否支持按 `user + client_id + principal_appid/agent_id` 保存历史授权账本。
- IDaaS 是否能在授权页区分“历史已授权”和“本次新增待确认”。
- IDaaS 返回的 `Tc.consented_scopes` 是覆盖 requested scopes，还是只覆盖新增 scopes。
- 用户拒绝新增权限点时，IDaaS 的错误返回和回跳方式是什么。
- 历史授权记录的默认生命周期是长期到撤销，还是固定有效期。
- 权限点停用、Agent 下架、用户离职时，历史授权记录如何失效。

## 7. 当前建议口径

如果需要先给出一个推荐默认方案，可以按以下口径推进：

- IDaaS 保存用户历史授权账本，生命周期默认长期到撤销。
- 补充授权时，业务 Agent 申请当前有效范围与本次所需范围的并集。
- IDaaS 授权页只突出新增权限点，但返回的 `Tc.consented_scopes` 覆盖本次完整申请范围。
- Agent 网关不保存历史授权账本，不保存业务 Agent 历史 `TR`。
- 业务 Agent 本地只缓存一个当前最佳短期 `TR`，新 `TR` 校验通过后替换旧 `TR`。
