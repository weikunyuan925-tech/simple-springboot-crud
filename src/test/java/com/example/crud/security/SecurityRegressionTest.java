package com.example.crud.security;

import com.example.crud.entity.SysUser;
import com.example.crud.repository.SysUserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USERNAME =
            "security_regression_user";

    private static final String TEST_PASSWORD =
            "TestPass123!";

    /**
     * SEC-01
     * 匿名用户访问用户列表
     * 预期：401 Unauthorized
     */
    @Test
    @DisplayName("SEC-01 匿名访问用户列表应返回401")
    void sec01_anonymousGetUserList_returns401() throws Exception {

        mockMvc.perform(
                        get("/api/users")
                )
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * SEC-02
     * 匿名用户访问用户详情
     * 预期：401 Unauthorized
     */
    @Test
    @DisplayName("SEC-02 匿名访问用户详情应返回401")
    void sec02_anonymousGetUserDetail_returns401() throws Exception {

        mockMvc.perform(
                        get("/api/users/1")
                )
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * SEC-03
     * 匿名用户访问健康检查
     * 预期：200 OK
     */
    @Test
    @DisplayName("SEC-03 匿名访问健康检查应返回200")
    void sec03_anonymousHealthCheck_returns200() throws Exception {

        mockMvc.perform(
                        get("/api/users/health")
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service")
                        .value("User Management API"));
    }

    /**
     * SEC-04
     * 携带无效 JWT 访问用户列表
     * 预期：401 Unauthorized
     */
    @Test
    @DisplayName("SEC-04 携带无效JWT访问用户列表应返回401")
    void sec04_invalidJwtGetUserList_returns401() throws Exception {

        mockMvc.perform(
                        get("/api/users")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-test-token"
                                )
                )
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * SEC-05
     * 不携带 Token 登录
     * 预期：
     * 1. 登录接口允许匿名访问
     * 2. 正确账号密码可以登录
     * 3. 返回 JWT Token
     */
    @Test
    @DisplayName("SEC-05 不携带Token登录应成功并返回JWT")
    void sec05_loginWithoutToken_returns200AndJwt() throws Exception {

        // 第一步：创建测试账号
        SysUser testUser = sysUserRepository
                .findByUsername(TEST_USERNAME)
                .orElseGet(SysUser::new);

        testUser.setUsername(TEST_USERNAME);

        // 使用 BCrypt 加密测试密码
        testUser.setPassword(
                passwordEncoder.encode(TEST_PASSWORD)
        );

        testUser.setRole("USER");

        sysUserRepository.save(testUser);

        // 第二步：构造登录请求
        Map<String, String> loginRequest = Map.of(
                "username", TEST_USERNAME,
                "password", TEST_PASSWORD
        );

        // 第三步：不设置 Authorization 请求头，直接登录
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }
}
