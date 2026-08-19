package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.LearningProgressUpdateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.LearningProgressService;

@SpringJUnitConfig(
        LearningProgressControllerTest.TestConfiguration.class)
@WebAppConfiguration
class LearningProgressControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private LearningProgressService learningProgressService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(learningProgressService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void startsLearningProgress() throws Exception {
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 9, 10, 9, 0);

        when(learningProgressService.startProgress(1000L))
                .thenReturn(new LearningProgressUpdateResponse(
                        1000L,
                        100L,
                        200L,
                        LearningCompletionStatus.IN_PROGRESS,
                        startedAt,
                        null,
                        new BigDecimal("0.00"),
                        EnrollmentStatus.IN_PROGRESS));

        mockMvc.perform(patch(
                        "/api/me/learning-progress/1000/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressId")
                        .value(1000))
                .andExpect(jsonPath("$.enrollmentId")
                        .value(100))
                .andExpect(jsonPath("$.moduleId")
                        .value(200))
                .andExpect(jsonPath("$.completionStatus")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt")
                        .value("2026-09-10T09:00:00"))
                .andExpect(jsonPath("$.completedAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.progressRate")
                        .value(0.00))
                .andExpect(jsonPath("$.enrollmentStatus")
                        .value("IN_PROGRESS"));

        verify(learningProgressService)
                .startProgress(1000L);
    }

    @Test
    void invalidProgressIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/me/learning-progress/not-a-number/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Progress ID must be a number"));

        verify(learningProgressService, never())
                .startProgress(any());
    }

    @Test
    void anotherEmployeesProgressReturnsForbidden()
            throws Exception {
        when(learningProgressService.startProgress(1000L))
                .thenThrow(new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "Learning progress belongs to another employee"));

        mockMvc.perform(patch(
                        "/api/me/learning-progress/1000/start"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"));
    }

    @Test
    void missingProgressReturnsNotFound()
            throws Exception {
        when(learningProgressService.startProgress(404L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Learning progress not found"));

        mockMvc.perform(patch(
                        "/api/me/learning-progress/404/start"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void completesLearningProgress() throws Exception {
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 9, 10, 8, 30);
        LocalDateTime completedAt =
                LocalDateTime.of(2026, 9, 10, 9, 0);

        when(learningProgressService.completeProgress(1000L))
                .thenReturn(new LearningProgressUpdateResponse(
                        1000L,
                        100L,
                        200L,
                        LearningCompletionStatus.COMPLETED,
                        startedAt,
                        completedAt,
                        new BigDecimal("100.00"),
                        EnrollmentStatus.COMPLETED));

        mockMvc.perform(patch(
                        "/api/me/learning-progress/1000/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressId")
                        .value(1000))
                .andExpect(jsonPath("$.enrollmentId")
                        .value(100))
                .andExpect(jsonPath("$.moduleId")
                        .value(200))
                .andExpect(jsonPath("$.completionStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.startedAt")
                        .value("2026-09-10T08:30:00"))
                .andExpect(jsonPath("$.completedAt")
                        .value("2026-09-10T09:00:00"))
                .andExpect(jsonPath("$.progressRate")
                        .value(100.00))
                .andExpect(jsonPath("$.enrollmentStatus")
                        .value("COMPLETED"));

        verify(learningProgressService)
                .completeProgress(1000L);
    }

    @Test
    void invalidCompleteProgressIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/me/learning-progress/not-a-number/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Progress ID must be a number"));

        verify(learningProgressService, never())
                .completeProgress(any());
    }

    @Configuration
    @EnableWebMvc
    @Import({
            LearningProgressController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        LearningProgressService learningProgressService() {
            return mock(LearningProgressService.class);
        }
    }
}