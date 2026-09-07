package com.teamproject.japan_newhire_rag_backend.document.version.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

import jakarta.persistence.LockModeType;

class DocumentPublicationServiceTest {

    private final DocumentVersionRepository repository = mock(DocumentVersionRepository.class);
    private final DocumentPublicationService service = new DocumentPublicationService(repository);

    @Test
    void publishesTargetVersionAndSetsPublicFieldsCorrectly() {
        DocumentVersion target = version(20L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        DocumentPublicationResult result = service.publish(10L, 20L, 77L);

        assertThat(target.getPublicationStatus()).isEqualTo("PUBLIC");
        assertThat(target.isActive()).isTrue();
        assertThat(target.getPublishedBy()).isEqualTo(77L);
        assertThat(target.getPublishedAt()).isNotNull();
        assertThat(result.documentId()).isEqualTo(10L);
        assertThat(result.documentVersionId()).isEqualTo(20L);
        assertThat(result.publicationStatus()).isEqualTo("PUBLIC");
        assertThat(result.active()).isTrue();
        assertThat(result.publishedAt()).isEqualTo(target.getPublishedAt());
        assertThat(result.publishedBy()).isEqualTo(77L);
    }

    @Test
    void deactivatesOtherActiveVersionsInSameDocument() {
        Document document = activeDocument();
        DocumentVersion previous = version(20L, document);
        previous.publish(11L, LocalDateTime.now().minusDays(1));
        DocumentVersion target = version(21L, document);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(previous, target));

        service.publish(10L, 21L, 77L);

        assertThat(previous.isActive()).isFalse();
        assertThat(previous.getPublicationStatus()).isEqualTo("PUBLIC");
        assertThat(target.isActive()).isTrue();
    }

    @Test
    void republicatesPreviouslyPublicInactiveVersion() {
        DocumentVersion target = version(20L, activeDocument());
        LocalDateTime originalPublishedAt = LocalDateTime.now().minusDays(1);
        target.publish(11L, originalPublishedAt);
        target.deactivate();
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        service.publish(10L, 20L, 77L);

        assertThat(target.getPublicationStatus()).isEqualTo("PUBLIC");
        assertThat(target.isActive()).isTrue();
        assertThat(target.getPublishedBy()).isEqualTo(77L);
        assertThat(target.getPublishedAt()).isAfter(originalPublishedAt);
    }

    @Test
    void throwsResourceNotFoundWhenVersionNotInLockedList() {
        DocumentVersion other = version(21L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(other));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.publish(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(other.isActive()).isFalse();
    }

    @Test
    void throwsResourceNotFoundWhenDocumentIsInactive() {
        Document inactiveDocument = mock(Document.class);
        when(inactiveDocument.getDocumentStatus()).thenReturn("INACTIVE");
        DocumentVersion target = version(20L, inactiveDocument);
        DocumentVersion previous = activeVersion(21L, inactiveDocument);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target, previous));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.publish(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(target.isActive()).isFalse();
        assertThat(previous.isActive()).isTrue();
    }

    @Test
    void throwsResourceNotFoundWhenDocumentIsDeleted() {
        Document deletedDocument = mock(Document.class);
        when(deletedDocument.getDocumentStatus()).thenReturn("ACTIVE");
        when(deletedDocument.getDeletedAt()).thenReturn(LocalDateTime.now());
        DocumentVersion target = version(20L, deletedDocument);
        DocumentVersion previous = activeVersion(21L, deletedDocument);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target, previous));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.publish(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(target.isActive()).isFalse();
        assertThat(previous.isActive()).isTrue();
    }

    @Test
    void throwsConflictWhenAlreadyPublicAndActive() {
        Document document = activeDocument();
        DocumentVersion target = activeVersion(20L, document);
        DocumentVersion previous = activeVersion(21L, document);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target, previous));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.publish(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(target.isActive()).isTrue();
        assertThat(previous.isActive()).isTrue();
    }

    @Test
    void usesPessimisticLockQuery() {
        DocumentVersion target = version(20L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        service.publish(10L, 20L, 77L);

        verify(repository).findForUpdateByDocument_DocumentId(10L);
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.publish(null, 20L, 77L));
        assertThrows(IllegalArgumentException.class, () -> service.publish(10L, null, 77L));
        assertThrows(IllegalArgumentException.class, () -> service.publish(10L, 20L, null));

        verifyNoInteractions(repository);
    }

    @Test
    void repositoryUsesPessimisticWriteLock() throws NoSuchMethodException {
        Method method = DocumentVersionRepository.class
                .getMethod("findForUpdateByDocument_DocumentId", Long.class);

        Lock lock = method.getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private Document activeDocument() {
        return Document.create(null, "규정", null, 1L);
    }

    private DocumentVersion activeVersion(Long id, Document document) {
        DocumentVersion version = version(id, document);
        version.publish(11L, LocalDateTime.now().minusDays(1));
        return version;
    }

    private DocumentVersion version(Long id, Document document) {
        DocumentVersion version = spy(DocumentVersion.create(
                document,
                "v" + id,
                LocalDate.now(),
                null,
                "규정.txt",
                "/documents/규정.txt",
                10L));
        doReturn(id).when(version).getDocumentVersionId();
        return version;
    }
}
