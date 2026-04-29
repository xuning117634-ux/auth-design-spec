# 内网 PC 本地部署 Agent 网关联调指南

本文用于说明：只在内网 PC 本地启动 `agent-gateway`，其余模块使用开发环境已部署服务时，应该如何配置和启动。

## 1. 部署目标

本地只启动：

- `services/agent-gateway`

开发环境使用：

- IDaaS：`https://uniportal-dev.huawei.com`
- IAM：`https://apig.hissit.huawei.com`
- 策略中心 APIG：`https://apig-beta.his.huawei.com/api/dev/policy-center`
- Agent 管理面 APIG：`https://apig-beta.his.huawei.com/api/dev`

不需要本地启动：

- `services/policy-center`
- `services/demo-business-agent`

## 2. 前置条件

本机需要满足：

- JDK 21 可用
- Maven 可用
- 内网网络能访问 IDaaS、IAM、策略中心 APIG、Agent 管理面 APIG
- 如果开发环境业务 Agent 要调用本机 Agent 网关，本机 `18080` 端口需要对开发环境业务 Agent 后端可访问

快速检查：

```powershell
java -version
mvn -version
Test-NetConnection uniportal-dev.huawei.com -Port 443
Test-NetConnection apig.hissit.huawei.com -Port 443
Test-NetConnection apig-beta.his.huawei.com -Port 443
```

## 3. 当前配置文件已经内置的开发环境信息

Agent 网关的开发环境配置在：

```text
services/agent-gateway/src/main/resources/application-real.yml
```

该文件已经写入以下非密钥配置：

```yaml
providers:
  idaas:
    authorize-url: https://uniportal-dev.huawei.com/saaslogin1/oauth2/agent/authorize
    token-url: https://uniportal-dev.huawei.com/saaslogin1/oauth2/agent/token
    userinfo-url: https://uniportal-dev.huawei.com/saaslogin1/oauth2/agent/userinfo
    client-id: Agent_33b71f66573c4f5bac20112c3f628442
  iam:
    base-url: https://apig.hissit.huawei.com
    gateway-account: Agent_33b71f66573c4f5bac20112c3f628442
    gateway-project: com.huawei.pass.roma.event
    gateway-enterprise: 11111111111111111111111111111111

clients:
  policy-center:
    base-url: https://apig-beta.his.huawei.com/api/dev/policy-center
  agent-management:
    base-url: https://apig-beta.his.huawei.com/api/dev
    query-by-agent-id-path: /public/agentMall/queryByAgentId
```

密钥不写入配置文件，需要通过环境变量注入。

## 4. 必填环境变量

在启动 Agent 网关的 PowerShell 窗口中配置：

```powershell
$env:SPRING_PROFILES_ACTIVE="real"
$env:IDAAS_CLIENT_SECRET="IDaaS client_secret"
$env:IAM_GATEWAY_SECRET="IAM AI 账号密钥"
$env:APIG_X_HW_APPKEY="APIG X-HW-APPKEY"
$env:GATEWAY_INSECURE_SKIP_TLS_VERIFY="true"
```

说明：

- `SPRING_PROFILES_ACTIVE=real`：启用真实开发环境配置。
- `IDAAS_CLIENT_SECRET`：Agent 网关在 IDaaS 的客户端密钥。
- `IAM_GATEWAY_SECRET`：Agent 网关 AI 账号密钥。
- `APIG_X_HW_APPKEY`：访问策略中心 APIG 和 Agent 管理面 APIG 的密钥。
- `GATEWAY_INSECURE_SKIP_TLS_VERIFY`：内网开发联调临时跳过 HTTPS 证书校验；生产环境必须配置正式证书并改为 `false`。

如果策略中心和 Agent 管理面使用不同 APPKEY，可以拆开配置：

```powershell
$env:POLICY_CENTER_X_HW_APPKEY="策略中心 APIG APPKEY"
$env:AGENT_MANAGEMENT_X_HW_APPKEY="Agent 管理面 APIG APPKEY"
```

