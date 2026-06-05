package com.intellicare.service;

import com.intellicare.dto.request.AIRequest;
import com.intellicare.dto.response.AIResponse;

/**
 * Primary AI gateway — routes requests to LangChain4j / OpenAI.
 */
public interface AIGatewayService {

    AIResponse.ChatCompletion chat(Long userId, AIRequest.ChatMessage request);

    AIResponse.ChatCompletion chatWithHistory(Long userId, AIRequest.ConversationHistory request,
                                              String sessionId);

    AIResponse.SpeechToText transcribe(Long userId, AIRequest.SpeechToText request);

    AIResponse.TextToSpeech synthesize(Long userId, AIRequest.TextToSpeech request);
}
