package com.intellicare.controller;

import com.intellicare.dto.request.AIRequest;
import com.intellicare.dto.response.AIResponse;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.entity.AIRequestLog;
import com.intellicare.entity.User;
import com.intellicare.repository.AIRequestLogRepository;
import com.intellicare.service.AIGatewayService;
import com.intellicare.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Orchestration", description = "AI chat, speech-to-text, text-to-speech")
public class AIController {

    private final AIGatewayService aiGatewayService;
    private final ConversationService conversationService;
    private final AIRequestLogRepository requestLogRepository;

    @PostMapping("/chat")
    @Operation(summary = "Send a chat message to the AI")
    public ResponseEntity<ApiResponse<AIResponse.ChatCompletion>> chat(
            @Valid @RequestBody AIRequest.ChatMessage request,
            @AuthenticationPrincipal User user) {

        AIResponse.ChatCompletion response = aiGatewayService.chat(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/chat/history")
    @Operation(summary = "Send a message with full conversation history")
    public ResponseEntity<ApiResponse<AIResponse.ChatCompletion>> chatWithHistory(
            @Valid @RequestBody AIRequest.ConversationHistory request,
            @RequestParam(required = false) String sessionId,
            @AuthenticationPrincipal User user) {

        AIResponse.ChatCompletion response = aiGatewayService.chatWithHistory(user.getId(), request, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/transcribe")
    @Operation(summary = "Transcribe audio using Whisper (speech-to-text)")
    public ResponseEntity<ApiResponse<AIResponse.SpeechToText>> transcribe(
            @Valid @RequestBody AIRequest.SpeechToText request,
            @AuthenticationPrincipal User user) {

        AIResponse.SpeechToText response = aiGatewayService.transcribe(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/synthesize")
    @Operation(summary = "Synthesize speech using ElevenLabs (text-to-speech)")
    public ResponseEntity<ApiResponse<AIResponse.TextToSpeech>> synthesize(
            @Valid @RequestBody AIRequest.TextToSpeech request,
            @AuthenticationPrincipal User user) {

        AIResponse.TextToSpeech response = aiGatewayService.synthesize(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ===== Conversations =====

    @GetMapping("/conversations")
    @Operation(summary = "List my AI conversations")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<AIResponse.ConversationSummary>>> listConversations(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AIResponse.ConversationSummary> page = conversationService.listConversations(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/conversations/{sessionId}")
    @Operation(summary = "Get a specific conversation by session ID")
    public ResponseEntity<ApiResponse<AIResponse.ConversationDetail>> getConversation(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {

        AIResponse.ConversationDetail detail = conversationService.getConversation(user.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {

        conversationService.deleteConversation(user.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Conversation deleted"));
    }

    // ===== Admin: request logs =====

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Search AI request logs")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<AIResponse.RequestLog>>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AIRequestLog.ServiceType serviceType,
            @RequestParam(required = false) AIRequestLog.RequestStatus status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 50) Pageable pageable) {

        java.time.Instant fromI = from != null ? java.time.Instant.parse(from) : null;
        java.time.Instant toI   = to   != null ? java.time.Instant.parse(to)   : null;

        Page<AIResponse.RequestLog> page = requestLogRepository
                .searchLogs(userId, serviceType, status, fromI, toI, pageable)
                .map(r -> AIResponse.RequestLog.builder()
                        .id(r.getId())
                        .userId(r.getUser() != null ? r.getUser().getId() : null)
                        .conversationId(r.getConversationId())
                        .serviceType(r.getServiceType())
                        .model(r.getModel())
                        .totalTokens(r.getTotalTokens())
                        .latencyMs(r.getLatencyMs())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build());

        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }
}
