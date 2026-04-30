# AGENTS.md

本文件是本仓库的项目级协作约定。后续 AI 助手在本仓库工作时，应优先阅读并遵守这些规则。

## 文档读取策略

- 不要默认全量读取 `docs/final_design`。
- 涉及正式设计口径时，先读：
  - `docs/final_design/README.md`
  - `docs/final_design/CHANGELOG.md`
- 然后根据任务只读取相关职责文档。
- 对跨文档统一字段或术语，优先用全文搜索精准定位旧词，再局部修改。
- 如果只是代码实现细节变化，不影响正式设计口径，可以不修改正式设计文档。
- 如果影响正式设计口径，先更新 `docs/final_design/CHANGELOG.md`，再同步相关文档。

## 常见文档影响范围

- 登录流程、`ticketST`、`site_session`：优先看 `02_引入Agent网关版方案.md`、`04_接口设计明细/02_Agent网关接口.md`、`06_时序图逐箭头说明.md`、`flow_details/01_base_login_ticketST.md`。
- 授权流程、`token_result_ticket`、`TR` 交换：优先看 `02_引入Agent网关版方案.md`、`03_令牌设计.md`、`04_接口设计明细/02_Agent网关接口.md`、`flow_details/02_resource_token_ticket.md`。
- `consented_scopes`、`agency_user`、`aud`：优先看 `03_令牌设计.md`、`04_接口设计明细/04_MCP网关接口.md`、`04_接口设计明细/05_外部依赖接口.md`。
- 策略中心权限点、策略、Agent 订阅：优先看 `05_策略中心设计.md`、`04_接口设计明细/03_策略中心接口.md`。
- 外部 IDaaS/IAM 接口签名：优先看 `04_接口设计明细/05_外部依赖接口.md`。
- Demo 页面和演示体验：通常只改 `services/demo-business-agent`；除非改变链路语义，否则不改正式设计文档。

## 代码修改策略

- 修改代码前先看相关模块现有结构和测试，不要凭记忆改。
- 保持三个服务的边界：
  - `services/agent-gateway`：Agent 网关。
  - `services/policy-center`：策略中心。
  - `services/demo-business-agent`：演示业务 Agent。
- `IDaaS`、`IAM`、`MCP` 外部系统本地默认可以 mock；真实联调用 `real` profile 和配置切换。
- 不要为了兼容旧设计保留过时字段，除非用户明确要求。
- 不要回滚用户已有改动。

## Git 与暂存

- 新增或删除必要文件后，应主动加入暂存区，避免遗漏提交。
- 不要把无关目录或临时文件加入暂存区。
- 提交前先查看 `git status --short --branch`。
- 如果用户要求提交或推送，提交信息使用中文，并简要说明主要变更点。

## 当前关键设计口径

- Agent 网关不维护长期用户登录态。
- 业务 Agent 维护自己的 `site_session` 和 `TR` 缓存。
- base 登录结果通过 `ticketST` 后端交换。
- 授权结果通过 `token_result_ticket` 后端交换。
- 浏览器 URL 不携带 `Tc`、`TR`、用户信息。
- `TR.agency_user.consented_scopes` 是权限点 code 字符串数组。
- `TR.scope` 只是预留字段，不作为运行时鉴权依据。
- MCP 运行时从 `TR.aud` 读取业务 Agent ID，从 `TR.agency_user.user.uid` 读取用户 ID。
