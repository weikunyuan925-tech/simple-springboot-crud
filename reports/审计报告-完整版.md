# simple-springboot-crud 安全审计报告

**审计阶段**：第一阶段（只读代码审计）  
**审计日期**：2026-11-09  
**审计对象**：`D:\simple-springboot-crud`  
**代码版本**：`main` 分支，HEAD `0bc98e7`（工作区干净）  
**审计方式**：静态代码审计 + 依赖/配置分析  
**限制说明**：本阶段未启动应用、未调用任何接口、未修改任何文件、未操作数据库、未执行 Git 提交或推送。下文所有验证命令均为**建议方法**，未实际执行。

---

## 0. 执行摘要

**总体风险：严重（Critical）**

| 等级 | 数量 | 结论 |
|---|---:|---|
| 严重 | 2 | 无认证、无对象级授权，匿名用户可读取/修改/删除任意用户 |
| 高 | 2 | CORS 通配符、H2 Console 暴露且空密码 |
| 中 | 5 | 批量赋值残余、无限流/无分页、输入校验缺口、PII 日志、依赖过期 |
| 低 | 5 | 用户枚举、LIKE 通配符、错误处理/审计日志、并发覆盖、安全头/TLS |

**核心结论：**

1. 项目**完全没有认证和授权层**：`pom.xml` 中没有 `spring-boot-starter-security`，代码中不存在 `SecurityFilterChain`、`@PreAuthorize`、自定义过滤器或任何身份模型。
2. 所有 `/api/users/**` 接口均为匿名可访问；用户 ID 为自增数字，攻击者可按 ID 遍历，完成**读取、篡改、删除全部用户**。
3. 由于没有“当前用户”和“资源归属”概念，**IDOR/越权访问成立**；即使后续只补认证而不补对象级授权，漏洞仍然存在。
4. **SQL 注入未发现**：Repository 使用 Spring Data 派生查询和带命名参数的 JPQL，参数由 Hibernate 绑定；但搜索接口存在 LIKE 通配符滥用。
5. 历史报告中的 **F-02（POST 传入 id 覆盖已有记录）已在 `0bc98e7` 修复**：POST 改用 `UserCreateRequest`，不再接收 id；但 PUT 仍绑定含 id 的 `UserDTO`，且遗留 `convertToEntity()` 死代码，存在回归风险。
6. **H2 Console 已启用且 sa 密码为空**；默认仅本机访问，但在本机浏览器、同主机反向代理、DNS rebinding 等场景下可被触达，控制台具备 SQL 执行能力，H2 的 `CREATE ALIAS` 还可进一步导致 JVM 代码执行。
7. 项目无分页、无限流、无审计日志，PII（姓名、邮箱、电话）会进入日志，依赖 Spring Boot 3.2.0 已过 OSS 支持期。

**若该项目仅在本机作为学习 Demo 且从不对外暴露，实际风险可控；一旦部署、演示、接入前端或监听非回环地址，应按“严重”处理。**

---

## 1. 审计范围与方法

### 1.1 审计范围

已完整阅读以下内容：

- `pom.xml`
- `src/main/java/com/example/crud/**` 全部 10 个 Java 文件
- `src/main/resources/application.properties`
- `src/main/resources/data.sql`
- `src/test/**` 全部测试与测试配置
- `README.md`、`CLAUDE.md`
- `reports/审计报告.md`、`reports/审计一个简单springboot问题归总.md`
- Git 历史与最近提交差异
- `.gitignore`、`.claude/settings.local.json`

### 1.2 审计方法

- 数据流追踪：HTTP → Controller → Service → Repository → H2
- 鉴权面检索：`SecurityFilterChain`、`@PreAuthorize`、`@Secured`、`Authentication`、`Authorization`、过滤器
- SQL 注入面检索：`@Query`、`nativeQuery`、`JdbcTemplate`、`EntityManager`、字符串拼接 SQL
- 配置面检查：H2 Console、数据源口令、SQL 日志、CORS
- 历史问题复核：`git show 0bc98e7`
- 依赖面检查：直接依赖、作用域、Spring Boot 版本生命周期

### 1.3 未执行内容

- 未运行 `mvn test` / `mvn spring-boot:run`
- 未发送任何 HTTP 请求
- 未执行 SCA（依赖漏洞扫描）
- 未执行 DAST（动态应用安全测试）
- 未检查运行时解析后的确切依赖版本

---

## 2. 项目结构与依赖分析

### 2.1 目录结构

```text
simple-springboot-crud/
├── pom.xml
├── README.md
├── CLAUDE.md
├── src/
│   ├── main/
│   │   ├── java/com/example/crud/
│   │   │   ├── CrudApplication.java
│   │   │   ├── controller/UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── UserCreateRequest.java
│   │   │   │   └── UserDTO.java
│   │   │   ├── entity/User.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserNotFoundException.java
│   │   │   ├── repository/UserRepository.java
│   │   │   └── service/UserService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql
│   └── test/
│       ├── java/com/example/crud/
│       │   ├── CrudApplicationTests.java
│       │   ├── controller/UserControllerTest.java
│       │   ├── repository/UserRepositoryTest.java
│       │   └── service/UserServiceTest.java
│       └── resources/application-test.properties
├── reports/                     # 既有审计文档
├── target/                      # 构建产物，已被 .gitignore 忽略
└── .claude/settings.local.json  # 被 Git 跟踪的本地工具配置
```

**分层结构**是标准的 Controller → Service → Repository → Entity，但**缺少 Security 层**。项目也没有 `config` 包、过滤器、拦截器或安全配置类。

### 2.2 `pom.xml` 依赖分析

| 依赖 | 作用域 | 用途 | 安全含义 |
|---|---|---|---|
| `spring-boot-starter-web` | compile | Spring MVC、内嵌 Tomcat、Jackson | 暴露 HTTP 攻击面；本身不提供认证 |
| `spring-boot-starter-data-jpa` | compile | Spring Data JPA、Hibernate | 当前查询全部参数化，未发现 SQL 注入 |
| `spring-boot-starter-validation` | compile | Bean Validation | 仅 POST/PUT 生效，覆盖不完整 |
| `h2` | runtime | 内存数据库 | H2 Console 是主要配置风险 |
| `spring-boot-starter-test` | test | JUnit、Mockito、MockMvc | 不影响生产 |
| `spring-boot-testcontainers` | test | 测试容器集成 | 项目中未使用，增加无用依赖面 |
| `testcontainers:junit-jupiter` | test | Testcontainers JUnit 5 | 项目中未使用 |

**关键缺失项：**

- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server` 或其他认证组件
- 速率限制组件（Bucket4j、Resilience4j、网关限流）
- SCA 插件（OWASP Dependency-Check、Snyk、Dependabot 等）

**版本风险：**

- `spring-boot-starter-parent` 固定为 **3.2.0**（`pom.xml:8-13`）。
- 3.2.0 是 2023 年 11 月的首个 3.2 版本；3.2.x 的 OSS 支持已于 2024 年结束，当前已 2026 年，意味着缺少后续大量安全补丁。
- 具体 CVE 需通过 `mvn dependency:tree` + OWASP Dependency-Check 确认，本报告不臆测具体编号。
- Java 17 是受支持的 LTS，本身无问题。

### 2.3 安全相关配置

| 配置 | 当前值 | 风险 |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:testdb` | 内存库，重启即丢数据；Console 可访问 |
| `spring.datasource.username/password` | `sa` / 空密码 | H2 Console 空口令；当前不是外网数据库，但属于不安全默认值 |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | 重启重建，数据不持久；不构成鉴权问题 |
| `spring.h2.console.enabled` | `true` | 暴露 `/h2-console`，高风险管理面 |
| `spring.h2.console.path` | `/h2-console` | 公开可发现路径 |
| `spring.jpa.show-sql` | `true` | SQL 日志进入控制台 |
| `org.hibernate.type.descriptor.sql.BasicBinder` | `TRACE` | **绑定参数值（邮箱、电话）写入日志** |
| `server.port` | `8080` | 默认 HTTP，无 TLS 配置 |
| `spring.mvc.throw-exception-if-no-handler-found` | `true` | 未知路由被统一异常处理器转为 500（错误语义问题） |
| `spring.web.resources.add-mappings` | `false` | 减少静态资源面，正面 |
| Actuator | 未引入 | 减少暴露面，正面 |

### 2.4 攻击面概览

