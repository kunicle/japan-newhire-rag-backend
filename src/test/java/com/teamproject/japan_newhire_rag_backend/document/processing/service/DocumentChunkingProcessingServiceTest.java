package com.teamproject.japan_newhire_rag_backend.document.processing.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.chunk.service.DocumentChunkPersistenceService;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJob;
import com.teamproject.japan_newhire_rag_backend.document.processing.entity.DocumentProcessingJobDetail;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobDetailRepository;
import com.teamproject.japan_newhire_rag_backend.document.processing.repository.DocumentProcessingJobRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;

class DocumentChunkingProcessingServiceTest {

    @Test
    void processesChunkingAndCreatesOneCompletedDetailForEachSavedChunk() {
        CapturingDocumentChunkRepository chunkRepository = new CapturingDocumentChunkRepository();
        CapturingJobRepository jobRepository = new CapturingJobRepository();
        CapturingDetailRepository detailRepository = new CapturingDetailRepository();
        DocumentChunkPersistenceService chunkPersistenceService =
                new DocumentChunkPersistenceService(chunkRepository.proxy());
        DocumentChunkingProcessingService service = new DocumentChunkingProcessingService(
                jobRepository.proxy(),
                detailRepository.proxy(),
                chunkPersistenceService,
                new DocumentChunkProperties(5, 1));
        DocumentVersion documentVersion = documentVersion();

        DocumentProcessingJob job = service.processChunking(
                documentVersion,
                "abcdefghijkl",
                10L);

        assertThat(chunkRepository.saveAllCallCount).isEqualTo(1);
        assertThat(chunkRepository.savedChunks).hasSize(3);
        assertThat(jobRepository.saveCallCount).isEqualTo(1);
        assertThat(jobRepository.savedJob).isSameAs(job);
        assertThat(detailRepository.saveAllCallCount).isEqualTo(1);
        assertThat(detailRepository.savedDetails).hasSize(3);

        assertThat(detailRepository.savedDetails)
                .extracting(DocumentProcessingJobDetail::getDocumentChunk)
                .containsExactlyElementsOf(chunkRepository.savedChunks)
                .doesNotContainNull();
        assertThat(detailRepository.savedDetails).allSatisfy(detail -> {
            assertThat(detail.getDocumentProcessingJob()).isSameAs(job);
            assertThat(detail.getProcessingStep()).isEqualTo("CHUNKING");
            assertThat(detail.getProcessingStatus()).isEqualTo("COMPLETED");
            assertThat(detail.getAttemptNumber()).isEqualTo(1);
            assertThat(detail.getFailureReason()).isNull();
            assertThat(detail.getProcessedAt()).isNotNull();
        });
        assertThat(detailRepository.savedDetails)
                .extracting(DocumentProcessingJobDetail::getProcessedAt)
                .containsOnly(detailRepository.savedDetails.get(0).getProcessedAt());

        assertThat(job.getProcessingStatus()).isEqualTo("PROCESSING");
        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getTotalChunkCount()).isEqualTo(3);
        assertThat(job.getCompletedChunkCount()).isZero();
        assertThat(job.getFailedChunkCount()).isZero();
        assertThat(job.getRetryCount()).isZero();
    }

    @Test
    void processChunkingIsTransactional() throws NoSuchMethodException {
        Transactional transactional = DocumentChunkingProcessingService.class
                .getDeclaredMethod(
                        "processChunking",
                        DocumentVersion.class,
                        String.class,
                        Long.class)
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

    private static class CapturingDocumentChunkRepository {

        private final List<DocumentChunk> savedChunks = new ArrayList<>();
        private int saveAllCallCount;

        private DocumentChunkRepository proxy() {
            return repositoryProxy(DocumentChunkRepository.class, (methodName, arguments) -> {
                if (methodName.equals("saveAll")) {
                    saveAllCallCount++;
                    ((Iterable<?>) arguments[0]).forEach(
                            value -> savedChunks.add((DocumentChunk) value));
                    return List.copyOf(savedChunks);
                }
                throw new UnsupportedOperationException(methodName);
            });
        }
    }

    private static class CapturingJobRepository {

        private DocumentProcessingJob savedJob;
        private int saveCallCount;

        private DocumentProcessingJobRepository proxy() {
            return repositoryProxy(DocumentProcessingJobRepository.class, (methodName, arguments) -> {
                if (methodName.equals("save")) {
                    saveCallCount++;
                    savedJob = (DocumentProcessingJob) arguments[0];
                    return savedJob;
                }
                throw new UnsupportedOperationException(methodName);
            });
        }
    }

    private static class CapturingDetailRepository {

        private final List<DocumentProcessingJobDetail> savedDetails = new ArrayList<>();
        private int saveAllCallCount;

        private DocumentProcessingJobDetailRepository proxy() {
            return repositoryProxy(
                    DocumentProcessingJobDetailRepository.class,
                    (methodName, arguments) -> {
                        if (methodName.equals("saveAll")) {
                            saveAllCallCount++;
                            ((Iterable<?>) arguments[0]).forEach(
                                    value -> savedDetails.add((DocumentProcessingJobDetail) value));
                            return List.copyOf(savedDetails);
                        }
                        throw new UnsupportedOperationException(methodName);
                    });
        }
    }

    private static <T> T repositoryProxy(Class<T> repositoryType, RepositoryInvocation invocation) {
        return repositoryType.cast(Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class<?>[] {repositoryType},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("toString")) {
                        return "Capturing" + repositoryType.getSimpleName();
                    }
                    return invocation.invoke(method.getName(), arguments);
                }));
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
