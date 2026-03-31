# System Architecture: Zero Trust AI Agent Authentication & Authorization

## 1. 系统概述 (System Overview)
本项目是一个基于零信任架构 (Zero Trust) 的 AI Agent 认证鉴权系统。该系统旨在安全地处理用户、AI Agent（如 ERP 数字助手）、认证中心（IDaaS/IAM）以及后端资源服务（MCP Server）之间的复杂交互。
核心目标是实现**用户身份与 Agent 身份的安全解耦**，并通过**权限委托机制**确保后端资源服务器能精确识别“哪个 Agent 正在代表哪个 User”发起请求。

## 2. 核心架构标准 (Core Standards)
本架构严格遵循以下业界标准：
*   **OAuth 2.0 / OIDC (OpenID Connect)**: 用于用户身份认证和初步授权。
*   **RFC 8693 (OAuth 2.0 Token Exchange)**: 架构的灵魂，用于实现多重令牌的交换与权限委托。

## 3. 核心实体定义 (Glossary & Participants)
*   **用户 (User)**: 系统的最终使用者，拥有业务数据的访问权限。
*   **IDaaS (Identity as a Service)**: 负责用户的统一身份认证和用户维度权限授权。
*   **IAM (Identity and Access Management)**: 负责内部系统、网关、Agent 之间的身份校验、令牌签发与 Token Exchange。
*   **Agent网关**: 整个认证鉴权流程的调度核心，负责拦截请求、处理 OAuth 授权流并进行令牌交换。
*   **ERP数字助手Agent**: AI 智能体应用，负责理解用户意图并调用后端资源，需要临时“借用”用户的权限。
*   **MCP网关 & MCP Server**: 后端资源提供方（Model Context Protocol 或微服务），负责拦截校验最终令牌并返回行级/列级脱敏数据。

## 4. 架构时序图 (Sequence Diagram)
以下是系统核心交互的 Mermaid 时序图：

```mermaid
sequenceDiagram
title 用户到Agent认证鉴权时序图
participant 用户
participant IDaaS
participant Agent网关
participant IAM
participant ERP数字助手Agent
participant MCP网关
participant MCP server

ERP数字助手Agent->>Agent网关: 0.1 通过IDaas进行授权委托ERP Agent->Agent网关
ERP数字助手Agent->>Agent网关: 0.2 通过IAM进行授权委托ERP Agent->Agent网关

用户->>Agent网关: 1.1 用户请求Agent：分析12月财务报表
Agent网关->>Agent网关: 1.2 用户登录判断 (内部逻辑)
Agent网关->>用户: 1.3 重定向用户登录
用户->>IDaaS: 1.4 用户登录
IDaaS->>Agent网关: 1.5 重定向返回SessionID

Agent网关->>Agent网关: 2.1 依策略查询分级数据的策略权限
Agent网关->>用户: 2.2 用户同意查询分录数据的策略
用户->>IDaaS: 2.3 重定向用户授权
IDaaS->>Agent网关: 2.4 重定向返回授权码
Agent网关->>IDaaS: 2.5 请求用户令牌Tc(依赖0.1)
IDaaS->>Agent网关: 2.6 颁发用户令牌（Tc）

Agent网关->>IAM: 3.1 使用Agent网关令牌请求Agent令牌T1(依赖0.2)
IAM->>Agent网关: 3.2 返回Agent令牌（T1）
Agent网关->>IAM: 3.3 Tc+T1交换资源令牌TR
IAM->>Agent网关: 3.4 返回资源令牌（TR）
Agent网关->>ERP数字助手Agent: 3.5 传递资源令牌（TR）

Note over ERP数字助手Agent: 黄色标注框: sessionID 绑定资源令牌TR

ERP数字助手Agent->>MCP网关: 4.1 使用TR调用MCP server
MCP网关->>MCP server: 4.2 解码TR并检查权限
MCP server->>MCP网关: 4.3 查询有权限的分级数据
MCP网关->>ERP数字助手Agent: 4.4 返回分级数据
MCP网关->>用户: 4.5 经由Agent处理后返回给用户

用户->>ERP数字助手Agent: 5.1 第二次及后续请求，携带SessionID
ERP数字助手Agent->>ERP数字助手Agent: 5.2 通过SessionID获得资源令牌TR
ERP数字助手Agent->>MCP网关: 5.3 使用TR调用MCP server
MCP网关->>IAM: 5.4 检查TR有效性，权限是否匹配
IAM->>MCP网关: 5.4 检查结果返回
MCP网关->>MCP网关: 5.5 权限有效性处理