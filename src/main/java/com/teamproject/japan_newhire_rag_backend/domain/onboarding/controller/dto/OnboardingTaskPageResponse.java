package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record OnboardingTaskPageResponse(
        List<OnboardingTaskResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static OnboardingTaskPageResponse from(
            Page<OnboardingTaskResponse> taskPage
    ) {
        return new OnboardingTaskPageResponse(
                taskPage.getContent(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isFirst(),
                taskPage.isLast());
    }
}