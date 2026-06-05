package com.intellicare.service.impl;

import com.intellicare.dto.request.NotificationRequest;
import com.intellicare.dto.response.NotificationResponse;
import com.intellicare.entity.AuditLog;
import com.intellicare.entity.Notification;
import com.intellicare.entity.User;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.NotificationRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.AuditLogService;
import com.intellicare.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher dispatcher;   // separate bean — @Async works
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public NotificationResponse send(NotificationRequest.Send request, String ipAddress) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .status(Notification.Status.PENDING)
                .templateName(request.getTemplateName())
                .build();

        notification = notificationRepository.save(notification);

        // Dispatch via separate bean so @Async proxy fires correctly
        dispatcher.dispatch(notification, user, request);

        auditLogService.log(AuditLog.ActionType.NOTIFICATION_SENT, user.getId(), user.getEmail(),
                "Notification", String.valueOf(notification.getId()),
                "Queued via " + request.getChannel(), ipAddress, AuditLog.Status.SUCCESS);

        return toResponse(notification);
    }

    @Override
    @Transactional
    public List<NotificationResponse> sendBulk(NotificationRequest.SendBulk request, String ipAddress) {
        List<NotificationResponse> results = new ArrayList<>();
        for (Long userId : request.getUserIds()) {
            try {
                NotificationRequest.Send single = new NotificationRequest.Send();
                single.setUserId(userId);
                single.setTitle(request.getTitle());
                single.setMessage(request.getMessage());
                single.setChannel(request.getChannel());
                single.setTemplateName(request.getTemplateName());
                single.setTemplateVariables(request.getTemplateVariables());
                results.add(send(single, ipAddress));
            } catch (Exception e) {
                log.error("Bulk send failed for userId={}: {}", userId, e.getMessage());
            }
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnread(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> filter(Long userId, NotificationRequest.Filter filter, Pageable pageable) {
        Instant from = filter.getFrom() != null ? Instant.parse(filter.getFrom()) : null;
        Instant to   = filter.getTo()   != null ? Instant.parse(filter.getTo())   : null;
        return notificationRepository.filterNotifications(
                userId, filter.getChannel(), filter.getStatus(), from, to, pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, Instant.now());
        if (updated == 0) throw new ResourceNotFoundException("Notification", notificationId);
        return toResponse(notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId)));
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {
        return notificationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel())
                .status(n.getStatus())
                .isRead(n.isRead())
                .templateName(n.getTemplateName())
                .externalRefId(n.getExternalRefId())
                .errorMessage(n.getErrorMessage())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
