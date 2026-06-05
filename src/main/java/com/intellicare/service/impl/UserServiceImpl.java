package com.intellicare.service.impl;

import com.intellicare.dto.request.UserRequest;
import com.intellicare.dto.response.UserResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.entity.Role;
import com.intellicare.entity.User;
import com.intellicare.exception.ConflictException;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.RoleRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.AuditLogService;
import com.intellicare.service.UserService;
import com.intellicare.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse.Full getUserById(Long id) {
        return userMapper.toFull(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse.Full> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toFull);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse.Full> getUsersByRole(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable).map(userMapper::toFull);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse.Full> searchUsers(String email, String firstName, String lastName,
                                               Boolean isActive, Pageable pageable) {
        return userRepository.searchUsers(email, firstName, lastName, isActive, pageable)
                .map(userMapper::toFull);
    }

    @Override
    @Transactional
    public UserResponse.Full updateProfile(Long userId, UserRequest.UpdateProfile request, String ipAddress) {
        User user = findUser(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        user = userRepository.save(user);

        auditLogService.log(AuditLog.ActionType.USER_UPDATED, userId, user.getEmail(),
                "User", String.valueOf(userId), "Profile updated", ipAddress, AuditLog.Status.SUCCESS);

        return userMapper.toFull(user);
    }

    @Override
    @Transactional
    public UserResponse.Full assignRoles(Long userId, UserRequest.AssignRoles request, String ipAddress) {
        User user = findUser(userId);

        Set<Role> roles = roleRepository.findByNameIn(request.getRoles());
        if (roles.size() != request.getRoles().size()) {
            Set<String> found = roles.stream().map(Role::getName).collect(Collectors.toSet());
            Set<String> missing = request.getRoles().stream()
                    .filter(r -> !found.contains(r)).collect(Collectors.toSet());
            throw new ResourceNotFoundException("Roles not found: " + missing);
        }

        user.setRoles(roles);
        user = userRepository.save(user);

        auditLogService.log(AuditLog.ActionType.ROLE_ASSIGNED, userId, user.getEmail(),
                "User", String.valueOf(userId),
                "Roles updated to: " + request.getRoles(), ipAddress, AuditLog.Status.SUCCESS);

        return userMapper.toFull(user);
    }

    @Override
    @Transactional
    public UserResponse.Full activateUser(Long userId, String ipAddress) {
        User user = findUser(userId);
        user.setActive(true);
        user = userRepository.save(user);
        auditLogService.log(AuditLog.ActionType.USER_ACTIVATED, userId, user.getEmail(),
                "User", String.valueOf(userId), "User activated", ipAddress, AuditLog.Status.SUCCESS);
        return userMapper.toFull(user);
    }

    @Override
    @Transactional
    public UserResponse.Full deactivateUser(Long userId, String ipAddress) {
        User user = findUser(userId);
        user.setActive(false);
        user = userRepository.save(user);
        auditLogService.log(AuditLog.ActionType.USER_DEACTIVATED, userId, user.getEmail(),
                "User", String.valueOf(userId), "User deactivated", ipAddress, AuditLog.Status.SUCCESS);
        return userMapper.toFull(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, String ipAddress) {
        User user = findUser(userId);
        String email = user.getEmail();
        userRepository.delete(user);
        auditLogService.log(AuditLog.ActionType.USER_DELETED, null, email,
                "User", String.valueOf(userId), "User deleted", ipAddress, AuditLog.Status.SUCCESS);
        log.info("User deleted: {} [id={}]", email, userId);
    }

    @Override
    @Transactional
    public UserResponse.Full adminCreateUser(UserRequest.AdminCreateUser request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        Set<Role> roles = roleRepository.findByNameIn(request.getRoles());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.isActive())
                .isVerified(request.isVerified())
                .roles(roles)
                .build();

        user = userRepository.save(user);

        auditLogService.log(AuditLog.ActionType.USER_CREATED, null, "admin",
                "User", String.valueOf(user.getId()),
                "User created by admin: " + user.getEmail(), ipAddress, AuditLog.Status.SUCCESS);

        return userMapper.toFull(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
