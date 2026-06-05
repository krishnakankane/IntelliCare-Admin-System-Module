package com.intellicare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellicare.config.SecurityConfig;
import com.intellicare.dto.request.AuthRequest;
import com.intellicare.dto.response.AuthResponse;
import com.intellicare.dto.response.UserResponse;
import com.intellicare.security.JwtAuthenticationFilter;
import com.intellicare.security.JwtUtil;
import com.intellicare.security.UserDetailsServiceImpl;
import com.intellicare.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /auth/register — valid request returns 201 with token pair")
    void register_validRequest_returns201() throws Exception {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setEmail("new@intellicare.com");
        request.setPassword("Strong@Pass1");
        request.setFirstName("First");
        request.setLastName("Last");

        AuthResponse.TokenPair tokenPair = AuthResponse.TokenPair.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(UserResponse.Summary.builder()
                        .id(1L).email("new@intellicare.com")
                        .firstName("First").lastName("Last")
                        .roles(Set.of("ROLE_PATIENT")).build())
                .build();

        when(authService.register(any(), any())).thenReturn(tokenPair);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"));
    }

    @Test
    @DisplayName("POST /auth/register — invalid email returns 400 with validation errors")
    void register_invalidEmail_returns400() throws Exception {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setEmail("not-an-email");
        request.setPassword("Str0ng@Pass");
        request.setFirstName("First");
        request.setLastName("Last");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /auth/login — valid credentials return 200")
    void login_validCredentials_returns200() throws Exception {
        AuthRequest.Login request = new AuthRequest.Login();
        request.setEmail("test@intellicare.com");
        request.setPassword("password");

        AuthResponse.TokenPair tokenPair = AuthResponse.TokenPair.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.login(any(), any())).thenReturn(tokenPair);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }
}
