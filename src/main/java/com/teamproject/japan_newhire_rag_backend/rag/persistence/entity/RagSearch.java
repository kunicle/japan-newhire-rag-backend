package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rag_search")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_search_id")
    private Long ragSearchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rag_question_id", nullable = false)
    private RagQuestion ragQuestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AiModel aiModel;

    @CreatedDate
    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt;

    private RagSearch(RagQuestion ragQuestion, AiModel aiModel) {
        this.ragQuestion = ragQuestion;
        this.aiModel = aiModel;
    }

    public static RagSearch create(RagQuestion ragQuestion, AiModel aiModel) {
        return new RagSearch(ragQuestion, aiModel);
    }
}
