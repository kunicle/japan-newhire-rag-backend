package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePublicationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseCreateRequest request
    ) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public CoursePageResponse getCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return courseService.getCourses(page, size);
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourse(@PathVariable String courseId) {
        return courseService.getCourse(parseCourseId(courseId));
    }

    @PutMapping("/{courseId}")
    public CourseResponse updateCourse(
            @PathVariable String courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return courseService.updateCourse(parseCourseId(courseId), request);
    }

    @PatchMapping("/{courseId}/publication")
    public CourseResponse changePublicationStatus(
            @PathVariable String courseId,
            @Valid @RequestBody CoursePublicationUpdateRequest request
    ) {
        return courseService.changePublicationStatus(parseCourseId(courseId), request);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        courseService.deleteCourse(parseCourseId(courseId));
        return ResponseEntity.noContent().build();
    }

    private Long parseCourseId(String courseId) {
        try {
            return Long.valueOf(courseId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Course ID must be a number");
        }
    }
}
