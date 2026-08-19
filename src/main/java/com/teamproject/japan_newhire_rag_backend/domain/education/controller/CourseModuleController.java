package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleService;

import jakarta.validation.Valid;

@RestController
public class CourseModuleController {

    private final CourseModuleService courseModuleService;

    public CourseModuleController(CourseModuleService courseModuleService) {
        this.courseModuleService = courseModuleService;
    }

    @GetMapping("/api/hr/courses/{courseId}/modules")
    public List<CourseModuleResponse> getModules(@PathVariable String courseId) {
        return courseModuleService.getModules(parseId(courseId, "Course"));
    }

    @PostMapping("/api/hr/courses/{courseId}/modules")
    public ResponseEntity<CourseModuleResponse> createModule(
            @PathVariable String courseId,
            @Valid @RequestBody CourseModuleCreateRequest request
    ) {
        CourseModuleResponse response = courseModuleService.createModule(
                parseId(courseId, "Course"),
                request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/hr/course-modules/{moduleId}")
    public CourseModuleResponse updateModule(
            @PathVariable String moduleId,
            @Valid @RequestBody CourseModuleUpdateRequest request
    ) {
        return courseModuleService.updateModule(parseId(moduleId, "Course module"), request);
    }

    @PatchMapping("/api/hr/course-modules/{moduleId}/activation")
    public CourseModuleResponse changeActivation(
            @PathVariable String moduleId,
            @Valid @RequestBody CourseModuleActivationRequest request
    ) {
        return courseModuleService.changeActivation(parseId(moduleId, "Course module"), request);
    }

    private Long parseId(String value, String fieldName) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " ID must be a number");
        }
    }
}
