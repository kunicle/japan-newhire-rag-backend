package com.teamproject.japan_newhire_rag_backend.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;

class DocumentDeletionServiceTest {

    private DocumentRepository documentRepository;
    private DocumentDeletionService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        service = new DocumentDeletionService(documentRepository);
    }

    @Test
    void softDeletesAnActiveDocument() {
        Document document = Document.create(null, "Policy", "Description", 1L);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        service.deleteDocument(1L);

        assertNotNull(document.getDeletedAt());
    }

    @Test
    void rejectsNullDocumentId() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteDocument(null));
    }

    @Test
    void rejectsMissingDocument() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("문서를 찾을 수 없습니다.",
                assertThrows(BusinessException.class, () -> service.deleteDocument(99L)).getMessage());
    }

    @Test
    void rejectsAlreadyDeletedDocument() {
        Document document = Document.create(null, "Policy", "Description", 1L);
        document.softDelete();
        LocalDateTime firstDeletedAt = document.getDeletedAt();
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThrows(BusinessException.class, () -> service.deleteDocument(1L));
        assertEquals(firstDeletedAt, document.getDeletedAt());
    }

    @Test
    void rejectsInactiveDocument() {
        Document document = mock(Document.class);
        when(document.getDocumentStatus()).thenReturn("INACTIVE");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThrows(BusinessException.class, () -> service.deleteDocument(1L));
    }
}
