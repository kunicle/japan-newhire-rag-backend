package com.teamproject.japan_newhire_rag_backend.rag.persistence.entity;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "rag_search_result",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rag_search_result_rank",
                        columnNames = {"rag_search_id", "result_rank"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RagSearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rag_search_result_id")
    private Long ragSearchResultId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rag_search_id", nullable = false)
    private RagSearch ragSearch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_chunk_id", nullable = false)
    private DocumentChunk documentChunk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Column(name = "result_rank", nullable = false)
    private int resultRank;

    private RagSearchResult(
            RagSearch ragSearch,
            DocumentChunk documentChunk,
            DocumentVersion documentVersion,
            double similarityScore,
            int resultRank) {
        this.ragSearch = ragSearch;
        this.documentChunk = documentChunk;
        this.documentVersion = documentVersion;
        this.similarityScore = similarityScore;
        this.resultRank = resultRank;
    }

    public static RagSearchResult create(
            RagSearch ragSearch,
            DocumentChunk documentChunk,
            DocumentVersion documentVersion,
            double similarityScore,
            int resultRank) {
        return new RagSearchResult(
                ragSearch,
                documentChunk,
                documentVersion,
                similarityScore,
                resultRank);
    }
}
