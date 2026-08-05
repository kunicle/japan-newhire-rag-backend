package com.teamproject.japan_newhire_rag_backend.domain.regulation.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "regulation_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_regulation_chunk_document_index",
                        columnNames = {"document_id", "chunk_index"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_regulation_chunk_document",
                        columnList = "document_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegulationChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_regulation_chunk_document")
    )
    private RegulationDocument document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "vector_store_id", length = 255)
    private String vectorStoreId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RegulationChunk(
            RegulationDocument document,
            Integer chunkIndex,
            String content,
            Integer pageNumber,
            Integer tokenCount
    ) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.pageNumber = pageNumber;
        this.tokenCount = tokenCount;
    }

    public static RegulationChunk create(
            RegulationDocument document,
            Integer chunkIndex,
            String content,
            Integer pageNumber,
            Integer tokenCount
    ) {
        return new RegulationChunk(
                document,
                chunkIndex,
                content,
                pageNumber,
                tokenCount
        );
    }

    public void connectVectorStore(String vectorStoreId) {
        this.vectorStoreId = vectorStoreId;
    }
}
