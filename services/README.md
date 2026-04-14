# Services Minimal Startup Guide

这份说明只覆盖 `services` 目录下的 3 个应用：

- `agent-gateway`
- `policy-center`
- `demo-business-agent`

## 1. 先让 IDEA 正确识别项目

如果你直接打开仓库根目录，发现：

- `src/main/java` 没有变成蓝色
- `src/test/java` 没有变成绿色
- `resources` 没有被识别成资源目录
- Maven 依赖树没有正常出现

通常不是代码有问题，而是 **IDEA 还没有把 `services` 下面的 3 个子工程导入成 Maven module**。

### 1.1 为什么会这样

当前仓库根目录的 `pom.xml` 不是一个已经整理好的 Maven 聚合父工程，所以 IDEA 直接打开根目录时，不一定会自动把下面这些服务识别成模块：

- `services/agent-gateway`
- `services/policy-center`
- `services/demo-business-agent`

### 1.2 正确导入方式

在 IDEA 里按下面步骤操作：

1. 打开右侧 `Maven` 工具窗口
2. 点击 `+` 或 `Add Maven Projects`
3. 依次选择这 3 个 `pom.xml`
   - `services/agent-gateway/pom.xml`
   - `services/policy-center/pom.xml`
   - `services/demo-business-agent/pom.xml`
4. 导入完成后，点击一次 `Reload All Maven Projects`

### 1.3 导入成功后你会看到什么

导入成功后，一般会出现这些效果：

- `src/main/java` 变成蓝色
- `src/test/java` 变成绿色
- `src/main/resources` 被识别成资源目录
- 每个服务都能在 IDEA 里作为独立 Maven module 运行

### 1.4 如果还没有颜色

可以再检查这几件事：

- 右键对应目录，看是否被错误标成普通目录
- 在 `Project Structure -> Modules` 中确认模块是否已经出现
- 在 Maven 工具窗口重新 `Reload`
- 必要时关闭项目后重新打开

### 1.5 推荐做法

建议以后始终把下面这 3 个 `pom.xml` 当作实际可运行模块：

- `services/agent-gateway/pom.xml`
- `services/policy-center/pom.xml`
- `services/demo-business-agent/pom.xml`

## 2. 启动顺序

建议按下面顺序启动：

1. `policy-center`
2. `agent-gateway`
3. `demo-business-agent`

## 3. 本地 mock 模式启动

### 3.1 启动策略中心

```powershell
cd D:\IDEA_Project\init_env\auth-design-spec\services\policy-center
mvn spring-boot:run
```

默认端口：`18081`

### 3.2 启动 Agent 网关

```powershell
cd D:\IDEA_Project\init_env\auth-design-spec\services\agent-gateway
mvn spring-boot:run
```

默认端口：`18080`

说明：

- 默认 profile 是 `mock`
- 会使用本地 mock 的 `IDaaS` 和 `IAM`
- 不需要真实外部系统即可跑通登录、授权和 `TR` 获取链路

### 3.3 启动 Demo Business Agent

```powershell
cd D:\IDEA_Project\init_env\auth-design-spec\services\demo-business-agent
mvn spring-boot:run
```

默认端口：`18082`

## 4. 验证路径

浏览器打开：

- [http://localhost:18082/agent](http://localhost:18082/agent)

验证步骤：

1. 首次进入会跳到 Agent 网关
2. Agent 网关会再跳到 mock IDaaS
3. 完成 mock 登录后回到 demo 页面
4. 输入包含“财报”或“发票”的问题
5. 首次会触发授权跳转
6. 授权完成后自动恢复消息并重发
7. 二次相同类型问题会优先命中本地覆盖式 `TR` 缓存

## 5. 关键端口

- `policy-center`: `18081`
- `agent-gateway`: `18080`
- `demo-business-agent`: `18082`

## 6. real profile 说明

当前只有 `agent-gateway` 区分 `mock` 和 `real` 两套 provider 实现。

`real` 相关实现位于：

- `agent-gateway/src/main/java/com/huawei/it/roma/liveeda/auth/client/idaas/RealIdaasAuthorizeSupport.java`
- `agent-gateway/src/main/java/com/huawei/it/roma/liveeda/auth/client/idaas/RealIdaasTokenClient.java`
- `agent-gateway/src/main/java/com/huawei/it/roma/liveeda/auth/client/iam/RealIamAssumeAgentTokenClient.java`
- `agent-gateway/src/main/java/com/huawei/it/roma/liveeda/auth/client/iam/RealIamResourceTokenClient.java`

默认配置文件：

- `agent-gateway/src/main/resources/application.yml`

当前默认：

- `spring.profiles.default=mock`

如果要切换到 `real`，可以这样启动：

```powershell
cd D:\IDEA_Project\init_env\auth-design-spec\services\agent-gateway
mvn spring-boot:run "-Dspring-boot.run.profiles=real"
```

或：

```powershell
cd D:\IDEA_Project\init_env\auth-design-spec\services\agent-gateway
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=real"
```

## 7. 切换到 real 前至少要补齐的配置

需要把 `agent-gateway/src/main/resources/application.yml` 中以下配置替换成真实值，或拆到 `application-real.yml`：

- `providers.idaas.authorize-url`
- `providers.idaas.token-url`
- `providers.idaas.client-id`
- `providers.iam.base-url`
- `providers.iam.proxy-project-id`
- `gateway.self-base-url`

如果真实 `IDaaS` 要求更多参数，还需要补：

- `client_secret`
- 认证头
- TLS / 证书
- 更严格的 timeout / retry

## 8. 当前限制

- 网关和策略中心当前都使用内存/Caffeine 存储
- 服务重启后会话、待授权事务、授权上下文会丢失
- demo-business-agent 只用于联调和演示，不是正式平台服务
- demo 当前不接真实 MCP 网关，而是使用本地 mock MCP 结果
