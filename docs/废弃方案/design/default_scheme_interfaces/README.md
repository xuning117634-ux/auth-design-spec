# 默认方案接口与模块交互

> 本目录只覆盖默认方案。财报、发票与 `erp:*` 策略 code 仅为示例业务场景。

## 文档导航

1. [01_全局约定.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/01_全局约定.md)
   统一约定、状态值、错误码、Token 和会话边界
2. [02_接入准备与认证授权.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/02_接入准备与认证授权.md)
   Agent 注册、登录启动、登录回调、默认方案授权、`Tc` 获取
3. [03_T1与TR生成.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/03_T1与TR生成.md)
   `T1` 获取、`TR` 生成、同步返回 `security_session_id + TR`
4. [04_资源访问与TR刷新.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/04_资源访问与TR刷新.md)
   MCP 调用、运行时校验、会话复用、`TR` 刷新
5. [05_数据对象与状态模型.md](/D:/IDEA_Project/init_env/auth-design-spec/docs/design/default_scheme_interfaces/05_数据对象与状态模型.md)
   数据对象、状态模型与典型交互场景

## 已锁定的设计前提

- 只设计默认方案，不展开插件方案接口
- 浏览器入口流量是：`ALB -> 接入Agent`
- `Agent网关` 是内部安全编排服务
- 接入 Agent 不直接对接 `IDaaS`
- 接入 Agent 前端只负责跳转和回到原页面
- 接入 Agent 后端/BFF 负责：
  - 调用 `Agent网关`
  - 调用 `MCP`
  - 保存 `request_id`
  - 保存 `security_session_id`
  - 保存当前 `TR`
- 接入 Agent 不持有 `Tc / T1`
- `TR` 过期后只能回 `Agent网关` 刷新
- `security_session_id` 只是会话索引，不是独立凭证
- Agent 调 `Agent网关` 时需要运行时身份认证，但当前文档不绑定具体技术实现

## 真实令牌接口基线

- `Tc` 接口：`POST /oauth2/token`
- `T1` 接口：`POST https://apig.hisuat.huawei.com/iam/projects/{proxy_project_id}/assume_agent_token`
- `TR` 接口：`POST https://apig.hisuat.huawei.com/iam/auth/resource-token`
- `TR` 刷新接口：`POST https://apig.hisuat.huawei.com/iam/auth/refresh-resource-token`
- 当前 IAM 文档未提供 `TR introspect` 官方接口，因此资源访问阶段保留内部 `TR` 运行时校验接口

## 接口分层

### 第一层：接入 Agent 可见接口

- `POST /api/v1/runtime/tr/acquire`
- `POST /api/v1/runtime/security-sessions/{security_session_id}/tr/refresh`
- `POST /api/v1/mcp/invoke`

### 第二层：Agent 网关内部编排接口

- `GET /api/v1/auth/login/start`
- `GET /api/v1/auth/login/callback`
- `GET /api/v1/auth/consent/start`
- `GET /api/v1/auth/consent/callback`
- `POST /internal/v1/policies/resolve-by-tools`
- `POST /oauth2/token`
- `POST https://apig.hisuat.huawei.com/iam/projects/{proxy_project_id}/assume_agent_token`
- `POST https://apig.hisuat.huawei.com/iam/auth/resource-token`
- `POST https://apig.hisuat.huawei.com/iam/auth/refresh-resource-token`

### 第三层：资源访问内部接口

- `POST /internal/v1/tokens/tr/introspect`
- `POST /internal/v1/tools/invoke`

## 静态结构图

```mermaid
flowchart LR
    U["用户浏览器"]
    FE["接入Agent前端<br/>页面 / 跳转 / 页面恢复"]
    BFF["接入Agent后端/BFF<br/>调用网关 / 调用MCP"]
    GW["Agent网关<br/>统一安全编排"]
    ID["IDaaS"]
    IAM["IAM"]
    PC["Agent策略中心"]
    MCP["MCP网关 / MCP Server"]

    U --> FE
    FE --> BFF
    BFF --> GW
    GW --> ID
    GW --> IAM
    GW --> PC
    BFF --> MCP
```

