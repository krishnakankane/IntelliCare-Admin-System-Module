package com.intellicare.controller;

import com.intellicare.dto.request.AuthRequest;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.dto.response.AuthResponse;
import com.intellicare.dto.response.UserResponse;
import com.intellicare.entity.User;
import com.intellicare.service.AuthService;
import com.intellicare.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, token management")
public class AuthController {

    private final AuthService authService;
    private final RequestUtil requestUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user (defaults to PATIENT role)")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> register(
            @Valid @RequestBody AuthRequest.Register request,
            HttpServletRequest httpRequest) {

        AuthResponse.TokenPair tokens = authService.register(request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User registered successfully", tokens));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> login(
            @Valid @RequestBody AuthRequest.Login request,
            HttpServletRequest httpRequest) {

        AuthResponse.TokenPair tokens = authService.login(request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Login successful", tokens));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> refresh(
            @Valid @RequestBody AuthRequest.RefreshToken request,
            HttpServletRequest httpRequest) {

        AuthResponse.TokenPair tokens = authService.refreshToken(
                request.getRefreshToken(), requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate current refresh token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AuthRequest.RefreshToken request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        authService.logout(request.getRefreshToken(), user.getId(), requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        authService.logoutAll(user.getId(), requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("All sessions invalidated"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody AuthRequest.ChangePassword request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        authService.changePassword(user.getId(), request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Password changed. Please log in again."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserResponse.Full>> me(
            @AuthenticationPrincipal User user) {

        UserResponse.Full profile = authService.getCurrentUser(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
