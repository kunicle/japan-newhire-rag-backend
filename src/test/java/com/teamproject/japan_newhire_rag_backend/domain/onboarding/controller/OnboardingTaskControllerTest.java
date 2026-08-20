package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingAssignmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller.dto.OnboardingTaskResponse;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingTaskService;

@SpringJUnitConfig(
        OnboardingTaskControllerTest.TestConfiguration.class)
@WebAppConfiguration
class OnboardingTaskControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private OnboardingTaskService onboardingTaskService;

    @Autowired
    private OnboardingAssignmentService onboardingAssignmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                onboardingTaskService,
                onboardingAssignmentService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void createsOnboardingTask() throws Exception {
        when(onboardingTaskService.createTask(any()))
                .thenReturn(taskResponse(true));

        mockMvc.perform(post("/api/hr/onboarding-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 10,
                                  "taskTitle": "Submit documents",
                                  "taskDescription": "Submit required documents.",
                                  "defaultDueDays": 7
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.departmentId").value(10))
                .andExpect(jsonPath("$.taskTitle")
                        .value("Submit documents"))
                .andExpect(jsonPath("$.defaultDueDays")
                        .value(7))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value(100));

        verify(onboardingTaskService).createTask(any());
    }

    @Test
    void invalidCreateRequestReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/hr/onboarding-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 0,
                                  "taskTitle": "",
                                  "taskDescription": "",
                                  "defaultDueDays": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(onboardingTaskService, never())
                .createTask(any());
    }

    @Test
    void updatesOnboardingTask() throws Exception {
        when(onboardingTaskService.updateTask(
                eq(1L),
                any()))
                .thenReturn(taskResponse(true));

        mockMvc.perform(put("/api/hr/onboarding-tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 10,
                                  "taskTitle": "Submit documents",
                                  "taskDescription": "Submit required documents.",
                                  "defaultDueDays": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.active").value(true));

        verify(onboardingTaskService)
                .updateTask(eq(1L), any());
    }

    @Test
    void changesOnboardingTaskActivation()
            throws Exception {
        when(onboardingTaskService.changeActivation(
                eq(1L),
                any()))
                .thenReturn(taskResponse(false));

        mockMvc.perform(patch(
                        "/api/hr/onboarding-tasks/1/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.active").value(false));

        verify(onboardingTaskService)
                .changeActivation(eq(1L), any());
    }

    @Test
    void createsOnboardingAssignments()
            throws Exception {
        when(onboardingAssignmentService.assign(
                eq(1L),
                any()))
                .thenReturn(
                        new OnboardingAssignmentCreateResponse(
                                1L,
                                4,
                                2,
                                2));

        mockMvc.perform(post(
                        "/api/hr/onboarding-tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeIds": [1, 1, 2, 3]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.onboardingTaskId")
                        .value(1))
                .andExpect(jsonPath("$.requestedCount")
                        .value(4))
                .andExpect(jsonPath("$.successCount")
                        .value(2))
                .andExpect(jsonPath("$.duplicateCount")
                        .value(2));

        verify(onboardingAssignmentService)
                .assign(eq(1L), any());
    }

    @Test
    void emptyAssignmentEmployeeIdsReturnBadRequest()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/onboarding-tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(onboardingAssignmentService, never())
                .assign(any(), any());
    }

    @Test
    void invalidAssignmentEmployeeIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/onboarding-tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeIds": [0]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(onboardingAssignmentService, never())
                .assign(any(), any());
    }

    @Test
    void invalidTaskIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/hr/onboarding-tasks/not-a-number/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Task ID must be a number"));

        verify(onboardingTaskService, never())
                .changeActivation(any(), any());
    }

    @Test
    void invalidAssignmentTaskIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(post(
                        "/api/hr/onboarding-tasks/not-a-number/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Task ID must be a number"));

        verify(onboardingAssignmentService, never())
                .assign(any(), any());
    }

    private OnboardingTaskResponse taskResponse(
            boolean active
    ) {
        return new OnboardingTaskResponse(
                1L,
                10L,
                "Submit documents",
                "Submit required documents.",
                7,
                active,
                100L,
                LocalDateTime.of(
                        2026, 8, 20, 10, 0),
                LocalDateTime.of(
                        2026, 8, 20, 10, 0));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            OnboardingTaskController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        OnboardingTaskService onboardingTaskService() {
            return mock(OnboardingTaskService.class);
        }

        @Bean
        OnboardingAssignmentService
                onboardingAssignmentService() {
            return mock(
                    OnboardingAssignmentService.class);
        }
    }
}