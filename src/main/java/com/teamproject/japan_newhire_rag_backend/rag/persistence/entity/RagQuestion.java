package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "rag_question")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_question_id")
    private Long ragQuestionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "failure_type", length = 50)
    private String failureType;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RagQuestion(String questionText, Long createdBy) {
        this.questionText = questionText;
        this.createdBy = createdBy;
        this.processingStatus = "PENDING";
    }

    public static RagQuestion create(String questionText, Long createdBy) {
        return new RagQuestion(questionText, createdBy);
    }

    public void markProcessing() {
        this.processingStatus = "PROCESSING";
    }

    public void markAnswered() {
        this.processingStatus = "ANSWERED";
        this.failureType = null;
        this.failureReason = null;
    }

    public void markRejected(String failureType, String failureReason) {
        this.processingStatus = "REJECTED";
        this.failureType = failureType;
        this.failureReason = failureReason;
    }

    public void markFailed(String failureType, String failureReason) {
        this.processingStatus = "FAILED";
        this.failureType = failureType;
        this.failureReason = failureReason;
    }
}
