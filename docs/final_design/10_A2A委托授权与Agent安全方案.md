# A2A 委托授权与 Agent 安全方案

本文说明从“用户授权单个 Agent”扩展到“Agent A 委托 Agent B 执行子任务”时，人在环授权、委托凭据、运行时鉴权和审计的整体设计。

## 1. 方案概述

A2A 不采用 Agent A 直接转发自身 `TR` 给 Agent B 的方式，而是由 Agent 网关统一生成、校验和审计一条清晰的委托链路：

```text
用户 U -> Agent A -> Agent B -> MCP 工具/资源
```

设计原则：

- 用户授权仍然是根权限来源。
- Agent A 是用户当前会话和人在环交互的承接方。
- Agent B 是被委托执行者，不直接跳转用户浏览器。
- Agent 网关位于 Agent A 与 Agent B 之间，负责 A2A 标准协议通讯、流量代理、授权代理和链路审计。
- Agent B 不需要在调用前拿到全量工具权限；它在运行时规划工具，发现缺权限后把所需工具、权限点和任务上下文返回给 Agent A。
- Agent A 基于 Agent B 返回的缺权限信息，请求 Agent 网关发起人在环授权。
- Agent 网关负责授权页、IDaaS/IAM 交互、委托策略校验、短期 `delegated_TR` 签发和审计。
- 最终 `delegated_TR` 只发给 Agent B，`aud=B`，不经过浏览器，也不由 Agent A 转交。

## 2. 单 Agent 授权方案的局限

当前单 Agent 场景中：

```text
用户 U 授权 Agent A 使用某些权限点，Agent A 持有 aud=A 的 TR 调 MCP。
```

A2A 场景多了一层委托关系：

```text
用户 U 正在使用 Agent A，但具体子任务由 Agent B 执行。
```

如果 Agent A 直接把自己的 `TR` 交给 Agent B，存在以下风险：

- `TR.aud=A`，但真实执行者是 B，身份语义错位。
- MCP 和资源侧只能看到 `用户 -> A`，看不到 `用户 -> A -> B`。
- 无法限制 B 只能在本次子任务、指定权限范围内执行。
- 审计、风控、责任归属会失真。

因此 A2A 必须引入短期、定向、可审计的委托凭据。

## 3. 核心对象

| 对象 | 含义 | 主要职责 |
| --- | --- | --- |
| 用户 U | 最终资源访问权限来源 | 在 Agent A 会话中完成补充授权 |
| Agent A | 发起委托的 Agent | 承接用户会话、经 Agent 网关调用 B、处理 A2A SDK 授权钩子 |
| Agent 网关 | A2A 授权控制面 | 代理 A/B 通讯、发起人在环授权、对接 IDaaS/IAM、签发 `delegated_TR`、审计 |
| Agent B | 被委托执行的 Agent | 规划子任务、发现缺权限、返回缺权限授权信息、恢复执行 |
| 策略中心 | 策略裁决中心 | 判断 A 是否可委托 B、B 是否具备能力、权限是否越界 |
| MCP 网关 | 工具运行时网关 | 将工具映射到权限点，校验 `delegated_TR` 和权限点 |

## 4. 总体架构

```mermaid
flowchart LR
    User["用户 U"]
    A["Agent A<br/>用户会话与编排方"]
    GW["Agent 网关<br/>A2A 协议与授权代理"]
    B["Agent B<br/>被委托执行方"]
    PC["策略中心<br/>委托与权限裁决"]
    ID["IDaaS / IAM<br/>登录授权与令牌交换"]
    MCP["MCP 网关<br/>工具运行时校验"]
    Tool["业务工具 / 资源"]

    User <-->|"聊天 / 人在环确认"| A
    A -->|"A2A 调用 / AUTH_REQUIRED 处理"| GW
    GW -->|"代理转发 A2A 子任务"| B
    B -->|"返回缺权限授权信息"| GW
    GW -->|"透传给 Agent A"| A
    A -->|"请求发起人在环授权"| GW
    GW -->|"委托策略裁决"| PC
    GW -->|"对接 IDaaS / IAM"| ID
    GW -->|"授权完成后签发 delegated_TR"| B
    B -->|"携带 delegated_TR 调工具"| MCP
    MCP -->|"权限点 / 工具映射校验"| PC
    MCP --> Tool
```