```text
暴露面：
  /api/users                GET/POST
  /api/users/{id}           GET/PUT/DELETE
  /api/users/email/{email}  GET
  /api/users/search         GET
  /api/users/health         GET
  /h2-console/**            H2 Web Console

安全控制：
  认证：无
  授权：无
  CSRF：无（当前无会话认证，经典 CSRF 不适用）
  CORS：允许任意 Origin
  速率限制：无
  输入校验：仅 Bean Validation（POST/PUT）
  审计日志：无
  安全响应头：无
```

---

## 3. Controller、Service、Repository 调用关系

### 3.1 分层调用链

```text
Client
  │  HTTP（无 SecurityFilterChain、无认证过滤器）
  ▼
DispatcherServlet
  ▼
UserController  @RestController + @CrossOrigin("*")
  │
  ├─ GET    /api/users              → UserService.getAllUsers()
  │                                     → UserRepository.findAllOrderByCreatedAtDesc()
  ├─ GET    /api/users/{id}         → UserService.getUserById(id)
  │                                     → UserRepository.findById(id)
  ├─ GET    /api/users/email/{email}→ UserService.getUserByEmail(email)
  │                                     → UserRepository.findByEmail(email)
  ├─ GET    /api/users/search       → UserService.searchUsersByName(name)
  │                                     → UserRepository.findByNameContaining(name)
  ├─ POST   /api/users              → UserService.createUser(UserCreateRequest)
  │                                     → UserRepository.save(User)  [persist]
  ├─ PUT    /api/users/{id}         → UserService.updateUser(id, UserDTO)
  │                                     → findById / existsByEmail / save
  ├─ DELETE /api/users/{id}         → UserService.deleteUser(id)
  │                                     → existsById / deleteById
  └─ GET    /api/users/health       → 直接返回静态 Map
  │
  ▼
UserService  @Transactional
  ▼
UserRepository  extends JpaRepository<User, Long>
  ▼
Hibernate 6 / H2 (jdbc:h2:mem:testdb)

异常路径：
  UserNotFoundException / MethodArgumentNotValidException / Exception
  → GlobalExceptionHandler (@RestControllerAdvice)
  → JSON 错误响应（404 / 400 / 500）
```

### 3.2 接口清单与鉴权状态

| # | 方法 | 路径 | Controller 方法 | Service 方法 | Repository / 查询 | 认证 | 授权 | 主要风险 |
|---:|---|---|---|---|---|---|---|---|
| 1 | GET | `/api/users` | `getAllUsers` | `getAllUsers` | `findAllOrderByCreatedAtDesc` | 无 | 无 | 全量 PII 泄露、无分页 |
| 2 | GET | `/api/users/{id}` | `getUserById` | `getUserById` | `findById` | 无 | 无 | IDOR、按 ID 遍历 |
| 3 | GET | `/api/users/email/{email}` | `getUserByEmail` | `getUserByEmail` | `findByEmail` | 无 | 无 | IDOR、邮箱枚举 |
| 4 | GET | `/api/users/search?name=` | `searchUsers` | `searchUsersByName` | `findByNameContaining` | 无 | 无 | 通配符枚举、无分页 |
| 5 | POST | `/api/users` | `createUser` | `createUser` | `save` | 无 | 无 | 匿名创建、垃圾数据/DoS |
| 6 | PUT | `/api/users/{id}` | `updateUser` | `updateUser` | `findById` / `existsByEmail` / `save` | 无 | 无 | IDOR、任意篡改、账号接管 |
| 7 | DELETE | `/api/users/{id}` | `deleteUser` | `deleteUser` | `existsById` / `deleteById` | 无 | 无 | IDOR、任意删除 |
| 8 | GET | `/api/users/health` | `healthCheck` | 无 | 无 | 无 | 无 | 轻微信息暴露 |

**事务与校验分布：**

- `UserService` 类级 `@Transactional`（`UserService.java:16`），读方法 `@Transactional(readOnly = true)`（`26`、`34`、`41`、`48`）。
- `@Valid` 仅用于 POST（`UserController.java:60`）和 PUT（`UserController.java:71`）。
- `@PathVariable`、`@RequestParam` 未做额外 `@Validated` / 长度限制。
- 全局异常处理覆盖 `UserNotFoundException`、参数校验异常和所有异常（`GlobalExceptionHandler.java:17-56`）。

---

## 4. 风险总览

> 编号按审计逻辑排列；下表按风险等级排序。

| ID | 等级 | 问题 | CWE | 位置 | 可利用性 |
|---|---|---|---|---|---|
| VUL-01 | **严重** | 所有接口完全缺失认证 | CWE-306 | `pom.xml`、`UserController.java:16-94` | 直接利用 |
| VUL-02 | **严重** | 对象级/功能级授权缺失（IDOR/越权） | CWE-639、CWE-862、CWE-863 | `UserController.java:37-86`、`UserService.java:35-101` | 直接利用 |
| VUL-03 | **高** | CORS 允许任意 Origin（`*`） | CWE-942 | `UserController.java:18` | 浏览器侧可利用 |
| VUL-04 | **高** | H2 Console 暴露 + sa 空密码 | CWE-1188、CWE-306；潜在 CWE-94 | `application.properties:8-9,19-20`、`README.md:58-62` | 条件利用 |
| VUL-14 | 中 | 依赖版本过期、无 SCA | CWE-1104 | `pom.xml:8-13` | 需 SCA 确认 |
| VUL-05 | 中 | 批量赋值残余面 / 死代码 | CWE-915 | `UserDTO.java:10,23-24`、`UserService.java:79-94,114-121` | 当前不可利用，回归风险 |
| VUL-06 | 中 | 无分页、无限流、无资源上限 | CWE-770、CWE-799 | `UserController.java:28-35,49-57` | 直接利用（DoS/批量导出） |
| VUL-07 | 中 | 输入校验缺口、存储型 XSS 输入面 | CWE-20、CWE-79 | `UserCreateRequest.java:13-15`、`User.java:22-28` | 条件利用 |
| VUL-08 | 中 | 敏感数据写入日志 | CWE-532 | `application.properties:14-15,23-25` | 直接发生 |
| VUL-09 | 低 | 用户枚举 | CWE-203、CWE-204 | `UserController.java:37-47`、`UserService.java:83-86` | 当前低，认证后中 |
| VUL-10 | 低 | LIKE 通配符注入 | CWE-943 | `UserRepository.java:19-20` | 直接利用（结果集操纵） |
| VUL-11 | 低 | 异常处理与审计日志不足 | CWE-209、CWE-778 | `GlobalExceptionHandler.java:47-56` | 直接观察 |
| VUL-12 | 低 | 并发覆盖 / TOCTOU | CWE-362 | `UserService.java:79-101`、`User.java` | 条件利用 |
| VUL-13 | 低 | 安全响应头 / TLS 缺失 | CWE-693、CWE-319 | `application.properties` | 部署相关 |

---

## 5. 详细发现

### VUL-01 [严重] 所有用户接口完全缺失认证

**类型**：Broken Authentication / Missing Authentication for Critical Function  
**CWE**：CWE-306  
**OWASP API**：API2:2023 Broken Authentication

**代码位置：**

- `pom.xml:29-68`：依赖列表中没有 `spring-boot-starter-security`，也没有任何 OAuth2/JWT 依赖。
- `src/main/java/com/example/crud/controller/UserController.java:16-19`：

```java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
```

- 全仓库检索确认：不存在 `SecurityFilterChain`、`WebSecurityConfigurer`、`@PreAuthorize`、`@Secured`、`@RolesAllowed`、`OncePerRequestFilter`、`Authentication` 或 `Authorization` 相关实现。

**问题分析：**

Spring Boot 只有在引入 `spring-boot-starter-security` 后才自动启用安全过滤链。当前项目没有该依赖，也没有手写过滤器，因此 **DispatcherServlet 之前没有任何身份认证**。所有接口默认都是匿名的，`@CrossOrigin` 只处理浏览器跨域，不是认证。

**影响：**

- 匿名者可读取全部用户姓名、邮箱、电话等 PII。
- 匿名者可创建任意用户，造成垃圾数据、资源耗尽、业务污染。
- 匿名者可修改、删除任意用户，造成数据篡改和丢失。
- 没有身份，就无法做审计、追责、限流、封禁和权限分级。
- 整个 API 相当于一个**无登录的管理后台**。

**验证方法（未执行）：**

```bash
# 预期直接 200，而不是 401
curl -i http://localhost:8080/api/users
```

**修复建议：**

