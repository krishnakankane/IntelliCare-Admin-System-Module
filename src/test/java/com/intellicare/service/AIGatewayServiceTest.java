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

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private AIProperties.OpenAI openAiProps;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("patient@test.com")
                .firstName("Test").lastName("User").isActive(true).build();

        testConversation = AIConversation.builder()
                .id(1L).user(testUser).sessionId("test-session")
                .messages(new ArrayList<>()).build();

        openAiProps = new AIProperties.OpenAI();
        openAiProps.setModel("gpt-4o");

        when(aiProperties.isMockEnabled()).thenReturn(true);
        when(aiProperties.getOpenai()).thenReturn(openAiProps);
    }

    @Test
    @DisplayName("chat — mock mode returns non-null response with content")
    void chat_mockMode_returnsContent() {
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

        when(requestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        AIResponse.SpeechToText response = aiGatewayService.transcribe(1L, request);

        assertThat(response.getTranscript()).isNotBlank();
        assertThat(response.isMockResponse()).isTrue();
    }

    @Test
    @DisplayName("synthesize — mock mode returns base64 audio")
    void synthesize_mockMode_returnsAudio() {
        AIRequest.TextToSpeech request = new AIRequest.TextToSpeech();
        request.setText("Take your medication at 8am daily.");

        when(requestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        AIResponse.TextToSpeech response = aiGatewayService.synthesize(1L, request);

        assertThat(response.getAudioData()).isNotBlank();
        assertThat(response.isMockResponse()).isTrue();
    }
}