## 5. A2A 缺权限授权信息定义

`A2A 缺权限授权信息` 是 Agent B 在运行时返回给 Agent A 的标准化协议响应，用来表达：

```text
Agent B 在执行 Agent A 委托的子任务时，发现继续执行需要用户补充授权。
```

它不是最终授权结果，也不是 `TR`，也不是由 Agent B 在 Agent 网关侧创建的持久授权记录。它的作用是让 Agent A 明确知道“缺什么权限、为什么缺、属于哪个任务、后续如何恢复”，再由 Agent A 请求 Agent 网关发起人在环授权。

参考字段：

```json
{
  "type": "A2A_AUTH_REQUIRED",
  "task_id": "task_789",
  "user_id": "user_001",
  "caller_agent_id": "agt_a",
  "callee_agent_id": "agt_b",
  "required_permission_points": ["erp:invoice:r"],
  "required_tools": [
    {
      "tool_id": "mcp:invoice-server/query_invoice",
      "tool_name": "发票查询",
      "tool_description": "读取用户授权范围内的发票数据"
    }
  ],
  "reason": "Agent B needs invoice read permission to complete invoice analysis.",
  "continuation_ref": "cont_b_001"
}
```

Agent B 返回该信息的目的：

- 固化“谁委托谁、为哪个用户、缺什么权限、哪个任务需要恢复”。
- 让 Agent A 通过固定 A2A SDK/钩子识别缺权限中间态。
- 让 Agent A 把所需工具列表、工具名称、工具描述、任务上下文等信息提交给 Agent 网关。
- 让 Agent 网关统一生成授权页、完成 IDaaS/IAM 交互，并在授权完成后签发短期 `delegated_TR`。

## 6. 正常 A2A 调用流程

如果 Agent B 执行过程中没有缺少额外授权，流程如下：

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户 U
    participant A as Agent A
    participant GW as Agent 网关
    participant B as Agent B
    participant MCP as MCP 网关
    participant PC as 策略中心
    participant T as 工具/资源

    U->>A: 发起任务
    A->>GW: A2A 调用子任务，携带委托上下文
    GW->>B: 代理转发到 Agent B
    B->>MCP: 按自身规划调用工具
    MCP->>PC: 校验 B 的能力、A->B 委托策略、用户权限上限
    PC-->>MCP: PERMIT
    MCP->>T: 执行工具调用
    T-->>MCP: 返回工具结果
    MCP-->>B: 返回结果
    B-->>GW: 子任务完成
    GW-->>A: 返回子任务结果
    A-->>U: 汇总回复用户
```

## 7. 缺权限时的人在环流程

缺权限时，Agent B 不直接找用户，也不直接生成授权页，也不在 Agent 网关侧创建持久授权记录。Agent B 将缺权限所需信息作为标准 `AUTH_REQUIRED` 中间态返回给 Agent A；Agent A 再请求 Agent 网关发起人在环授权。

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户 U
    participant A as Agent A
    participant GW as Agent 网关
    participant B as Agent B
    participant PC as 策略中心
    participant ID as IDaaS / IAM
    participant MCP as MCP 网关
    participant T as 工具/资源

    U->>A: 请求完成复杂任务
    A->>GW: A2A 调用子任务
    GW->>B: 代理转发子任务
    B->>MCP: 尝试调用 invoice.query
    MCP-->>B: AUTH_REQUIRED，缺 erp:invoice:r

    B-->>GW: 返回缺权限授权信息
    GW-->>A: 透传 A2A_AUTH_REQUIRED
    A->>GW: 请求发起人在环授权
    GW->>PC: 校验 A 是否可委托 B、B 是否具备能力、权限是否越界
    PC-->>GW: 允许进入用户授权
    GW-->>A: authorize_url + display_summary
    A-->>U: 展示授权提示并引导跳转
    U->>GW: 在授权页确认允许 A 本次委托 B
    GW->>ID: 与 IDaaS/IAM 完成 Tc、T1、TR 交换
    ID-->>GW: 返回短期 delegated_TR 所需结果
    GW-->>A: 回调一次性 ticket / 授权完成结果
    A->>GW: exchangeA2aAuthTicket(ticket)
    GW-->>A: 授权完成，返回 auth_flow_id

    A->>GW: 请求恢复子任务
    GW->>B: 转发恢复指令并交付 delegated_TR
    B->>MCP: 携带 delegated_TR 重新调用 invoice.query
    MCP->>PC: 运行时校验 user + A + B + 权限点
    PC-->>MCP: PERMIT
    MCP->>T: 执行工具调用
    T-->>MCP: 返回工具结果
    MCP-->>B: 返回结果
    B-->>GW: 子任务完成
    GW-->>A: 返回子任务结果
    A-->>U: 汇总回复
```

