package com.intellicare.service.impl;

import com.intellicare.config.JwtProperties;
import com.intellicare.dto.request.AuthRequest;
import com.intellicare.dto.response.AuthResponse;
import com.intellicare.dto.response.UserResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.entity.RefreshToken;
import com.intellicare.entity.Role;
import com.intellicare.entity.User;
import com.intellicare.exception.BadRequestException;
import com.intellicare.exception.ConflictException;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.exception.TokenException;
import com.intellicare.repository.RefreshTokenRepository;
import com.intellicare.repository.RoleRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.security.JwtUtil;
import com.intellicare.service.AuditLogService;
import com.intellicare.service.AuthService;
import com.intellicare.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        Role patientRole = roleRepository.findByName(Role.RoleName.ROLE_PATIENT.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_PATIENT"));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .isVerified(false)
                .roles(Set.of(patientRole))
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} [id={}]", user.getEmail(), user.getId());

        auditLogService.log(AuditLog.ActionType.USER_CREATED, user.getId(), user.getEmail(),
                "User", String.valueOf(user.getId()),
                "User registered via self-service", ipAddress, AuditLog.Status.SUCCESS);

        return buildTokenPair(user);
    }

    @Override
    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception ex) {
            auditLogService.log(AuditLog.ActionType.LOGIN_FAILED, null, request.getEmail(),
                    null, null, "Login failed: " + ex.getMessage(), ipAddress, AuditLog.Status.FAILURE);
            throw ex;
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        userRepository.updateLastLoginAt(user.getId(), Instant.now());

        auditLogService.log(AuditLog.ActionType.USER_LOGIN, user.getId(), user.getEmail(),
                "User", String.valueOf(user.getId()),
                "Successful login", ipAddress, AuditLog.Status.SUCCESS);

        return buildTokenPair(user);
    }

    @Override
    @Transactional
    public AuthResponse.TokenPair refreshToken(String rawToken, String ipAddress) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            throw new TokenException("Refresh token has been revoked");
        }
        if (storedToken.isExpired()) {
            throw new TokenException("Refresh token has expired");
        }

        User user = storedToken.getUser();

        // Rotate refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        auditLogService.log(AuditLog.ActionType.TOKEN_REFRESHED, user.getId(), user.getEmail(),
                null, null, "Token refreshed", ipAddress, AuditLog.Status.SUCCESS);

        return buildTokenPair(user);
    }

    @Override
    @Transactional
    public void logout(String rawToken, Long userId, String ipAddress) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getEmail() : "unknown";

        auditLogService.log(AuditLog.ActionType.USER_LOGOUT, userId, username,
                null, null, "User logged out", ipAddress, AuditLog.Status.SUCCESS);
    }

    @Override
    @Transactional
    public void logoutAll(Long userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        refreshTokenRepository.revokeAllUserTokens(user);

        auditLogService.log(AuditLog.ActionType.USER_LOGOUT, userId, user.getEmail(),
                null, null, "All sessions invalidated", ipAddress, AuditLog.Status.SUCCESS);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, AuthRequest.ChangePassword request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all tokens to force re-login
        refreshTokenRepository.revokeAllUserTokens(user);

        auditLogService.log(AuditLog.ActionType.PASSWORD_CHANGED, userId, user.getEmail(),
                "User", String.valueOf(userId),
                "Password changed", ipAddress, AuditLog.Status.SUCCESS);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse.Full getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userMapper.toFull(user);
    }

    // ===== Private helpers =====

    private AuthResponse.TokenPair buildTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);

        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(Instant.now().plusMillis(
                        // Read from JwtUtil property
                        jwtProperties.getRefreshExpirationMs()
                ))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .user(userMapper.toSummary(user))
                .build();
    }
}
