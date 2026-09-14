package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptResultResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptSubmitRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.QuizAttemptService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quizzes")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    public QuizAttemptController(
            QuizAttemptService quizAttemptService
    ) {
        this.quizAttemptService = quizAttemptService;
    }

    @PostMapping("/{quizId}/attempts")
    public QuizAttemptResultResponse submit(
            @PathVariable String quizId,
            @Valid @RequestBody QuizAttemptSubmitRequest request
    ) {
        return quizAttemptService.submit(
                parseQuizId(quizId),
                request);
    }

    private Long parseQuizId(String quizId) {
        try {
            long parsed = Long.parseLong(quizId);

            if (parsed <= 0) {
                throw new NumberFormatException();
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Quiz ID must be a positive number");
        }
    }
}