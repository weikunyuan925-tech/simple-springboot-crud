package com.example.crud.security;

import com.example.crud.entity.SysUser;
import com.example.crud.repository.SysUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtTokenNegativeTest {

    private static final String TEST_USERNAME = "jwt_negative_test_user";
    private static final String TEST_PASSWORD = "JwtNegativePass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );

        if (!sysUserRepository.existsByUsername(TEST_USERNAME)) {
            SysUser user = new SysUser();
            user.setUsername(TEST_USERNAME);
            user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
            user.setRole("USER");
            sysUserRepository.save(user);
        }
    }

    /**
     * JWT-N01：完全伪造的 Token
     */
    @Test
    void fakeToken_ShouldReturn401() throws Exception {
        String token = "not-a-valid-jwt";

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                //.andDo(print())
                .andExpect(status().isUnauthorized());

    }

    /**
     * JWT-N02：篡改 Token Payload
     */
    @Test
    void tamperedPayload_ShouldReturn401() throws Exception {
        String token = createValidToken(TEST_USERNAME);

        String[] parts = token.split("\\.");

        String tamperedPayload = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        "{\"sub\":\"attacker\"}".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * JWT-N03：篡改 Token Signature
     */
    @Test
    void tamperedSignature_ShouldReturn401() throws Exception {
        String token = createValidToken(TEST_USERNAME);

        String[] parts = token.split("\\.");

        String tamperedSignature =
                (parts[2].charAt(0) == 'a' ? "b" : "a")
                        + parts[2].substring(1);

        String tamperedToken =
                parts[0] + "." + parts[1] + "." + tamperedSignature;

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * JWT-N04：签名合法但已经过期的 Token
     */
    @Test
    void expiredToken_ShouldReturn401() throws Exception {
        Date now = new Date();
        Date issuedAt = new Date(now.getTime() - 120_000);
        Date expiration = new Date(now.getTime() - 60_000);

        String expiredToken = Jwts.builder()
                .subject(TEST_USERNAME)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * JWT-N05：Authorization 格式异常
     */
    @Test
    void wrongAuthorizationScheme_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abc123"))
                //.andDo(print())
                .andExpect(status().isUnauthorized());
    }

    private String createValidToken(String username) {
        Date now = new Date();

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 300_000))
                .signWith(secretKey)
                .compact();
    }
}