## 5. 本机访问地址选择

### 5.1 只在本机浏览器自测

如果业务 Agent 也在本机，或者只是用本机浏览器验证网关跳转链路，可以使用默认配置：

```yaml
gateway:
  self-base-url: http://localhost:18080
  secure-cookies: false
```

此时不需要额外配置 `GATEWAY_SELF_BASE_URL`。

### 5.2 开发环境业务 Agent 后端要调用本机网关

如果开发环境已经部署的业务 Agent 后端需要调用你本机的 Agent 网关，则不能使用 `localhost`，因为开发环境机器访问 `localhost` 只会访问它自己。

需要把网关外部访问地址改成本机内网 IP 或代理域名，例如：

```powershell
$env:GATEWAY_SELF_BASE_URL="http://<你的内网PC-IP>:18080"
```

同时需要确保：

- 开发环境业务 Agent 后端能访问 `http://<你的内网PC-IP>:18080`
- 本机防火墙放通 `18080`
- 业务 Agent 配置的 Agent 网关地址也指向 `http://<你的内网PC-IP>:18080`
- Agent 管理面中该业务 Agent 的 `allowedReturnHosts` 包含业务 Agent 回跳域名

如果开发环境无法直接访问本机 IP，需要使用内网代理、端口映射或隧道方案。

## 6. 启动命令

在仓库根目录执行：

```powershell
mvn -f services/agent-gateway/pom.xml spring-boot:run
```

启动成功后检查健康状态：

```powershell
Invoke-RestMethod http://localhost:18080/actuator/health
```

期望返回：

```json
{
  "status": "UP"
}
```

## 7. 联调链路

### 7.1 登录链路

业务 Agent 发起登录：

```text
GET /gw/auth/login?agent_id=<业务AgentID>&return_url=<业务Agent回跳地址>&state=<业务侧state>
```

Agent 网关会：

- 调 Agent 管理面查询 `agent_id` 对应的 Agent 信息
- 校验 `return_url` host 是否在 `allowedReturnHosts` 内
- 拼接 IDaaS authorize 地址
- 跳转到 IDaaS 登录
- IDaaS 回调 `/gw/auth/base/callback`
- 网关返回一次性 `ticketST`

业务 Agent 后端随后调用：

```text
POST /gw/auth/ticket/exchange
```

用 `ticketST` 换用户信息，并创建自己的 `site_session`。

### 7.2 授权获取 TR 链路

业务 Agent 后端调用：

```text
POST /gw/token/resource-token
```

请求体示例：

```json
{
  "agentId": "2d513fbfee9b4cfe96722060bc7f1b9d",
  "requiredTools": [
    "mcp:contract-server/get_contract"
  ],
  "returnUrl": "https://业务Agent域名/agent",
  "state": "业务侧状态"
}
```

Agent 网关会：

- 调 Agent 管理面查询 Agent 信息
- 调策略中心把 `requiredTools` 解析为权限点
- 拼接 IDaaS authorize 地址，`scope` 使用权限点 code，多个权限点用空格分隔
- 用户授权后，IDaaS 回调 `/gw/auth/consent/callback`
- 网关完成 `code -> Tc -> T1 -> TR`
- 网关回跳业务 Agent，并只返回一次性 `token_result_ticket`

业务 Agent 后端随后调用：

```text
POST /gw/token/result/exchange
```

用 `token_result_ticket` 换取 `TR`。

## 8. 常见问题

### 8.1 为什么不能把密钥写进配置文件？

配置文件会进入 Git，密钥不应该提交。当前方案只把开发环境地址、appid、enterprise 这类非密钥写入配置文件，密钥统一通过环境变量注入。

### 8.2 为什么开发环境业务 Agent 不能直接访问 `localhost:18080`？

因为 `localhost` 对每台机器来说都是它自己。开发环境业务 Agent 后端访问 `localhost:18080` 时，不会访问你的 PC，而是访问开发环境机器自身。

