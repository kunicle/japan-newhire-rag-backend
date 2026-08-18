package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rag_answer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_answer_id")
    private Long ragAnswerId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rag_search_id", nullable = false, unique = true)
    private RagSearch ragSearch;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RagAnswer(RagSearch ragSearch, String answerText) {
        this.ragSearch = ragSearch;
        this.answerText = answerText;
    }

    public static RagAnswer create(RagSearch ragSearch, String answerText) {
        return new RagAnswer(ragSearch, answerText);
    }
}
