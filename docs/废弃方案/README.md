# 文档总览

这份目录已经按统一阅读主线整理。

如果你要理解或继续推进这套方案，建议从下面这条主线进入：

## 推荐阅读入口

1. [design/README.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/README.md)
   文档总入口，先看这份
2. [01_新人指南.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/01_新人指南.md)
   新人 5 分钟速读
3. [02_总体架构设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/02_总体架构设计.md)
   总体架构与主流程
4. [03_数据流转说明.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/03_数据流转说明.md)
   阶段流转与状态推进
5. [04_令牌设计_策略中心版.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/04_令牌设计_策略中心版.md)
   `Tc / T1 / TR` 设计
6. [05_默认方案接口与交互设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/05_默认方案接口与交互设计.md)
   默认方案的接口与模块交互入口
7. [06_模块接口总表.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/06_模块接口总表.md)
   适合评审的一页版模块接口总览
8. [07_30分钟评审发言稿.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/07_30分钟评审发言稿.md)
   适合接口评审会直接使用

## 关键口径

- 用户入口是 `ALB -> 接入Agent`
- `Agent网关` 是内部安全编排服务
- 接入 Agent 不直接对接 `IDaaS`
- 登录、授权、`Tc / T1 / TR` 的申请、校验与刷新由 `Agent网关` 统一负责
- Web Agent 场景下，需要区分：
  - 站点登录
  - Agent 代表用户访问资源的认证授权


