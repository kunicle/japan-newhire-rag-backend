package com.teamproject.japan_newhire_rag_backend.rag.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;

import jakarta.persistence.LockModeType;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    List<AiModel> findByModelTypeAndModelStatusAndIsDefaultTrue(
            String modelType,
            String modelStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AiModel> findForUpdateByModelType(String modelType);
}
