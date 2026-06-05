package com.intellicare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intellicare.entity.Notification;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private Notification.Channel channel;
    private Notification.Status status;
    private boolean isRead;
    private String templateName;
    private String externalRefId;
    private String errorMessage;
    private Instant sentAt;
    private Instant readAt;
    private Instant createdAt;
}
