package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseEnrollmentService;

@SpringJUnitConfig(
        CourseEnrollmentControllerTest.TestConfiguration.class)
@WebAppConfiguration
class CourseEnrollmentControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private CourseEnrollmentService courseEnrollmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(courseEnrollmentService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void validRequestReturnsAssignmentResult() throws Exception {
        when(courseEnrollmentService.createEnrollments(
                any(Long.class),
                any(CourseEnrollmentCreateRequest.class)))
                .thenReturn(new CourseEnrollmentCreateResponse(
                        3,
                        2,
                        List.of(12L, 19L)));

        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCount").value(3))
                .andExpect(jsonPath("$.duplicateCount").value(2))
                .andExpect(jsonPath(
                        "$.duplicateEmployeeIds[0]").value(12))
                .andExpect(jsonPath(
                        "$.duplicateEmployeeIds[1]").value(19));

        verify(courseEnrollmentService).createEnrollments(
                any(Long.class),
                any(CourseEnrollmentCreateRequest.class));
    }

    @Test
    void missingTargetTypeReturnsValidationError() throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 100,
                                  "enrollmentRound": "1",
                                  "enrollmentStartDate": "2026-09-01",
                                  "enrollmentDueDate": "2026-09-30"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(courseEnrollmentService, never())
                .createEnrollments(any(), any());
    }

    @Test
    void invalidTargetTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()
                                .replace(
                                        "\"DEPARTMENT\"",
                                        "\"UNKNOWN\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(courseEnrollmentService, never())
                .createEnrollments(any(), any());
    }

    @Test
    void nonPositiveTargetIdReturnsValidationError()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()
                                .replace(
                                        "\"departmentId\": 100",
                                        "\"departmentId\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(courseEnrollmentService, never())
                .createEnrollments(any(), any());
    }

    @Test
    void missingEnrollmentDateReturnsValidationError()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "DEPARTMENT",
                                  "departmentId": 100,
                                  "enrollmentRound": "1",
                                  "enrollmentStartDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(courseEnrollmentService, never())
                .createEnrollments(any(), any());
    }

    @Test
    void courseConflictReturnsConflictResponse()
            throws Exception {
        when(courseEnrollmentService.createEnrollments(
                any(),
                any()))
                .thenThrow(new BusinessException(
                        ErrorCode.CONFLICT,
                        "Only public courses can be assigned"));

        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("Only public courses can be assigned"));
    }

    @Test
    void unauthorizedRoleReturnsForbiddenResponse()
            throws Exception {
        when(courseEnrollmentService.createEnrollments(
                any(),
                any()))
                .thenThrow(new BusinessException(
                        ErrorCode.FORBIDDEN));

        mockMvc.perform(post(
                        "/api/hr/courses/10/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void invalidCourseIdFormatReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/not-a-number/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(courseEnrollmentService, never())
                .createEnrollments(any(), any());
    }

    private String validDepartmentJson() {
        return """
                {
                  "targetType": "DEPARTMENT",
                  "employeeId": null,
                  "departmentId": 100,
                  "jobGradeId": null,
                  "enrollmentRound": "1",
                  "enrollmentStartDate": "2026-09-01",
                  "enrollmentDueDate": "2026-09-30"
                }
                """;
    }

    @Configuration
    @EnableWebMvc
    @Import({
            CourseEnrollmentController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        CourseEnrollmentService courseEnrollmentService() {
            return mock(CourseEnrollmentService.class);
        }
    }
}