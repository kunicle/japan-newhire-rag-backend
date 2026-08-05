package com.teamproject.japan_newhire_rag_backend.domain.chat.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "session_key",
            nullable = false,
            unique = true,
            length = 36
    )
    private String sessionKey;

    @Column(length = 200)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ChatSession(String sessionKey, String title) {
        this.sessionKey = sessionKey;
        this.title = title;
    }

    public static ChatSession create(String title) {
        return new ChatSession(
                UUID.randomUUID().toString(),
                title
        );
    }

    public void changeTitle(String title) {
        this.title = title;
    }
}
