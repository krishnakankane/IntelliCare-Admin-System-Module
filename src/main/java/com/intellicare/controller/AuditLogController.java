package com.intellicare.controller;

import com.intellicare.dto.response.ApiResponse;
import com.intellicare.dto.response.AuditLogResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit Logs", description = "System audit trail — admin only")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getById(id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<AuditLogResponse>>> getByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<AuditLogResponse> page = auditLogService.getByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs with filters")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<AuditLogResponse>>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditLog.Status status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<AuditLogResponse> page = auditLogService.search(
                userId, action, entityType, status, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }
}
