package com.teamproject.japan_newhire_rag_backend.document.chunk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunking.ChunkSplitter;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

@Service
public class DocumentChunkPersistenceService {

    private final ChunkSplitter chunkSplitter;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentChunkPersistenceService(DocumentChunkRepository documentChunkRepository) {
        this.chunkSplitter = new ChunkSplitter();
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional
    public List<com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk>
            splitAndSave(
                    DocumentVersion documentVersion,
                    String text,
                    int maxChunkSize,
                    int overlapSize) {
        if (documentVersion == null) {
            throw new IllegalArgumentException("DocumentVersion은 null일 수 없습니다.");
        }

        List<com.teamproject.japan_newhire_rag_backend.document.chunking.DocumentChunk>
                splitChunks = chunkSplitter.split(text, maxChunkSize, overlapSize);

        List<com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk>
                persistentChunks = splitChunks.stream()
                        .map(chunk -> toEntity(documentVersion, chunk))
                        .toList();

        return documentChunkRepository.saveAll(persistentChunks);
    }

    private com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk toEntity(
            DocumentVersion documentVersion,
            com.teamproject.japan_newhire_rag_backend.document.chunking.DocumentChunk chunk) {
        return com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk.create(
                documentVersion,
                chunk.index() + 1,
                null,
                null,
                chunk.content(),
                null);
    }
}