为便于快速理解，下面给出不展开接口细节的简化时序：

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant A as Agent A
    participant GW as Agent 网关
    participant B as Agent B
    participant MCP as MCP / 工具

    U->>A: 提出任务
    A->>GW: 发起 A2A 委托
    GW->>B: 代理转发子任务
    B->>MCP: 执行时发现需要额外权限
    B-->>GW: 返回缺权限授权信息
    GW-->>A: 透传需要用户授权
    A-->>U: 说明 B 需要什么权限
    A->>GW: 请求发起人在环授权
    U->>GW: 在授权页确认或拒绝
    GW-->>A: 告知授权结果
    A->>GW: 请求继续或终止
    GW->>B: 转发恢复指令和短期委托凭据
    B->>MCP: 继续调用工具
    B-->>GW: 返回子任务结果
    GW-->>A: 返回子任务结果
    A-->>U: 汇总最终结果
```

## 8. Agent A 固定钩子机制

Agent A 不能依赖模型对自然语言响应进行推断。A2A 缺权限必须表达为协议级中间态，由固定 SDK/钩子处理。

Agent B 返回：

```json
{
  "type": "A2A_AUTH_REQUIRED",
  "caller_agent_id": "agt_a",
  "callee_agent_id": "agt_b",
  "task_id": "task_789",
  "required_permission_points": ["erp:invoice:r"],
  "required_tools": [
    {
      "tool_id": "mcp:invoice-server/query_invoice",
      "tool_name": "发票查询",
      "tool_description": "读取用户授权范围内的发票数据"
    }
  ],
  "resume_mode": "agent_callback"
}
```

Agent A 的 A2A SDK 钩子固定处理：

```text
收到 A2A_AUTH_REQUIRED
-> 携带缺权限授权信息请求 Agent 网关发起人在环授权
-> 拿 authorize_url 和展示摘要
-> 返回前端需要用户处理
-> 授权完成后经 Agent 网关 resume Agent B
```

这样 Agent A 不需要理解 Agent B 的具体工具细节，也不需要自己拼授权页。

## 9. delegated_TR 的生成维度和内容

`delegated_TR` 按一次 A2A 授权结果生成，不是按 Agent B 全量能力生成。

生成维度：

```text
user + caller_agent + callee_agent + delegation_id + auth_flow_id + task_id + granted_permission_points + ttl
```

示例：

```json
{
  "iss": "agent-gateway",
  "aud": "agt_b",
  "sub": "user_001",
  "token_type": "delegated_tr",
  "iat": 1710000000,
  "exp": 1710000300,
  "agency_user": {
    "user": {
      "uid": "user_001"
    },
    "consented_scopes": ["erp:invoice:r"]
  },
  "delegation": {
    "caller_agent_id": "agt_a",
    "callee_agent_id": "agt_b",
    "delegation_id": "dlg_456",
    "auth_flow_id": "a2a_auth_flow_123",
    "task_id": "task_789",
    "grant_type": "one_time",
    "actor_chain": [
      {"type": "user", "id": "user_001"},
      {"type": "agent", "id": "agt_a"},
      {"type": "agent", "id": "agt_b"}
    ]
  }
}
```

关键规则：

- `aud` 必须是 Agent B。
- `consented_scopes` 是本次 A 委托 B 可使用的权限点交集。
- `delegated_TR` 不经过浏览器。
- Agent A 不接触 Agent B 的最终 `delegated_TR`。
- Agent 网关根据 Agent A 发起的授权结果和 Agent B 的执行主体，向 Agent B 交付面向 B 的短期 `delegated_TR`。

## 10. Agent 网关职责边界

Agent 网关在 A2A 中承担授权控制面职责：

| 职责 | 说明 |
| --- | --- |
| A2A 通讯代理 | 位于 Agent A 与 Agent B 之间，代理标准 A2A 请求、响应和恢复指令 |
| 发起人在环授权 | 接收 Agent A 提交的缺权限授权信息，生成授权页和跳转信息 |
| 生成授权页信息 | 返回 `authorize_url`、展示摘要、过期时间 |
| 承接用户授权 | 展示“允许 A 本次委托 B 使用某权限点”的授权页 |
| 对接 IDaaS/IAM | 完成 Tc、T1、TR 交换；长期授权记录仍由 IDaaS 作为权威保存 |
| 校验委托策略 | 调策略中心判断 A 是否可委托 B、权限是否越界 |
| 签发 `delegated_TR` | 按执行主体向 B 签发短期、定向、可审计的运行时令牌 |
| 恢复编排 | 支持 Agent A 经 Agent 网关恢复或终止 Agent B 子任务 |
| 审计 | 记录 `用户 -> A -> B -> 权限点/工具/资源` 完整链路 |

## 11. 状态机

```mermaid
stateDiagram-v2
    [*] --> Running: B 执行子任务
    Running --> Completed: 无需补授权
    Running --> AuthRequired: 发现缺权限点
    AuthRequired --> ReturnedToA: B 返回缺权限授权信息
    ReturnedToA --> WaitingUserConsent: A 请求网关发起授权
    WaitingUserConsent --> AuthGranted: 用户同意
    WaitingUserConsent --> AuthDenied: 用户拒绝
    WaitingUserConsent --> Expired: 授权流程过期
    AuthGranted --> TokenIssued: 网关签发 delegated_TR
    TokenIssued --> Resumed: 网关恢复 B 子任务
    Resumed --> Completed: 子任务完成
    AuthDenied --> Failed: A 降级或终止主任务
    Expired --> Failed: A 降级或重试授权
    Completed --> [*]
    Failed --> [*]
