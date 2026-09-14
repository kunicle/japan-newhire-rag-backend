package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.HrOxQuizResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizActivationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.HrQuizManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/courses/{courseId}/quizzes")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrQuizController {

    private final HrQuizManagementService quizManagementService;

    public HrQuizController(
            HrQuizManagementService quizManagementService
    ) {
        this.quizManagementService = quizManagementService;
    }

    @PostMapping
    public ResponseEntity<HrOxQuizResponse> createCourseQuiz(
            @PathVariable String courseId,
            @Valid @RequestBody OxQuizCreateRequest request
    ) {
        HrOxQuizResponse response =
                quizManagementService.createCourseQuiz(
                    parsePositiveId(courseId, "Course"),
                    request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<HrOxQuizResponse> getCourseQuizzes(
            @PathVariable String courseId
    ) {
        return quizManagementService.getCourseQuizzes(
                parsePositiveId(courseId, "Course"));
    }

    @GetMapping("/{quizId}")
    public HrOxQuizResponse getCourseQuiz(
            @PathVariable String courseId,
            @PathVariable String quizId
    ) {
        return quizManagementService.getCourseQuiz(
                parsePositiveId(courseId, "Course"),
                parsePositiveId(quizId, "Quiz"));
    }

    private Long parsePositiveId(
            String value,
            String name
    ) {
        try {
            long parsed = Long.parseLong(value);

            if (parsed <= 0) {
                throw new NumberFormatException();
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    name + " ID must be a positive number");
        }
    }

    @PatchMapping("/{quizId}/activation")
    public HrOxQuizResponse changeCourseQuizActivation(
            @PathVariable String courseId,
            @PathVariable String quizId,
            @Valid @RequestBody QuizActivationUpdateRequest request
    ) {
        return quizManagementService.changeCourseQuizActivation(
                parsePositiveId(courseId, "Course"),
                parsePositiveId(quizId, "Quiz"),
                request);
    }
}