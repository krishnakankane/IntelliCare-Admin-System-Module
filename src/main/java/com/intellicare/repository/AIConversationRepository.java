package com.intellicare.repository;

import com.intellicare.entity.AIConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    Optional<AIConversation> findBySessionId(String sessionId);

    Optional<AIConversation> findBySessionIdAndUserId(String sessionId, Long userId);

    Page<AIConversation> findByUserIdAndIsActiveOrderByUpdatedAtDesc(Long userId, boolean isActive, Pageable pageable);
}
