package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePublicationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(CourseControllerTest.TestConfiguration.class)
@WebAppConfiguration
class CourseControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private CourseService courseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(courseService);
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void validRequestReturnsCreatedResponse() throws Exception {
        when(courseService.createCourse(any(CourseCreateRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/hr/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(100))
                .andExpect(jsonPath("$.publicationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value(7));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        when(courseService.createCourse(any(CourseCreateRequest.class)))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"));

        mockMvc.perform(post("/api/hr/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void employeeRequestReturnsForbidden() throws Exception {
        when(courseService.createCourse(any(CourseCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/hr/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void blankCourseNameReturnsBadRequest() throws Exception {
        assertValidationError(validJson().replace("New hire fundamentals", "   "));
    }

    @Test
    void courseNameLongerThanOneHundredCharactersReturnsBadRequest() throws Exception {
        assertValidationError(validJson().replace("New hire fundamentals", "a".repeat(101)));
    }

    @Test
    void descriptionLongerThanTwoThousandCharactersReturnsBadRequest() throws Exception {
        assertValidationError(validJson().replace("Company onboarding basics", "a".repeat(2001)));
    }

    @Test
    void missingRequiredReturnsBadRequest() throws Exception {
        assertValidationError("""
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "trainingStartDate":"2026-09-01",
                  "trainingEndDate":"2026-09-30"
                }
                """);
    }

    @Test
    void missingStartDateReturnsBadRequest() throws Exception {
        assertValidationError("""
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "required":true,
                  "trainingEndDate":"2026-09-30"
                }
                """);
    }

    @Test
    void missingEndDateReturnsBadRequest() throws Exception {
        assertValidationError("""
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "required":true,
                  "trainingStartDate":"2026-09-01"
                }
                """);
    }

    @Test
    void courseListReturnsDefaultPageResponse() throws Exception {
        when(courseService.getCourses(0, 20)).thenReturn(pageResponse());

        mockMvc.perform(get("/api/hr/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].courseId").value(100))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(courseService).getCourses(0, 20);
    }

    @Test
    void courseDetailReturnsCourseResponse() throws Exception {
        when(courseService.getCourse(100L)).thenReturn(response());

        mockMvc.perform(get("/api/hr/courses/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(100))
                .andExpect(jsonPath("$.courseName").value("New hire fundamentals"))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());
    }

    @Test
    void missingOrDeletedCourseReturnsNotFound() throws Exception {
        when(courseService.getCourse(999L))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/hr/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void invalidCourseIdFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/hr/courses/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(courseService, never()).getCourse(any());
    }

    @Test
    void listAuthenticationAndAuthorizationErrorsUseCommonResponses() throws Exception {
        when(courseService.getCourses(0, 20))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/hr/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/hr/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validUpdateReturnsOkCourseResponse() throws Exception {
        when(courseService.updateCourse(any(), any(CourseUpdateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/hr/courses/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(100))
                .andExpect(jsonPath("$.courseName").value("New hire fundamentals"));

        verify(courseService).updateCourse(any(), any(CourseUpdateRequest.class));
    }

    @Test
    void updateWithBlankCourseNameReturnsBadRequest() throws Exception {
        assertUpdateValidationError(validUpdateJson().replace("New hire fundamentals", "   "));
    }

    @Test
    void updateWithCourseNameOverOneHundredCharactersReturnsBadRequest() throws Exception {
        assertUpdateValidationError(validUpdateJson()
                .replace("New hire fundamentals", "a".repeat(101)));
    }

    @Test
    void updateWithDescriptionOverTwoThousandCharactersReturnsBadRequest() throws Exception {
        assertUpdateValidationError(validUpdateJson()
                .replace("Company onboarding basics", "a".repeat(2001)));
    }

    @Test
    void updateWithMissingRequiredReturnsBadRequest() throws Exception {
        assertUpdateValidationError("""
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "trainingStartDate":"2026-09-01",
                  "trainingEndDate":"2026-09-30"
                }
                """);
    }

    @Test
    void updateWithMissingDateReturnsBadRequest() throws Exception {
        assertUpdateValidationError("""
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "required":true,
                  "trainingStartDate":"2026-09-01"
                }
                """);
    }

    @Test
    void updateWithInvalidCourseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/hr/courses/not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(courseService, never()).updateCourse(any(), any());
    }

    @Test
    void updatingMissingOrDeletedCourseReturnsNotFound() throws Exception {
        when(courseService.updateCourse(any(), any(CourseUpdateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"));

        mockMvc.perform(put("/api/hr/courses/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void validPublicationChangeReturnsCourseResponse() throws Exception {
        when(courseService.changePublicationStatus(
                any(), any(CoursePublicationUpdateRequest.class)))
                .thenReturn(publicResponse());

        mockMvc.perform(patch("/api/hr/courses/100/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicationStatus\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"));
    }

    @Test
    void missingPublicationStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/hr/courses/100/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(courseService, never()).changePublicationStatus(any(), any());
    }

    @Test
    void invalidPublicationStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/hr/courses/100/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicationStatus\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(courseService, never()).changePublicationStatus(any(), any());
    }

    @Test
    void invalidPublicationTransitionUsesCommonConflictResponse()
            throws Exception {
        when(courseService.changePublicationStatus(any(), any()))
                .thenThrow(new BusinessException(
                        ErrorCode.CONFLICT,
                        "Course publication status cannot transition "
                                + "from DRAFT to PRIVATE"));


        mockMvc.perform(patch("/api/hr/courses/100/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"publicationStatus\":\"PRIVATE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        "Course publication status cannot transition "
                                + "from DRAFT to PRIVATE"));
    }

    @Test
    void deletingCourseReturnsNoContentWithoutBody() throws Exception {
        mockMvc.perform(delete("/api/hr/courses/100"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(courseService).deleteCourse(100L);
    }

    @Test
    void deletingMissingOrDeletedCourseReturnsNotFound() throws Exception {
        org.mockito.Mockito.doThrow(
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"))
                .when(courseService).deleteCourse(999L);

        mockMvc.perform(delete("/api/hr/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private void assertValidationError(String body) throws Exception {
        mockMvc.perform(post("/api/hr/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(courseService, never()).createCourse(any());
    }

    private void assertUpdateValidationError(String body) throws Exception {
        mockMvc.perform(put("/api/hr/courses/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(courseService, never()).updateCourse(any(), any());
    }

    private String validJson() {
        return """
                {
                  "courseName":"New hire fundamentals",
                  "courseDescription":"Company onboarding basics",
                  "required":true,
                  "trainingStartDate":"2026-09-01",
                  "trainingEndDate":"2026-09-30"
                }
                """;
    }

    private String validUpdateJson() {
        return validJson();
    }

    private CourseResponse response() {
        return new CourseResponse(
                100L,
                "New hire fundamentals",
                "Company onboarding basics",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                CoursePublicationStatus.DRAFT,
                7L,
                LocalDateTime.of(2026, 8, 12, 10, 0),
                LocalDateTime.of(2026, 8, 12, 10, 0));
    }

    private CoursePageResponse pageResponse() {
        return new CoursePageResponse(
                List.of(response()),
                0,
                20,
                1,
                1,
                true,
                true);
    }

    private CourseResponse publicResponse() {
        CourseResponse response = response();
        return new CourseResponse(
                response.courseId(),
                response.courseName(),
                response.courseDescription(),
                response.required(),
                response.trainingStartDate(),
                response.trainingEndDate(),
                CoursePublicationStatus.PUBLIC,
                response.createdBy(),
                response.createdAt(),
                response.updatedAt());
    }

    @Configuration
    @EnableWebMvc
    @Import({CourseController.class, GlobalExceptionHandler.class})
    static class TestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        CourseService courseService() {
            return mock(CourseService.class);
        }
    }
}
