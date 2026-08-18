package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rag_citation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rag_citation_position",
                        columnNames = {"rag_answer_id", "position"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_citation_id")
    private Long ragCitationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rag_answer_id", nullable = false)
    private RagAnswer ragAnswer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;

    @Column(name = "position", nullable = false)
    private int position;

    private RagCitation(RagAnswer ragAnswer, DocumentChunk documentChunk, int position) {
        this.ragAnswer = ragAnswer;
        this.documentChunk = documentChunk;
        this.position = position;
    }

    public static RagCitation create(
            RagAnswer ragAnswer,
            DocumentChunk documentChunk,
            int position) {
        return new RagCitation(ragAnswer, documentChunk, position);
    }
}
