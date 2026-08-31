package com.teamproject.japan_newhire_rag_backend.document.category.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.repository.DocumentCategoryRepository;

@Service
@Transactional(readOnly = true)
public class DocumentCategoryQueryService {

    private final DocumentCategoryRepository documentCategoryRepository;

    public DocumentCategoryQueryService(DocumentCategoryRepository documentCategoryRepository) {
        this.documentCategoryRepository = documentCategoryRepository;
    }

    public List<DocumentCategory> getActiveCategories() {
        return documentCategoryRepository.findByIsActiveTrue()
                .stream()
                .sorted(Comparator.comparing(DocumentCategory::getCategoryName))
                .toList();
    }
}
