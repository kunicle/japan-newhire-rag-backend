package com.teamproject.japan_newhire_rag_backend.document.chunk.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "document_chunk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_chunk_id")
    private Long documentChunkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "chunk_sequence", nullable = false)
    private int chunkSequence;

    @Column(name = "article_number", length = 50)
    private String articleNumber;

    @Column(name = "article_title", length = 200)
    private String articleTitle;

    @Column(name = "chunk_content", nullable = false, columnDefinition = "TEXT")
    private String chunkContent;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "chunk_status", nullable = false, length = 20)
    private String chunkStatus;

    private DocumentChunk(
            DocumentVersion documentVersion,
            int chunkSequence,
            String articleNumber,
            String articleTitle,
            String chunkContent,
            Integer tokenCount) {
        this.documentVersion = documentVersion;
        this.chunkSequence = chunkSequence;
        this.articleNumber = articleNumber;
        this.articleTitle = articleTitle;
        this.chunkContent = chunkContent;
        this.tokenCount = tokenCount;
        this.chunkStatus = "ACTIVE";
    }

    public static DocumentChunk create(
            DocumentVersion documentVersion,
            int chunkSequence,
            String articleNumber,
            String articleTitle,
            String chunkContent,
            Integer tokenCount) {
        return new DocumentChunk(
                documentVersion,
                chunkSequence,
                articleNumber,
                articleTitle,
                chunkContent,
                tokenCount);
    }
}
