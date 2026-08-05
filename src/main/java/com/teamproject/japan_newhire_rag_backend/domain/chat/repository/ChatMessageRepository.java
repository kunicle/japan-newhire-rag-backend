package com.teamproject.japan_newhire_rag_backend.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllBySession_IdOrderByCreatedAtAsc(
            Long sessionId
    );
}
