package com.intellicare.service.impl;

import com.intellicare.dto.response.AIResponse;
import com.intellicare.entity.AIConversation;
import com.intellicare.entity.User;
import com.intellicare.exception.ResourceNotFoundException;
import com.intellicare.repository.AIConversationRepository;
import com.intellicare.repository.UserRepository;
import com.intellicare.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final AIConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AIResponse.ConversationDetail getOrCreateConversation(Long userId, String sessionId) {
        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();

        AIConversation conv = conversationRepository.findBySessionIdAndUserId(sid, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    return conversationRepository.save(
                            AIConversation.builder()
                                    .user(user)
                                    .sessionId(sid)
                                    .title("New Conversation")
                                    .build()
                    );
                });

        return toDetail(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AIResponse.ConversationSummary> listConversations(Long userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndIsActiveOrderByUpdatedAtDesc(userId, true, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse.ConversationDetail getConversation(Long userId, String sessionId) {
        AIConversation conv = conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "sessionId", sessionId));
        return toDetail(conv);
    }

    @Override
    @Transactional
    public void closeConversation(Long userId, String sessionId) {
        AIConversation conv = conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "sessionId", sessionId));
        conv.setActive(false);
        conversationRepository.save(conv);
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, String sessionId) {
        AIConversation conv = conversationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "sessionId", sessionId));
        conversationRepository.delete(conv);
    }

    // ===== Mapping helpers =====

    private AIResponse.ConversationSummary toSummary(AIConversation conv) {
        return AIResponse.ConversationSummary.builder()
                .id(conv.getId())
                .sessionId(conv.getSessionId())
                .title(conv.getTitle())
                .messageCount(conv.getMessages().size())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    private AIResponse.ConversationDetail toDetail(AIConversation conv) {
        return AIResponse.ConversationDetail.builder()
                .id(conv.getId())
                .sessionId(conv.getSessionId())
                .title(conv.getTitle())
                .messages(conv.getMessages().stream().map(m ->
                        AIResponse.MessageItem.builder()
                                .id(m.getId())
                                .role(m.getRole().name())
                                .content(m.getContent())
                                .createdAt(m.getCreatedAt())
                                .build()
                ).collect(Collectors.toList()))
                .createdAt(conv.getCreatedAt())
                .build();
    }
}
