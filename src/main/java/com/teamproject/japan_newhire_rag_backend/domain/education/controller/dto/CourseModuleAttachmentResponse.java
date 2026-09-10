package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;

public record CourseModuleAttachmentResponse(
        Long moduleId,
        String fileName,
        Long fileSize
) {

    public static CourseModuleAttachmentResponse from(CourseModule module) {
        return new CourseModuleAttachmentResponse(
                module.getCourseModuleId(),
                module.getAttachmentOriginalFileName(),
                module.getAttachmentFileSize());
    }
}
