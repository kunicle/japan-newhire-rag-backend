package com.teamproject.japan_newhire_rag_backend.document.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentUploadResponse;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentIngestionOrchestrationService;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentIngestionResult;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('HR_MANAGER', 'SYSTEM_ADMIN')")
public class DocumentUploadController {

    private static final String INITIAL_VERSION_NAME = "v1";

    private final DocumentIngestionOrchestrationService documentIngestionOrchestrationService;
    private final CurrentUserProvider currentUserProvider;

    public DocumentUploadController(
            DocumentIngestionOrchestrationService documentIngestionOrchestrationService,
            CurrentUserProvider currentUserProvider) {
        this.documentIngestionOrchestrationService = documentIngestionOrchestrationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long documentCategoryId,
            @RequestParam String title,
            @RequestParam(required = false) String description) {
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        byte[] content = readBytes(file);

        DocumentIngestionResult result = documentIngestionOrchestrationService.ingest(
                documentCategoryId,
                title,
                description,
                INITIAL_VERSION_NAME,
                file.getOriginalFilename(),
                content,
                appUserId);

        return DocumentUploadResponse.from(result);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("업로드된 파일을 읽을 수 없습니다.", exception);
        }
    }
}
