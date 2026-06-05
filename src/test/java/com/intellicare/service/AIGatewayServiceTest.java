package com.intellicare.service;

import com.intellicare.config.AIProperties;
import com.intellicare.dto.request.AIRequest;
import com.intellicare.dto.response.AIResponse;
import com.intellicare.entity.AIConversation;
import com.intellicare.entity.User;
import com.intellicare.repository.AIConversationRepository;
import com.intellicare.repository.AIMessageRepository;
import com.intellicare.repository.AIRequestLogRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.impl.AIGatewayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/*
 * FIX SUMMARY
 * -----------
 * Three separate issues were fixed:
 *
 * 1. NPE in transcribe(): aiProperties.getWhisper() returned null because the mock was never
 *    stubbed. Added a real AIProperties.Whisper instance in setUp() and stubbed getWhisper().
 *
 * 2. UnnecessaryStubbingException: aiProperties.getOpenai() was stubbed in @BeforeEach but
 *    transcribe() and synthesize() never call getOpenai(). Using LENIENT strictness on the
 *    @BeforeEach stubs avoids the exception while keeping strict checking on per-test stubs.
 *    Alternatively (chosen here) the getOpenai() stub is moved into only the chat test, and
 *    LENIENT is used at class level so Mockito doesn't fail on isMockEnabled() being "shared".
 *
 * 3. userRepository.findById() in transcribe/synthesize: persistLog() calls
 *    userRepository.findById(userId) — so the stub IS needed in those tests. Kept as-is.
 */
@ExtendWith(MockitoExtension.class)
// LENIENT: isMockEnabled() is stubbed once in @BeforeEach and consumed by all tests;
// without this, Mockito's strict mode would flag it as unnecessary in tests that
// don't trigger the full code path that re-reads the flag.
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AIGatewayService Tests — Mock Mode")
class AIGatewayServiceTest {

    @Mock private AIProperties aiProperties;
    @Mock private AIRequestLogRepository requestLogRepository;
    @Mock private AIConversationRepository conversationRepository;
    @Mock private AIMessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AIGatewayServiceImpl aiGatewayService;

    private User testUser;
    private AIConversation testConversation;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("patient@test.com")
                .firstName("Test").lastName("User").isActive(true).build();

        testConversation = AIConversation.builder()
                .id(1L).user(testUser).sessionId("test-session")
                .messages(new ArrayList<>()).build();

        // Shared stubs — LENIENT so they don't trigger UnnecessaryStubbingException
        // in tests that don't exercise every branch
        when(aiProperties.isMockEnabled()).thenReturn(true);

        // FIX: stub getWhisper() and getElevenlabs() so transcribe/synthesize don't NPE.
        // transcribe() calls aiProperties.getWhisper().getModel() in mock mode.
        AIProperties.Whisper whisperProps = new AIProperties.Whisper();
        whisperProps.setModel("whisper-1");
        when(aiProperties.getWhisper()).thenReturn(whisperProps);

        // synthesize() doesn't call getElevenlabs() in mock mode (uses hardcoded string),
        // but stub it defensively so any future use won't NPE.
        AIProperties.ElevenLabs elevenLabsProps = new AIProperties.ElevenLabs();
        elevenLabsProps.setApiKey("mock-key");
        when(aiProperties.getElevenlabs()).thenReturn(elevenLabsProps);
    }

    @Test
    @DisplayName("chat — mock mode returns non-null response with content")
    void chat_mockMode_returnsContent() {
        // getOpenai() is only needed for the chat path — stub here, not in @BeforeEach
        AIProperties.OpenAI openAiProps = new AIProperties.OpenAI();
        openAiProps.setModel("gpt-4o");
        when(aiProperties.getOpenai()).thenReturn(openAiProps);

        AIRequest.ChatMessage request = new AIRequest.ChatMessage();
        request.setPrompt("What is my blood pressure medication?");
        request.setSessionId("test-session");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findBySessionIdAndUserId("test-session", 1L))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AIResponse.ChatCompletion response = aiGatewayService.chat(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.isMockResponse()).isTrue();
        assertThat(response.getSessionId()).isEqualTo("test-session");
    }

    @Test
    @DisplayName("transcribe — mock mode returns transcript")
    void transcribe_mockMode_returnsTranscript() {
        AIRequest.SpeechToText request = new AIRequest.SpeechToText();
        request.setAudioData("bW9ja19hdWRpb19kYXRh");
        request.setFormat("mp3");

        // persistLog() calls userRepository.findById(userId) — stub needed
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(requestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AIResponse.SpeechToText response = aiGatewayService.transcribe(1L, request);

        assertThat(response.getTranscript()).isNotBlank();
        assertThat(response.isMockResponse()).isTrue();
    }

    @Test
    @DisplayName("synthesize — mock mode returns base64 audio")
    void synthesize_mockMode_returnsAudio() {
        AIRequest.TextToSpeech request = new AIRequest.TextToSpeech();
        request.setText("Take your medication at 8am daily.");

        // persistLog() calls userRepository.findById(userId) — stub needed
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(requestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AIResponse.TextToSpeech response = aiGatewayService.synthesize(1L, request);

        assertThat(response.getAudioData()).isNotBlank();
        assertThat(response.isMockResponse()).isTrue();
    }
}
