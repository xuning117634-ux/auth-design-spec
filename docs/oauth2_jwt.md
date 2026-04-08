##说明
基于java：
<dependency>

<groupId>com.auth0</groupId>

<artifactId>java-jwt</artifactId>

<version>4.5.0</version>

</dependency>
算法版本：
{

"alg": "RS512",

"typ": "JWT"

}

### T1令牌文档

#### URI

POST /iam/projects/{代理_project_id}/assume_agent_token

#### 请求示例

1. Postman调用示例：
   curl --location --request POST 'https://iam-blue.his.huawei.com/iam/projects/{代理_project_id}/assume_agent_token' \

   --header 'Content-Type: application/json' \

   --data-raw '{

   ```
   "data": {
   
       "type": "assume_agent_token",
   
       "attributes": {
   
           "agent_service_account": "账号名称",
   
           "principal": "agent_app_id",
   
           "agent_id": "1231"
   
       }
   
   }
   ```

   }'
2. 成功返回报文如下：
   返回体：

   {
   "message": "OK",
   "code": "201",
   "enterprise": "11111111111111111111111111111111",
   "access_token": "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJpYW0iLCJzdWIiOiJjb20uaHVhd2VpLmhyLnRlc3QudWF0MTIyNTE3NTciLCJuYmYiOjE3NzUwMjQ2ODgsImV4cCI6MTc3NTE5NzQ4OCwiaWF0IjoxNzc1MTExMDg4LCJqdGkiOiI2YmZiZTk4YS0xZDczLTRiYzctYTc0NS02ZWZlOTRlOWUwZjkiLCJuYW1lIjoiY29tLmh1YXdlaS5oci50ZXN0LnVhdDEyMjUxNzU3IiwiYWNjb3VudF9pZCI6Ijg3M2E5NDIyOGM5ZWEyYWIwMThjYTA2YzQyZTQwMDBlIiwiZW50ZXJwcmlzZSI6IjExMTExMTExMTExMTExMTExMTExMTExMTExMTExMTExIiwiYWNjb3VudF90eXBlIjoiSUFNX1NFUlZJQ0UiLCJwcm9qZWN0IjoiY29tLmh1YXdlaS5oci50ZXN0LnVhdDEyMjUxNzU3IiwiYWNjZXNzX2RvbWFpbiI6Im1pZGRsZS1zZWNyZXQifQ.jKwgs6ssqdyD2vGGjtCeBt5smqckbzm8Dj5CWv0WRCyVEV_7UXe7FbJNFy5LweTW22J5-sxHhOnJDzpSbzHUfhwF0euc4OudpAEjKRUqELWyKd8AHeH_4wComE6ulK-AZJyUkPDxyILM8bUEqU3Aq40rudfYy-yS68vai0JRDzvI9AsAQgQKR-1qNB_4xhGR8qmuQVRoS3vVvO4A6RWATZmWw6pgFt6kdDBjI-P_QIg--EU6sdYzq7xDB3Ok3XCluyWgqJODpld0nK8Bkt6vybf5nL58xUV0Z-iOoDncxy33OM2SGj9aCqbNFvx_nN6O2D7akmc2Yoy2nrwB12aa7Q",
   "expires_at": "2026-04-03T14:24:48+08:00",
   "expires_in": 86399,
   "token_id": "1234",
   "token_type": "IAM_AI_SERVICE",
   "expires_on": 1775197488000
   }

#### 请求参数：

| 名称                    | 类型   | 位置   | 描述                   |
| ------------------------- | -------- | -------- | ------------------------ |
| agent_service_account | String | 请求体 | AI程序集成账号名名称   |
| principal               | String | 请求体 | 需要获取的agent的appid |
| agent_id               | String | 请求体 | agentId                |

#### 成功响应参数：

| 名称          | 类型   | 描述                                  |
| --------------- | -------- | --------------------------------------- |
| code          | String | http状态码                            |
| message       | String | 状态信息，OK表示成功                  |
| enterprise    | String | 用户所属企业id                        |
| access_token | String | 用户token                             |
| expires_in   | String | 过期时间（多少秒内过期）              |
| expires_at   | String | 过期时间（具体过期时间）              |
| expires_on   | String | 过期时间（过期时间戳）                |
| token_type   | String | token类型，只返回IAM_AI_SERVICE类型 |

