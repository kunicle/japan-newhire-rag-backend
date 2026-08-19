package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseModuleResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.MyCourseQueryService;

@SpringJUnitConfig(MyCourseControllerTest.TestConfiguration.class)
@WebAppConfiguration
class MyCourseControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private MyCourseQueryService myCourseQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(myCourseQueryService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void getsMyCoursesWithDefaultPagination() throws Exception {
        MyCourseSummaryResponse course =
                new MyCourseSummaryResponse(
                        100L,
                        50L,
                        "New hire course",
                        true,
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("25.00"),
                        EnrollmentStatus.IN_PROGRESS);

        when(myCourseQueryService.getMyCourses(0, 20))
                .thenReturn(new MyCoursePageResponse(
                        List.of(course),
                        0,
                        20,
                        1,
                        1,
                        true,
                        true));

        mockMvc.perform(get("/api/me/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].enrollmentId")
                        .value(100))
                .andExpect(jsonPath("$.content[0].courseId")
                        .value(50))
                .andExpect(jsonPath("$.content[0].courseName")
                        .value("New hire course"))
                .andExpect(jsonPath("$.content[0].required")
                        .value(true))
                .andExpect(jsonPath("$.content[0].enrollmentDueDate")
                        .value("2026-09-30"))
                .andExpect(jsonPath("$.content[0].progressRate")
                        .value(25.00))
                .andExpect(jsonPath("$.content[0].status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(myCourseQueryService).getMyCourses(0, 20);
    }

    @Test
    void getsMyCoursesWithRequestedPagination() throws Exception {
        when(myCourseQueryService.getMyCourses(2, 10))
                .thenReturn(new MyCoursePageResponse(
                        List.of(),
                        2,
                        10,
                        25,
                        3,
                        false,
                        true));

        mockMvc.perform(get("/api/me/courses")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(myCourseQueryService).getMyCourses(2, 10);
    }

    @Test
    void getsMyCourseDetail() throws Exception {
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 9, 2, 10, 0);

        MyCourseModuleResponse module =
                new MyCourseModuleResponse(
                        1000L,
                        200L,
                        "Company rules",
                        "Read the company rules.",
                        "https://example.com/rules",
                        1,
                        true,
                        LearningCompletionStatus.IN_PROGRESS,
                        startedAt,
                        null);

        MyCourseDetailResponse response =
                new MyCourseDetailResponse(
                        100L,
                        50L,
                        "New hire course",
                        "Basic onboarding course",
                        true,
                        "1",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("25.00"),
                        EnrollmentStatus.IN_PROGRESS,
                        null,
                        List.of(module));

        when(myCourseQueryService.getMyCourse(100L))
                .thenReturn(response);

        mockMvc.perform(get("/api/me/courses/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(100))
                .andExpect(jsonPath("$.courseId").value(50))
                .andExpect(jsonPath("$.courseName")
                        .value("New hire course"))
                .andExpect(jsonPath("$.enrollmentRound")
                        .value("1"))
                .andExpect(jsonPath("$.progressRate")
                        .value(25.00))
                .andExpect(jsonPath("$.status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.modules[0].progressId")
                        .value(1000))
                .andExpect(jsonPath("$.modules[0].moduleId")
                        .value(200))
                .andExpect(jsonPath("$.modules[0].moduleOrder")
                        .value(1))
                .andExpect(jsonPath("$.modules[0].required")
                        .value(true))
                .andExpect(jsonPath(
                        "$.modules[0].completionStatus")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.modules[0].startedAt")
                        .value("2026-09-02T10:00:00"));

        verify(myCourseQueryService).getMyCourse(100L);
    }

    @Test
    void invalidEnrollmentIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get(
                        "/api/me/courses/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Enrollment ID must be a number"));

        verify(myCourseQueryService, never())
                .getMyCourse(any());
    }

    @Test
    void anotherEmployeesEnrollmentReturnsForbidden()
            throws Exception {
        when(myCourseQueryService.getMyCourse(100L))
                .thenThrow(new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "Course enrollment belongs to another employee"));

        mockMvc.perform(get("/api/me/courses/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"));
    }

    @Test
    void missingEnrollmentReturnsNotFound()
            throws Exception {
        when(myCourseQueryService.getMyCourse(404L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course enrollment not found"));

        mockMvc.perform(get("/api/me/courses/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            MyCourseController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        MyCourseQueryService myCourseQueryService() {
            return mock(MyCourseQueryService.class);
        }
    }
}