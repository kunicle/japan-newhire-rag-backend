package com.teamproject.japan_newhire_rag_backend.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.repository.DocumentCategoryRepository;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.repository.DocumentRepository;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

class DocumentLifecycleServiceTest {

    private final DocumentCategoryRepository categoryRepository =
            mock(DocumentCategoryRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentVersionRepository versionRepository =
            mock(DocumentVersionRepository.class);
    private final DocumentLifecycleService service = new DocumentLifecycleService(
            categoryRepository,
            documentRepository,
            versionRepository);

    @Test
    void createsDocumentWithInitialVersion() {
        DocumentCategory category = DocumentCategory.create("POLICY", "사내 규정", null);
        DocumentVersion savedVersion = mock(DocumentVersion.class);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        DocumentVersion result = service.createDocumentWithInitialVersion(
                10L,
                "육아휴직 규정",
                "육아휴직 안내 문서",
                "2026-01",
                "육아휴직.txt",
                "/documents/육아휴직.txt",
                1024L,
                30L);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(documentCaptor.capture());
        Document document = documentCaptor.getValue();
        assertThat(document.getDocumentCategory()).isSameAs(category);
        assertThat(document.getDocumentName()).isEqualTo("육아휴직 규정");
        assertThat(document.getDocumentDescription()).isEqualTo("육아휴직 안내 문서");
        assertThat(document.getDocumentStatus()).isEqualTo("ACTIVE");
        assertThat(document.getCreatedBy()).isEqualTo(30L);

        ArgumentCaptor<DocumentVersion> versionCaptor =
                ArgumentCaptor.forClass(DocumentVersion.class);
        verify(versionRepository).save(versionCaptor.capture());
        DocumentVersion version = versionCaptor.getValue();
        assertThat(version.getDocument()).isSameAs(document);
        assertThat(version.getVersionName()).isEqualTo("2026-01");
        assertThat(version.getEffectiveDate()).isEqualTo(LocalDate.now());
        assertThat(version.getExpirationDate()).isNull();
        assertThat(version.getOriginalFileName()).isEqualTo("육아휴직.txt");
        assertThat(version.getStoredFilePath()).isEqualTo("/documents/육아휴직.txt");
        assertThat(version.getFileSize()).isEqualTo(1024L);
        assertThat(version.getMimeType()).isEqualTo("text/plain");
        assertThat(version.getPublicationStatus()).isEqualTo("DRAFT");
        assertThat(version.isActive()).isFalse();
        assertThat(version.getPublishedBy()).isNull();
        assertThat(result).isSameAs(savedVersion);

        InOrder order = inOrder(categoryRepository, documentRepository, versionRepository);
        order.verify(categoryRepository).findById(10L);
        order.verify(documentRepository).save(document);
        order.verify(versionRepository).save(version);
    }

    @Test
    void rejectsMissingDocumentCategory() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDocumentWithInitialVersion(
                10L, "규정", null, "v1", "규정.txt", "/documents/규정.txt", 10L, 30L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(documentRepository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }

    @Test
    void rejectsNullDocumentCategoryIdWithoutRepositoryAccess() {
        assertThatThrownBy(() -> service.createDocumentWithInitialVersion(
                null, "규정", null, "v1", "규정.txt", "/documents/규정.txt", 10L, 30L))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(categoryRepository, documentRepository, versionRepository);
    }
}
