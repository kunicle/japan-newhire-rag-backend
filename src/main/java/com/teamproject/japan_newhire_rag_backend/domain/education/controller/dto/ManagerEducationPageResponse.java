package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record ManagerEducationPageResponse(
        List<ManagerEducationItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static ManagerEducationPageResponse from(
            Page<ManagerEducationItemResponse> educationPage
    ) {
        return new ManagerEducationPageResponse(
                educationPage.getContent(),
                educationPage.getNumber(),
                educationPage.getSize(),
                educationPage.getTotalElements(),
                educationPage.getTotalPages(),
                educationPage.isFirst(),
                educationPage.isLast());
    }
}