1. 引入 `spring-boot-starter-security`。
2. 采用“默认拒绝”策略：`anyRequest().authenticated()`，仅放行 `/api/users/health` 等必要探活接口。
3. 根据业务选择认证方式：
   - 面向浏览器：Session + CSRF 保护；
   - 面向 API：OAuth2/OIDC + JWT（推荐），或短期令牌；
   - 若需要自建账号：增加独立账户表、`PasswordEncoder`（BCrypt/Argon2），不要把凭据字段放进当前 `User` 业务表。
4. 管理接口与自助接口分离：管理端要求 `ADMIN` 角色，普通用户只能访问 `/api/users/me`。
5. 增加登录失败审计、MFA（管理端）、令牌过期与吊销机制。

> 注意：仅增加认证而不增加对象级授权，仍会保留 VUL-02。

---

### VUL-02 [严重] 对象级/功能级授权缺失，IDOR/越权成立

**类型**：Broken Object Level Authorization（BOLA/IDOR）、Broken Function Level Authorization  
**CWE**：CWE-639（Authorization Bypass Through User-Controlled Key）、CWE-862、CWE-863  
**OWASP API**：API1:2023 BOLA、API5:2023 BFLA

**代码位置：**

| 接口 | 位置 | 问题 |
|---|---|---|
| `GET /api/users/{id}` | `UserController.java:37-41` | 直接用路径 id 查询，无归属校验 |
| `GET /api/users/email/{email}` | `UserController.java:43-47` | 直接用邮箱查询，无归属校验 |
| `GET /api/users/search` | `UserController.java:49-57` | 可搜索全部用户 |
| `PUT /api/users/{id}` | `UserController.java:68-77` | 直接用路径 id 更新任意记录 |
| `DELETE /api/users/{id}` | `UserController.java:79-86` | 直接用路径 id 删除任意记录 |
| Service 层 | `UserService.java:35-46,79-101` | 只按 id/email 操作，无当前用户上下文 |

`User` 实体（`User.java:10-45`）中没有 `ownerId`、`tenantId`、`role` 或任何归属字段；项目也没有“当前登录用户”的概念。

**问题分析：**

这是典型的**用户可控对象键**问题：客户端通过 URL 路径提供 `id` 或 `email`，服务端直接用它定位记录，既不验证调用者身份，也不验证调用者与目标记录的关系。

严格来说，IDOR 通常描述“已认证用户 A 访问用户 B 的对象”。本项目连认证都没有，属于更严重的 **BOLA + Missing Authentication**；但漏洞模式与 IDOR 完全一致，按需求仍标记为 IDOR/越权。

此外，`User.id` 使用 `GenerationType.IDENTITY`（`User.java:13-15`），ID 连续递增，攻击者可从 `1` 开始遍历，几乎不需要猜测。

**影响：**

- **水平越权**：任意用户可读取、修改、删除其他用户。
- **垂直越权**：所有管理功能（列表、删除）对匿名者开放。
- **账号接管**：如果邮箱/电话是身份凭据或找回凭据，攻击者可通过 PUT 修改目标用户的邮箱/电话，完成接管。
- **批量数据破坏**：遍历 ID 调用 DELETE 可清空整表。
- **批量数据导出**：GET 全量列表 + 搜索 + 邮箱查询，可完整导出 PII。

**验证方法（未执行，务必在一次性隔离实例中）：**

```bash
# 1) 匿名读取任意对象
curl -i http://localhost:8080/api/users/1
curl -i http://localhost:8080/api/users/email/john.doe@example.com

# 2) 匿名篡改任意对象
curl -i -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Audit Test","email":"audit-test@example.test","phone":"000"}'

# 3) 匿名删除任意对象
curl -i -X DELETE http://localhost:8080/api/users/2

# 4) 遍历 ID（请勿对共享环境执行）
for i in $(seq 1 5); do curl -s -o /dev/null -w "%{http_code} " http://localhost:8080/api/users/$i; done
```

**修复建议：**

1. **每个对象访问都必须做授权判断**，不能只依赖“知道 ID”。
2. 引入角色模型：
   - `ADMIN`：可管理所有用户；
   - `USER`：只能读取/修改自己；
   - 匿名：仅允许健康检查或登录接口。
3. 提供 `/api/users/me` 而不是让普通用户传 `{id}` 访问自己；管理端使用独立路径（如 `/api/admin/users/{id}`）并要求 `ADMIN`。
4. 在 Service 层做防御性校验（不要只在 Controller 或网关做）：

```java
// 示例：对象级授权
if (!currentUser.isAdmin() && !targetUser.getId().equals(currentUser.getId())) {
    throw new AccessDeniedException("Forbidden");
}
```

5. 若未来支持多租户，增加 `tenantId`/`ownerId` 并在每次查询中强制带条件（`findByIdAndOwnerId`），避免“先查出再判断”的遗漏。
6. 将 ID 改为 UUID 只能提高枚举成本，**不能替代授权**，只能作为纵深防御。
7. 增加授权测试：用户 A 访问用户 B 的资源必须返回 403。

---

### VUL-03 [高] CORS 允许任意 Origin（`*`）

**类型**：Permissive Cross-domain Policy  
**CWE**：CWE-942  
**OWASP API**：API8:2023 Security Misconfiguration

**代码位置：**

```java
// UserController.java:16-19
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
```

**问题分析：**

类级 `@CrossOrigin(origins = "*")` 对控制器内所有接口生效。Spring 在处理 `@CrossOrigin` 时，如果未显式指定 `methods`，会将处理器映射中声明的方法加入允许列表；因此：

- GET 接口允许任意源读取响应；
- PUT/DELETE 接口的预检请求也会按各自映射方法被允许；
- `allowCredentials` 默认为 false，所以不会携带 Cookie，但当前 API 本来就不需要认证。

这意味着：**用户浏览器中打开的任意恶意网站，都可以通过 JavaScript 跨域读取甚至删除该用户本机 `http://localhost:8080` 上的数据**。浏览器发往 `127.0.0.1` 的请求源地址也是本机，CORS 通配符使响应可被恶意页面读取。部分浏览器的 Private Network Access（PNA）可能拦截，但不能作为安全控制依赖。

如果未来引入基于 Cookie 的认证，通配符 CORS 会进一步放大风险；即使 `allowCredentials=true` 与 `*` 组合会被 Spring 拒绝，错误配置的 `allowedOriginPatterns("*")` 仍可能放行。

**验证方法（未执行）：**

```bash
# 简单请求：观察 Access-Control-Allow-Origin
curl -i -H "Origin: https://evil.example" http://localhost:8080/api/users

# 预检：观察是否允许 DELETE
curl -i -X OPTIONS http://localhost:8080/api/users/1 \
  -H "Origin: https://evil.example" \
  -H "Access-Control-Request-Method: DELETE" \
  -H "Access-Control-Request-Headers: content-type"
```

**修复建议：**

1. 删除 `@CrossOrigin(origins = "*")`。
2. 使用全局 `CorsConfigurationSource` 明确白名单：

```java
configuration.setAllowedOrigins(List.of("https://app.example.com"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
configuration.setAllowCredentials(true);
```

3. 生产环境优先同源部署（前端与 API 同域），避免不必要的 CORS。
4. 不要用 `originPatterns("*")` 替代 `origins("*")`，本质相同。
5. CORS 不是认证；真正的修复仍是 VUL-01/VUL-02。

---

### VUL-04 [高] H2 Console 暴露且使用空密码

**类型**：Insecure Default / Exposed Administrative Interface  
**CWE**：CWE-1188、CWE-306；潜在 CWE-94（代码注入）  
**OWASP API**：API8:2023 Security Misconfiguration

**代码位置：**

```properties
# src/main/resources/application.properties:6-9
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# src/main/resources/application.properties:19-20
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

```markdown
<!-- README.md:58-62 -->
### Access H2 Database Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)
```

**问题分析：**

- H2 Console 是一个 Web 管理界面，登录后可以执行任意 SQL。
- 当前 `sa` 密码为空，README 还公开了连接信息。
- Spring Boot 的 `h2.console.settings.web-allow-others` 默认为 `false`，H2 Servlet 会拒绝非本机 IP；因此**默认情况下远程主机不能直接访问**。
- 但“仅本机”并不等于安全：
  - 本机任意进程/用户可以访问；
  - 浏览器发往 `127.0.0.1` 的请求源地址也是本机，恶意网页结合 DNS rebinding / CSRF 技术可尝试触达；
  - 若应用位于 Nginx 等反向代理之后，代理到上游的连接来自 `127.0.0.1`，H2 会视为本机，从而把控制台暴露给远程；
  - 容器、端口转发、开发机共享等场景也可能改变访问边界。
- H2 支持 `CREATE ALIAS ... AS ...` 执行 Java 代码；拥有 SA 权限的攻击者可能进一步在应用 JVM 中执行任意代码，即从数据库控制升级为 **RCE**。
- `ddl-auto=create-drop` 会在重启时清空数据，降低持久化影响，但不能阻止运行期间的数据窃取/篡改或代码执行。

**影响：**

- 读取、修改、删除全部业务数据。
- 绕过所有应用层校验和未来的认证。
- 潜在 JVM 代码执行，危及宿主开发机。
- README 公开凭据，降低攻击门槛。

**验证方法（未执行；H2 SQL 与 CREATE ALIAS 操作严禁在共享环境执行）：**

```bash
# 确认控制台页面存在
curl -i http://localhost:8080/h2-console/
```

浏览器访问 `http://localhost:8080/h2-console`，使用：

