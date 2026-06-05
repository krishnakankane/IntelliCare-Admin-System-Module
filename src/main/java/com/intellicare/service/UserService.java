package com.intellicare.service;

import com.intellicare.dto.request.UserRequest;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse.Full getUserById(Long id);

    Page<UserResponse.Full> getAllUsers(Pageable pageable);

    Page<UserResponse.Full> getUsersByRole(String roleName, Pageable pageable);

    Page<UserResponse.Full> searchUsers(String email, String firstName, String lastName,
                                        Boolean isActive, Pageable pageable);

    UserResponse.Full updateProfile(Long userId, UserRequest.UpdateProfile request, String ipAddress);

    UserResponse.Full assignRoles(Long userId, UserRequest.AssignRoles request, String ipAddress);

    UserResponse.Full activateUser(Long userId, String ipAddress);

    UserResponse.Full deactivateUser(Long userId, String ipAddress);

    void deleteUser(Long userId, String ipAddress);

    UserResponse.Full adminCreateUser(UserRequest.AdminCreateUser request, String ipAddress);
}
