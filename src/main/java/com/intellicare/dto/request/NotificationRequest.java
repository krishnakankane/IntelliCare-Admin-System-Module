package com.intellicare.dto.request;

import com.intellicare.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

public final class NotificationRequest {

    private NotificationRequest() {}

    @Data
    public static class Send {

        @NotNull(message = "User ID is required")
        private Long userId;

        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Message is required")
        private String message;

        @NotNull(message = "Channel is required")
        private Notification.Channel channel;

        private String templateName;
        private Map<String, String> templateVariables;
    }

    @Data
    public static class SendBulk {
        @NotNull(message = "Recipient user IDs are required")
        private java.util.List<Long> userIds;

        @NotBlank
        private String title;

        @NotBlank
        private String message;

        @NotNull
        private Notification.Channel channel;

        private String templateName;
        private Map<String, String> templateVariables;
    }

    @Data
    public static class Filter {
        private Notification.Channel channel;
        private Notification.Status status;
        private String from;
        private String to;
    }
}
