package com.intellicare.service;

import com.intellicare.dto.request.AuthRequest;
import com.intellicare.dto.response.AuthResponse;
import com.intellicare.dto.response.UserResponse;

public interface AuthService {

    AuthResponse.TokenPair register(AuthRequest.Register request, String ipAddress);

    AuthResponse.TokenPair login(AuthRequest.Login request, String ipAddress);

    AuthResponse.TokenPair refreshToken(String refreshToken, String ipAddress);

    void logout(String refreshToken, Long userId, String ipAddress);

    void logoutAll(Long userId, String ipAddress);

    void changePassword(Long userId, AuthRequest.ChangePassword request, String ipAddress);

    UserResponse.Full getCurrentUser(Long userId);
}
