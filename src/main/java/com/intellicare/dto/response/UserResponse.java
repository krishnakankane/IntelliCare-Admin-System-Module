package com.intellicare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.Set;

public final class UserResponse {

    private UserResponse() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Full {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private boolean isActive;
        private boolean isVerified;
        private Set<String> roles;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant lastLoginAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Set<String> roles;
    }
}