```text
JDBC URL: jdbc:h2:mem:testdb
User Name: sa
Password: （空）
```

登录后执行 `SELECT * FROM users;` 即可验证数据暴露。代码执行能力仅建议在完全隔离的实验环境中由授权人员验证。

**修复建议：**

1. 默认和生产配置中关闭控制台：

```properties
spring.h2.console.enabled=false
```

2. 如开发确实需要，放入独立的 `dev` profile，并：
   - 设置强随机数据库密码；
   - 保持 `web-allow-others=false`；
   - 仅绑定 `127.0.0.1`；
   - 在安全配置中显式 `requestMatchers("/h2-console/**").denyAll()`，或至少在非 dev 环境拒绝；
   - 不要把控制台凭据写进 README。
3. 使用真实数据库时，数据库账号遵循最小权限，禁止应用账号拥有 DDL/超级权限。
4. 对管理端口做网络隔离，不要与业务端口共享公网入口。

---

### VUL-05 [中] 批量赋值残余面与死代码（当前不可利用，回归风险）

**类型**：Mass Assignment / Over-Posting  
**CWE**：CWE-915  
**OWASP API**：API3:2023 Broken Object Property Level Authorization

**代码位置：**

```java
// UserDTO.java:10, 23-24
private Long id;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;

// UserController.java:68-77
@PutMapping("/{id}")
public ResponseEntity<Map<String, Object>> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserDTO userDTO) {
    UserDTO updatedUser = userService.updateUser(id, userDTO);
    ...
}

// UserService.java:79-94
public UserDTO updateUser(Long id, UserDTO userDTO) {
    User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    ...
    existingUser.setName(userDTO.getName());
    existingUser.setEmail(userDTO.getEmail());
    existingUser.setPhone(userDTO.getPhone());
    ...
}

// UserService.java:114-121（死代码）
private User convertToEntity(UserDTO userDTO) {
    User user = new User();
    user.setId(userDTO.getId());
    ...
}

// UserService.java:56-64（被注释的旧实现）
/*public UserDTO createUser(UserDTO userDTO) { ... }*/
```

**当前状态：**

- `POST /api/users` 已改用 `UserCreateRequest`（`UserController.java:59-66`），该类没有 `id` 字段（`UserCreateRequest.java:7-43`）。
- `UserService.createUser(UserCreateRequest)` 只复制 `name`、`email`、`phone`（`UserService.java:66-77`），不再调用 `setId()`。
- 因此历史 F-02（POST 传 id 覆盖已有记录）**已修复**。静态确认：`save()` 得到的实体 id 为 null，走 `persist()` 新增。
- Spring Boot 默认的 Jackson 配置会忽略未知属性，所以 POST 请求即使带 `id`，也会被静默忽略，不会触发覆盖。

**残余风险：**

1. `PUT` 仍绑定 `UserDTO`，其中包含 `id`、`createdAt`、`updatedAt`。当前 Service 只取三个业务字段，路径 id 优先，**暂不可利用**；但这是 over-posting 面，未来重构若直接 `save(userDTO)` 或复用 `convertToEntity()`，会立即退化为批量赋值/覆盖漏洞。
2. `convertToEntity()` 仍保留了 `user.setId(userDTO.getId())`，是历史漏洞的“火药桶”；当前未被调用，但属于死代码。
3. 路径 id 与 body id 不一致时，服务端静默采用路径 id，不报错，容易造成客户端误解和测试盲区。
4. 没有回归测试证明“POST 传 id 不会覆盖已有记录”；现有 `UserServiceTest` 已与新实现不一致（见第 9 节）。

**验证方法（未执行）：**

```bash
# POST 带 id=1，预期：创建新用户（新 ID），id=1 的原记录不变
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"Audit Test","email":"audit-test-2026@example.test","phone":"123456789"}'

curl -s http://localhost:8080/api/users/1

# PUT /1 但 body 带 id=2，预期：修改 1，2 不变（路径 id 优先）
curl -i -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"id":2,"name":"Updated","email":"updated@example.test","phone":"000"}'
```

**修复建议：**

1. 新增 `UserUpdateRequest`，只包含允许修改的字段（`name`、`email`、`phone`），不包含 `id`、`createdAt`、`updatedAt`、`role`、`ownerId`。
2. 删除 `convertToEntity()` 和被注释的旧 `createUser()`。
3. Controller 改用 `UserUpdateRequest`；Service 不接触 `UserDTO` 的审计字段。
4. 对安全敏感 DTO 可配置拒绝未知字段，或至少对 `id` 做显式校验/忽略测试。
5. 增加回归测试：
   - POST 带 `id` 时不得修改已有记录；
   - PUT body 中 `id` 与路径不一致时应返回 400，或明确忽略且目标不变；
   - 未来新增 `role`/`isAdmin` 字段时不得被客户端绑定。

---

### VUL-06 [中] 无分页、无限流、无资源上限

**类型**：Unrestricted Resource Consumption  
**CWE**：CWE-770、CWE-799  
**OWASP API**：API4:2023 Unrestricted Resource Consumption

**代码位置：**

```java
// UserController.java:28-35
@GetMapping
public ResponseEntity<Map<String, Object>> getAllUsers() {
    List<UserDTO> users = userService.getAllUsers();
    ...
}

// UserController.java:49-57
@GetMapping("/search")
public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String name) {
    List<UserDTO> users = userService.searchUsersByName(name);
    ...
}

// UserRepository.java:22-23
@Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
List<User> findAllOrderByCreatedAtDesc();
```

**问题分析：**

- 列表和搜索不分页，一次性把全部匹配记录加载进内存并序列化返回。
- 没有 `Pageable`、最大页大小、总条数上限或截断。
- 没有速率限制，攻击者可高频调用列表、搜索、创建、删除接口。
- 当前样例数据只有 5 条，问题不明显；数据量增长后会造成内存、CPU、数据库连接池和网络带宽耗尽。
- 删除接口可被循环调用，批量清空数据。

**影响：**

- 批量 PII 导出（结合 VUL-01/VUL-02）。
- 资源耗尽型 DoS。
- 数据库慢查询（`LIKE '%...%'` 无法使用普通索引）。
- 创建接口可被用于垃圾数据灌入。

**验证方法（未执行）：**

```bash
# 观察响应是否包含分页信息；当前只有 users/total
curl -s http://localhost:8080/api/users | head -c 500

# 连续请求观察是否出现 429；当前预期全部 200
for i in $(seq 1 100); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/users
done | sort | uniq -c
```

**修复建议：**

1. 使用 Spring Data `Pageable`：

```java
@GetMapping
public Page<UserDTO> getAllUsers(
        @PageableDefault(size = 20) Pageable pageable) { ... }
```

2. 限制最大页大小（如 100），超出返回 400。
3. 搜索接口同样分页，并对 `name` 设最大长度。
4. 在网关或应用层增加速率限制（按 IP / 用户 / 接口），返回 429。
5. 请求体大小、查询串长度、连接数设置上限。
6. 为 `created_at`、`email` 建索引；`LIKE '%x%'` 考虑全文检索或前缀匹配。
7. 批量导出走异步任务并做权限与审计，不要直接暴露全量列表。

---

### VUL-07 [中] 输入校验缺口与存储型 XSS 输入面

**类型**：Improper Input Validation / Stored XSS Surface  
**CWE**：CWE-20、CWE-79

**代码位置：**

```java
// UserCreateRequest.java:13-15 —— email 没有 @Size
@NotBlank
@Email
private String email;

// UserDTO.java:16-18 —— 同样没有 @Size
@NotBlank(message = "Email is required")
@Email(message = "Email should be valid")
private String email;

// User.java:22-28
@NotBlank
@Email
@Column(nullable = false, unique = true)
private String email;

@Size(max = 15, ...)
private String phone;
```

