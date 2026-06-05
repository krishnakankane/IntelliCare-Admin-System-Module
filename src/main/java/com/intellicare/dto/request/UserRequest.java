package com.intellicare.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

public final class UserRequest {

    private UserRequest() {}

    @Data
    public static class UpdateProfile {
        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Size(max = 20)
        private String phoneNumber;
    }

    @Data
    public static class AssignRoles {
        @NotNull(message = "Roles are required")
        private Set<String> roles;
    }

    @Data
    public static class AdminCreateUser {

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8, max = 64)
        private String password;

        @NotBlank @Size(max = 100)
        private String firstName;

        @NotBlank @Size(max = 100)
        private String lastName;

        @Size(max = 20)
        private String phoneNumber;

        @NotNull
        private Set<String> roles;

        private boolean isActive = true;
        private boolean isVerified = false;
    }
}
