# T2：JWT 身份认证测试报告

## 1. 测试基本信息

| 项目     | 内容                                      |
| -------- | ----------------------------------------- |
| 测试阶段 | T2：JWT 身份认证测试                      |
| 测试对象 | simple-springboot-crud                    |
| 测试方式 | Spring Boot 集成测试                      |
| 测试框架 | JUnit 5、MockMvc、Spring Boot Test        |
| 测试环境 | application-test.properties               |
| 测试目标 | 验证 JWT 登录签发、身份认证及用户数据访问 |

## 2. 测试目的

验证系统能否正确处理 JWT 身份认证，重点检查：

1. 用户使用正确账号密码登录后，系统是否成功签发 JWT。
2. 客户端携带有效 JWT 时，是否能够访问受保护接口。
3. 用户携带有效 JWT 查询用户资料时，系统是否能够正常返回数据。

## 3. 测试准备

新增测试类：

```
src/test/java/com/example/crud/security/JwtAuthenticationTest.java
```

测试采用：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`

测试过程中准备独立测试账号：

| 字段   | 测试数据                                                     |
| ------ | ------------------------------------------------------------ |
| 用户名 | jwt_auth_test_user                                           |
| 密码   | JwtTestPass123!                                              |
| 角色   | USER                                                         |
| 邮箱   | [jwt-auth-test@example.test](mailto:jwt-auth-test@example.test) |

测试账号密码通过 BCrypt 加密后保存。

测试用户资料与账号建立关联，通过数据库保存后的实体 ID 获取 `ownedProfileId`，不使用固定 ID。

## 4. 测试用例及结果

| 编号   | 测试内容                                            | 预期结果                   | 实际结果 |
| ------ | --------------------------------------------------- | -------------------------- | -------- |
| JWT-01 | 正确账号密码登录                                    | HTTP 200，返回 JWT         | 通过     |
| JWT-02 | 携带有效 JWT 访问 `GET /api/users`                  | HTTP 200，返回用户列表     | 通过     |
| JWT-03 | 携带有效 JWT 访问 `GET /api/users/{ownedProfileId}` | HTTP 200，返回对应用户资料 | 通过     |

### JWT-01：正确账号密码登录

测试过程：

使用测试账号和正确密码请求登录接口。

预期结果：

- HTTP 状态码为 200。
- 登录成功。
- 响应中包含 JWT。

测试结论：通过。

### JWT-02：有效 JWT 访问用户列表

测试过程：

1. 使用测试账号登录，获取 JWT。
2. 在请求头中添加：

```
Authorization: Bearer {token}
```

1. 请求 `GET /api/users`。

预期结果：

- HTTP 状态码为 200。
- 系统识别 JWT 对应的用户身份。
- 返回用户列表。

测试结论：通过。

### JWT-03：有效 JWT 查询用户资料

测试过程：

1. 创建测试账号及关联用户资料。
2. 保存用户资料后获取数据库生成的 ID。
3. 使用有效 JWT 请求 `GET /api/users/{ownedProfileId}`。

预期结果：

- HTTP 状态码为 200。
- 返回对应用户资料。

测试结论：通过。

## 5. 测试执行命令

```
mvn "-Dtest=JwtAuthenticationTest" "-DforkCount=0" test
```

## 6. 测试结论

目前已确认：

- 正确账号密码能够完成登录并获取 JWT。
- 携带有效 JWT 能够访问受保护的用户列表接口。
- 携带有效 JWT 能够访问受保护的用户自己的数据。

本阶段主要验证 JWT 正向认证流程，不代表已经覆盖伪造 Token、篡改签名、Token 过期、异常 Authorization 请求头等负向场景。

上述负向场景将在 T3 阶段进一步测试。

## 7. 下一阶段

**T3：JWT 异常与过期测试**

重点验证：

- 伪造 JWT 是否被拒绝。
- 篡改 Payload 是否被拒绝。
- 篡改 Signature 是否被拒绝。
- 过期 JWT 是否被拒绝。
- 异常 Authorization 请求头是否被正确处理。