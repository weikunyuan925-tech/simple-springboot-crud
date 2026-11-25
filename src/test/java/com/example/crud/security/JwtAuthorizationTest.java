package com.example.crud.security;

import com.example.crud.entity.SysUser;
import com.example.crud.entity.User;
import com.example.crud.repository.SysUserRepository;
import com.example.crud.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthorizationTest {

    private static final String USER_A = "auth_user_a";
    private static final String USER_B = "auth_user_b";

    private static final String PASSWORD = "AuthTestPass123!";

    private static final String EMAIL_A = "auth-a@example.test";
    private static final String EMAIL_B = "auth-b@example.test";

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

    private Long profileAId;
    private Long profileBId;

    @BeforeEach
    void setUp() {

        SysUser accountA = createOrUpdateAccount(USER_A);
        SysUser accountB = createOrUpdateAccount(USER_B);

        User profileA = createOrGetProfile(
                accountA,
                EMAIL_A,
                "Authorization User A",
                "13800000011"
        );

        User profileB = createOrGetProfile(
                accountB,
                EMAIL_B,
                "Authorization User B",
                "13800000012"
        );

        profileAId = profileA.getId();
        profileBId = profileB.getId();

        assertNotNull(profileAId);
        assertNotNull(profileBId);
        assertNotEquals(profileAId, profileBId);
    }

    /**
     * T4-AUTH-01
     * 用户 A 访问自己的资料。
     * 预期：200。
     */
    @Test
    @DisplayName("T4-AUTH-01 用户访问自己的资料应返回200")
    void userCanAccessOwnProfile() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        get("/api/users/" + profileAId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileAId))
                .andExpect(jsonPath("$.email").value(EMAIL_A));
    }

    /**
     * T4-AUTH-02
     * 用户 A 尝试访问用户 B 的资料。
     * 预期：404。
     */
    @Test
    @DisplayName("T4-AUTH-02 用户不能访问其他用户的资料")
    void userCannotAccessOtherUsersProfile() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        get("/api/users/" + profileBId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                //.andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * T4-AUTH-03
     * 用户 A 查询列表时，只能看到自己的资料。
     * 预期：200，列表不包含用户 B 的资料。
     */
    @Test
    @DisplayName("T4-AUTH-03 用户列表只能返回自己的资料")
    void userListShouldOnlyContainOwnProfiles() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        get("/api/users")
                                .header("Authorization", "Bearer " + tokenA)
                )
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].email").value(hasItem(EMAIL_A)))
                .andExpect(jsonPath("$.users[*].email").value(not(hasItem(EMAIL_B))));
    }

    /**
     * T4-AUTH-04
     * 用户 A 尝试修改用户 B 的资料。
     * 预期：404。
     */
    @Test
    @DisplayName("T4-AUTH-04 用户不能修改其他用户的资料")
    void userCannotUpdateOtherUsersProfile() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        Map<String, String> updateRequest = Map.of(
                "name", "Hacked By User A",
                "email", EMAIL_B,
                "phone", "13800000099"
        );

        mockMvc.perform(
                        put("/api/users/" + profileBId)
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                //.andDo(print())
                .andExpect(status().isNotFound());

        User profileB = userRepository.findById(profileBId).orElseThrow();

        assertFalse(
                "Hacked By User A".equals(profileB.getName()),
                "用户 B 的资料不应被用户 A 修改"
        );
    }

    /**
     * T4-AUTH-05
     * 用户 A 尝试删除用户 B 的资料。
     * 预期：404，数据库记录仍存在。
     */
    @Test
    @DisplayName("T4-AUTH-05 用户不能删除其他用户的资料")
    void userCannotDeleteOtherUsersProfile() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        delete("/api/users/" + profileBId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                //.andDo(print())
                .andExpect(status().isNotFound());

        assertFalse(
                !userRepository.existsById(profileBId),
                "用户 B 的资料不应被用户 A 删除"
        );
    }


    /**
     * T4-AUTH-06
     * 用户 A 尝试通过用户 B 的邮箱查询资料。
     * 预期：404。
     */
    @Test
    @DisplayName("T4-AUTH-06 用户不能通过邮箱查询其他用户的资料")
    void userCannotFindOtherUsersProfileByEmail() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        get("/api/users/email/" + EMAIL_B)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isNotFound());
    }

    /**
     * T4-AUTH-07
     * 用户 A 搜索资料时，不能搜索出用户 B 的资料。
     * 预期：200，结果只包含用户 A 的资料。
     */
    @Test
    @DisplayName("T4-AUTH-07 姓名搜索只能返回当前用户的资料")
    void searchShouldOnlyReturnOwnProfiles() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        mockMvc.perform(
                        get("/api/users/search")
                                .param("name", "Authorization User")
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].email").value(hasItem(EMAIL_A)))
                .andExpect(jsonPath("$.users[*].email").value(not(hasItem(EMAIL_B))));
    }

    /**
     * T4-AUTH-08
     * 用户 A 创建资料时，尝试伪造 ownerId 为用户 B。
     * 预期：新资料仍归属于用户 A。
     */
    @Test
    @DisplayName("T4-AUTH-08 创建资料时不能伪造 ownerId")
    void createProfileShouldUseCurrentUserAsOwner() throws Exception {

        String tokenA = loginAndGetToken(USER_A);

        SysUser ownerA = sysUserRepository
                .findByUsername(USER_A)
                .orElseThrow();

        String requestBody = """
            {
                "name": "Forged Owner Test",
                "email": "forged-owner-test@example.test",
                "phone": "13800138000",
                "ownerId": 999999
            }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        User createdUser = userRepository
                .findByEmailAndOwner_Id(
                        "forged-owner-test@example.test",
                        ownerA.getId()
                )
                .orElseThrow(() -> new AssertionError(
                        "新资料没有归属于当前登录用户 A"
                ));

        assertEquals(ownerA.getId(), createdUser.getOwner().getId());
    }

    private SysUser createOrUpdateAccount(String username) {

        SysUser account = sysUserRepository
                .findByUsername(username)
                .orElseGet(SysUser::new);

        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(PASSWORD));
        account.setRole("USER");

        return sysUserRepository.save(account);
    }

    private User createOrGetProfile(
            SysUser owner,
            String email,
            String name,
            String phone) {

        return userRepository.findByEmailAndOwner_Id(
                        email,
                        owner.getId()
                )
                .orElseGet(() -> {
                    User profile = new User(name, email, phone);
                    profile.setOwner(owner);
                    return userRepository.save(profile);
                });
    }

    private String loginAndGetToken(String username) throws Exception {

        Map<String, String> loginRequest = Map.of(
                "username", username,
                "password", PASSWORD
        );

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String token = response.path("token").asText();

        assertFalse(token.isBlank(), "JWT 不能为空");

        return token;
    }
}
