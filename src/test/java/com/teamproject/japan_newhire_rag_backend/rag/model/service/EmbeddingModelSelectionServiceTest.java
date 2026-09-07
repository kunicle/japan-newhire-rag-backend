package com.teamproject.japan_newhire_rag_backend.rag.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

class EmbeddingModelSelectionServiceTest {

    private final AiModelRepository repository = mock(AiModelRepository.class);
    private final EmbeddingModelSelectionService service =
            new EmbeddingModelSelectionService(repository);

    @Test
    void returnsSelectionWhenExactlyOneDefaultActiveEmbeddingModelExists() {
        AiModel model = model(10L, "provider-a", "model-a", 1536);
        when(repository.findByModelTypeAndModelStatusAndIsDefaultTrue("EMBEDDING", "ACTIVE"))
                .thenReturn(List.of(model));

        EmbeddingModelSelection selection = service.selectDefaultEmbeddingModel();

        assertEquals(10L, selection.aiModelId());
        assertEquals("provider-a", selection.providerName());
        assertEquals("model-a", selection.modelName());
        assertEquals(1536, selection.embeddingDimension());
        verify(repository)
                .findByModelTypeAndModelStatusAndIsDefaultTrue("EMBEDDING", "ACTIVE");
    }

    @Test
    void failsClosedWhenDefaultEmbeddingModelDoesNotExist() {
        when(repository.findByModelTypeAndModelStatusAndIsDefaultTrue("EMBEDDING", "ACTIVE"))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, service::selectDefaultEmbeddingModel);
    }

    @Test
    void failsClosedWhenMultipleDefaultEmbeddingModelsExist() {
        AiModel firstModel = model(10L, "provider-a", "model-a", 1536);
        AiModel secondModel = model(20L, "provider-b", "model-b", 768);
        when(repository.findByModelTypeAndModelStatusAndIsDefaultTrue("EMBEDDING", "ACTIVE"))
                .thenReturn(List.of(firstModel, secondModel));

        assertThrows(IllegalStateException.class, service::selectDefaultEmbeddingModel);
    }

    private AiModel model(
            Long aiModelId,
            String providerName,
            String modelName,
            Integer embeddingDimension) {
        AiModel model = mock(AiModel.class);
        when(model.getAiModelId()).thenReturn(aiModelId);
        when(model.getProviderName()).thenReturn(providerName);
        when(model.getModelName()).thenReturn(modelName);
        when(model.getEmbeddingDimension()).thenReturn(embeddingDimension);
        return model;
    }
}
