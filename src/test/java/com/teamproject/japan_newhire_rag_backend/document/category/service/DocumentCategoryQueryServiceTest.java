package com.teamproject.japan_newhire_rag_backend.document.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.repository.DocumentCategoryRepository;

@ExtendWith(MockitoExtension.class)
class DocumentCategoryQueryServiceTest {

    @Mock DocumentCategoryRepository documentCategoryRepository;

    private DocumentCategoryQueryService service;

    @BeforeEach
    void setUp() {
        service = new DocumentCategoryQueryService(documentCategoryRepository);
    }

    @Test
    void returnsActiveCategoriesSortedByNameAscending() {
        DocumentCategory personnel = category("인사");
        DocumentCategory welfare = category("복지");
        DocumentCategory policy = category("사규");
        when(documentCategoryRepository.findByIsActiveTrue())
                .thenReturn(List.of(personnel, welfare, policy));

        List<DocumentCategory> result = service.getActiveCategories();

        assertEquals(List.of(welfare, policy, personnel), result);
        verify(documentCategoryRepository).findByIsActiveTrue();
    }

    @Test
    void returnsEmptyListWhenNoActiveCategoriesExist() {
        when(documentCategoryRepository.findByIsActiveTrue()).thenReturn(List.of());

        List<DocumentCategory> result = service.getActiveCategories();

        assertTrue(result.isEmpty());
        verify(documentCategoryRepository).findByIsActiveTrue();
    }

    private DocumentCategory category(String categoryName) {
        DocumentCategory category = mock(DocumentCategory.class);
        when(category.getCategoryName()).thenReturn(categoryName);
        return category;
    }
}
