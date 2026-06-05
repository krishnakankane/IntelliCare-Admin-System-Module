package com.intellicare.security;

import com.intellicare.config.JwtProperties;
import com.intellicare.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("IntelliCareTestSecretKeyForJWTThatIsLongEnoughForHMACSHA512Signing2024!!!");
        props.setExpirationMs(900_000L);
        props.setRefreshExpirationMs(604_800_000L);

        jwtUtil = new JwtUtil(props);

        testUser = User.builder()
                .id(1L).email("test@intellicare.com")
                .passwordHash("hashed").firstName("Test").lastName("User")
                .isActive(true).build();
    }

    @Test
    @DisplayName("generateAccessToken — returns a non-blank JWT string")
    void generateAccessToken_returnsToken() {
        String token = jwtUtil.generateAccessToken(testUser);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("extractUsername — returns email from valid token")
    void extractUsername_fromValidToken() {
        String token = jwtUtil.generateAccessToken(testUser);
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("test@intellicare.com");
    }

    @Test
    @DisplayName("isTokenValid — valid token for same user returns true")
    void isTokenValid_sameUser_returnsTrue() {
        String token = jwtUtil.generateAccessToken(testUser);
        assertThat(jwtUtil.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid — tampered token returns false")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateAccessToken(testUser) + "tampered";
        assertThat(jwtUtil.isTokenValid(token, testUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired — fresh token is not expired")
    void freshToken_isNotExpired() {
        String token = jwtUtil.generateAccessToken(testUser);
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }
}
