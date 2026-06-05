package com.intellicare.service;

import com.intellicare.dto.request.NotificationRequest;
import com.intellicare.dto.response.NotificationResponse;
import com.intellicare.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse send(NotificationRequest.Send request, String ipAddress);

    List<NotificationResponse> sendBulk(NotificationRequest.SendBulk request, String ipAddress);

    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    Page<NotificationResponse> getUnread(Long userId, Pageable pageable);

    Page<NotificationResponse> filter(Long userId, NotificationRequest.Filter filter, Pageable pageable);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    long countUnread(Long userId);

    NotificationResponse getById(Long id);
}