**问题分析：**

1. **email 无最大长度**：`@Email` 只校验格式，不限制长度。数据库列默认 `varchar(255)`，超长但格式合法的邮箱可能在写入时触发 `DataException`/`DataIntegrityViolationException`，最终被全局处理器转成 500。请求体本身也没有应用层大小限制。
2. **phone 只有长度限制，没有格式限制**：任意字符（包括 HTML 片段）都可以存入。
3. **name 允许任意内容（2-100 字符）**：当前 API 返回 JSON，不会直接在服务端渲染 HTML；但如果下游前端把 `name`、`phone` 用 `innerHTML` 渲染，就形成**存储型 XSS**。
4. **email 未规范化**：未做 `trim` / 小写化；H2 的唯一约束默认区分大小写，`John@Example.com` 与 `john@example.com` 可同时存在，造成重复账号、身份混淆和枚举差异。
5. **搜索参数无最大长度**：受 Tomcat 请求头大小限制，但仍可构造较长查询。

**影响：**

- 超长输入导致 500、日志噪声、潜在资源消耗。
- 存储型 XSS（取决于前端渲染方式）。
- 重复邮箱导致账号/身份不一致，影响后续认证设计。
- 非法手机号进入下游短信/通知系统。

**验证方法（未执行）：**

```bash
# 超长 email（300 字符，格式尽量合法）→ 观察 500
# 存储 XSS 载荷 → 创建后读取，确认原样返回
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"<img src=x onerror=alert(1)>","email":"xss@example.test","phone":"123"}'

# 大小写重复
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Case Test","email":"John.Doe@Example.com","phone":"123"}'
```

**修复建议：**

1. email 增加 `@Size(max = 254)`（RFC 5321 上限），并在实体列上明确长度。
2. phone 增加 `@Pattern`（如 E.164 或业务允许的字符集）。
3. name 拒绝控制字符、换行和明显的 HTML 标签；在 API 层做规范化。
4. email 统一 `trim().toLowerCase(Locale.ROOT)` 后存储，并建立大小写不敏感的唯一索引。
5. 前端对 `name`、`phone`、`email` 做输出编码；设置 CSP；不要把用户输入拼进 HTML。
6. 限制请求体大小（网关/Nginx/Spring 层）。
7. 增加边界测试：254/255 字符邮箱、Unicode、控制字符、超长手机号。

---

### VUL-08 [中] 敏感数据写入日志

**类型**：Insertion of Sensitive Information into Log File  
**CWE**：CWE-532

**代码位置：**

```properties
# application.properties:14-15
spring.jpa.show-sql=true
spring.jpa.format-sql=true

# application.properties:23-25
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**问题分析：**

- `BasicBinder=TRACE` 会把 Hibernate 绑定的参数值打印出来，包括姓名、邮箱、电话。
- `show-sql=true` 和 `hibernate.SQL=DEBUG` 会输出 SQL 语句。
- 日志通常会进入控制台、日志采集、SIEM、CI 输出或开发者终端；这些位置可能被更多人员访问。
- 项目没有日志脱敏、保留策略或访问控制配置。
- `GlobalExceptionHandler` 不记录异常，反而没有安全审计日志；形成“敏感信息过多、安全事件信息过少”的反差。

**影响：**

- PII 泄露到日志系统，违反数据最小化和合规要求。
- 日志被转发/截图/提交到仓库时造成二次泄露。
- 日志中的换行/控制字符可能造成日志伪造（取决于日志框架转义策略）。

**验证方法（未执行）：**

启动应用后执行一次查询/创建，检查控制台或日志中是否出现：

```text
binding parameter [1] as [VARCHAR] - [john.doe@example.com]
binding parameter [2] as [VARCHAR] - [010-1234-5678]
```

**修复建议：**

1. 生产/预发关闭 SQL 日志：

```properties
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN
```

2. 仅 dev profile 开启调试，并且不要在生产日志中输出参数值。
3. 使用结构化日志 + 字段级脱敏（邮箱掩码、电话后四位）。
4. 设置日志保留期、访问权限和传输加密。
5. 增加安全事件日志：认证失败、授权失败、批量删除、敏感查询，但不记录完整 PII。

---

### VUL-09 [低] 用户枚举

**类型**：Observable Discrepancy / User Enumeration  
**CWE**：CWE-203、CWE-204

**代码位置：**

- `UserController.java:37-47`：按 ID / 邮箱查询，存在返回 200，不存在返回 404。
- `UserService.java:35-46`：异常消息包含 id/email。
- `UserService.java:83-86`：更新邮箱时的重复检查抛出 `IllegalArgumentException`。
- `UserService.java:66-77`：创建时未预检邮箱，重复邮箱由数据库唯一约束拒绝，最终被通用处理器转为 500。

**问题分析：**

- 响应码差异可直接判断某 ID/邮箱是否存在。
- `/api/users/email/{email}` 把邮箱放在 URL 中，容易进入访问日志、代理日志和浏览器历史。
- 更新为已存在邮箱时返回 500（通用异常），创建重复邮箱也返回 500；不同状态码/消息可辅助判断。
- 当前无认证、全量列表公开，枚举的额外影响较低；一旦引入认证，枚举会成为撞库、钓鱼、账号接管的侦察步骤。

**验证方法（未执行）：**

```bash
curl -i http://localhost:8080/api/users/1          # 200
curl -i http://localhost:8080/api/users/9999       # 404

curl -i "http://localhost:8080/api/users/email/john.doe@example.com"   # 200
curl -i "http://localhost:8080/api/users/email/nobody@example.com"     # 404

# 重复邮箱创建 → 当前预期 500（应改为 409）
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"john.doe@example.com","phone":"123"}'
```

**修复建议：**

1. 对未授权/无权限的资源统一返回 404 或 403，不区分“不存在”和“无权访问”。
2. 不要把邮箱放在 URL 路径中；使用 `/api/users/me` 或受保护的查询接口。
3. 对查询、登录、找回密码等接口做速率限制和异常检测。
4. 重复邮箱返回 409，但消息保持中性；不要回显完整邮箱。
5. 增加审计与告警：短时间大量 404 / 枚举行为。

---

### VUL-10 [低] LIKE 通配符注入（非 SQL 注入）

**类型**：Improper Neutralization of Special Elements in Data Query Logic  
**CWE**：CWE-943

**代码位置：**

```java
// UserRepository.java:19-20
@Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
List<User> findByNameContaining(@Param("name") String name);

// UserController.java:49-57
@GetMapping("/search")
public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String name) {
    List<UserDTO> users = userService.searchUsersByName(name);
```

**问题分析：**

- JPQL 使用命名参数 `:name`，由 Hibernate 绑定为预编译参数，**不存在 SQL 注入**。
- 但 `%` 和 `_` 是 LIKE 通配符，未做转义。传入 `name=%` 会匹配全部记录；`_` 可匹配任意单字符。
- 这属于“查询逻辑操纵/通配符注入”，影响的是结果集，不是 SQL 结构。
- 当前全量列表本就公开，单独影响低；若未来搜索用于权限过滤或数据分割，可能造成越权读取。

**验证方法（未执行）：**

```bash
# %25 是 URL 编码的 %
curl -s "http://localhost:8080/api/users/search?name=%25"
# 预期返回全部用户
```

**修复建议：**

1. 绑定前转义 `\`、`%`、`_`，并在 JPQL 中声明 `ESCAPE '\'`：

```java
@Query("SELECT u FROM User u WHERE u.name LIKE :name ESCAPE '\\'")
List<User> findByNameContaining(@Param("name") String name);
```

```java
String escaped = name.replace("\\", "\\\\")
                     .replace("%", "\\%")
                     .replace("_", "\\_");
```

2. 限制 `name` 最大长度，配合分页与速率限制。
3. 若需要更复杂搜索，使用 Criteria API/Specification，避免字符串拼接。

---

### VUL-11 [低] 异常处理与审计日志不足

**类型**：Error Handling / Insufficient Logging  
**CWE**：CWE-209、CWE-778

**代码位置：**

```java
// GlobalExceptionHandler.java:47-56
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    ...
    errorResponse.put("message", "An unexpected error occurred");
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

相关业务异常：

- `UserService.java:83-86`：重复邮箱抛出 `IllegalArgumentException`。
- `UserService.java:66-77`：创建时不预检邮箱，依赖数据库唯一约束。
- `UserService.java:35-46`：404 消息回显 id/email。

**问题分析：**

- 通用 `Exception` 处理器把所有未预期异常转成 500，**不记录异常日志**，导致安全事件和故障无法追踪。
- `IllegalArgumentException`（重复邮箱）和 `DataIntegrityViolationException` 也落入 500，语义错误：应为 409/400。
- `MethodArgumentTypeMismatchException`（如 `/api/users/abc`）同样可能变成 500。
- 404 消息回显 id/email，属于低度信息暴露。
- 没有审计日志：谁在何时读取/修改/删除了哪条记录（当前也没有“谁”）。
- 未来引入 Spring Security 后，`AccessDeniedException` 可能被通用处理器捕获成 500，掩盖 403。

**验证方法（未执行）：**

```bash
# 类型错误 → 当前可能 500
curl -i http://localhost:8080/api/users/abc

# 重复邮箱 → 当前 500，而非 409
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"john.doe@example.com","phone":"123"}'
```

**修复建议：**

1. 增加针对性异常处理：

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<?> handleConflict(...) { return ResponseEntity.status(409)...; }

@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<?> handleBadRequest(...) { return ResponseEntity.badRequest()...; }
```

