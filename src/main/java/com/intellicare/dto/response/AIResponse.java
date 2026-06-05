package com.intellicare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intellicare.entity.AIRequestLog;
import lombok.*;

import java.time.Instant;
import java.util.List;

public final class AIResponse {

    private AIResponse() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletion {
        private String sessionId;
        private String conversationId;
        private String content;
        private String model;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Long latencyMs;
        private boolean mockResponse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpeechToText {
        private String transcript;
        private String language;
        private Double confidence;
        private Long latencyMs;
        private boolean mockResponse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextToSpeech {
        // base64-encoded audio
        private String audioData;
        private String format;
        private Long latencyMs;
        private boolean mockResponse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestLog {
        private Long id;
        private Long userId;
        private String conversationId;
        private AIRequestLog.ServiceType serviceType;
        private String model;
        private Integer totalTokens;
        private Long latencyMs;
        private AIRequestLog.RequestStatus status;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversationSummary {
        private Long id;
        private String sessionId;
        private String title;
        private int messageCount;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversationDetail {
        private Long id;
        private String sessionId;
        private String title;
        private List<MessageItem> messages;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageItem {
        private Long id;
        private String role;
        private String content;
        private Instant createdAt;
    }
}
