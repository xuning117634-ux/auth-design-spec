# final_design 阅读索引

本目录是当前正式设计的唯一入口。`01_无Agent网关版方案.md` 仅作参考；当前正式方案以 `02/03/04/05` 为主。

## 推荐阅读路径

| 目标 | 建议阅读 |
| --- | --- |
| 5 分钟理解主线 | [00_当前方案速记.md](./00_当前方案速记.md) |
| 理解整体架构和模式 A | [02_引入Agent网关版方案.md](./02_引入Agent网关版方案.md) |
| 理解 `Tc / T1 / TR` | [03_令牌设计.md](./03_令牌设计.md) |
| 查接口 | [04_接口设计.md](./04_接口设计.md) |
| 查策略中心模型 | [05_策略中心设计.md](./05_策略中心设计.md) |
| 开会解释流程箭头 | [06_时序图逐箭头说明.md](./06_时序图逐箭头说明.md) |
| AI 快速载入上下文 | [quick_read](./quick_read/README.md) |

## 轻拆分说明

为了避免单个文档过长，流程细节拆到 `flow_details`：

- [flow_details/01_base_login_ticketST.md](./flow_details/01_base_login_ticketST.md)：base 登录和 `ticketST`。
- [flow_details/02_resource_token_ticket.md](./flow_details/02_resource_token_ticket.md)：业务授权、`TR` 获取和 `token_result_ticket`。
- [flow_details/03_one_time_ticket_security.md](./flow_details/03_one_time_ticket_security.md)：一次性凭据安全约束。

## 当前正式口径

- Agent 网关不维护长期用户登录态。
- IDaaS 负责判断用户是否已登录，必要时展示登录页或授权页。
- 业务 Agent 维护自己的 `site_session` 和 `TR` 缓存。
- base 登录结果通过 `ticketST` 后端交换。
- 授权结果通过 `token_result_ticket` 后端交换。
- 浏览器 URL 不携带 `Tc`、`TR`、用户信息。
- 令牌中的授权范围字段继续使用 `consented_scopes`。