2. 通用 500 处理器保留中性消息，但**必须记录完整异常与 correlation ID**（不记录 PII）。
3. 404 消息不要回显用户输入；统一为 “Resource not found”。
4. 增加安全审计日志：认证、授权失败、批量删除、敏感数据导出。
5. 接入指标与告警（5xx 率、404 激增、删除量异常）。

---

### VUL-12 [低] 并发覆盖与 TOCTOU

**类型**：Race Condition / Time-of-check Time-of-use  
**CWE**：CWE-362

**代码位置：**

```java
// UserService.java:79-94
if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
    userRepository.existsByEmail(userDTO.getEmail())) {
    throw new IllegalArgumentException("User with email " + userDTO.getEmail() + " already exists");
}
...
User updatedUser = userRepository.save(existingUser);

// UserService.java:96-101
if (!userRepository.existsById(id)) {
    throw new UserNotFoundException(...);
}
userRepository.deleteById(id);
```

`User` 实体没有 `@Version` 字段（`User.java:10-45`）。

**问题分析：**

- 更新邮箱是“先查重、再保存”的 TOCTOU：两个并发请求可同时通过检查，随后一个触发数据库唯一约束异常（当前返回 500）。
- 没有乐观锁：两个并发 PUT 会后者覆盖前者，造成**丢失更新**。
- `existsById` + `deleteById` 之间记录可能已被删除，存在竞态；当前影响较小。
- 当前无认证/多用户，实际并发场景有限；引入认证后会变得重要。

**影响：**

- 数据不一致、更新丢失、偶发 500。
- 攻击者可利用并发窗口进行竞态篡改（在复杂业务中可能更严重）。

**验证方法（未执行）：**

```bash
# 并发把两个用户改成同一个邮箱，观察是否出现 500
curl -s -X PUT .../api/users/1 -H "Content-Type: application/json" -d '{"name":"A","email":"same@example.test","phone":"1"}' &
curl -s -X PUT .../api/users/2 -H "Content-Type: application/json" -d '{"name":"B","email":"same@example.test","phone":"2"}' &
wait

# 并发更新同一用户的不同字段，观察最后写入者覆盖
```

**修复建议：**

1. 为 `User` 增加 `@Version` 乐观锁字段，冲突时返回 409/412。
2. 依赖数据库唯一约束作为最终防线，并将 `DataIntegrityViolationException` 映射为 409。
3. 使用 `@Transactional` 保证读写一致性（当前已有），但不要用“先查后写”代替约束。
4. 对外提供 ETag / `If-Match`，支持条件更新。
5. 删除操作可改为幂等（不存在也返回成功或统一 404），避免 TOCTOU 暴露内部状态。

---

### VUL-13 [低] 安全响应头与 TLS 缺失

**类型**：Security Misconfiguration  
**CWE**：CWE-693、CWE-319

**代码位置：**

- `application.properties` 中没有 TLS、HSTS、安全头配置。
- 项目没有 Spring Security，因此不会自动添加 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、CSP、HSTS 等。

**问题分析：**

- 应用直接以 HTTP 监听 8080；若直接对外，PII 明文传输。
- 缺少 `Cache-Control: no-store`，用户数据可能被浏览器/代理缓存。
- 缺少 `X-Content-Type-Options: nosniff`，存在 MIME 嗅探风险（尽管 JSON 响应风险较低）。
- 缺少 HSTS，存在降级/中间人风险。

**验证方法（未执行）：**

```bash
curl -I http://localhost:8080/api/users
# 观察是否缺少 Strict-Transport-Security、X-Content-Type-Options 等
```

**修复建议：**

1. 生产环境强制 HTTPS，在反向代理或应用层终止 TLS。
2. 启用 HSTS（含子域，preload 视情况）。
3. 通过 Spring Security 或过滤器添加：
   - `X-Content-Type-Options: nosniff`
   - `Referrer-Policy: no-referrer`
   - `Cache-Control: no-store`（PII 响应）
   - CSP（若有前端页面）
   - `X-Frame-Options: DENY` / `frame-ancestors 'none'`
4. 限制管理端口和数据库端口的网络访问。

---

### VUL-14 [中] 依赖版本过期且无 SCA

**类型**：Use of Unmaintained Third Party Components  
**CWE**：CWE-1104

**代码位置：**

```xml
<!-- pom.xml:8-13 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>
```

**问题分析：**

- Spring Boot 3.2.0 是 2023 年 11 月的初始版本；3.2.x 的 OSS 支持已于 2024 年结束。
- 当前是 2026 年 11 月，项目缺少 3.2.x 后续所有安全修复，也没有升级到受支持的 3.5.x/4.x 系列。
- 由 BOM 管理的 Spring Framework、Tomcat、Hibernate、Jackson、H2 等版本同样停留在旧版本。
- 没有 OWASP Dependency-Check、Snyk、Dependabot 或 SBOM 生成，无法持续发现供应链漏洞。
- `spring-boot-testcontainers` 和 `testcontainers:junit-jupiter` 在项目中未使用，增加了无谓的依赖面。

**影响：**

- 可能包含已公开的框架/容器漏洞（具体 CVE 需 SCA 确认）。
- 升级成本随时间增加；安全补丁无法及时应用。
- 供应链缺少可见性。

**验证方法（未执行）：**

```bash
mvn dependency:tree
mvn versions:display-dependency-updates
mvn org.owasp:dependency-check-maven:check
```

**修复建议：**

1. 升级到当前受支持的 Spring Boot 版本，并执行完整回归测试。
2. 在 CI 中集成 OWASP Dependency-Check / Snyk / Dependabot，设置高危漏洞阻断。
3. 生成并保存 SBOM（CycloneDX）。
4. 移除未使用的 Testcontainers 依赖。
5. 使用 Maven Enforcer 锁定 Java/Maven 版本，避免环境漂移。

---

## 6. 专项审计结论（对应任务 4/5）

### 6.1 用户新增、查询、修改、删除接口逐项结论

| 操作 | 接口 | 输入校验 | 认证 | 授权 | 结论 |
|---|---|---|---|---|---|
| 新增 | `POST /api/users` | `@Valid UserCreateRequest`，name/email/phone 有基本校验 | **无** | **无** | 匿名可创建；mass assignment 已修复；无重复邮箱预检，重复时 500 |
| 查询全部 | `GET /api/users` | 无 | **无** | **无** | 全量 PII 泄露；无分页 |
| 按 ID 查询 | `GET /api/users/{id}` | 仅 Long 类型转换 | **无** | **无** | **IDOR**，可遍历 |
| 按邮箱查询 | `GET /api/users/email/{email}` | 无 | **无** | **无** | **IDOR**，邮箱枚举 |
| 搜索 | `GET /api/users/search?name=` | 无长度限制 | **无** | **无** | 通配符枚举；无分页 |
| 修改 | `PUT /api/users/{id}` | `@Valid UserDTO`；id/createdAt/updatedAt 可绑定但被忽略 | **无** | **无** | **IDOR**，任意篡改；over-posting 残余面 |
| 删除 | `DELETE /api/users/{id}` | 仅存在性检查 | **无** | **无** | **IDOR**，任意删除 |
| 健康检查 | `GET /api/users/health` | 无 | **无** | **无** | 轻微信息暴露，可保留为公开探活 |

