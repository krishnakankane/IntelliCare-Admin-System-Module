package com.intellicare.controller;

import com.intellicare.dto.request.UserRequest;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.dto.response.UserResponse;
import com.intellicare.entity.User;
import com.intellicare.service.UserService;
import com.intellicare.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User profile and admin user management")
public class UserController {

    private final UserService userService;
    private final RequestUtil requestUtil;

    // ===== Self-service =====

    @GetMapping("/me")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<UserResponse.Full>> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(user.getId())));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update own profile")
    public ResponseEntity<ApiResponse<UserResponse.Full>> updateMyProfile(
            @Valid @RequestBody UserRequest.UpdateProfile request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        UserResponse.Full updated = userService.updateProfile(
                user.getId(), request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
    }

    // ===== Admin operations =====

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] List all users")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<UserResponse.Full>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserResponse.Full> page = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Search users")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<UserResponse.Full>>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserResponse.Full> page = userService.searchUsers(email, firstName, lastName, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/by-role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get users by role")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<UserResponse.Full>>> getUsersByRole(
            @PathVariable String roleName,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserResponse.Full> page = userService.getUsersByRole(roleName, pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse.Full>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Create a user")
    public ResponseEntity<ApiResponse<UserResponse.Full>> createUser(
            @Valid @RequestBody UserRequest.AdminCreateUser request,
            HttpServletRequest httpRequest) {

        UserResponse.Full user = userService.adminCreateUser(request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created", user));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Assign roles to a user")
    public ResponseEntity<ApiResponse<UserResponse.Full>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest.AssignRoles request,
            HttpServletRequest httpRequest) {

        UserResponse.Full user = userService.assignRoles(id, request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Roles updated", user));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Activate a user account")
    public ResponseEntity<ApiResponse<UserResponse.Full>> activateUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok("User activated",
                userService.activateUser(id, requestUtil.extractClientIp(httpRequest))));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse.Full>> deactivateUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok("User deactivated",
                userService.deactivateUser(id, requestUtil.extractClientIp(httpRequest))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        userService.deleteUser(id, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("User deleted"));
    }
}
