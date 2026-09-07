package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

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
import org.springframework.http.MediaType;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.MyOnboardingResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingAssignmentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.enums.OnboardingCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.MyOnboardingService;

@SpringJUnitConfig(
        MyOnboardingControllerTest.TestConfiguration.class)
@WebAppConfiguration
class MyOnboardingControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private MyOnboardingService myOnboardingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(myOnboardingService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void getsCurrentEmployeesOnboarding()
            throws Exception {
        when(myOnboardingService.getMyOnboarding())
                .thenReturn(List.of(
                        onboardingResponse()));

        mockMvc.perform(get("/api/me/onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].onboardingAssignmentId")
                        .value(1))
                .andExpect(jsonPath("$[0].onboardingTaskId")
                        .value(10))
                .andExpect(jsonPath("$[0].departmentId")
                        .value(20))
                .andExpect(jsonPath("$[0].taskTitle")
                        .value("Submit documents"))
                .andExpect(jsonPath("$[0].assignedDate")
                        .value("2026-08-01"))
                .andExpect(jsonPath("$[0].dueDate")
                        .value("2026-08-19"))
                .andExpect(jsonPath("$[0].assignmentStatus")
                        .value("ASSIGNED"))
                .andExpect(jsonPath("$[0].completionStatus")
                        .value("NOT_STARTED"))
                .andExpect(jsonPath("$[0].overdue")
                        .value(true));

        verify(myOnboardingService).getMyOnboarding();
    }

    @Test
    void returnsEmptyListWhenEmployeeHasNoOnboarding()
            throws Exception {
        when(myOnboardingService.getMyOnboarding())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/me/onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(myOnboardingService).getMyOnboarding();
    }

    @Test
    void startsMyOnboardingProgress()
                throws Exception {
        when(myOnboardingService.start(1L))
                .thenReturn(startedOnboardingResponse());

        mockMvc.perform(patch(
                        "/api/me/onboarding/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingAssignmentId")
                        .value(1))
                .andExpect(jsonPath("$.onboardingTaskId")
                        .value(10))
                .andExpect(jsonPath("$.assignmentStatus")
                        .value("ASSIGNED"))
                .andExpect(jsonPath("$.completionStatus")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.overdue")
                        .value(false));

        verify(myOnboardingService).start(1L);
    }

    @Test
    void invalidStartAssignmentIdReturnsBadRequest()
                throws Exception {
        mockMvc.perform(patch(
                        "/api/me/onboarding/not-a-number/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Assignment ID must be a number"));

        verify(myOnboardingService, never())
                .start(any());
    }

    @Test
    void completesMyOnboardingProgress()
                throws Exception {
        when(myOnboardingService.complete(
                eq(1L),
                any()))
                .thenReturn(completedOnboardingResponse());

        mockMvc.perform(patch(
                        "/api/me/onboarding/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "completionNote": "All documents submitted"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingAssignmentId")
                        .value(1))
                .andExpect(jsonPath("$.assignmentStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.completionStatus")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.completionNote")
                        .value("All documents submitted"))
                .andExpect(jsonPath("$.overdue")
                        .value(false));

        verify(myOnboardingService)
                .complete(eq(1L), any());
    }

    @Test
    void completionNoteOverLimitReturnsBadRequest()
                throws Exception {
        String tooLongNote = "a".repeat(1001);

        mockMvc.perform(patch(
                        "/api/me/onboarding/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "completionNote": "%s"
                                }
                                """.formatted(tooLongNote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(myOnboardingService, never())
                .complete(any(), any());
    }

    @Test
    void invalidCompleteAssignmentIdReturnsBadRequest()
                throws Exception {
        mockMvc.perform(patch(
                        "/api/me/onboarding/not-a-number/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "completionNote": "Completed"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Assignment ID must be a number"));

        verify(myOnboardingService, never())
                .complete(any(), any());
    }

    private MyOnboardingResponse onboardingResponse() {
        return new MyOnboardingResponse(
                1L,
                10L,
                20L,
                "Submit documents",
                "Submit all required documents.",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 19),
                OnboardingAssignmentStatus.ASSIGNED,
                OnboardingCompletionStatus.NOT_STARTED,
                null,
                null,
                true);
    }

    private MyOnboardingResponse startedOnboardingResponse() {
        return new MyOnboardingResponse(
                1L,
                10L,
                20L,
                "Submit documents",
                "Submit all required documents.",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 25),
                OnboardingAssignmentStatus.ASSIGNED,
                OnboardingCompletionStatus.IN_PROGRESS,
                null,
                null,
                false);
    }

    private MyOnboardingResponse completedOnboardingResponse() {
        return new MyOnboardingResponse(
                1L,
                10L,
                20L,
                "Submit documents",
                "Submit all required documents.",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 19),
                OnboardingAssignmentStatus.COMPLETED,
                OnboardingCompletionStatus.COMPLETED,
                "All documents submitted",
                LocalDateTime.of(
                        2026, 8, 20, 0, 0),
                false);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            MyOnboardingController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        MyOnboardingService myOnboardingService() {
            return mock(MyOnboardingService.class);
        }
    }
}