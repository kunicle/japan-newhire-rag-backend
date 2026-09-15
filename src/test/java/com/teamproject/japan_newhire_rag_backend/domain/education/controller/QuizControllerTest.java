package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptReviewResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizOptionResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizQuestionResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizQuestionReviewResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.QuizQueryService;

@SpringJUnitConfig(QuizControllerTest.TestConfiguration.class)
@WebAppConfiguration
class QuizControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private QuizQueryService quizQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(quizQueryService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void getsQuizWithoutExposingCorrectAnswer() throws Exception {
        QuizOptionResponse option =
                new QuizOptionResponse(100L, "Option A", 1);

        QuizQuestionResponse question =
                new QuizQuestionResponse(
                        10L,
                        "Which option is correct?",
                        1,
                        new BigDecimal("20.00"),
                        List.of(option));

        QuizDetailResponse response =
                new QuizDetailResponse(
                        1L,
                        2L,
                        3L,
                        "Basic quiz",
                        new BigDecimal("80.00"),
                        3,
                        1,
                        List.of(question));

        when(quizQueryService.getQuiz(1L, 50L))
                .thenReturn(response);

        mockMvc.perform(get("/api/quizzes/1")
                        .param("enrollmentId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(1))
                .andExpect(jsonPath("$.courseId").value(2))
                .andExpect(jsonPath("$.quizTitle")
                        .value("Basic quiz"))
                .andExpect(jsonPath("$.attemptsUsed").value(1))
                .andExpect(jsonPath("$.questions[0].questionId")
                        .value(10))
                .andExpect(jsonPath("$.questions[0].options[0].optionId")
                        .value(100))
                .andExpect(jsonPath(
                        "$.questions[0].options[0].correct")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$.questions[0].options[0].isCorrect")
                        .doesNotExist());

        verify(quizQueryService).getQuiz(1L, 50L);
    }

    @Test
    void exposesCorrectAnswerOnlyInsideTerminalAttemptReview()
            throws Exception {
        QuizOptionResponse option =
                new QuizOptionResponse(100L, "Wrong answer", 1);

        QuizQuestionResponse question =
                new QuizQuestionResponse(
                        10L,
                        "Which option is correct?",
                        1,
                        new BigDecimal("100.00"),
                        List.of(option));

        QuizQuestionReviewResponse questionReview =
                new QuizQuestionReviewResponse(
                        10L,
                        "Which option is correct?",
                        1,
                        new BigDecimal("100.00"),
                        100L,
                        "Wrong answer",
                        101L,
                        "Correct answer",
                        false,
                        new BigDecimal("0.00"));

        QuizAttemptReviewResponse attemptReview =
                new QuizAttemptReviewResponse(
                        900L,
                        3,
                        new BigDecimal("0.00"),
                        false,
                        0,
                        LocalDateTime.of(
                                2026, 9, 15, 10, 30),
                        List.of(questionReview));

        QuizDetailResponse response =
                new QuizDetailResponse(
                        1L,
                        2L,
                        3L,
                        "Basic quiz",
                        new BigDecimal("80.00"),
                        3,
                        true,
                        3,
                        List.of(question),
                        attemptReview);

        when(quizQueryService.getQuiz(1L, 50L))
                .thenReturn(response);

        mockMvc.perform(get("/api/quizzes/1")
                        .param("enrollmentId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptsUsed").value(3))
                .andExpect(jsonPath(
                        "$.questions[0].options[0].correct")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$.questions[0].options[0].isCorrect")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$.latestAttemptReview.attemptId")
                        .value(900))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.attemptNumber")
                        .value(3))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.passed")
                        .value(false))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.remainingAttemptCount")
                        .value(0))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.questions[0].questionContent")
                        .value("Which option is correct?"))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.questions[0].selectedOptionContent")
                        .value("Wrong answer"))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.questions[0].correctOptionContent")
                        .value("Correct answer"))
                .andExpect(jsonPath(
                        "$.latestAttemptReview.questions[0].correct")
                        .value(false));

        verify(quizQueryService).getQuiz(1L, 50L);
    }

    @Test
    void rejectsInvalidQuizId() throws Exception {
        mockMvc.perform(get("/api/quizzes/not-a-number")
                        .param("enrollmentId", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));
    }

    @Test
    void rejectsInvalidEnrollmentId() throws Exception {
        mockMvc.perform(get("/api/quizzes/1")
                        .param("enrollmentId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            QuizController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        QuizQueryService quizQueryService() {
            return mock(QuizQueryService.class);
        }
    }
}