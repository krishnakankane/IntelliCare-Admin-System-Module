package com.intellicare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intellicare.entity.AuditLog;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private AuditLog.Status status;
    private Instant createdAt;
}
