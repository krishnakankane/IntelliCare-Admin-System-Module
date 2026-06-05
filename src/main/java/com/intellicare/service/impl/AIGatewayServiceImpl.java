package com.intellicare.service.impl;

import com.intellicare.config.AIProperties;
import com.intellicare.dto.request.AIRequest;
import com.intellicare.dto.response.AIResponse;
import com.intellicare.entity.AIConversation;
import com.intellicare.entity.AIMessage;
import com.intellicare.entity.AIRequestLog;
import com.intellicare.entity.User;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.AIConversationRepository;
import com.intellicare.repository.AIMessageRepository;
import com.intellicare.repository.AIRequestLogRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.AIGatewayService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIGatewayServiceImpl implements AIGatewayService {

    private final AIProperties aiProperties;
    private final AIRequestLogRepository requestLogRepository;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AIResponse.ChatCompletion chat(Long userId, AIRequest.ChatMessage request) {
        long startMs = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        // Persist user message
        AIConversation conversation = getOrCreateConversation(userId, sessionId);
        AIMessage userMsg = AIMessage.builder()
                .conversation(conversation)
                .role(AIMessage.MessageRole.USER)
                .content(request.getPrompt())
                .build();
        messageRepository.save(userMsg);

        try {
            String assistantContent;
            int promptTokens = 0;
            int completionTokens = 0;

            if (aiProperties.isMockEnabled()) {
                assistantContent = buildMockResponse(request.getPrompt());
                promptTokens = request.getPrompt().split("\\s+").length;
                completionTokens = assistantContent.split("\\s+").length;
            } else {
                ChatLanguageModel model = buildChatModel(request);

                List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
                if (request.getSystemPrompt() != null) {
                    messages.add(SystemMessage.from(request.getSystemPrompt()));
                }
                messages.add(UserMessage.from(request.getPrompt()));

                Response<AiMessage> response = model.generate(messages);
                assistantContent = response.content().text();

                if (response.tokenUsage() != null) {
                    promptTokens     = response.tokenUsage().inputTokenCount();
                    completionTokens = response.tokenUsage().outputTokenCount();
                }
            }

            long latencyMs = System.currentTimeMillis() - startMs;

            // Persist assistant message
            AIMessage assistantMsg = AIMessage.builder()
                    .conversation(conversation)
                    .role(AIMessage.MessageRole.ASSISTANT)
                    .content(assistantContent)
                    .tokenCount(completionTokens)
                    .build();
            messageRepository.save(assistantMsg);

            // Log request
            persistLog(userId, sessionId, AIRequestLog.ServiceType.CHAT_COMPLETION,
                    aiProperties.getOpenai().getModel(), promptTokens, completionTokens,
                    latencyMs, AIRequestLog.RequestStatus.SUCCESS, null, request.getPrompt());

            return AIResponse.ChatCompletion.builder()
                    .sessionId(sessionId)
                    .conversationId(conversation.getSessionId())
                    .content(assistantContent)
                    .model(aiProperties.getOpenai().getModel())
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .latencyMs(latencyMs)
                    .mockResponse(aiProperties.isMockEnabled())
                    .build();

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.error("AI chat failed for userId={}: {}", userId, e.getMessage());
            persistLog(userId, sessionId, AIRequestLog.ServiceType.CHAT_COMPLETION,
                    aiProperties.getOpenai().getModel(), 0, 0,
                    latencyMs, AIRequestLog.RequestStatus.FAILED, e.getMessage(), request.getPrompt());
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AIResponse.ChatCompletion chatWithHistory(Long userId, AIRequest.ConversationHistory request,
                                                     String sessionId) {
        long startMs = System.currentTimeMillis();
        sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();

        try {
            String assistantContent;
            int promptTokens = 0;
            int completionTokens = 0;

            if (aiProperties.isMockEnabled()) {
                String lastUserMsg = request.getMessages().stream()
                        .filter(m -> m.getRole() == AIMessage.MessageRole.USER)
                        .reduce((a, b) -> b)
                        .map(AIRequest.MessageItem::getContent)
                        .orElse("...");
                assistantContent = buildMockResponse(lastUserMsg);
                promptTokens = request.getMessages().stream()
                        .mapToInt(m -> m.getContent().split("\\s+").length).sum();
                completionTokens = assistantContent.split("\\s+").length;
            } else {
                ChatLanguageModel model = buildChatModel(null);

                List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
                if (request.getSystemPrompt() != null) {
                    messages.add(SystemMessage.from(request.getSystemPrompt()));
                }
                for (AIRequest.MessageItem item : request.getMessages()) {
                    messages.add(item.getRole() == AIMessage.MessageRole.USER
                            ? UserMessage.from(item.getContent())
                            : AiMessage.from(item.getContent()));
                }

                Response<AiMessage> response = model.generate(messages);
                assistantContent = response.content().text();
                if (response.tokenUsage() != null) {
                    promptTokens     = response.tokenUsage().inputTokenCount();
                    completionTokens = response.tokenUsage().outputTokenCount();
                }
            }

            long latencyMs = System.currentTimeMillis() - startMs;
            persistLog(userId, sessionId, AIRequestLog.ServiceType.CHAT_COMPLETION,
                    aiProperties.getOpenai().getModel(), promptTokens, completionTokens,
                    latencyMs, AIRequestLog.RequestStatus.SUCCESS, null, null);

            return AIResponse.ChatCompletion.builder()
                    .sessionId(sessionId)
                    .content(assistantContent)
                    .model(aiProperties.getOpenai().getModel())
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .latencyMs(latencyMs)
                    .mockResponse(aiProperties.isMockEnabled())
                    .build();

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startMs;
            persistLog(userId, sessionId, AIRequestLog.ServiceType.CHAT_COMPLETION,
                    null, 0, 0, latencyMs, AIRequestLog.RequestStatus.FAILED, e.getMessage(), null);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public AIResponse.SpeechToText transcribe(Long userId, AIRequest.SpeechToText request) {
        long startMs = System.currentTimeMillis();

        if (aiProperties.isMockEnabled()) {
            log.info("[MOCK STT] userId={} format={}", userId, request.getFormat());
            long latencyMs = System.currentTimeMillis() - startMs;
            persistLog(userId, null, AIRequestLog.ServiceType.SPEECH_TO_TEXT,
                    aiProperties.getWhisper().getModel(), 0, 0,
                    latencyMs, AIRequestLog.RequestStatus.SUCCESS, null, null);
            return AIResponse.SpeechToText.builder()
                    .transcript("This is a mock transcription of the provided audio.")
                    .language(request.getLanguage() != null ? request.getLanguage() : "en")
                    .confidence(0.95)
                    .latencyMs(latencyMs)
                    .mockResponse(true)
                    .build();
        }

        // Production: call OpenAI Whisper via HTTP
        throw new UnsupportedOperationException("Whisper integration: add openai-java SDK and implement");
    }

    @Override
    public AIResponse.TextToSpeech synthesize(Long userId, AIRequest.TextToSpeech request) {
        long startMs = System.currentTimeMillis();

        if (aiProperties.isMockEnabled()) {
            log.info("[MOCK TTS] userId={} text='{}'", userId, request.getText().substring(0, Math.min(40, request.getText().length())));
            long latencyMs = System.currentTimeMillis() - startMs;
            persistLog(userId, null, AIRequestLog.ServiceType.TEXT_TO_SPEECH,
                    "eleven_monolingual_v1", 0, 0,
                    latencyMs, AIRequestLog.RequestStatus.SUCCESS, null, null);
            return AIResponse.TextToSpeech.builder()
                    .audioData("bW9ja19hdWRpb19kYXRh") // base64("mock_audio_data")
                    .format("mp3")
                    .latencyMs(latencyMs)
                    .mockResponse(true)
                    .build();
        }

        // Production: call ElevenLabs REST API
        throw new UnsupportedOperationException("ElevenLabs integration: configure API key and implement HTTP call");
    }

    // ===== Private helpers =====

    private AIConversation getOrCreateConversation(Long userId, String sessionId) {
        return conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    AIConversation conv = AIConversation.builder()
                            .user(user)
                            .sessionId(sessionId)
                            .title("Conversation " + Instant.now())
                            .build();
                    return conversationRepository.save(conv);
                });
    }

    private ChatLanguageModel buildChatModel(AIRequest.ChatMessage request) {
        double temperature = (request != null && request.getTemperature() != null)
                ? request.getTemperature()
                : aiProperties.getOpenai().getTemperature();
        int maxTokens = (request != null && request.getMaxTokens() != null)
                ? request.getMaxTokens()
                : aiProperties.getOpenai().getMaxTokens();

        return OpenAiChatModel.builder()
                .apiKey(aiProperties.getOpenai().getApiKey())
                .modelName(aiProperties.getOpenai().getModel())
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    private String buildMockResponse(String prompt) {
        return "This is a mock AI response for IntelliCare. Your query was: \"" +
                prompt.substring(0, Math.min(60, prompt.length())) +
                (prompt.length() > 60 ? "..." : "") +
                "\". In production this will be answered by GPT-4o.";
    }

    private void persistLog(Long userId, String conversationId, AIRequestLog.ServiceType serviceType,
                            String model, int promptTokens, int completionTokens,
                            long latencyMs, AIRequestLog.RequestStatus status,
                            String errorMessage, String promptText) {
        try {
            User user = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            String hash = null;
            if (promptText != null) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    hash = HexFormat.of().formatHex(md.digest(promptText.getBytes()));
                } catch (Exception ignored) {}
            }

            AIRequestLog entry = AIRequestLog.builder()
                    .user(user)
                    .conversationId(conversationId)
                    .serviceType(serviceType)
                    .model(model)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .latencyMs(latencyMs)
                    .status(status)
                    .errorMessage(errorMessage)
                    .requestHash(hash)
                    .build();

            requestLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist AI request log: {}", e.getMessage());
        }
    }
}