### 6.2 SQL 注入结论：**未发现可利用点**

**依据：**

1. `UserRepository.findByNameContaining` 使用 JPQL 命名参数（`UserRepository.java:19-20`），Hibernate 会绑定为预编译参数，不会拼接字符串。
2. `findByEmail`、`existsByEmail` 是 Spring Data 派生查询，参数同样绑定。
3. `findAllOrderByCreatedAtDesc` 是静态 JPQL，无用户输入（`UserRepository.java:22-23`）。
4. 全仓库没有 `createNativeQuery`、`nativeQuery=true`、`JdbcTemplate`、`NamedParameterJdbcTemplate`、`EntityManager` 动态 SQL 或字符串拼接 SQL。
5. `data.sql` 是静态初始化脚本，不接收用户输入。
6. 没有动态 `ORDER BY`、表名或列名拼接。

**残余风险：**

- LIKE 通配符注入（VUL-10），只影响结果集，不是 SQL 注入。
- 未来若增加排序/过滤参数，必须使用字段白名单或 Criteria API，禁止字符串拼接。
- 若未来引入原生查询，必须继续使用绑定参数，并对无法参数化的表名/列名做白名单。

### 6.3 IDOR / 越权访问结论：**存在，且为严重**

- **水平越权**：任意调用者可按 id 读取/修改/删除任意用户。
- **垂直越权**：所有管理功能（全量列表、创建、删除）对匿名者开放。
- **功能级授权**：没有角色概念，没有 `@PreAuthorize`，没有 Service 层授权。
- **对象级授权**：`User` 无归属字段，Service 只按 id 查询，没有“当前用户”校验。
- 由于 ID 自增，枚举成本极低；GET 全量列表进一步降低攻击门槛。
- 修复必须同时覆盖 **认证**（是谁）和 **授权**（能做什么/能访问哪条记录）。

### 6.4 参数篡改结论：**历史问题已修复，仍有设计残余**

| 场景 | 当前行为 | 结论 |
|---|---|---|
| POST body 传 `id` | `UserCreateRequest` 无 id；Jackson 忽略未知字段；Service 不调用 `setId()` | **已修复**，不会覆盖已有记录 |
| POST body 传 `createdAt`/`updatedAt` | 被忽略；由 `@PrePersist` 设置 | 安全 |
| PUT body 传 `id` | 路径 id 优先；body id 被 Service 忽略 | 当前不可利用；建议拒绝不一致 |
| PUT body 传 `createdAt`/`updatedAt` | 被忽略；由 `@PreUpdate` 设置 | 当前不可利用；建议从 DTO 移除 |
| PUT body 传 `role`/`isAdmin` | DTO/实体无该字段；未知字段被忽略 | 当前无提权面；未来新增字段需防绑定 |
| Path/query 参数注入 | Spring 类型绑定 + JPA 参数化 | 无 SQL 注入 |
| 邮箱大小写 | 未规范化，唯一约束区分大小写 | 可产生重复账号（VUL-07） |

### 6.5 历史 F-02 复核

- 既有 `reports/审计报告.md` 记录了 F-02：POST 允许客户端提交 `id`，`save()` 走 `merge()` 覆盖已有记录。
- `git show 0bc98e7` 显示修复方式：
  - 新增 `UserCreateRequest`（无 id）；
  - `UserController.createUser` 改为接收 `UserCreateRequest`；
  - `UserService.createUser` 手动复制 `name/email/phone`，不设置 id。
- 静态复核结论：**F-02 已修复，POST 不再能覆盖已有记录**。
- 但：
  - `UserDTO` 仍用于 PUT，包含 id/createdAt/updatedAt；
  - `convertToEntity()` 死代码仍保留 `setId()`；
  - 测试未同步，缺少回归保障（见第 9 节）。
- 建议把 F-02 的修复固化为自动化测试，防止回归。

### 6.6 CSRF / CORS 结论

- **经典 CSRF**：当前没有 Session/Cookie 认证，API 也不依赖浏览器自动携带凭据，因此经典 CSRF 不构成主要风险。
- **CORS**：`@CrossOrigin(origins = "*")` 允许任意源读取响应，且 PUT/DELETE 预检按映射方法放行；结合本机部署，任何恶意网站都可能在用户浏览器中读取/删除 `localhost:8080` 的数据。这是当前最现实的浏览器侧攻击路径之一。
- **H2 Console**：不是 CORS 注解覆盖范围，但同样存在浏览器触达、CSRF/DNS rebinding 风险。
- **未来引入 Cookie 认证时**：必须启用 CSRF 防护（Spring Security 默认开启），并与 CORS 白名单一起设计。

### 6.7 其他未发现的问题类型

| 类型 | 结论 |
|---|---|
| SSRF | 未发现出站 HTTP 调用 |
| 路径遍历 | 无文件读写/下载接口 |
| 文件上传 | 无 |
| XXE | 无 XML 解析 |
| 不安全反序列化 | 仅 Jackson JSON，未启用多态类型；无 Java 原生反序列化入口 |
| 命令注入 | 应用代码无；H2 Console 的 `CREATE ALIAS` 是例外（VUL-04） |
| Open Redirect | 无 |
| 越权修改角色 | 当前无角色字段，不存在该提权面 |
| 日志注入 | 输入可含换行，日志未脱敏（VUL-08/VUL-07 关联） |
| Actuator 暴露 | 未引入 Actuator，正面 |
| Log4Shell | 使用 Spring Boot 默认 Logback，未引入 Log4j2，正面 |

---

## 7. 漏洞验证方法（隔离环境，本次未执行）

> **警告**：本节仅为验证步骤。涉及 PUT、DELETE、H2 SQL、批量循环的命令会**修改数据**。必须只在一次性隔离实例中执行，推荐使用默认的内存 H2（`create-drop`，重启即销毁）；**严禁对共享、测试、预发或生产环境执行**。

### 7.1 启动隔离实例

```bash
cd /path/to/simple-springboot-crud
mvn spring-boot:run
```

默认地址：`http://localhost:8080`。

### 7.2 匿名读取与枚举（VUL-01/VUL-02/VUL-09）

```bash
# 全量读取：预期 200，返回所有用户 PII
curl -i http://localhost:8080/api/users

# 任意 ID 读取：预期 200
curl -i http://localhost:8080/api/users/1

# 按邮箱读取：预期 200
curl -i "http://localhost:8080/api/users/email/john.doe@example.com"

# 不存在的 ID/邮箱：预期 404，可据此枚举
curl -i http://localhost:8080/api/users/9999
curl -i "http://localhost:8080/api/users/email/nobody@example.com"

# 搜索通配符：%25 是 URL 编码的 %，预期返回全部
curl -s "http://localhost:8080/api/users/search?name=%25"
```

### 7.3 匿名篡改与删除（VUL-02；会修改数据）

```bash
# 修改用户 1
curl -i -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Audit Test","email":"audit-test@example.test","phone":"000"}'

# 确认已修改
curl -s http://localhost:8080/api/users/1

# 删除用户 2
curl -i -X DELETE http://localhost:8080/api/users/2

# 确认已删除
curl -i http://localhost:8080/api/users/2
```

### 7.4 CORS 验证（VUL-03）

```bash
# 简单请求
curl -i -H "Origin: https://evil.example" http://localhost:8080/api/users

# DELETE 预检
curl -i -X OPTIONS http://localhost:8080/api/users/1 \
  -H "Origin: https://evil.example" \
  -H "Access-Control-Request-Method: DELETE" \
  -H "Access-Control-Request-Headers: content-type"
```

观察响应中是否包含：

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: ... DELETE ...
```

浏览器侧验证（在任意其他 Origin 的页面控制台执行）：

```js
fetch('http://localhost:8080/api/users')
  .then(r => r.json())
  .then(console.log);
```

若可读取，说明任意网站均可跨域获取 PII。注意：部分浏览器的 Private Network Access 可能拦截，需在目标浏览器验证。

### 7.5 H2 Console 验证（VUL-04）

```bash
curl -i http://localhost:8080/h2-console/
```

浏览器打开 `http://localhost:8080/h2-console`，使用：

```text
JDBC URL: jdbc:h2:mem:testdb
User Name: sa
Password: （空）
```

登录后执行：

```sql
SELECT * FROM users;
```

**不要**在共享环境执行 `DROP`、`UPDATE`、`CREATE ALIAS` 等操作。H2 的代码执行能力仅应在完全隔离的实验环境中由授权人员验证。

### 7.6 批量赋值回归验证（VUL-05）

