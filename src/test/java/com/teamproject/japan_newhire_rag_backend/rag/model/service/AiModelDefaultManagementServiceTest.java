package com.teamproject.japan_newhire_rag_backend.rag.model.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.Lock;

import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;

import jakarta.persistence.LockModeType;

class AiModelDefaultManagementServiceTest {

    private final AiModelRepository repository = mock(AiModelRepository.class);
    private final AiModelDefaultManagementService service =
            new AiModelDefaultManagementService(repository);

    @Test
    void designatesActiveEmbeddingModelAsDefault() {
        AiModel target = model(10L, "EMBEDDING", 1536);
        arrangeLookupAndLock(10L, "EMBEDDING", List.of(target));

        service.designateDefaultModel(10L);

        assertTrue(target.isDefault());
        verify(repository).findForUpdateByModelType("EMBEDDING");
    }

    @Test
    void clearsExistingDefaultForSameModelType() {
        AiModel existing = model(10L, "EMBEDDING", 1536);
        existing.markAsDefault();
        AiModel target = model(20L, "EMBEDDING", 768);
        arrangeLookupAndLock(20L, "EMBEDDING", List.of(existing, target));

        service.designateDefaultModel(20L);

        assertFalse(existing.isDefault());
        assertTrue(target.isDefault());
    }

    @Test
    void idempotentWhenTargetAlreadyDefault() {
        AiModel target = model(10L, "EMBEDDING", 1536);
        target.markAsDefault();
        arrangeLookupAndLock(10L, "EMBEDDING", List.of(target));

        assertDoesNotThrow(() -> service.designateDefaultModel(10L));

        assertTrue(target.isDefault());
    }

    @Test
    void rejectsNullIdBeforeRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.designateDefaultModel(null));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMissingTargetWithoutLocking() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.designateDefaultModel(10L));

        verify(repository, never()).findForUpdateByModelType("EMBEDDING");
    }

    @Test
    void rejectsTargetMissingFromLockedModels() {
        arrangeLookupAndLock(10L, "EMBEDDING", List.of());

        assertThrows(IllegalArgumentException.class, () -> service.designateDefaultModel(10L));
    }

    @Test
    void rejectsEmbeddingModelWithNullDimensionBeforeClearingExistingDefault() {
        AiModel existing = model(10L, "EMBEDDING", 1536);
        existing.markAsDefault();
        AiModel target = model(20L, "EMBEDDING", null);
        arrangeLookupAndLock(20L, "EMBEDDING", List.of(existing, target));

        assertThrows(IllegalStateException.class, () -> service.designateDefaultModel(20L));

        assertTrue(existing.isDefault());
        assertFalse(target.isDefault());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsEmbeddingModelWithNonPositiveDimension(int embeddingDimension) {
        AiModel target = model(10L, "EMBEDDING", embeddingDimension);
        arrangeLookupAndLock(10L, "EMBEDDING", List.of(target));

        assertThrows(IllegalStateException.class, () -> service.designateDefaultModel(10L));

        assertFalse(target.isDefault());
    }

    @Test
    void doesNotClearDefaultOfDifferentModelType() {
        AiModel otherTypeDefault = model(10L, "LLM", null);
        otherTypeDefault.markAsDefault();
        AiModel target = model(20L, "EMBEDDING", 1536);
        arrangeLookupAndLock(20L, "EMBEDDING", List.of(target));

        service.designateDefaultModel(20L);

        assertTrue(otherTypeDefault.isDefault());
        assertTrue(target.isDefault());
        verify(repository).findForUpdateByModelType("EMBEDDING");
        verify(repository, never()).findForUpdateByModelType("LLM");
    }

    @Test
    void repositoryUsesPessimisticWriteLock() throws NoSuchMethodException {
        Method method = AiModelRepository.class
                .getMethod("findForUpdateByModelType", String.class);

        Lock lock = method.getAnnotation(Lock.class);

        assertTrue(lock != null && lock.value() == LockModeType.PESSIMISTIC_WRITE);
    }

    private void arrangeLookupAndLock(
            Long aiModelId,
            String modelType,
            List<AiModel> lockedModels) {
        AiModel initiallyFoundModel = mock(AiModel.class);
        when(initiallyFoundModel.getModelType()).thenReturn(modelType);
        when(repository.findById(aiModelId)).thenReturn(Optional.of(initiallyFoundModel));
        when(repository.findForUpdateByModelType(modelType)).thenReturn(lockedModels);
    }

    private AiModel model(Long aiModelId, String modelType, Integer embeddingDimension) {
        AiModel model = spy(AiModel.create(
                "example-provider",
                "example-model-" + aiModelId,
                modelType,
                embeddingDimension));
        doReturn(aiModelId).when(model).getAiModelId();
        return model;
    }
}
