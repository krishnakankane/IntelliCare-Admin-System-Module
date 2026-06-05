package com.intellicare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_logs_user",    columnList = "user_id"),
           @Index(name = "idx_audit_logs_action",  columnList = "action"),
           @Index(name = "idx_audit_logs_created", columnList = "created_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 255)
    private String username;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SUCCESS;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ===== Enums =====

    public enum Status {
        SUCCESS, FAILURE, WARNING
    }

    public enum ActionType {
        // Auth
        USER_LOGIN, USER_LOGOUT, LOGIN_FAILED, TOKEN_REFRESHED,
        // User management
        USER_CREATED, USER_UPDATED, USER_DELETED, USER_ACTIVATED, USER_DEACTIVATED,
        // Role management
        ROLE_ASSIGNED, ROLE_REMOVED, ROLE_CREATED,
        // Notification
        NOTIFICATION_SENT, NOTIFICATION_FAILED,
        // Security
        PASSWORD_CHANGED, PASSWORD_RESET_REQUESTED,
        // AI
        AI_REQUEST_MADE, AI_REQUEST_FAILED,
        // Admin
        ADMIN_ACTION
    }
}