### 8.3 本机只部署 Agent 网关，还需要启动策略中心吗？

不需要。`real` profile 下 Agent 网关会访问开发环境策略中心 APIG：

```text
https://apig-beta.his.huawei.com/api/dev/policy-center
```

### 8.4 本机只部署 Agent 网关，还需要启动 Demo Agent 吗？

不需要。如果你使用开发环境业务 Agent 联调，只需要让开发环境业务 Agent 指向本机 Agent 网关地址。

如果开发环境业务 Agent 暂时不能改地址，才建议本机启动 `demo-business-agent` 做自测。

## 9. 最小启动清单

```powershell
$env:SPRING_PROFILES_ACTIVE="real"
$env:IDAAS_CLIENT_SECRET="IDaaS client_secret"
$env:IAM_GATEWAY_SECRET="IAM AI 账号密钥"
$env:APIG_X_HW_APPKEY="APIG X-HW-APPKEY"
$env:GATEWAY_INSECURE_SKIP_TLS_VERIFY="true"

# 仅当开发环境业务 Agent 后端要访问本机网关时需要配置
$env:GATEWAY_SELF_BASE_URL="http://<你的内网PC-IP>:18080"

mvn -f services/agent-gateway/pom.xml spring-boot:run
```

## 10. 本机同时启动 Demo Agent

如果内网 PC 也要本地启动 `demo-business-agent` 做演示，可以在启动 Agent 网关后，再开一个 PowerShell 窗口启动 Demo Agent。

### 10.1 Demo Agent 启动命令

```powershell
$env:SPRING_PROFILES_ACTIVE="real"
$env:APIG_X_HW_APPKEY="APIG X-HW-APPKEY"

mvn -f services/demo-business-agent/pom.xml spring-boot:run
```

启动后访问：

```text
http://localhost:18082/agent
```

### 10.2 Demo Agent 的联调链路

启用 `real` profile 后，Demo Agent 的链路是：

```text
本地 demo-business-agent
  -> 本地 agent-gateway
  -> 开发环境 IDaaS / IAM / 策略中心 / Agent 管理面
  -> 本地 mock MCP 返回演示结果
```

也就是说：

- Demo Agent 仍然连接本地 Agent 网关：`http://localhost:18080`
- Agent 网关连接开发环境 IDaaS、IAM、策略中心和 Agent 管理面
- Demo Agent 使用真实业务 Agent ID：`2d513fbfee9b4cfe96722060bc7f1b9d`
- Demo Agent 会获取真实 TR
- 最终 MCP 工具调用默认仍走本地 mock，不访问真实 MCP 网关

### 10.3 MCP 网关未准备好时是否能演示？

可以。

当前 `demo-business-agent` 增加了独立的 MCP 调用模式开关：

```yaml
demo-agent:
  mcp:
    mode: mock
```

即使启用 `real` profile，默认仍然是 `mcp.mode=mock`。这表示：

- 本地 Demo Agent 可以真实登录
- 可以真实授权
- 可以真实获取 TR
- 可以访问开发环境策略中心
- 最终工具调用由本地 mock MCP 返回固定演示结果
- 不会因为真实 MCP 网关接口未准备好而阻塞演示

### 10.4 真实 MCP 网关准备好后的切换方式

等真实 MCP 网关接口准备好后，再把配置切成：

```yaml
demo-agent:
  mcp:
    mode: real
```

并补齐真实 MCP 网关地址、调用路径和请求头：

```yaml
demo-agent:
  mcp:
    mode: real
    gateway-base-url: https://apig-beta.his.huawei.com/api/dev/mcp-gateway
    invoke-path: /internal/v1/tools/invoke
    headers:
      X-HW-ID: com.huawei.pass.roma.event
      X-HW-APPKEY: ${MCP_GATEWAY_X_HW_APPKEY:${APIG_X_HW_APPKEY:}}
```

当前真实 MCP 接口还未最终确认，所以默认不要切到 `real`。
