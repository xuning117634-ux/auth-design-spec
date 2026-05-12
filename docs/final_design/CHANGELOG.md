# final_design 设计变更记录

本文件只记录会影响正式设计口径的变化。它不是提交日志，也不记录纯代码实现细节。

## 使用规则

- 每次修改正式设计前，先在这里追加一条变更摘要。
- 每条记录写清楚：变化内容、影响文件、代码影响范围。
- 后续 AI 或团队成员先读本文件，再按 [README.md](./README.md) 定位相关文档。
- 如果只是修错别字、调整排版、补充不改变口径的说明，可以不记录。

## 2026-05-08

### 通用权限模型收口为 PARC，并预留 Cedar 编译路径

- 变化内容：通用权限模型正式采用 `Principal + Action + Resource + Context` 命名，作为后续 Cedar-friendly JSON IR。
- 关键口径：管理面仍采用 Agent 优先心智；管理员先选 Agent 确定 `Context`，再通过访问控制、Skill 控制、权限点控制确定 `Action` 和 `Resource`。
- 关键口径：`OPEN / RESTRICTED` 是管控目标的“未命中策略时默认裁决”，不作为单条策略字段；策略配置区只创建 `PERMIT / DENY`。
- 关键口径：管理员不直接对 MCP tool 建策略；MCP tool 仍通过权限点映射参与运行时校验。
- 关键口径：未来接入 Cedar 时，管理面表单生成结构化 JSON IR，再编译为 Cedar policy、schema、entities 和 request。
- 影响文档：`07_通用权限模型与管理员旅程.md`、`08_Cedar接入改造提醒.md`、`README.md`。
- 影响代码：无。

### 新增通用权限模型与管理员旅程说明

- 变化内容：新增 `07_通用权限模型与管理员旅程.md`，说明策略中心从“Agent + 权限点策略”向通用权限管理演进的模型。
- 关键口径：策略中心统一回答“谁，在什么场景下，能不能执行某个动作，并作用到某个资源”；资源类型可覆盖 `AGENT`、`SKILL`、`PERMISSION_POINT`、`MCP_TOOL`。
- 关键口径：Web 门面不是 Agent，不进入策略中心资源模型；所有业务 Agent 都按平铺的 `AGENT` 管理，策略中心不关心父子关系。
- 关键口径：策略中心管理面采用 Agent 优先心智，管理员先选择 Agent，再配置该 Agent 的访问控制、Skill 控制和权限点控制；底层由页面自动带入 `context.agentId`。
- 关键口径：通用权限模型和管理页面采用 `Principal + Action + Resource + Context` 命名；先确定 `Action`，再按动作筛选可选 `Resource`。
- 关键口径：能力清单是当前 Agent 的只读概览，策略测试是独立验证工具；二者不作为策略配置 Tab，与访问控制、Skill 控制、权限点控制分开。
- 关键口径：`enabledSkills` 和 `subscribedPermissionPoints` 是 Agent 注册或 Agent 管理面维护的静态能力清单；策略中心策略只在这些能力边界内控制用户是否可使用，不通过策略表达 Agent 是否绑定 Skill。
- 关键口径：业务方调用通用权限判断时只传 `principal.type + principal.id`；人员类型、角色、部门、用户组等用户画像由策略中心通过内部 `UserProfileResolver` 查询。
- 关键口径：MCP 网关负责工具和权限点映射，Agent 管理面或 Agent 网关负责 Agent 注册和权限点订阅，策略中心负责用户、Agent、Skill、权限点之间的使用规则。
- 影响文档：`07_通用权限模型与管理员旅程.md`、`README.md`。
- 影响代码：无。

### 策略中心新增权限点和策略硬删除接口

- 变化内容：策略中心在原有 `batch-upsert + status` 状态删除之外，新增物理硬删除接口。
- 新增接口：`POST /internal/v1/permission-points/batch-hard-delete`，按 `enterprise + permissionPointCodes` 批量硬删除权限点。
- 新增接口：`POST /internal/v1/agent-strategies/batch-hard-delete`，按 `agentId + strategyIds` 批量硬删除策略。
- 关键口径：权限点硬删除采用级联清理，会移除工具映射、引用该权限点的策略和策略条件值，并从 Agent 订阅快照中移除该权限点；不会删除 `pc_tool` 工具主表。
- 关键口径：策略硬删除会显式删除策略条件值，再删除策略主表；不存在或不匹配的数据返回 `NOT_FOUND` item，不导致整个批次失败。
- 影响文档：`05_策略中心设计.md`、`04_接口设计明细/03_策略中心接口.md`、`00_当前方案速记.md`、`quick_read/04_接口速查.md`。
- 影响代码：`services/policy-center` 新增硬删除 Controller、Service、Mapper、DTO 和测试。

## 2026-05-06

### 长期授权由 IDaaS 保存，`TR` 仍保持短期

- 变化内容：长期/永久授权不设计成长期 `TR`；长期授权记录由 `IDaaS` 作为权威保存。
- 关键口径：`TR` 始终是短期资源访问令牌，业务 Agent 仍只按 `expires_in/exp` 做本地 `tr_cache`。
- 关键口径：当前阶段采用浏览器静默闪跳；业务 Agent 无可用 `TR` 时仍走 Agent 网关授权链路，IDaaS 命中长期授权记录后可不展示授权页，直接返回 `code`。
- 关键口径：Agent 网关不保存长期授权账本，不新增后端无感刷新接口。
- 影响文档：`02_引入Agent网关版方案.md`、`03_令牌设计.md`、`04_接口设计明细/02_Agent网关接口.md`、`docs/业务Agent接入开发指南.md`。
- 影响代码：无。

## 2026-04-30

### 统一 `consented_scopes` 字符串数组格式

- 变化内容：全链路将 `consented_scopes` 统一为权限点 code 字符串数组，例如 `["erp:report:read", "erp:invoice:read"]`。
- 关键口径：运行时权限点只从 `TR.agency_user.consented_scopes` 读取，`TR.scope` 仅作为预留字段。
- 关键口径：用户 ID 从 `TR.agency_user.user` JSON 字符串里的 `uid` 读取，业务 Agent ID 从 `TR.aud` 读取。
- 影响文档：`03_令牌设计.md`、`04_接口设计明细/02_Agent网关接口.md`、`04_接口设计明细/04_MCP网关接口.md`、`04_接口设计明细/05_外部依赖接口.md`、`00_当前方案速记.md`、`02_引入Agent网关版方案.md`、`05_策略中心设计.md`、`06_时序图逐箭头说明.md`、`quick_read/*`。
- 影响代码：Agent 网关 mock TR 生成、授权结果交换响应、Demo MCP 运行时 TR 解析。

### 建立按需阅读机制

- 变化内容：`README.md` 增加文件职责、按需读取规则、常见变更影响范围。
- 关键口径：后续不再默认全量读取 `final_design`，优先读取 `README.md + CHANGELOG.md + 相关职责文档`。
- 影响文档：`README.md`、`CHANGELOG.md`。
- 影响代码：无。
