版本号：

4ba36e8cb0f516936125675bafe584f07f36473c



## 1.哪些接口可以被外部请求访问？

get  /api/users

get  /api/users/search

get  /api/users/{id}

get  /api/users/email/{email}

get  /api/users/health

post  /api/users

put  /api/users/{id}

delete  /api/users/{id}



## 2Controller 接收到请求后，调用了哪个 Service？

get  /api/users --> UserService.getAllUsers

get  /api/users/{id} --> UserService.getUserById

get  /api/users/email/{email} --> UserService.getUserByEmail

get  /api/users/search --> UserService.searchUsersByName

post  /api/users --> UserService.createUser

put  /api/users/{id} --> UserService.updateUser

delete  /api/users/{id} --> UserService.deleteUser



## 3.Service 如何操作数据库？

UserService：

```java
private final UserRepository userRepository;
```

这是声明数据库访问对象。

构造方法：

```java
public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

Spring 会自动注入 UserRepository。

例如查询用户：

```java
public UserDTO getUserById(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() ->
                new UserNotFoundException("User not found with id: " + id)
            );
    return convertToDTO(user);
}
```

它的执行过程是：

```
Controller 收到 GET /api/users/1
              |
              v
UserController.getUserById(1)
              |
              v
UserService.getUserById(1)
              |
              v
UserRepository.findById(1)
              |
              v
Spring Data JPA 执行数据库查询
              |
              v
得到 User 实体
              |
              v
convertToDTO()
              |
              v
返回 JSON 响应
```

项目的数据库访问流程。

其中 `data.sql` 是初始化数据文件。

作用是：在应用初始化时，向数据库插入预设的测试数据。

而真正的数据库操作是在运行期间由 UserService 和 UserRepository 完成的。

H2 是一个 Java 关系型数据库。这个项目使用的是内存数据库：

```properties
spring.datasource.url=jdbc:h2:mem:testdb 
#mem:testdb表示使用内存中的数据库。
spring.datasource.username=sa
spring.datasource.password=
```

项目还配置了：

```
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

启动项目后，可以通过浏览器访问：

```
http://localhost:8080/h2-console
```

连接后可以查看数据库表和数据，也可以执行 SQL 查询。



## 4.哪些地方对用户输入进行了校验？

phone，email，name

打开 `UserDTO.java`：

```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100,
      message = "Name must be between 2 and 100 characters")
private String name;

@NotBlank(message = "Email is required")
@Email(message = "Email should be valid")
private String email;

@Size(max = 15,
      message = "Phone number cannot exceed 15 characters")
private String phone;
```



## 5.如果执行：userRepository.save(user);它究竟是新增数据，还是修改数据？

在 Spring Data JPA 中，默认情况下，对这个项目的 `User` 实体：

| Entity 的 ID | `save()` 默认行为            |
| ------------ | ---------------------------- |
| `null`       | 调用 `persist()`，走新增路径 |
| 非 null      | 调用 `merge()`，走合并路径   |

因为项目中使用了：

```
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

并且没有定义 `@Version`，所以默认会根据 ID 是否为 null 判断是否为新实体。

这意味着：

```
User user = new User();
user.setId(null);
userRepository.save(user);
```

通常是新增。

而：

```
User user = new User();
user.setId(1L);
userRepository.save(user);
```

会走 `merge()` 路径。如果数据库已经存在 ID 为 1 的用户，传入实体的状态就可能合并到该记录中。

注意：`merge()` 不等同于无条件 UPDATE。如果 ID 对应的记录不存在，实际行为还取决于实体状态判断、JPA 实现和数据库生成策略。

所以，你要记住：`save()` 不会因为你调用了 POST，就自动保证是新增。



## 6.下面这段代码：   你认为哪一行最值得安全审计人员关注？为什么？

```java
private User convertToEntity(UserDTO userDTO) {
    User user = new User();
    user.setId(userDTO.getId());
    user.setName(userDTO.getName());
    user.setEmail(userDTO.getEmail());
    user.setPhone(userDTO.getPhone());
    return user;
}
```

数据库已经通过：

```
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

保证主键的生成和唯一性。

真正的问题是：

```
user.setId(userDTO.getId());
```

它让客户端有机会控制数据库实体的主键。

例如，数据库当前存在：

| ID   | Name       | Email                  |
| ---- | ---------- | ---------------------- |
| 1    | John Doe   | john.doe@example.com   |
| 2    | Jane Smith | jane.smith@example.com |

客户端调用创建用户接口时，提交：

```json
{
  "id": 1,
  "name": "Audit Test",
  "email": "audit-test-2026@example.test",
  "phone": "123456789"
}
```

然后执行：

```java
User user = convertToEntity(userDTO);
User savedUser = userRepository.save(user);
```

客户端传入的 ID 就进入了 Entity。

如果数据库中 ID 为 1 的记录存在，JPA 的 `merge()` 就可能将传入的数据合并到原有记录。

所以客户端不应该有权决定新建 Entity 的数据库主键。

这属于需要重点防范的客户端可控属性赋值问题，也可以从 Mass Assignment（批量赋值）角度分析。



## 7.如果攻击者通过 POST 请求传入：   你认为接口会如何处理？分别分析三个字段。

```json
{
  "name": "A",
  "email": "not-an-email",
  "phone": "12345678901234567890"
}
```

请求进入 Controller 后，@Valid 会先触发 DTO 校验。

Bean Validation 可以一次收集多个字段的校验错误，不是第一个字段失败后就一定停止。

随后项目的全局异常就会抛出

```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "name": "Name must be between 2 and 100 characters",
    "email": "Email should be valid",
    "phone": "Phone number cannot exceed 15 characters"
  }
}
```

这次请求不会进入 `UserService.createUser()`。



## 8.问题：为什么创建用户时，不让客户端提交 `id`，而更新用户时又可以通过 URL 中的 `/api/users/{id}` 指定 ID？

创建用户时，不让客户端提交 `id`是因为这时的id已经是数据库自增长的，且唯一的，不需要指定，更新用户时又可以通过 URL 中的 `/api/users/{id}` 指定 ID，是为了将具体修改的哪一条数据进行修改。逻辑应该是这样。

创建时由数据库生成 ID；更新时由 URL 中的 ID 定位已有记录，而不是由客户端通过请求体决定要写入哪个主键。



