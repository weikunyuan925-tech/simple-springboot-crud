
package com.example.crud.config;

import com.example.crud.entity.SysUser;
import com.example.crud.entity.User;
import com.example.crud.repository.SysUserRepository;
import com.example.crud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.seed.enabled",
        havingValue = "true"
)
public class DataInitializer implements CommandLineRunner {

    @Value("${app.seed.user-a.password}")
    private String userAPassword;

    @Value("${app.seed.user-b.password}")
    private String userBPassword;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    private final SysUserRepository sysUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SysUserRepository sysUserRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.sysUserRepository = sysUserRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 创建三个测试账号
        SysUser userA = createAccount(
                "audit_user_a",
                userAPassword,
                "USER"
        );

        SysUser userB = createAccount(
                "audit_user_b",
                userBPassword,
                "USER"
        );

        SysUser admin = createAccount(
                "audit_admin",
                adminPassword,
                "ADMIN"
        );

        // 创建归属于不同账号的业务资料
        // audit_user_a：4 条业务资料
        createProfile("Audit User A", "audit-a@example.test", "13800000001", userA);
        createProfile("张三", "zhangsan@example.test", "13800000011", userA);
        createProfile("李四", "lisi@example.test", "13800000012", userA);
        createProfile("王五", "wangwu@example.test", "13800000013", userA);

        // audit_user_b：4 条业务资料
        createProfile("Audit User B", "audit-b@example.test", "13800000002", userB);
        createProfile("赵六", "zhaoliu@example.test", "13800000021", userB);
        createProfile("钱七", "qianqi@example.test", "13800000022", userB);
        createProfile("孙八", "sunba@example.test", "13800000023", userB);

        // audit_admin：2 条业务资料
        createProfile("Audit Admin", "audit-admin@example.test", "13800000003", admin);
        createProfile("管理员测试资料", "admin-test@example.test", "13800000031", admin);

    }

    private SysUser createAccount(
            String username,
            String rawPassword,
            String role
    ) {
        return sysUserRepository.findByUsername(username)
                .orElseGet(() -> {
                    SysUser sysUser = new SysUser();
                    sysUser.setUsername(username);
                    //Spring Security 的 `PasswordEncoder` 对密码进行 BCrypt 加密
                    sysUser.setPassword(
                            passwordEncoder.encode(rawPassword)
                    );
                    sysUser.setRole(role);

                    return sysUserRepository.save(sysUser);
                });
    }

    private void createProfile(
            String name,
            String email,
            String phone,
            SysUser owner
    ) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User(name, email, phone);
        user.setOwner(owner);

        userRepository.save(user);
    }
}
