package com.example.crud.security;

import com.example.crud.entity.SysUser;
import com.example.crud.entity.User;
import com.example.crud.repository.SysUserRepository;
import com.example.crud.repository.UserRepository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {

    private static final String TEST_USERNAME =
            "jwt_auth_test_user";
    private static final String TEST_PASSWORD =
            "JwtTestPass123!";
    private static final String TEST_EMAIL =
            "jwt-auth-test@example.test";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SysUserRepository sysUserRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    private Long ownedProfileId;

    /**
     * 每个测试方法执行前：
     * 1. 创建或获取测试登录账号
     * 2. 使用 BCrypt 加密密码
     * 3. 创建属于该账号的业务资料
     */
    @BeforeEach
    void setUp() {

        SysUser account = sysUserRepository
                .findByUsername(TEST_USERNAME)
                .orElseGet(SysUser::new);

        account.setUsername(TEST_USERNAME);

        account.setPassword(
                passwordEncoder.encode(TEST_PASSWORD)
        );

        account.setRole("USER");

        SysUser savedAccount =
                sysUserRepository.save(account);

        User profile = userRepository
                .findByEmailAndOwner_Id(
                        TEST_EMAIL,
                        savedAccount.getId()
                )
                .orElse(null);

        if (profile == null) {

            profile = new User(
                    "JWT Authentication Test",
                    TEST_EMAIL,
                    "13800000001"
            );

            profile.setOwner(savedAccount);

            profile = userRepository.save(profile);
        }

        ownedProfileId = profile.getId();
    }

    /**
     * JWT-01
     * 使用正确账号密码登录
     * 预期：200，返回 JWT
     */
    @Test
    @DisplayName("JWT-01 正确账号密码登录应返回JWT")
    void jwt01_loginReturnsJwt() throws Exception {

        Map<String, String> loginRequest = Map.of(
                "username", TEST_USERNAME,
                "password", TEST_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }


    /**
     * JWT-01-NEG
     * 使用正确用户名、错误密码登录
     * 预期：401 Unauthorized
     */
    @Test
    @DisplayName("JWT-01-NEG 错误密码登录应返回401")
    void jwt01_wrongPasswordReturns401() throws Exception {

        Map<String, String> loginRequest = Map.of(
                "username", TEST_USERNAME,
                "password", "WrongPassword123!"
        );

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }



    /**
     * JWT-02
     * 使用有效 JWT 访问用户列表
     * 预期：200
     */
    @Test
    @DisplayName("JWT-02 有效JWT访问用户列表应返回200")
    void jwt02_validJwtAccessUserList() throws Exception {

        String token = loginAndGetToken();

        mockMvc.perform(
                        get("/api/users")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(
                        jsonPath("$.users[*].email")
                                .value(hasItem(TEST_EMAIL))
                );
    }

    /**
     * JWT-03
     * 使用有效 JWT 访问自己的用户资料
     * 预期：200
     */
    @Test
    @DisplayName("JWT-03 有效JWT访问自己的用户资料应返回200")
    void jwt03_validJwtAccessOwnUserDetail() throws Exception {

        String token = loginAndGetToken();

        mockMvc.perform(
                        get("/api/users/" + ownedProfileId)
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownedProfileId))
                .andExpect(jsonPath("$.name")
                        .value("JWT Authentication Test"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    /**
     * 登录并提取 JWT
     */
    private String loginAndGetToken() throws Exception {

        Map<String, String> loginRequest = Map.of(
                "username", TEST_USERNAME,
                "password", TEST_PASSWORD
        );

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String token = response.path("token").asText();

        assertFalse(token.isBlank(), "登录返回的 JWT 不能为空");

        return token;
    }
}
