# 文档入口

这一目录存放当前标准方案的设计文档。

建议按下面顺序阅读：

1. [phase1_direct_idaas/README.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/phase1_direct_idaas/README.md)
   第一阶段简化方案：业务 Agent 直连 IDaaS
2. [phase2_direct_iam_tokens/README.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/phase2_direct_iam_tokens/README.md)
   第二阶段草案：无 Agent 网关，引入 Tc / T1 / TR
1. [00_一图读懂当前方案.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/00_一图读懂当前方案.md)
   先把主线讲顺
2. [01_新人指南.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/01_新人指南.md)
   先建立整体感
3. [02_总体架构设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/02_总体架构设计.md)
   理解模块边界、主流程和站点会话复用
4. [03_数据流转说明.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/03_数据流转说明.md)
   理解 `request_id`、`security_session_id` 与状态推进
5. [04_令牌设计_策略中心版.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/04_令牌设计_策略中心版.md)
   理解 `Tc / T1 / TR`
6. [05_默认方案接口与交互设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/05_默认方案接口与交互设计.md)
   查看默认方案接口文档入口
7. [06_模块接口总表.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/06_模块接口总表.md)
   评审时快速确认接口边界
8. [07_30分钟评审发言稿.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/07_30分钟评审发言稿.md)
   会前可直接打开使用

## 统一架构口径

- 浏览器流量入口：`ALB -> 接入Agent`
- `Agent网关` 是内部安全编排服务
- 接入 Agent 分成两部分：
  - 前端：负责页面展示、跳转和回到原页面
  - 后端/BFF：负责调用 `Agent网关` 和 `MCP`
- 接入 Agent 不直接对接 `IDaaS`
- `Tc / T1 / TR` 的申请、校验与刷新由 `Agent网关` 统一处理
- 接入 Agent 前端不感知：
  - `request_id` 的内部语义
  - `security_session_id`
  - `tr_token`
- 接入 Agent 后端/BFF 持有：
  - `request_id`
  - `security_session_id`
  - `tr_token`
- 同一浏览器站点会话下，同一用户、同一 Agent 默认复用同一条安全会话

## 推荐先读哪几张图

如果想最快进入状态，先看：

- [02_总体架构设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/02_总体架构设计.md) 里的系统总架构图和 `5.4` 关系图
- [README.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/README.md) 里的总时序图
- [06_模块接口总表.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/06_模块接口总表.md) 里的核心接口总表
