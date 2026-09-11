package com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;

public record QuizOptionResponse(
        Long optionId,
        String optionContent,
        int optionOrder
) {
    public static QuizOptionResponse from(QuizOption option) {
        return new QuizOptionResponse(
                option.getQuizOptionId(),
                option.getOptionContent(),
                option.getOptionOrder());
    }
}