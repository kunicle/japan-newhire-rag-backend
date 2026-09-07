package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.MyCourseQueryService;

@RestController
@RequestMapping("/api/me/courses")
public class MyCourseController {

    private final MyCourseQueryService myCourseQueryService;

    public MyCourseController(
            MyCourseQueryService myCourseQueryService
    ) {
        this.myCourseQueryService = myCourseQueryService;
    }

    @GetMapping
    public MyCoursePageResponse getMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return myCourseQueryService.getMyCourses(page, size);
    }

    @GetMapping("/{enrollmentId}")
    public MyCourseDetailResponse getMyCourse(
            @PathVariable String enrollmentId
    ) {
        return myCourseQueryService.getMyCourse(
                parseEnrollmentId(enrollmentId));
    }

    private Long parseEnrollmentId(String enrollmentId) {
        try {
            return Long.valueOf(enrollmentId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Enrollment ID must be a number");
        }
    }
}