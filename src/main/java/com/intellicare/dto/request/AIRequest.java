package com.intellicare.dto.request;

import com.intellicare.entity.AIMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public final class AIRequest {

    private AIRequest() {}

    @Data
    public static class ChatMessage {
        @NotBlank(message = "Prompt is required")
        private String prompt;

        private String sessionId;

        private String systemPrompt;

        private Double temperature;
        private Integer maxTokens;
    }

    @Data
    public static class ConversationHistory {
        @NotNull
        private List<MessageItem> messages;
        private String systemPrompt;
    }

    @Data
    public static class MessageItem {
        @NotNull
        private AIMessage.MessageRole role;
        @NotBlank
        private String content;
    }

    @Data
    public static class SpeechToText {
        // base64-encoded audio data
        @NotBlank(message = "Audio data is required")
        private String audioData;

        private String language;
        private String format = "mp3";
    }

    @Data
    public static class TextToSpeech {
        @NotBlank(message = "Text is required")
        private String text;

        private String voiceId;
        private String modelId = "eleven_monolingual_v1";
    }
}
