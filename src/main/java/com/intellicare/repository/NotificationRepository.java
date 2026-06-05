package com.intellicare.repository;

import com.intellicare.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(
            Long userId, Notification.Channel channel, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Query("""
           SELECT n FROM Notification n
           WHERE n.user.id = :userId
             AND (:channel IS NULL OR n.channel = :channel)
             AND (:status  IS NULL OR n.status  = :status)
             AND (:from    IS NULL OR n.createdAt >= :from)
             AND (:to      IS NULL OR n.createdAt <= :to)
           ORDER BY n.createdAt DESC
           """)
    Page<Notification> filterNotifications(
            @Param("userId") Long userId,
            @Param("channel") Notification.Channel channel,
            @Param("status") Notification.Status status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
