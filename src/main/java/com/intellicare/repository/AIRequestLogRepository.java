package com.intellicare.repository;

import com.intellicare.entity.AIRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AIRequestLogRepository extends JpaRepository<AIRequestLog, Long> {

    Page<AIRequestLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AIRequestLog> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(a.totalTokens), 0) FROM AIRequestLog a
           WHERE a.user.id = :userId
             AND a.createdAt >= :from
             AND a.status = 'SUCCESS'
           """)
    Long sumTokensByUserAndPeriod(@Param("userId") Long userId, @Param("from") Instant from);

    @Query("""
           SELECT a FROM AIRequestLog a
           WHERE (:userId      IS NULL OR a.user.id     = :userId)
             AND (:serviceType IS NULL OR a.serviceType = :serviceType)
             AND (:status      IS NULL OR a.status      = :status)
             AND (:from        IS NULL OR a.createdAt  >= :from)
             AND (:to          IS NULL OR a.createdAt  <= :to)
           ORDER BY a.createdAt DESC
           """)
    Page<AIRequestLog> searchLogs(
            @Param("userId") Long userId,
            @Param("serviceType") AIRequestLog.ServiceType serviceType,
            @Param("status") AIRequestLog.RequestStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
