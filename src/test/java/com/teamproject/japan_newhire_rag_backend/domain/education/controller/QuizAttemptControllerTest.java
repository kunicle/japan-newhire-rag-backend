package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptResultResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptSubmitRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.QuizAttemptStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.QuizAttemptService;

@SpringJUnitConfig(QuizAttemptControllerTest.TestConfiguration.class)
@WebAppConfiguration
class QuizAttemptControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private QuizAttemptService quizAttemptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(quizAttemptService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void submitsQuizAttempt() throws Exception {
        QuizAttemptResultResponse response =
                new QuizAttemptResultResponse(
                        100L,
                        1,
                        new BigDecimal("100.00"),
                        true,
                        QuizAttemptStatus.GRADED,
                        2,
                        LocalDateTime.of(
                                2026, 9, 11, 15, 0));

        when(quizAttemptService.submit(
                any(Long.class),
                any(QuizAttemptSubmitRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": 50,
                                  "answers": [
                                    {
                                      "questionId": 10,
                                      "optionId": 101
                                    },
                                    {
                                      "questionId": 20,
                                      "optionId": 201
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(100))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.totalScore").value(100.00))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.attemptStatus")
                        .value("GRADED"))
                .andExpect(jsonPath("$.remainingAttemptCount")
                        .value(2))
                .andExpect(jsonPath("$.submittedAt")
                        .value("2026-09-11T15:00:00"));

        verify(quizAttemptService).submit(
                any(Long.class),
                any(QuizAttemptSubmitRequest.class));
    }

    @Test
    void rejectsInvalidQuizId() throws Exception {
        mockMvc.perform(post("/api/quizzes/invalid/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": 50,
                                  "answers": [
                                    {
                                      "questionId": 10,
                                      "optionId": 101
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(quizAttemptService, never())
                .submit(any(), any());
    }

    @Test
    void rejectsEmptyAnswers() throws Exception {
        mockMvc.perform(post("/api/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentId": 50,
                                  "answers": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(quizAttemptService, never())
                .submit(any(), any());
    }

    @Configuration
    @EnableWebMvc
    @Import({
            QuizAttemptController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        QuizAttemptService quizAttemptService() {
            return mock(QuizAttemptService.class);
        }
    }
}