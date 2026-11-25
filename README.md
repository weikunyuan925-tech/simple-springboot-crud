# Simple Spring Boot CRUD

A Spring Boot REST API project for user management, security auditing, automated security testing, and dependency vulnerability verification.

本项目从基础 CRUD 应用逐步扩展为一个用于学习和实践 Java 后端安全审计的项目，重点覆盖 JWT 身份认证、对象级访问控制、安全回归测试、漏洞复测和依赖安全扫描。

## 1. Project Overview

- **Project:** simple-springboot-crud
- **Language:** Java 17
- **Framework:** Spring Boot 4.1.3
- **Build Tool:** Maven
- **Database:** H2
- **Security:** Spring Security, JWT
- **JSON:** Jackson 3 (Spring Boot Web layer)
- **Testing:** JUnit 5, Spring Boot Test, MockMvc
- **Dependency Scanning:** OWASP Dependency-Check 13.0.0

## 2. Features

### Business Features

- User registration and login
- JWT-based authentication
- User profile CRUD operations
- User data ownership isolation
- Request parameter validation
- Centralized exception handling
- H2 in-memory database integration

### Security Features

- Stateless authentication using JWT
- JWT signature and expiration validation
- Protected API endpoints
- Object-level authorization
- Cross-user data access prevention
- Server-side ownership assignment
- Strict JSON input type validation
- HTTP security response headers
- H2 Console disabled by default
- Environment-variable-based configuration for JWT secrets and seeded account passwords

## 3. Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.3 |
| Web | Spring MVC |
| Persistence | Spring Data JPA |
| Database | H2 |
| Authentication | Spring Security |
| Token | JJWT 0.13.0 |
| JSON | Jackson 3 |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, MockMvc |
| Build | Maven |
| Dependency Security | OWASP Dependency-Check 13.0.0 |

Note: JJWT uses its `jjwt-jackson` JSON adapter, which brings in Jackson 2. The Spring Boot Web layer uses Jackson 3. Both dependencies currently coexist.

## 4. API Overview

Base URL:

```text
http://localhost:8080
```

### Authentication APIs

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | `/api/auth/register` | Register a user | Public |
| POST | `/api/auth/login` | Login and obtain JWT | Public |

### User APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users` | Get current user's profiles |
| GET | `/api/users/{id}` | Get a profile by ID |
| GET | `/api/users/email/{email}` | Query profile by email |
| GET | `/api/users/search?name={name}` | Search profiles by name |
| POST | `/api/users` | Create a profile |
| PUT | `/api/users/{id}` | Update a profile |
| DELETE | `/api/users/{id}` | Delete a profile |
| GET | `/api/users/health` | Health check |

The user profile APIs require JWT authentication, except for the health-check endpoint.

For protected endpoints, include the token in the request header:

```http
Authorization: Bearer <JWT>
```

Object-level authorization is applied to user profile operations. A user should only be able to access and modify their own data.

## 5. Getting Started

### Prerequisites

- JDK 17 or later
- Maven 3.6 or later
- Git

### Clone the Repository

```bash
git clone https://github.com/weikunyuan925-tech/simple-springboot-crud.git
cd simple-springboot-crud
```

### Configure Environment Variables

The application configuration references the following environment variables:

- `JWT_SECRET`
- `SEED_USER_A_PASSWORD`
- `SEED_USER_B_PASSWORD`
- `SEED_ADMIN_PASSWORD`

Configure these variables in your local environment before starting the application. Use strong, private values and do not commit secrets to Git.

PowerShell example (replace the placeholders with your own values):

```powershell
$env:JWT_SECRET = "<your-strong-jwt-secret>"
$env:SEED_USER_A_PASSWORD = "<your-user-a-password>"
$env:SEED_USER_B_PASSWORD = "<your-user-b-password>"
$env:SEED_ADMIN_PASSWORD = "<your-admin-password>"
```

### Build the Project

```bash
mvn clean compile
```

### Run Automated Tests

```bash
mvn clean test
```

### Start the Application

```bash
mvn spring-boot:run
```

The application listens on:

```text
http://localhost:8080
```

The default database is an H2 in-memory database. H2 Console is disabled in the default application configuration.

## 6. Automated Security Testing

The project contains four main automated security test groups.

| Test Group | Test Focus | Main Coverage |
|---|---|---|
| T1 | Security Regression Baseline | Anonymous access, health check, invalid JWT, login authentication |
| T2 | JWT Authentication | Login, valid JWT, authenticated access |
| T3 | JWT Negative Testing | Forged tokens, modified payload, modified signature, expired tokens, invalid Authorization format |
| T4 | Authorization and IDOR/BOLA | Cross-user reads, queries, updates, deletes, and forged ownerId |

### Test Execution

Run all security regression tests:

```bash
mvn "-Dtest=SecurityRegressionTest,JwtAuthenticationTest,JwtTokenNegativeTest,JwtAuthorizationTest" "-DforkCount=0" test
```

