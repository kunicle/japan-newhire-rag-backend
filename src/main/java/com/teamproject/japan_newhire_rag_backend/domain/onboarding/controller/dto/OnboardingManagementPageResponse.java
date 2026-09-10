package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record OnboardingManagementPageResponse(
        List<OnboardingManagementItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static OnboardingManagementPageResponse from(
            Page<OnboardingManagementItemResponse> responsePage
    ) {
        return new OnboardingManagementPageResponse(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast());
    }
}