#### payload参数：

| 名称           | 类型   | 描述                                                                                                                                                |
| ---------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| token_id      | String | token的唯一id                                                                                                                                       |
| iss            | String | 签发人 / 令牌发行者。指明该 token 由谁生成，通常是一个字符串或 URI，接收方可以据此验证 token 的来源是否可信。                                       |
| sub            | String | 主题 / 面向的用户。标识 token 所代表的用户或实体，通常是一个唯一标识符（如用户 ID）。用于区分“这个 token 是给谁的”。                              |
| nbf            | String | 生效时间（Unix 时间戳，秒）。token 在`nbf`指定的时间之前​**不可接受**​（未生效）。即只有到达该时间点后才能使用该 token。                |
| exp            | String | 过期时间（Unix 时间戳，秒）。token 在`exp`指定的时间之后​**必须拒绝**​。用于限制 token 的生命周期，提升安全性。                         |
| iat            | String | 签发时间（Unix 时间戳，秒）。表示 token 生成的具体时刻。可用于记录 token 年龄或配合其他逻辑（如允许一定时间偏移）。                                 |
| jti            | String | 唯一标识符（UUID 格式）。为 token 分配一个全局唯一的 ID，主要用来​**防止重放攻击**​（服务端可缓存已使用的 jti，拒绝重复提交）。             |
| name           | String | 程序集成账号名称                                                                                                                                    |
| account_id    | String | 程序集成账号id                                                                                                                                      |
| enterprise     | String | 企业id                                                                                                                                              |
| account_type  | String | 账号类型，此接口只支持IAM_AI_SERVICE类型                                                                                                          |
| project        | String | appid                                                                                                                                               |
| access_domain | String | 高/中/低秘                                                                                                                                          |
| agent          | Object | {"agent_id"：”"231213124", // agent唯一id"agent_name": "ERP助手", // agent中文名称"agent_type": "server/client" // agent类型(客户端还是服务端)} |
| proxy_id      | String | 代理的appid，用于记录审计日志                                                                                                                       |

#### 失败响应参数：

| 名称      | 类型   | 描述                        |
| ----------- | -------- | ----------------------------- |
| timestamp | String | 时间戳                      |
| id        | String | 请求id                      |
| status    | String | http状态码，401表示认证失败 |
| method    | String | 原请求方法                  |
| path      | String | 原请求uri                   |
| title     | String | 错误标题                    |
| errors    | Array  | 错误信息                    |

### Tc令牌文档

暂未设计响应体，你可以参考T1设计

#### Payload 示例

```json
{
  "token_id": "utk_7c3f2e1d8b9a0654",
  "issuer": "idaas相关地址",
  "intended_recipient": "agent-gateway",
  "issued_at": "2026-03-19T06:30:00Z",
  "expires_at": "2026-03-19T07:30:00Z",
  "user": {
    "user_id": "z01062668",
    "uuid": "uuid-lj12541d2a461fdf",
    "username": "张建国",
    "email": "jianguo.zhang@huawei.com",
    "department": "财经财务管理部"
  },
  "consented_scopes": [
    "erp:report:read"
  ]
}
```

#### 字段说明

| 字段 | 类型 | 必选 | 含义 | 业务作用 |
|---|---|---|---|---|
| `token_id` | string | 是 | Token 唯一 ID | 防止同一个 Token 被重复提交（防重放），同时用于审计追踪 |
| `issuer` | string | 是 | 签发者 — IDaaS 的地址 | 校验这个 Token 确实是 IDaaS 发出来的，防伪造 |
| `intended_recipient` | string | 是 | 目标接收方，值为 `agent-gateway` | Agent 网关收到后检查此值是否等于自己的标识，不是就拒绝，防止 Token 被拿去别的系统冒用 |
| `issued_at` | string | 是 | 签发时间（ISO 8601） | 配合 `expires_at` 判断是否过期 |
| `expires_at` | string | 是 | 过期时间（ISO 8601） | 过期后用户需重新授权 |
| `user` | object | 是 | 用户信息对象 | 用对象承载真实用户身份信息，便于结构化传递和扩展 |
| `user.user_id` | string | 是 | 用户工号 | 标识发起请求的真实用户，后续被继承到 TR |
| `user.uuid` | string | 是 | 用户全局唯一标识 | 跨系统关联用户身份的不可变 ID |
| `user.username` | string | 是 | 用户姓名 | 审计日志和前端展示 |
| `user.email` | string | 否 | 用户邮箱 | 通知或展示 |
| `user.department` | string | 否 | 用户所属部门 | MCP Server 可据此做部门级数据隔离 |
| `consented_scopes` | string[] | 是 | **用户同意授权的策略 code 集合** | 每个 code 对应 Agent 策略中心中的一条策略，如 `erp:report:read`。它表达的是“用户允许执行哪些操作”，**具体是写工具名还是策略code待定** |

### TR令牌文档

#### URI

POST /iam/auth/resource-token

#### 请求示例

1. Postman调用示例：
   curl --location --request POST 'https://iam-blue.his.huawei.com/iam/auth/resource-token' \

   --header 'Authorization: agent_token' \

   --header 'Content-Type: application/json' \

   --data-raw '{

   ```
   "data": {
   
       "type": "resource_token",
   
       "attributes": {
   
           "user_token": "1223123"
   
       }
   
   }
   ```

   }'
