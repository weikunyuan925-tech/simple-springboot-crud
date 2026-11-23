# T3：JWT 异常与过期测试报告

## 一、测试目的

验证系统对异常 JWT、被篡改 JWT 和过期 JWT 的识别与拒绝能力，检查是否存在通过伪造或篡改 Token 绕过身份认证的风险。

本轮测试仅验证，不修改生产代码。

## 二、测试环境

| 项目       | 内容                        |
| ---------- | --------------------------- |
| 项目       | simple-springboot-crud      |
| 框架       | Spring Boot 3.2.0           |
| Java       | 17                          |
| JWT 组件   | JJWT 0.13.0                 |
| 测试框架   | JUnit 5 + MockMvc           |
| 测试配置   | application-test.properties |
| 测试数据库 | H2 内存数据库               |
| 测试接口   | GET /api/users              |

## 三、测试用例及结果

| 编号    | 测试场景                     | 预期结果 | 实际结果 | 结论 |
| ------- | ---------------------------- | -------- | -------- | ---- |
| JWT-N01 | 完全伪造 Token               | 401      | 401      | 通过 |
| JWT-N02 | 篡改 JWT Payload             | 401      | 401      | 通过 |
| JWT-N03 | 篡改 JWT Signature           | 401      | 401      | 通过 |
| JWT-N04 | 使用合法签名但已过期的 Token | 401      | 401      | 通过 |
| JWT-N05 | 使用错误 Authorization 格式  | 401      | 401      | 通过 |

## 四、关键测试说明

### 1. 伪造 Token

使用无效字符串作为 Bearer Token，验证系统能否拒绝无法解析的 JWT。

### 2. 篡改 Payload

构造有效 JWT 后修改 Payload 中的 subject，将用户名替换为 attacker，但保留原签名。

由于 Payload 已发生变化，原签名无法通过验证，系统返回 401。

### 3. 篡改 Signature

构造有效 JWT 后修改签名部分，验证系统是否执行签名校验。

测试结果返回 401。

### 4. 过期 Token

使用测试专用 JWT 密钥生成签名合法、但 expiration 时间早于当前时间的 Token。

测试结果返回 401，说明过期 Token 未能通过认证。

### 5. 异常 Authorization 格式

使用 Basic 认证格式访问需要身份认证的接口。

测试结果返回 401。

## 五、测试执行命令

```
mvn "-Dtest=JwtTokenNegativeTest" "-DforkCount=0" test
```

## 六、测试结果汇总

- 测试用例总数：5
- 通过数量：5
- 失败数量：0
- HTTP 401：5 项
- 测试通过率：100%

## 七、审计结论

本轮 JWT 异常与过期测试全部通过。

在本轮测试覆盖的场景下，系统能够拒绝伪造 Token、Payload 篡改、Signature 篡改、过期 Token 以及错误 Authorization 格式。

暂未发现上述测试场景下的 JWT 认证绕过问题。

本结论仅适用于本轮测试覆盖的接口、配置和测试场景，不代表 JWT 认证机制不存在其他安全风险。

## 八、测试代码

```
src/test/java/com/example/crud/security/JwtTokenNegativeTest.java
```

## 九、后续计划

进入 T4：JWT 权限控制与越权访问测试，重点验证：

1. 普通 USER 是否能够访问 ADMIN 专属功能。
2. 普通用户是否能够访问其他用户的数据。
3. 用户是否能够通过修改请求中的用户 ID 越权操作。
4. 不同角色之间是否存在权限边界失效问题。