```bash
# 记录用户 1 当前值
curl -s http://localhost:8080/api/users/1

# POST 携带 id=1，预期：创建新用户（新 ID），用户 1 不变
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"Audit Test","email":"audit-test-2026@example.test","phone":"123456789"}'

# 再次确认用户 1 未变
curl -s http://localhost:8080/api/users/1
```

**预期结果**：返回新用户（ID 为 6 或更大），用户 1 的 name/email/phone 保持不变，说明 F-02 修复有效。

```bash
# PUT /1 但 body id=2，预期：修改用户 1，用户 2 不变
curl -i -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"id":2,"name":"Updated","email":"updated@example.test","phone":"000"}'
curl -s http://localhost:8080/api/users/2
```

### 7.7 错误处理与枚举验证（VUL-09/VUL-11）

```bash
# 类型错误：当前可能 500
curl -i http://localhost:8080/api/users/abc

# 重复邮箱：当前预期 500，而非 409
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"john.doe@example.com","phone":"123"}'
```

### 7.8 无限流验证（VUL-06）

```bash
# 连续 100 次请求，观察是否有 429；当前预期全部 200
for i in $(seq 1 100); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/users
done | sort | uniq -c
```

### 7.9 日志泄露验证（VUL-08）

执行一次查询/创建后，检查控制台或日志文件，搜索：

```text
binding parameter
john.doe@example.com
010-1234-5678
```

若出现，说明 PII 已进入日志。

---

## 8. 修复建议与优先级

### P0：立即修复（阻断直接攻击）

1. **引入认证与授权**
   - 添加 `spring-boot-starter-security`。
   - 默认拒绝所有请求，仅放行健康检查/登录。
   - 管理接口要求 `ADMIN`；普通用户只能访问 `/api/users/me`。
   - 增加对象级授权检查，防止 IDOR。
2. **移除 CORS 通配符**
   - 删除 `@CrossOrigin(origins = "*")`。
   - 使用显式 Origin 白名单，限制方法和请求头。
3. **关闭 H2 Console**
   - 非 dev 环境 `spring.h2.console.enabled=false`。
   - 如 dev 必须使用，设置强密码、限制本机、在安全配置中隔离。
4. **不要将服务暴露到非回环地址**
   - 在完成 P0 前，仅绑定 `127.0.0.1`，不要部署到公网/共享网络。

### P1：短期修复（降低影响面）

1. **DTO 拆分**
   - `UserCreateRequest`：只含 name/email/phone（已存在）。
   - `UserUpdateRequest`：只含允许修改的字段，移除 id/createdAt/updatedAt。
   - 删除 `convertToEntity()` 和旧注释代码。
2. **输入校验完善**
   - email `@Size(max=254)`，统一小写化并做大小写不敏感唯一约束。
   - phone `@Pattern`。
   - name 拒绝控制字符/HTML 片段。
   - 搜索参数最大长度。
3. **分页与限流**
   - 列表/搜索使用 `Pageable`，限制最大页大小。
   - 网关/应用层速率限制，返回 429。
   - 请求体/查询串大小限制。
4. **错误处理与日志**
   - 重复邮箱返回 409；类型错误返回 400。
   - 通用 500 记录异常与 correlation ID，不记录 PII。
   - 关闭 `show-sql` 和 `BasicBinder=TRACE`（非 dev）。
   - 增加安全审计日志。
5. **并发控制**
   - `User` 增加 `@Version`，冲突返回 409/412。
   - 依赖数据库唯一约束并映射冲突异常。

### P2：中期加固（体系化）

1. **依赖与供应链**
   - 升级到受支持的 Spring Boot 版本。
   - 集成 OWASP Dependency-Check/Snyk/Dependabot。
   - 生成 SBOM，移除未使用依赖。
2. **安全响应头与 TLS**
   - HTTPS/HSTS、`X-Content-Type-Options`、`Referrer-Policy`、`Cache-Control: no-store`。
3. **API 设计**
   - 管理端与用户端路径分离；避免邮箱/敏感标识出现在 URL。
   - 考虑 UUID 作为纵深防御（不能替代授权）。
4. **测试与验收**
   - 增加认证/授权测试、mass assignment 回归测试、分页/限流测试。
   - 修复当前失效的 Service 测试（见第 9 节）。
5. **监控与响应**
   - 5xx、401/403、404 激增、批量删除、导出行为告警。
   - 日志脱敏、保留策略、访问控制。

### 8.1 建议的安全验收用例

| 用例 | 预期 |
|---|---|
| 匿名 `GET /api/users` | 401 |
| 匿名 `PUT/DELETE /api/users/1` | 401 |
| 用户 A `GET /api/users/{B的ID}` | 403 |
| 普通用户 `GET /api/users` | 403（仅管理员） |
| 管理员 `GET /api/users?page=0&size=20` | 200，分页 |
| POST 带 `id` | 新 ID，原记录不变（或 400 拒绝 id） |
| PUT body `id` 与路径不一致 | 400，目标不变 |
| 重复邮箱创建 | 409 |
| email 超过 254 字符 | 400 |
| `search?name=%` | 按字面量处理，不返回全部（或仅管理员可搜索） |
| 访问 `/h2-console` | 404/403 |
| 恶意 Origin 跨域请求 | 无 `Access-Control-Allow-Origin` |
| 高频请求 | 429 |
| 日志中不出现完整 email/phone | 通过 |

---

## 9. 非安全观察（不影响本次定级，但建议处理）

1. **README 存在未解决的合并冲突标记**
   - `README.md:1` 为 `<<<<<<< HEAD`，说明合并未完成，文档可信度受影响。
2. **Service 测试已与新实现不一致，当前很可能失败**
   - `UserServiceTest.java:35` 定义了 `private UserCreateRequest Request;`，但从未初始化。
   - `UserServiceTest.java:84-103` 仍按旧逻辑 mock `existsByEmail`，并调用 `userService.createUser(Request)`；新 `createUser(UserCreateRequest)` 会立即访问 `request.getName()`，对 null 抛 NPE。
   - Mockito 严格模式还会因未使用的 stub 报 `UnnecessaryStubbingException`。
   - 结论：修复 F-02 后未同步测试，**CI 质量门禁可能已经失效**，也无法防止漏洞回归。
3. **README 行为文档与实际不一致**
   - README 写“重复邮箱返回 400”，实际 `createUser` 未预检，数据库唯一约束异常被通用处理器转为 500。
4. **`.claude/settings.local.json` 被 Git 跟踪**
   - 当前内容仅为本地命令白名单，无敏感信息；但本地工具配置通常应加入 `.gitignore`，避免污染仓库。
5. **Testcontainers 依赖未使用**
   - 增加构建时间与依赖面，建议移除或实际使用。
6. **数据持久化策略**
   - `ddl-auto=create-drop` + 内存 H2 意味着每次重启数据清空；对演示是便利，对真实业务是数据丢失风险。若切换到持久化数据库，VUL-04 的 H2 空密码问题会升级为更严重的配置风险。
7. **既有审计报告 F-02 已闭环**
   - `reports/审计报告.md` 的 F-02 描述与当前代码对比后确认已修复；建议在报告中更新“整改状态”，并补充自动化回归测试。

---

## 10. 结论与下一步

### 10.1 最终结论

- **当前项目是一个没有安全边界的 CRUD 演示应用**：所有用户数据和管理操作均暴露给匿名调用者。
- **最严重的问题不是某一个接口，而是缺少认证/授权体系**；单点修补（例如只加 CORS 白名单或只关闭 H2 Console）不能解决根本问题。
- **SQL 注入不存在**，Repository 层参数化写法是正确的；但 LIKE 通配符、输入长度、分页和限流仍需完善。
- **历史 F-02 已修复**，但 PUT 的 over-posting 面和死代码仍构成回归风险，测试也未同步。
- **H2 Console + CORS 通配符**使本机开发环境也可能被浏览器侧攻击利用，不能因为“只监听 localhost”而忽视。
- 在完成 P0 修复前，建议仅在本机回环地址运行，不要部署到任何共享或公网环境。

### 10.2 本次审计状态

- ✅ 已完成：目录结构、依赖、配置、调用链、全部用户接口、IDOR/越权、SQL 注入、参数篡改、历史 F-02 复核、风险定级、验证方法、修复建议。
- ⛔ 未执行：应用启动、HTTP 请求、数据库变更、文件修改、Git 提交/推送、SCA/DAST。
- 报告未落盘，未修改项目中的任何文件。

**已完成第一阶段只读代码审计，等待你的下一步指示。**
