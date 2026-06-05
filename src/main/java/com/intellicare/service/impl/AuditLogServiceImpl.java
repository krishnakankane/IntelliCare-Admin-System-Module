package com.intellicare.service.impl;

import com.intellicare.dto.response.AuditLogResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.AuditLogRepository;
import com.intellicare.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog.ActionType action, Long userId, String username,
                    String entityType, String entityId, String description,
                    String ipAddress, AuditLog.Status status) {
        log(action, userId, username, entityType, entityId, description, ipAddress, null, status);
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog.ActionType action, Long userId, String username,
                    String entityType, String entityId, String description,
                    String ipAddress, String userAgent, AuditLog.Status status) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action.name())
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must never propagate errors to the caller
            log.error("Failed to persist audit log for action={} userId={}: {}", action, userId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return auditLogRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(Long userId, String action, String entityType,
                                         AuditLog.Status status, String from, String to,
                                         Pageable pageable) {
        Instant fromInstant = from != null ? Instant.parse(from) : null;
        Instant toInstant   = to   != null ? Instant.parse(to)   : null;

        return auditLogRepository.searchAuditLogs(userId, action, entityType, status,
                fromInstant, toInstant, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
