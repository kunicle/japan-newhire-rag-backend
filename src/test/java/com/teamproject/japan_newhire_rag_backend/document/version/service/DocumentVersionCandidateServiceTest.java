package com.teamproject.japan_newhire_rag_backend.document.version.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;

@ExtendWith(MockitoExtension.class)
class DocumentVersionCandidateServiceTest {

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    private DocumentVersionCandidateService service;

    @BeforeEach
    void setUp() {
        service = new DocumentVersionCandidateService(documentVersionRepository);
    }

    @Test
    void queriesRepositoryWithActiveDocumentPublicVersionAndToday() {
        when(documentVersionRepository
                .findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                        anyString(), anyString(), any()))
                .thenReturn(List.of());

        service.findCandidateDocumentVersionIds();

        ArgumentCaptor<String> documentStatusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> publicationStatusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> todayCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(documentVersionRepository)
                .findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                        documentStatusCaptor.capture(),
                        publicationStatusCaptor.capture(),
                        todayCaptor.capture());

        assertEquals("ACTIVE", documentStatusCaptor.getValue());
        assertEquals("PUBLIC", publicationStatusCaptor.getValue());
        assertEquals(LocalDate.now(), todayCaptor.getValue());
    }

    @Test
    void appliesExpirationDateInclusiveJavaFilter() {
        LocalDate today = LocalDate.now();
        DocumentVersion noExpiration = documentVersion(1L, null);
        DocumentVersion expiresToday = documentVersion(2L, today);
        DocumentVersion expiresInFuture = documentVersion(3L, today.plusDays(1));
        DocumentVersion expiredYesterday = documentVersion(4L, today.minusDays(1));
        when(documentVersionRepository
                .findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                        anyString(), anyString(), any()))
                .thenReturn(List.of(noExpiration, expiresToday, expiresInFuture, expiredYesterday));

        Set<Long> result = service.findCandidateDocumentVersionIds();

        assertEquals(Set.of(1L, 2L, 3L), result);
    }

    @Test
    void returnsEmptySetWhenRepositoryResultIsEmpty() {
        when(documentVersionRepository
                .findByDocument_DocumentStatusAndDocument_DeletedAtIsNullAndPublicationStatusAndIsActiveTrueAndEffectiveDateLessThanEqual(
                        anyString(), anyString(), any()))
                .thenReturn(List.of());

        assertEquals(Set.of(), service.findCandidateDocumentVersionIds());
    }

    private DocumentVersion documentVersion(Long documentVersionId, LocalDate expirationDate) {
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        lenient().when(documentVersion.getDocumentVersionId()).thenReturn(documentVersionId);
        when(documentVersion.getExpirationDate()).thenReturn(expirationDate);
        return documentVersion;
    }
}
