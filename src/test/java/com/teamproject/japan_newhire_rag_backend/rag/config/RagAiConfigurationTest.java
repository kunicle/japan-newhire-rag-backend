package com.teamproject.japan_newhire_rag_backend.rag.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryService;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;

class RagAiConfigurationTest {

    @Test
    void createsRagBeanGraphWithoutEvidenceThresholdDependency() {
        DocumentSearchScopeService documentSearchScopeService =
                new DocumentSearchScopeService(null, null);
        EmbeddingModelSelectionService embeddingModelSelectionService =
                new EmbeddingModelSelectionService(null);

        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            context.registerBean(
                    DocumentChunkRepository.class,
                    () -> mock(DocumentChunkRepository.class));
            context.registerBean(
                    DocumentSearchScopeService.class,
                    () -> documentSearchScopeService);
            context.registerBean(
                    EmbeddingModelSelectionService.class,
                    () -> embeddingModelSelectionService);
            context.register(RagAiConfiguration.class);
            context.refresh();

            AiRagClient aiRagClient = context.getBean(AiRagClient.class);
            AiEmbeddingClient aiEmbeddingClient = context.getBean(AiEmbeddingClient.class);
            RagOrchestrator ragOrchestrator = context.getBean(RagOrchestrator.class);
            RagQueryService ragQueryService = context.getBean(RagQueryService.class);

            assertInstanceOf(PythonAiRagClient.class, aiRagClient);
            assertInstanceOf(PythonAiEmbeddingClient.class, aiEmbeddingClient);
            assertSame(
                    aiRagClient,
                    ReflectionTestUtils.getField(ragOrchestrator, "aiRagClient"));
            assertSame(
                    documentSearchScopeService,
                    ReflectionTestUtils.getField(ragQueryService, "documentSearchScopeService"));
            assertSame(
                    embeddingModelSelectionService,
                    ReflectionTestUtils.getField(ragQueryService, "embeddingModelSelectionService"));
        }
    }
}
