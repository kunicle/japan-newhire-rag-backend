package com.teamproject.japan_newhire_rag_backend.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.chat.entity.ChatSession;

public interface ChatSessionRepository
        extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionKey(String sessionKey);
}
