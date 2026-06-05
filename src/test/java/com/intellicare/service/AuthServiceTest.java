package com.intellicare.service;

import com.intellicare.config.JwtProperties;
import com.intellicare.dto.request.AuthRequest;
import com.intellicare.dto.response.AuthResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.entity.RefreshToken;
import com.intellicare.entity.Role;
import com.intellicare.entity.User;
import com.intellicare.exception.ConflictException;
import com.intellicare.repository.RefreshTokenRepository;
import com.intellicare.repository.RoleRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.security.JwtUtil;
import com.intellicare.service.impl.AuthServiceImpl;
import com.intellicare.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditLogService auditLogService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role patientRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        patientRole = Role.builder().id(1L).name("ROLE_PATIENT").build();
        testUser = User.builder()
                .id(1L)
                .email("test@intellicare.com")
                .passwordHash("$2a$12$encoded")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .roles(Set.of(patientRole))
                .build();
    }

    @Test
    @DisplayName("register — success: new email creates user and returns token pair")
    void register_success() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setEmail("new@intellicare.com");
        request.setPassword("Strong@Pass1");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByEmail("new@intellicare.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_PATIENT")).thenReturn(Optional.of(patientRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-jwt-token");
        when(jwtUtil.getExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());
        when(userMapper.toSummary(any())).thenReturn(null);

        AuthResponse.TokenPair result = authService.register(request, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-jwt-token");
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(eq(AuditLog.ActionType.USER_CREATED), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("register — conflict: duplicate email throws ConflictException")
    void register_duplicateEmail_throws() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setEmail("existing@intellicare.com");
        request.setPassword("Strong@Pass1");
        request.setFirstName("Existing");
        request.setLastName("User");

        when(userRepository.existsByEmail("existing@intellicare.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("login — success: correct credentials return token pair and update last login")
    void login_success() {
        AuthRequest.Login request = new AuthRequest.Login();
        request.setEmail("test@intellicare.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@intellicare.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.getExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());
        when(userMapper.toSummary(any())).thenReturn(null);

        AuthResponse.TokenPair result = authService.login(request, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).updateLastLoginAt(eq(1L), any());
    }

    @Test
    @DisplayName("logout — revokes refresh token and logs audit event")
    void logout_revokesToken() {
        RefreshToken token = RefreshToken.builder()
                .token("some-refresh-token")
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("some-refresh-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        authService.logout("some-refresh-token", 1L, "127.0.0.1");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}
