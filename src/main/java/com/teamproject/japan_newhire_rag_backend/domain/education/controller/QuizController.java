package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.QuizQueryService;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizQueryService quizQueryService;

    public QuizController(QuizQueryService quizQueryService) {
        this.quizQueryService = quizQueryService;
    }

    @GetMapping("/{quizId}")
    public QuizDetailResponse getQuiz(
            @PathVariable String quizId,
            @RequestParam String enrollmentId
    ) {
        return quizQueryService.getQuiz(
                parsePositiveId(quizId, "Quiz"),
                parsePositiveId(enrollmentId, "Enrollment"));
    }

    private Long parsePositiveId(String value, String name) {
        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    name + " ID must be a positive number");
        }
    }
}