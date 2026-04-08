# 文档入口

这一目录下存放的是当前使用的设计文档。

如果你刚接手这个项目，建议按下面顺序阅读：

1. [01_新人指南.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/01_新人指南.md)
   先用 5 分钟建立全局感
2. [02_总体架构设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/02_总体架构设计.md)
   理解总体边界和主流程
3. [03_数据流转说明.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/03_数据流转说明.md)
   理解阶段流转与状态变化
4. [04_令牌设计_策略中心版.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/04_令牌设计_策略中心版.md)
   理解 `Tc / T1 / TR`
5. [05_默认方案接口与交互设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/05_默认方案接口与交互设计.md)
   看默认方案的接口与模块交互设计
6. [06_模块接口总表.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/06_模块接口总表.md)
   适合评审的一页版接口总表

## 统一架构口径

- 浏览器流量入口：`ALB -> 接入Agent`
- `Agent网关` 是内部安全编排服务
- 接入 Agent 负责页面、业务交互和在合适的节点调用 `Agent网关`
- 接入 Agent 不直接对接 `IDaaS`
- `Agent网关` 统一负责：
  - 登录启动
  - 登录回调
  - 授权启动
  - 授权回调
  - `Tc`
  - `T1`
  - `TR`
- 当前接口与交互文档只覆盖默认方案

## 推荐先读哪一张图

如果你想最快进入状态，先看：

- [02_总体架构设计.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/02_总体架构设计.md) 里的系统总架构图
- [README.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/README.md) 里的总时序图和 Web Agent 两层认证图
- [06_模块接口总表.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/06_模块接口总表.md) 里的核心接口总表
