通过委托获取AI应用的应用令牌App Token(T1)
2026-04-02 17:13 由 蓝皓旻 00770285 创建，于 2026-04-03 14:32 由 蓝皓旻 00770285 最后修改。 内容存疑 点我
功能介绍
可通过委托获取AI应用的应用令牌App Token，前提要求已获得AI应用的程序集成账号的委托授权



常用IAMEndPoint如下，如需对接其他环境，可以→ 点击这里

本地测试环境：https://apig.hisuat.huawei.com

HIS3.0beta：https://iam.his-op-beta.huawei.com

HIS3.0生产环境（OP企业）：https://iam.his-op.huawei.com

HIS3.0生产环境（华技企业）：https://iam-blue.his.huawei.com



URI
POST /iam/projects/{代理_project_id}/assume_agent_token

请求示例
Postman调用示例：
curl --location --request POST 'https://iam-blue.his.huawei.com/iam/projects/{代理_project_id}/assume_agent_token' \

--header 'Content-Type: application/json' \

--data-raw '{

    "data": {

        "type": "assume_agent_token",

        "attributes": {

            "agent_service_account": "账号名称",

            "principal": "agent_app_id",

            "agent_id": "1231"

        }

    }

}'

成功返回报文如下：
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


请求参数：
名称	类型	位置	描述
agent_service_account	String	请求体	AI程序集成账号名名称
principal	String	请求体	需要获取的agent的appid
agent_id	String	请求体	agentId


成功响应参数：
名称	类型	描述
code	String	http状态码
message	String	状态信息，OK表示成功
enterprise	String	用户所属企业id
access_token	String	用户token
expires_in	String	过期时间（多少秒内过期）
expires_at	String	过期时间（具体过期时间）
expires_on	String	过期时间（过期时间戳）
token_type	String	token类型，只返回IAM_AI_SERVICE类型


payload参数：
名称	类型	描述
token_id	String	token的唯一id
iss	String
签发人 / 令牌发行者。指明该 token 由谁生成，通常是一个字符串或 URI，接收方可以据此验证 token 的来源是否可信。

sub	String	主题 / 面向的用户。标识 token 所代表的用户或实体，通常是一个唯一标识符（如用户 ID）。用于区分“这个 token 是给谁的”。
nbf	String	生效时间（Unix 时间戳，秒）。token 在 nbf 指定的时间之前不可接受（未生效）。即只有到达该时间点后才能使用该 token。
exp	String	过期时间（Unix 时间戳，秒）。token 在 exp 指定的时间之后必须拒绝。用于限制 token 的生命周期，提升安全性。
iat	String	签发时间（Unix 时间戳，秒）。表示 token 生成的具体时刻。可用于记录 token 年龄或配合其他逻辑（如允许一定时间偏移）。
jti	String	唯一标识符（UUID 格式）。为 token 分配一个全局唯一的 ID，主要用来防止重放攻击（服务端可缓存已使用的 jti，拒绝重复提交）。
name	String	程序集成账号名称
account_id	String	程序集成账号id
enterprise	String	企业id
account_type	String	账号类型，此接口只支持IAM_AI_SERVICE类型
project	String	appid
access_domain	String	高/中/低秘
agent	Object
{

"agent_id"：”"231213124", // agent唯一id

"agent_name": "ERP助手", // agent中文名称

"agent_type": "server/client" // agent类型(客户端还是服务端)

}

proxy_id	String	代理的appid，用于记录审计日志


失败响应参数：
名称	类型	描述
timestamp	String	时间戳
id	String	请求id
status	String	http状态码，401表示认证失败
method	String	原请求方法
path	String	原请求uri
title	String	错误标题
errors	Array	错误信息



获取AI应用的资源令牌Resource Token（TR）
2026-04-02 17:31 由 蓝皓旻 00770285 创建，于 2026-04-03 14:32 由 蓝皓旻 00770285 最后修改。 内容存疑 点我
功能介绍
支持AI应用在用户委托授权场景下通过用户令牌+应用令牌，获取代表AI双重身份的资源令牌Resource Token；

支持AI应用在自主创建下通过应用令牌，获取代表AI自有身份的资源令牌Resource Token



常用IAMEndPoint如下，如需对接其他环境，可以→ 点击这里

本地测试环境：https://apig.hisuat.huawei.com

HIS3.0beta：https://iam.his-op-beta.huawei.com

HIS3.0生产环境（OP企业）：https://iam.his-op.huawei.com

HIS3.0生产环境（华技企业）：https://iam-blue.his.huawei.com



URI
POST /iam/auth/resource-token

请求示例
Postman调用示例：
curl --location --request POST 'https://iam-blue.his.huawei.com/iam/auth/resource-token' \

--header 'Authorization: agent_token' \

--header 'Content-Type: application/json' \

--data-raw '{

    "data": {

        "type": "resource_token",

        "attributes": {

            "user_token": "1223123"

        }

    }

}'

成功返回报文如下：
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
"refresh_token": "11111111113",
"expires_on": 1775197488000
}


请求参数：
名称	类型	位置	描述
Authorization	String	请求头	agent_token(只支持AI应用类型的Token调用)
user_token	String	请求体	用户令牌Token（Tc）


成功响应参数：
名称	类型	描述
code	String	http状态码
message	String	状态信息，OK表示成功
enterprise	String	用户所属企业id
access_token	String	用户token
expires_in	String	过期时间（多少秒内过期）
expires_at	String	过期时间（具体过期时间）
expires_on	String	过期时间（过期时间戳）
token_type	String	token类型，只返回IAM_AI_SERVICE类型
refresh_token	String	长期令牌，刷新access_token使用


