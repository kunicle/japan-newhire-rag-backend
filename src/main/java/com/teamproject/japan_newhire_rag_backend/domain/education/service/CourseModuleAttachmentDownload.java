package com.teamproject.japan_newhire_rag_backend.domain.education.service;

public record CourseModuleAttachmentDownload(
        String fileName,
        byte[] content
) {
}
