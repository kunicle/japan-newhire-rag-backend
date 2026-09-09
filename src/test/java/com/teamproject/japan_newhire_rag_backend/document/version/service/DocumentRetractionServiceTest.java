package com.teamproject.japan_newhire_rag_backend.document.version.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.AuditLogRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;

class DocumentRetractionServiceTest {

    private final DocumentVersionRepository repository = mock(DocumentVersionRepository.class);
    private final AuditLogRecordService auditLogRecordService = mock(AuditLogRecordService.class);
    private DocumentRetractionService service;

    @BeforeEach
    void setUp() {
        service = new DocumentRetractionService(repository, auditLogRecordService);
    }

    @Test
    void retractsOnlyTargetPublicVersionAndReturnsMetadata() {
        Document document = activeDocument();
        DocumentVersion target = activeVersion(20L, document);
        DocumentVersion sibling = version(21L, document);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(target, sibling));

        DocumentRetractionResult result = service.retract(10L, 20L, 77L);

        assertThat(target.getPublicationStatus()).isEqualTo("RETRACTED");
        assertThat(target.isActive()).isFalse();
        assertThat(sibling.getPublicationStatus()).isEqualTo("DRAFT");
        assertThat(sibling.isActive()).isFalse();
        assertThat(result.documentId()).isEqualTo(10L);
        assertThat(result.documentVersionId()).isEqualTo(20L);
        assertThat(result.publicationStatus()).isEqualTo("RETRACTED");
        assertThat(result.isActive()).isFalse();
        assertThat(result.retractedAt()).isNotNull();
        assertThat(result.retractedBy()).isEqualTo(77L);
    }

    @Test
    void recordsExactAuditTransitionForTargetVersion() {
        DocumentVersion target = activeVersion(20L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        service.retract(10L, 20L, 77L);

        ArgumentCaptor<AuditLogRecordCommand> commandCaptor =
                ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(auditLogRecordService).record(commandCaptor.capture());
        AuditLogRecordCommand command = commandCaptor.getValue();
        assertThat(command.actorUserId()).isEqualTo(77L);
        assertThat(command.actionType()).isEqualTo(AuditActionType.DOCUMENT_VERSION_RETRACTED);
        assertThat(command.actionType().targetType()).isEqualTo(AuditTargetType.DOCUMENT_VERSION);
        assertThat(command.targetId()).isEqualTo(20L);
        assertThat(command.previousValue()).isEqualTo(
                java.util.Map.of("publicationStatus", "PUBLIC", "isActive", true));
        assertThat(command.changedValue()).isEqualTo(
                java.util.Map.of("publicationStatus", "RETRACTED", "isActive", false));
    }

    @Test
    void rejectsAlreadyRetractedVersionWithConflict() {
        DocumentVersion target = activeVersion(20L, activeDocument());
        target.retract();
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.retract(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("이미 철회된 버전입니다.");
        verifyNoInteractions(auditLogRecordService);
    }

    @Test
    void rejectsDraftAndInactivePublicVersionsWithConflict() {
        Document document = activeDocument();
        DocumentVersion draft = version(20L, document);
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(draft));

        BusinessException draftException = assertThrows(
                BusinessException.class,
                () -> service.retract(10L, 20L, 77L));
        assertThat(draftException.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);

        DocumentVersion inactivePublic = activeVersion(21L, document);
        inactivePublic.deactivate();
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(inactivePublic));

        BusinessException inactiveException = assertThrows(
                BusinessException.class,
                () -> service.retract(10L, 21L, 77L));
        assertThat(inactiveException.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        verifyNoInteractions(auditLogRecordService);
    }

    @Test
    void rejectsVersionThatDoesNotBelongToDocumentAsNotFound() {
        DocumentVersion otherVersion = activeVersion(21L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(otherVersion));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.retract(10L, 20L, 77L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(otherVersion.getPublicationStatus()).isEqualTo("PUBLIC");
        assertThat(otherVersion.isActive()).isTrue();
        verifyNoInteractions(auditLogRecordService);
    }

    @Test
    void rejectsInactiveOrDeletedDocumentWithoutChangingVersions() {
        Document inactiveDocument = mock(Document.class);
        when(inactiveDocument.getDocumentStatus()).thenReturn("INACTIVE");
        DocumentVersion inactiveTarget = activeVersion(20L, inactiveDocument);
        when(repository.findForUpdateByDocument_DocumentId(10L))
                .thenReturn(List.of(inactiveTarget));

        assertThat(assertThrows(
                BusinessException.class,
                () -> service.retract(10L, 20L, 77L)).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(inactiveTarget.getPublicationStatus()).isEqualTo("PUBLIC");

        Document deletedDocument = mock(Document.class);
        when(deletedDocument.getDocumentStatus()).thenReturn("ACTIVE");
        when(deletedDocument.getDeletedAt()).thenReturn(LocalDateTime.now());
        DocumentVersion deletedTarget = activeVersion(21L, deletedDocument);
        when(repository.findForUpdateByDocument_DocumentId(11L))
                .thenReturn(List.of(deletedTarget));

        assertThat(assertThrows(
                BusinessException.class,
                () -> service.retract(11L, 21L, 77L)).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(deletedTarget.getPublicationStatus()).isEqualTo("PUBLIC");
        verifyNoInteractions(auditLogRecordService);
    }

    @Test
    void usesDocumentScopedPessimisticLockAndTransactionalBoundary() {
        DocumentVersion target = activeVersion(20L, activeDocument());
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));

        service.retract(10L, 20L, 77L);

        verify(repository).findForUpdateByDocument_DocumentId(10L);
        assertThat(DocumentRetractionService.class.getAnnotation(Transactional.class))
                .isNotNull();
    }

    @Test
    void propagatesAuditFailureSoTheTransactionCannotCommitPartially() {
        DocumentVersion target = activeVersion(20L, activeDocument());
        RuntimeException auditFailure = new RuntimeException("audit unavailable");
        when(repository.findForUpdateByDocument_DocumentId(10L)).thenReturn(List.of(target));
        doThrow(auditFailure).when(auditLogRecordService)
                .record(org.mockito.ArgumentMatchers.any(AuditLogRecordCommand.class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.retract(10L, 20L, 77L));

        assertThat(exception).isSameAs(auditFailure);
        verify(repository).findForUpdateByDocument_DocumentId(10L);
    }

    @Test
    void rejectsNullArgumentsBeforeRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.retract(null, 20L, 77L));
        assertThrows(IllegalArgumentException.class, () -> service.retract(10L, null, 77L));
        assertThrows(IllegalArgumentException.class, () -> service.retract(10L, 20L, null));

        verifyNoInteractions(repository, auditLogRecordService);
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
