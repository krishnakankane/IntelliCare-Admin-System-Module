package com.intellicare.service;

import com.intellicare.dto.response.AIResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversationService {

    AIResponse.ConversationDetail getOrCreateConversation(Long userId, String sessionId);

    Page<AIResponse.ConversationSummary> listConversations(Long userId, Pageable pageable);

    AIResponse.ConversationDetail getConversation(Long userId, String sessionId);

    void closeConversation(Long userId, String sessionId);

    void deleteConversation(Long userId, String sessionId);
}