2. 成功返回报文如下：
   返回体：

   {
   "message": "OK",
   "code": "201",
   "enterprise": "11111111111111111111111111111111",
   "refresh_token": "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJpYW0iLCJzdWIiOiJjb20uaHVhd2VpLmhyLnRlc3QudWF0MTIyNTE3NTciLCJuYmYiOjE3NzUwMjQ2ODgsImV4cCI6MTc3NTE5NzQ4OCwiaWF0IjoxNzc1MTExMDg4LCJqdGkiOiI2YmZiZTk4YS0xZDczLTRiYzctYTc0NS02ZWZlOTRlOWUwZjkiLCJuYW1lIjoiY29tLmh1YXdlaS5oci50ZXN0LnVhdDEyMjUxNzU3IiwiYWNjb3VudF9pZCI6Ijg3M2E5NDIyOGM5ZWEyYWIwMThjYTA2YzQyZTQwMDBlIiwiZW50ZXJwcmlzZSI6IjExMTExMTExMTExMTExMTExMTExMTExMTExMTExMTExIiwiYWNjb3VudF90eXBlIjoiSUFNX1NFUlZJQ0UiLCJwcm9qZWN0IjoiY29tLmh1YXdlaS5oci50ZXN0LnVhdDEyMjUxNzU3IiwiYWNjZXNzX2RvbWFpbiI6Im1pZGRsZS1zZWNyZXQifQ.jKwgs6ssqdyD2vGGjtCeBt5smqckbzm8Dj5CWv0WRCyVEV_7UXe7FbJNFy5LweTW22J5-sxHhOnJDzpSbzHUfhwF0euc4OudpAEjKRUqELWyKd8AHeH_4wComE6ulK-AZJyUkPDxyILM8bUEqU3Aq40rudfYy-yS68vai0JRDzvI9AsAQgQKR-1qNB_4xhGR8qmuQVRoS3vVvO4A6RWATZmWw6pgFt6kdDBjI-P_QIg--EU6sdYzq7xDB3Ok3XCluyWgqJODpld0nK8Bkt6vybf5nL58xUV0Z-iOoDncxy33OM2SGj9aCqbNFvx_nN6O2D7akmc2Yoy2nrwB12aa7Q",
   "expires_at": "2026-04-03T14:24:48+08:00",
   "expires_in": 86399,
   "token_id": "1234",
   "token_type": "IAM_AI_SERVICE",
   "refresh_token": "11111111113",
   "expires_on": 1775197488000
   }

#### 请求参数：

