package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleAttachmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleAttachmentDownload;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleAttachmentService;

@RestController
public class CourseModuleAttachmentController {

    private final CourseModuleAttachmentService attachmentService;

    public CourseModuleAttachmentController(
            CourseModuleAttachmentService attachmentService
    ) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/api/hr/course-modules/{moduleId}/attachment",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CourseModuleAttachmentResponse> upload(
            @PathVariable String moduleId,
            @RequestPart("file") MultipartFile file
    ) {
        CourseModuleAttachmentResponse response =
                attachmentService.upload(
                        parseId(moduleId),
                        file.getOriginalFilename(),
                        readBytes(file));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/course-modules/{moduleId}/attachment")
    public ResponseEntity<byte[]> download(
            @PathVariable String moduleId
    ) {
        CourseModuleAttachmentDownload download =
                attachmentService.download(parseId(moduleId));

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                download.fileName(),
                                StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(new MediaType(
                        "text",
                        "plain",
                        StandardCharsets.UTF_8))
                .contentLength(download.content().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(download.content());
    }

    @DeleteMapping("/api/hr/course-modules/{moduleId}/attachment")
    public ResponseEntity<Void> delete(
            @PathVariable String moduleId
    ) {
        attachmentService.delete(parseId(moduleId));
        return ResponseEntity.noContent().build();
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Course module ID must be a number");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "업로드된 파일을 읽을 수 없습니다.",
                    exception);
        }
    }
}