```

## 12. 策略裁决口径

A2A 运行时有效权限取交集：

```text
effective_scopes =
  用户对 A 的原始授权
  ∩ A 可委托 B 的策略范围
  ∩ B 已订阅/具备的能力范围
  ∩ 本次用户同意的授权范围
  ∩ MCP 工具映射出的权限点
```

策略中心可继续沿用 PARC 思路：

| PARC | A2A 中的含义 |
| --- | --- |
| Principal | 用户 U，或发起委托的 Agent A |
| Action | `DELEGATE_AGENT`、`USE_PERMISSION_POINT`、`CALL_MCP_TOOL` |
| Resource | Agent B、权限点、MCP 工具 |
| Context | callerAgentId、calleeAgentId、taskType、delegationDepth、enterprise |

## 13. 安全底线

- 禁止 Agent A 直接转发 `aud=A` 的 `TR` 给 Agent B。
- `delegated_TR` 必须短期有效，并绑定 `aud=B`。
- 权限只能收敛，不能扩张。
- Agent B 领取 `delegated_TR` 时必须使用自身服务身份。
- 用户授权页必须明确展示“Agent A 正在委托 Agent B”。
- 默认只允许一跳委托；多跳委托需要显式策略开启。
- 所有运行时调用必须能审计到 `user -> A -> B -> tool/resource`。

## 14. 方案要点总结

1. 单 Agent 授权解决的是“用户能不能让这个 Agent 访问资源”；A2A 解决的是“这个 Agent 能不能再委托另一个 Agent 替用户做事”。
2. Agent 之间不直接转发用户令牌，由 Agent 网关生成一次性、短期、定向的委托凭据，确保权限不扩张、链路可审计。
3. Agent B 只有在真正规划到具体工具时才知道缺什么权限；这时它把所需工具、权限点和任务上下文返回给 Agent A，Agent A 请求 Agent 网关发起授权，Agent 网关负责 IDaaS/IAM 交互和凭据签发。