| 名称          | 类型   | 位置   | 描述                                        |
| --------------- | -------- | -------- | --------------------------------------------- |
| Authorization | String | 请求头 | agent_tokenT1(只支持AI应用类型的Token调用) |
| user_token   | String | 请求体 | 用户令牌Token（Tc）                         |

#### 成功响应参数：

| 名称           | 类型   | 描述                                  |
| ---------------- | -------- | --------------------------------------- |
| code           | String | http状态码                            |
| message        | String | 状态信息，OK表示成功                  |
| enterprise     | String | 用户所属企业id                        |
| access_token  | String | 用户token                             |
| expires_in    | String | 过期时间（多少秒内过期）              |
| expires_at    | String | 过期时间（具体过期时间）              |
| expires_on    | String | 过期时间（过期时间戳）                |
| token_type    | String | token类型，只返回IAM_AI_SERVICE类型 |
| refresh_token | String | 长期令牌，刷新access_token使用       |

#### payload参数：

| 名称           | 类型   | 描述                                                                                                                                                                                                                                                                                                                            |
| ---------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| token_id      | String | token的唯一id                                                                                                                                                                                                                                                                                                                   |
| iss            | String | 签发人 / 令牌发行者。指明该 token 由谁生成，通常是一个字符串或 URI，接收方可以据此验证 token 的来源是否可信。                                                                                                                                                                                                                   |
| sub            | String | 主题 / 面向的用户。标识 token 所代表的用户或实体，通常是一个唯一标识符（如用户 ID）。用于区分“这个 token 是给谁的”。                                                                                                                                                                                                          |
| nbf            | String | 生效时间（Unix 时间戳，秒）。token 在`nbf`指定的时间之前​**不可接受**​（未生效）。即只有到达该时间点后才能使用该 token。                                                                                                                                                                                            |
| exp            | String | 过期时间（Unix 时间戳，秒）。token 在`exp`指定的时间之后​**必须拒绝**​。用于限制 token 的生命周期，提升安全性。                                                                                                                                                                                                     |
| iat            | String | 签发时间（Unix 时间戳，秒）。表示 token 生成的具体时刻。可用于记录 token 年龄或配合其他逻辑（如允许一定时间偏移）。                                                                                                                                                                                                             |
| jti            | String | 唯一标识符（UUID 格式）。为 token 分配一个全局唯一的 ID，主要用来​**防止重放攻击**​（服务端可缓存已使用的 jti，拒绝重复提交）。                                                                                                                                                                                         |
| name           | String | AI应用程序集成账号名称                                                                                                                                                                                                                                                                                                          |
| account_id    | String | AI应用程序集成账号id                                                                                                                                                                                                                                                                                                            |
| enterprise     | String | 企业id                                                                                                                                                                                                                                                                                                                          |
| account_type  | String | 账号类型，固定为IAM_AI_SERVICE                                                                                                                                                                                                                                                                                                |
| project        | String | AI应用所属的项目ID                                                                                                                                                                                                                                                                                                              |
| access_domain | String | 高/中/低秘                                                                                                                                                                                                                                                                                                                      |
| agent          | Object | {"agent_id"：”"231213124", // agent唯一id"agent_name": "ERP助手", // agent中文名称"agent_type": "server/client" // agent类型(客户端还是服务端)}                                                                                                                                                                             |
| agency_user   | Object | 代理的用户信息及授权信息{"idp": "idaas", // 身份提供者"idp_id": "2134", // 身份提供者id"user_id": "uuid\~1234556", // 用户uuid"global_user_id": "23131212121", // 用户id"ouath_client_id": "23123", // OAuth 客户端标识符"outh_client_app_id": "12312", //OAuth 客户端appid"consented_scopes": [ "erp:report:read" ]} |

#### 失败响应参数：

| 名称      | 类型   | 描述                        |
| ----------- | -------- | ----------------------------- |
| timestamp | String | 时间戳                      |
| id        | String | 请求id                      |
| status    | String | http状态码，401表示认证失败 |
| method    | String | 原请求方法                  |
| path      | String | 原请求uri                   |
| title     | String | 错误标题                    |
| errors    | Array  | 错误信息                    |
