package com.teamproject.japan_newhire_rag_backend.document.category.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.document.category.controller.dto.DocumentCategoryResponse;
import com.teamproject.japan_newhire_rag_backend.document.category.service.DocumentCategoryQueryService;

@RestController
@RequestMapping("/api/documents/categories")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentCategoryController {

    private final DocumentCategoryQueryService documentCategoryQueryService;

    public DocumentCategoryController(DocumentCategoryQueryService documentCategoryQueryService) {
        this.documentCategoryQueryService = documentCategoryQueryService;
    }

    @GetMapping
    public List<DocumentCategoryResponse> getActiveCategories() {
        return documentCategoryQueryService.getActiveCategories()
                .stream()
                .map(DocumentCategoryResponse::from)
                .toList();
    }
}
