package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record CoursePageResponse(
        List<CourseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static CoursePageResponse from(Page<CourseResponse> coursePage) {
        return new CoursePageResponse(
                coursePage.getContent(),
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements(),
                coursePage.getTotalPages(),
                coursePage.isFirst(),
                coursePage.isLast());
    }
}
