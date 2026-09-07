package com.teamproject.japan_newhire_rag_backend.document.chunk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

class DocumentChunkPersistenceServiceTest {

    @Test
    void splitsMapsAndSavesAllChunksInSequenceOrder() {
        CapturingRepository repository = new CapturingRepository();
        DocumentChunkPersistenceService service =
                new DocumentChunkPersistenceService(repository.proxy());
        DocumentVersion version = documentVersion();

        List<DocumentChunk> result = service.splitAndSave(version, "abcdefghijkl", 5, 1);

        assertThat(repository.saveAllCallCount).isEqualTo(1);
        assertThat(repository.savedChunks).containsExactlyElementsOf(result);
        assertThat(result).hasSize(3);
        assertThat(result).extracting(DocumentChunk::getChunkSequence)
                .containsExactly(1, 2, 3);
        assertThat(result).extracting(DocumentChunk::getChunkContent)
                .containsExactly("abcde", "efghi", "ijkl");
        assertThat(result).allSatisfy(chunk -> {
            assertThat(chunk.getDocumentVersion()).isSameAs(version);
            assertThat(chunk.getChunkSequence()).isPositive();
            assertThat(chunk.getArticleNumber()).isNull();
            assertThat(chunk.getArticleTitle()).isNull();
            assertThat(chunk.getTokenCount()).isNull();
            assertThat(chunk.getChunkStatus()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void delegatesChunkingValidationAndDoesNotSaveOnFailure() {
        CapturingRepository repository = new CapturingRepository();
        DocumentChunkPersistenceService service =
                new DocumentChunkPersistenceService(repository.proxy());

        assertThatThrownBy(() -> service.splitAndSave(documentVersion(), "   ", 5, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분할할 텍스트가 비어 있습니다.");
        assertThat(repository.saveAllCallCount).isZero();
    }

    @Test
    void rejectsNullDocumentVersionBeforeSaving() {
        CapturingRepository repository = new CapturingRepository();
        DocumentChunkPersistenceService service =
                new DocumentChunkPersistenceService(repository.proxy());

        assertThatThrownBy(() -> service.splitAndSave(null, "text", 5, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DocumentVersion은 null일 수 없습니다.");
        assertThat(repository.saveAllCallCount).isZero();
    }

    @Test
    void splitAndSaveIsTransactional() throws NoSuchMethodException {
        Transactional transactional = DocumentChunkPersistenceService.class
                .getDeclaredMethod(
                        "splitAndSave",
                        DocumentVersion.class,
                        String.class,
                        int.class,
                        int.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    private DocumentVersion documentVersion() {
        return DocumentVersion.create(
                null,
                "v1",
                LocalDate.of(2026, 1, 1),
                null,
                "rules.txt",
                "/documents/rules.txt",
                100L);
    }

    private static class CapturingRepository {

        private final List<DocumentChunk> savedChunks = new ArrayList<>();
        private int saveAllCallCount;

        private DocumentChunkRepository proxy() {
            return (DocumentChunkRepository) Proxy.newProxyInstance(
                    DocumentChunkRepository.class.getClassLoader(),
                    new Class<?>[] {DocumentChunkRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("saveAll")) {
                            saveAllCallCount++;
                            ((Iterable<?>) arguments[0]).forEach(
                                    value -> savedChunks.add((DocumentChunk) value));
                            return List.copyOf(savedChunks);
                        }
                        if (method.getName().equals("toString")) {
                            return "CapturingDocumentChunkRepository";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
