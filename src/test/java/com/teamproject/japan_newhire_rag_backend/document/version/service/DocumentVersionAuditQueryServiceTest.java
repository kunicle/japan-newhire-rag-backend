package com.teamproject.japan_newhire_rag_backend.document.version.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.controller.dto.DocumentVersionAuditEventPageResponse;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.AuditLogQueryService;

class DocumentVersionAuditQueryServiceTest {

    private DocumentVersionRepository documentVersionRepository;
    private AuditLogQueryService auditLogQueryService;
    private DocumentVersionAuditQueryService service;

    @BeforeEach
    void setUp() {
        documentVersionRepository = mock(DocumentVersionRepository.class);
        auditLogQueryService = mock(AuditLogQueryService.class);
        service = new DocumentVersionAuditQueryService(
                documentVersionRepository,
                auditLogQueryService);
    }

    @Test
    void returnsNarrowPagedEventsUsingServerControlledAuditFilters() {
        stubVersion(10L, 20L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 9, 12, 30);
        AuditLogResponse auditLog = new AuditLogResponse(
                100L,
                77L,
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                AuditTargetType.DOCUMENT_VERSION,
                20L,
                "{\"publicationStatus\":\"PUBLIC\",\"isActive\":true}",
                "{\"publicationStatus\":\"RETRACTED\",\"isActive\":false}",
                "127.0.0.1",
                "request-id",
                createdAt);
        when(auditLogQueryService.findAll(
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                null,
                AuditTargetType.DOCUMENT_VERSION,
                20L,
                null,
                null,
                2,
                10)).thenReturn(new AuditLogPageResponse(List.of(auditLog), 2, 10, 21, 3));

        DocumentVersionAuditEventPageResponse result = service.findAll(10L, 20L, 2, 10);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).hasSize(1);
        var event = result.content().get(0);
        assertThat(event.actionType()).isEqualTo(AuditActionType.DOCUMENT_VERSION_RETRACTED);
        assertThat(event.actorUserId()).isEqualTo(77L);
        assertThat(event.previousValue()).isEqualTo(auditLog.previousValue());
        assertThat(event.changedValue()).isEqualTo(auditLog.changedValue());
        assertThat(event.createdAt()).isEqualTo(createdAt);
        verify(auditLogQueryService).findAll(
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                null,
                AuditTargetType.DOCUMENT_VERSION,
                20L,
                null,
                null,
                2,
                10);
    }

    @Test
    void returnsEmptyPageForVersionWithoutEvents() {
        stubVersion(10L, 20L);
        when(auditLogQueryService.findAll(
                AuditActionType.DOCUMENT_VERSION_RETRACTED,
                null,
                AuditTargetType.DOCUMENT_VERSION,
                20L,
                null,
                null,
                0,
                20)).thenReturn(new AuditLogPageResponse(List.of(), 0, 20, 0, 0));

        DocumentVersionAuditEventPageResponse result = service.findAll(10L, 20L, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void rejectsMissingOrForeignDocumentVersionCombination() {
        when(documentVersionRepository.findById(20L)).thenReturn(Optional.empty());

        BusinessException missing = assertThrows(
                BusinessException.class,
                () -> service.findAll(10L, 20L, 0, 20));
        assertThat(missing.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(auditLogQueryService);

        stubVersion(11L, 20L);
        BusinessException foreign = assertThrows(
                BusinessException.class,
                () -> service.findAll(10L, 20L, 0, 20));
        assertThat(foreign.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(auditLogQueryService);
    }

    @Test
    void rejectsNullIdentifiersBeforeRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.findAll(null, 20L, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.findAll(10L, null, 0, 20));

        verifyNoInteractions(documentVersionRepository, auditLogQueryService);
    }

    private void stubVersion(Long documentId, Long documentVersionId) {
        Document document = mock(Document.class);
        when(document.getDocumentId()).thenReturn(documentId);
        DocumentVersion version = mock(DocumentVersion.class);
        when(version.getDocument()).thenReturn(document);
        when(version.getDocumentVersionId()).thenReturn(documentVersionId);
        when(documentVersionRepository.findById(documentVersionId))
                .thenReturn(Optional.of(version));
    }
}
