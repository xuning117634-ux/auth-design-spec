# final_design 设计变更记录

本文件只记录会影响正式设计口径的变化。它不是提交日志，也不记录纯代码实现细节。

## 使用规则

- 每次修改正式设计前，先在这里追加一条变更摘要。
- 每条记录写清楚：变化内容、影响文件、代码影响范围。
- 后续 AI 或团队成员先读本文件，再按 [README.md](./README.md) 定位相关文档。
- 如果只是修错别字、调整排版、补充不改变口径的说明，可以不记录。

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
