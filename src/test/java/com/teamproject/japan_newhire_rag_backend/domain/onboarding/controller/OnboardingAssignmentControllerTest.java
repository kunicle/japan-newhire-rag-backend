package com.teamproject.japan_newhire_rag_backend.domain.onboarding.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
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

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.onboarding.service.OnboardingAssignmentService;

@SpringJUnitConfig(
        OnboardingAssignmentControllerTest.TestConfiguration.class)
@WebAppConfiguration
class OnboardingAssignmentControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private OnboardingAssignmentService assignmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(assignmentService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void cancelsOnboardingAssignment()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/hr/onboarding-assignments/1/cancel"))
                .andExpect(status().isNoContent());

        verify(assignmentService).cancel(1L);
    }

    @Test
    void invalidAssignmentIdReturnsBadRequest()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/hr/onboarding-assignments/not-a-number/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Assignment ID must be a number"));

        verify(assignmentService, never())
                .cancel(org.mockito.ArgumentMatchers.any());
    }

    @Configuration
    @EnableWebMvc
    @Import({
            OnboardingAssignmentController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        OnboardingAssignmentService assignmentService() {
            return mock(
                    OnboardingAssignmentService.class);
        }
    }
}