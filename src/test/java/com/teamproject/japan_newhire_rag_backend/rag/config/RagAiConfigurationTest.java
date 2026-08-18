package com.teamproject.japan_newhire_rag_backend.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiEmbeddingClient;
import com.teamproject.japan_newhire_rag_backend.rag.ai.PythonAiRagClient;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryService;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;

class RagAiConfigurationTest {

    @Test
    void createsRagBeanGraphWithConfiguredEvidenceThreshold() {
        DocumentSearchScopeService documentSearchScopeService =
                new DocumentSearchScopeService(null, null);
        EmbeddingModelSelectionService embeddingModelSelectionService =
                new EmbeddingModelSelectionService(null);

        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource(
                            "rag-test-properties",
                            Map.of("rag.evidence-threshold", 0.7)));
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
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
            assertEquals(
                    0.7,
                    ReflectionTestUtils.getField(ragOrchestrator, "evidenceThreshold"));
            assertSame(
                    documentSearchScopeService,
                    ReflectionTestUtils.getField(ragQueryService, "documentSearchScopeService"));
            assertSame(
                    embeddingModelSelectionService,
                    ReflectionTestUtils.getField(ragQueryService, "embeddingModelSelectionService"));
        }
    }
}
