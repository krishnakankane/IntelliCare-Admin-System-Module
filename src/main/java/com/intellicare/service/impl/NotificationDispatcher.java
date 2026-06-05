package com.intellicare.service.impl;

import com.intellicare.dto.request.NotificationRequest;
import com.intellicare.entity.AuditLog;
import com.intellicare.entity.Notification;
import com.intellicare.entity.User;
import com.intellicare.exception.BadRequestException;
import com.intellicare.repository.NotificationRepository;
import com.intellicare.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Separate bean so @Async proxy works correctly (avoids self-invocation issue).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final PushNotificationService pushNotificationService;
    private final AuditLogService auditLogService;

    @Async
    public void dispatch(Notification notification, User user, NotificationRequest.Send request) {
        try {
            String externalRef = switch (notification.getChannel()) {
                case EMAIL -> {
                    if (user.getEmail() == null) throw new BadRequestException("User has no email");
                    if (request.getTemplateName() != null) {
                        yield emailService.sendTemplate(user.getEmail(), request.getTemplateName(),
                                request.getTemplateVariables());
                    }
                    yield emailService.send(user.getEmail(), notification.getTitle(), notification.getMessage());
                }
                case SMS -> {
                    if (user.getPhoneNumber() == null) throw new BadRequestException("User has no phone number");
                    yield smsService.send(user.getPhoneNumber(), notification.getMessage());
                }
                case WHATSAPP -> {
                    if (user.getPhoneNumber() == null) throw new BadRequestException("User has no phone number");
                    yield whatsAppService.sendTextMessage(user.getPhoneNumber(), notification.getMessage());
                }
                case IN_APP -> pushNotificationService.send(user.getId(),
                        notification.getTitle(), notification.getMessage());
            };

            notification.setStatus(Notification.Status.SENT);
            notification.setSentAt(Instant.now());
            notification.setExternalRefId(externalRef);

        } catch (Exception e) {
            log.error("Notification dispatch failed id={} channel={}: {}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            notification.setStatus(Notification.Status.FAILED);
            notification.setErrorMessage(e.getMessage());

            auditLogService.log(AuditLog.ActionType.NOTIFICATION_FAILED, user.getId(), user.getEmail(),
                    "Notification", String.valueOf(notification.getId()),
                    "Dispatch failed: " + e.getMessage(), null, AuditLog.Status.FAILURE);
        }

        notificationRepository.save(notification);
    }
}