payload参数：
名称	类型	描述
token_id	String	token的唯一id
iss	String
签发人 / 令牌发行者。指明该 token 由谁生成，通常是一个字符串或 URI，接收方可以据此验证 token 的来源是否可信。

sub	String	主题 / 面向的用户。标识 token 所代表的用户或实体，通常是一个唯一标识符（如用户 ID）。用于区分“这个 token 是给谁的”。
nbf	String	生效时间（Unix 时间戳，秒）。token 在 nbf 指定的时间之前不可接受（未生效）。即只有到达该时间点后才能使用该 token。
exp	String	过期时间（Unix 时间戳，秒）。token 在 exp 指定的时间之后必须拒绝。用于限制 token 的生命周期，提升安全性。
iat	String	签发时间（Unix 时间戳，秒）。表示 token 生成的具体时刻。可用于记录 token 年龄或配合其他逻辑（如允许一定时间偏移）。
jti	String	唯一标识符（UUID 格式）。为 token 分配一个全局唯一的 ID，主要用来防止重放攻击（服务端可缓存已使用的 jti，拒绝重复提交）。
name	String	AI应用程序集成账号名称
account_id	String	AI应用程序集成账号id
enterprise	String	企业id
account_type	String	账号类型，固定为IAM_AI_SERVICE
project	String	AI应用所属的项目ID
access_domain	String	高/中/低秘
agent	Object
{

"agent_id"：”"231213124", // agent唯一id

"agent_name": "ERP助手", // agent中文名称

"agent_type": "server/client" // agent类型(客户端还是服务端)

}

agency_user	Object
代理的用户信息及授权信息

{

        "idp": "idaas", // 身份提供者

        "idp_id": "2134", // 身份提供者id

        "user_id": "uuid~1234556", // 用户uuid

        "global_user_id": "23131212121", // 用户id

        "ouath_client_id": "23123", // OAuth 客户端标识符

        "outh_client_app_id": "12312", //OAuth 客户端appid

        "scope": {

            "appid1": "xxx",  //示意，实际需要IDaaS确认

            "appid2": "xxx"   //示意，实际需要IDaaS确认

        } 

}



失败响应参数：
名称	类型	描述
timestamp	String	时间戳
id	String	请求id
status	String	http状态码，401表示认证失败
method	String	原请求方法
path	String	原请求uri
title	String	错误标题
errors	Array	错误信息


通过Refresh Token刷新Resource Token
2026-04-02 17:41 由 蓝皓旻 00770285 创建，于 2026-04-03 10:37 由 肖钦涛 00847094 最后修改。 内容存疑 点我
功能介绍
通过Refresh Token刷新Resource Token，获取最新有效的Resource Token和新的Refresh Token



常用IAMEndPoint如下，如需对接其他环境，可以→ 点击这里

本地测试环境：https://apig.hisuat.huawei.com

HIS3.0beta：https://iam.his-op-beta.huawei.com

HIS3.0生产环境（OP企业）：https://iam.his-op.huawei.com

HIS3.0生产环境（华技企业）：https://iam-blue.his.huawei.com



URI
POST /iam/auth/refresh-resource-token

请求示例
Postman调用示例：
curl --location --request POST 'https://iam-blue.his.huawei.com/iam/auth/refresh-resource-token' \

--header 'Content-Type: application/json' \

--data-raw '{

    "data": {

        "type": "token",

        "attributes": {

            "refresh_token": "1223123"

        }

    }

}'

成功返回报文如下：
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


请求参数：
名称	类型	位置	描述
refresh_token	String	请求体	refresh_token


成功响应参数：
名称	类型	描述
code	String	http状态码
message	String	状态信息，OK表示成功
enterprise	String	用户所属企业id
access_token	String	用户token
refresh_token	String	refresh_token
expires_in	String	过期时间（多少秒内过期）
expires_at	String	过期时间（具体过期时间）
expires_on	String	过期时间（过期时间戳）
token_type	String	token类型，只返回IAM_AI_SERVICE类型


payload参数：
名称	类型	描述
token_id	String	token的唯一id
iss	String
签发人 / 令牌发行者。指明该 token 由谁生成，通常是一个字符串或 URI，接收方可以据此验证 token 的来源是否可信。

sub	String	主题 / 面向的用户。标识 token 所代表的用户或实体，通常是一个唯一标识符（如用户 ID）。用于区分“这个 token 是给谁的”。
nbf	String	生效时间（Unix 时间戳，秒）。token 在 nbf 指定的时间之前不可接受（未生效）。即只有到达该时间点后才能使用该 token。
exp	String	过期时间（Unix 时间戳，秒）。token 在 exp 指定的时间之后必须拒绝。用于限制 token 的生命周期，提升安全性。
iat	String	签发时间（Unix 时间戳，秒）。表示 token 生成的具体时刻。可用于记录 token 年龄或配合其他逻辑（如允许一定时间偏移）。
jti	String	唯一标识符（UUID 格式）。为 token 分配一个全局唯一的 ID，主要用来防止重放攻击（服务端可缓存已使用的 jti，拒绝重复提交）。
name	String	程序集成账号名称
account_id	String	程序集成账号id
enterprise	String	企业id
account_type	String	账号类型，此接口只支持IAM_AI_SERVICE类型
project	String	appid
access_domain	String	高/中/低秘


失败响应参数：
名称	类型	描述
timestamp	String	时间戳
id	String	请求id
status	String	http状态码，401表示认证失败
method	String	原请求方法
path	String	原请求uri
title	String	错误标题
errors	Array	错误信息