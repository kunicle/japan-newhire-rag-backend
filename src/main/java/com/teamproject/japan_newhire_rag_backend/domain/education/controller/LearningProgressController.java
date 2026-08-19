package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.LearningProgressUpdateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.LearningProgressService;

@RestController
@RequestMapping("/api/me/learning-progress")
public class LearningProgressController {

    private final LearningProgressService learningProgressService;

    public LearningProgressController(
            LearningProgressService learningProgressService
    ) {
        this.learningProgressService = learningProgressService;
    }

    @PatchMapping("/{progressId}/start")
    public LearningProgressUpdateResponse startProgress(
            @PathVariable String progressId
    ) {
        return learningProgressService.startProgress(
                parseProgressId(progressId));
    }

    @PatchMapping("/{progressId}/complete")
    public LearningProgressUpdateResponse completeProgress(
            @PathVariable String progressId
    ) {
        return learningProgressService.completeProgress(
                parseProgressId(progressId));
    }

    private Long parseProgressId(String progressId) {
        try {
            return Long.valueOf(progressId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Progress ID must be a number");
        }
    }
}