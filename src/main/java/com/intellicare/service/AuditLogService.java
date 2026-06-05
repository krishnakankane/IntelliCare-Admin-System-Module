package com.intellicare.service;

import com.intellicare.dto.response.AuditLogResponse;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void log(AuditLog.ActionType action, Long userId, String username,
             String entityType, String entityId, String description,
             String ipAddress, AuditLog.Status status);

    void log(AuditLog.ActionType action, Long userId, String username,
             String entityType, String entityId, String description,
             String ipAddress, String userAgent, AuditLog.Status status);

    AuditLogResponse getById(Long id);

    Page<AuditLogResponse> getByUserId(Long userId, Pageable pageable);

    Page<AuditLogResponse> search(Long userId, String action, String entityType,
                                  AuditLog.Status status, String from, String to,
                                  Pageable pageable);
}
