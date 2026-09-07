package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseEnrollmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/courses")
public class CourseEnrollmentController {

    private final CourseEnrollmentService courseEnrollmentService;

    public CourseEnrollmentController(
            CourseEnrollmentService courseEnrollmentService
    ) {
        this.courseEnrollmentService = courseEnrollmentService;
    }

    @PostMapping("/{courseId}/enrollments")
    public ResponseEntity<CourseEnrollmentCreateResponse> createEnrollments(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseEnrollmentCreateRequest request
    ) {
        CourseEnrollmentCreateResponse response =
                courseEnrollmentService.createEnrollments(
                        courseId,
                        request);

        return ResponseEntity.ok(response);
    }
}