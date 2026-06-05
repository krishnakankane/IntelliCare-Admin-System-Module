package com.intellicare.repository;

import com.intellicare.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:userId     IS NULL OR a.userId   = :userId)
             AND (:action     IS NULL OR a.action   = :action)
             AND (:entityType IS NULL OR a.entityType = :entityType)
             AND (:status     IS NULL OR a.status   = :status)
             AND (:from       IS NULL OR a.createdAt >= :from)
             AND (:to         IS NULL OR a.createdAt <= :to)
           ORDER BY a.createdAt DESC
           """)
    Page<AuditLog> searchAuditLogs(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("status") AuditLog.Status status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