## 总时序图

```mermaid
sequenceDiagram
participant User as 用户
participant FE as 接入Agent前端
participant BFF as 接入Agent后端/BFF
participant Gateway as Agent网关
participant Policy as Agent策略中心
participant IDaaS
participant IAM
participant MCPGW as MCP网关
participant MCPS as MCP Server

User->>FE: 打开页面并发起资源型请求
FE->>BFF: 发送问题与 client_return_url
BFF->>Gateway: POST /api/v1/runtime/tr/acquire
Gateway->>Gateway: 创建 request_id，并检查网关侧登录态 / 用户身份上下文

alt 需要身份确认
    Gateway-->>BFF: 200 { request_id, status=REDIRECT_REQUIRED, redirect_url }
    BFF-->>FE: 返回 redirect_url
    FE-->>User: 302 到 Gateway 登录启动地址
    User->>Gateway: GET /api/v1/auth/login/start
    Gateway-->>User: 302 到 IDaaS 登录页
    User->>IDaaS: 登录
    IDaaS-->>Gateway: GET /api/v1/auth/login/callback
    Gateway-->>User: 302 回到原聊天页 client_return_url
    User->>FE: 回到 Agent 页面
    FE->>BFF: 继续当前请求
end

BFF->>Gateway: POST /api/v1/runtime/tr/acquire { request_id }
Gateway->>Gateway: 确认 request_id 已绑定用户身份
Gateway->>Policy: POST /internal/v1/policies/resolve-by-tools
Policy-->>Gateway: 200 { policy_codes }

alt 未授权
    Gateway-->>BFF: 200 { request_id, status=REDIRECT_REQUIRED, redirect_url }
    BFF-->>FE: 返回 redirect_url
    FE-->>User: 302 到 Gateway 授权启动地址
    User->>Gateway: GET /api/v1/auth/consent/start
    Gateway-->>User: 302 到 IDaaS 授权页
    User->>IDaaS: 确认授权
    IDaaS-->>Gateway: GET /api/v1/auth/consent/callback
    Gateway->>IDaaS: POST /oauth2/token
    IDaaS-->>Gateway: 201 { access_token=tc_access_token }
    Gateway-->>User: 302 回到原聊天页 client_return_url
    User->>FE: 回到 Agent 页面
    FE->>BFF: 继续当前请求
end

BFF->>Gateway: POST /api/v1/runtime/tr/acquire { request_id }
Gateway->>IAM: POST /iam/projects/{proxy_project_id}/assume_agent_token
IAM-->>Gateway: 201 { access_token=t1_access_token }
Gateway->>IAM: POST /iam/auth/resource-token
IAM-->>Gateway: 201 { access_token=tr_access_token, refresh_token }
Gateway->>Policy: POST /internal/v1/tools/resolve-by-codes
Policy-->>Gateway: 200 { tool_mappings }
Gateway->>Gateway: 计算 effective_resources 并创建安全会话
Gateway-->>BFF: 200 { status=READY, request_id, security_session_id, tr_token }
BFF->>MCPGW: POST /api/v1/mcp/invoke
MCPGW->>MCPGW: POST /internal/v1/tokens/tr/introspect
MCPGW->>MCPS: POST /internal/v1/tools/invoke
MCPS-->>MCPGW: 200 { result }
MCPGW-->>BFF: 200 { result }
BFF-->>FE: 返回业务结果
FE-->>User: 返回业务结果
```

## 实现时的 4 条硬规则

- `security_request` 是唯一流程主记录，登录回调、授权回调、再次获取 `TR` 都围绕它推进
- `POST /api/v1/runtime/tr/acquire` 是接入 Agent 唯一的 `TR` 获取入口
- 安全会话只在首次进入 `READY` 后创建
- 浏览器回跳只负责让前端回到原聊天页，再由后端/BFF继续获取 `TR`
