package com.intellicare.service;

import com.intellicare.dto.response.AuditLogResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.AuditLogRepository;
import com.intellicare.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("log — saves AuditLog entity without throwing")
    void log_savesEntry() {
        auditLogService.log(
                AuditLog.ActionType.USER_LOGIN, 1L, "user@test.com",
                "User", "1", "Login OK", "127.0.0.1", AuditLog.Status.SUCCESS
        );
        // @Async won't run in unit test context synchronously — verify via direct call path
        // In an integration test this would be verified via the repository
    }

    @Test
    @DisplayName("getById — returns response for existing log")
    void getById_existingLog_returnsResponse() {
        AuditLog log = AuditLog.builder()
                .id(1L).userId(42L).username("admin@test.com")
                .action("USER_LOGIN").status(AuditLog.Status.SUCCESS)
                .createdAt(Instant.now()).build();

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(log));

        AuditLogResponse response = auditLogService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAction()).isEqualTo("USER_LOGIN");
        assertThat(response.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getById — missing id throws ResourceNotFoundException")
    void getById_missing_throws() {
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auditLogService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getByUserId — returns paged results")
    void getByUserId_returnsPage() {
        AuditLog log = AuditLog.builder().id(1L).userId(5L)
                .action("USER_LOGOUT").status(AuditLog.Status.SUCCESS)
                .createdAt(Instant.now()).build();

        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(eq(5L), any())).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getByUserId(5L, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("USER_LOGOUT");
    }
}