Run the complete test suite:

```bash
mvn clean test
```

### Recorded Security Regression Results

According to the existing T1–T4 regression report:

| Metric | Result |
|---|---:|
| Test classes | 8 |
| Test cases | 48 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Pass rate | 100% |

The project was also re-tested after upgrading to Spring Boot 4.1.3. The full Maven test suite completed successfully.

These results describe the recorded test runs and covered scenarios; they do not prove the absence of all security vulnerabilities.

## 7. Dependency Upgrade and Vulnerability Scanning

The project was upgraded from Spring Boot 3.2.0 to Spring Boot 4.1.3.

Main migration work:

- Spring Boot version upgrade
- Jackson 3 migration for the Web layer
- Spring Boot 4 test dependency adjustments
- Test package and annotation migration
- JSON coercion configuration migration
- Full automated test regression

### OWASP Dependency-Check

Tool: OWASP Dependency-Check 13.0.0

Recorded scan results on November 25, 2026:

| Metric | Before Upgrade | After Upgrade |
|---|---:|---:|
| Dependencies Scanned | 76 | 92 |
| Unique Dependencies | 45 | 50 |
| Vulnerable Dependencies | 8 | 0 |
| Vulnerability Records | 105 | 0 |
| Critical | 24 | 0 |
| High | 42 | 0 |
| Medium | 34 | 0 |
| Low | 5 | 0 |

The latest recorded scan found no known CVEs in the scanned dependencies.

Important: The before-and-after scans were performed at different times, and the NVD vulnerability database changes over time. The results should not be interpreted as proof that every previously reported vulnerability was eliminated solely by the dependency upgrade.

Dependency scanning does not detect every application-level or business-logic vulnerability.

## 8. F16 JSON Input Validation Retest

After migrating to Jackson 3, the project re-tested strict JSON type validation.

| Input Type | Expected Result | Actual Result |
|---|---|---|
| Integer | HTTP 400 | HTTP 400 |
| Floating-point number | HTTP 400 | HTTP 400 |
| Boolean | HTTP 400 | HTTP 400 |
| Array | HTTP 400 | HTTP 400 |
| Object | HTTP 400 | HTTP 400 |

All five test cases passed. Database verification found no user records created by these invalid inputs.

Date serialization was also checked. `createdAt` and `updatedAt` were returned as ISO-8601 strings rather than numeric timestamps.

## 9. Security Audit Reports

The `reports/` directory contains the project's audit and testing documentation.

| Report | Description |
|---|---|
| `2026.11.20simple-spring-crud审计报告.md` | Second-stage security audit and remediation review |
| `Claude Code审计simple-springboot-crud报告，第一次静态审计报告.md` | Initial static audit report |
| `审计simple-springboot-crud问题归总.md` | Consolidated security findings |
| `T1：安全回归测试基线报告.md` | Security regression baseline |
| `T2：JWT 身份认证测试报告.md` | JWT authentication tests |
| `T3：JWT 异常与过期测试报告.md` | JWT negative and expiration tests |
| `T4：JWT 权限控制与越权访问测试报告.md` | Authorization and IDOR/BOLA tests |
| `第三阶段：自动化安全测试与漏洞复测总结报告.md` | Automated security testing summary |
| `dependency-check/` | Before-and-after dependency scan evidence and upgrade report |

## 10. Project Structure

```text
simple-springboot-crud/
├── src/
│   ├── main/
│   │   ├── java/com/example/crud/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   └── test/
│       ├── java/com/example/crud/
│       │   ├── controller/
│       │   ├── repository/
│       │   ├── service/
│       │   └── security/
│       └── resources/
├── reports/
│   ├── dependency-check/
│   └── security audit and test reports
├── pom.xml
└── README.md
```

## 11. Known Limitations and Future Improvements

The current project is a security learning and testing project, not a production-certified system.

Areas for further testing and improvement include:

- Role-based access control (RBAC) with explicit ADMIN-only business endpoints
- Rate limiting and brute-force protection
- Concurrent request and data consistency testing
- Pagination and resource consumption controls
- Additional security testing for newly introduced endpoints
- Automated security regression execution through GitHub Actions
- Further review of logging, deployment configuration, and production database security

## 12. Project Status

Completed:

- Initial CRUD application
- Security audit and remediation iterations
- JWT authentication
- Object-level authorization testing
- T1–T4 automated security regression tests
- F16 strict JSON type validation retest
- Spring Boot 4.1.3 dependency upgrade
- Jackson 3 migration for the Web layer
- OWASP Dependency-Check scan and evidence archiving
- Git version control and report organization

**Current status:** Third-stage automated security testing, dependency upgrade, and documented security retesting completed.

The project demonstrates a practical workflow of security review, issue verification, remediation, automated regression testing, dependency scanning, and evidence-based reporting.

---

Repository: https://github.com/weikunyuan925-tech/simple-springboot-crud