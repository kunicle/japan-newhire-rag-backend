package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizActivationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.HrOxQuizResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.HrQuizManagementService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(HrQuizControllerTest.TestConfiguration.class)
@WebAppConfiguration
class HrQuizControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private HrQuizManagementService quizManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(quizManagementService);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void validRequestReturnsCreatedQuiz() throws Exception {
        when(quizManagementService.createCourseQuiz(
                eq(10L),
                any(OxQuizCreateRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/hr/courses/10/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quizId").value(200))
                .andExpect(jsonPath("$.courseId").value(10))
                .andExpect(jsonPath("$.quizTitle")
                        .value("Security basics"))
                .andExpect(jsonPath("$.passingScore")
                        .value(80.00))
                .andExpect(jsonPath("$.maxAttemptCount")
                        .value(3))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value(7))
                .andExpect(jsonPath("$.questions.length()")
                        .value(2))
                .andExpect(jsonPath(
                        "$.questions[0].correctAnswer")
                        .value("X"))
                .andExpect(jsonPath(
                        "$.questions[1].correctAnswer")
                        .value("O"));

        verify(quizManagementService)
                .createCourseQuiz(
                        eq(10L),
                        any(OxQuizCreateRequest.class));
    }

    @Test
    void invalidCourseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post(
                        "/api/hr/courses/not-a-number/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(quizManagementService, never())
                .createCourseQuiz(any(), any());
    }

    @Test
    void zeroCourseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/hr/courses/0/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(quizManagementService, never())
                .createCourseQuiz(any(), any());
    }

    @Test
    void blankQuizTitleReturnsBadRequest() throws Exception {
        assertValidationError(
                validJson().replace(
                        "Security basics",
                        "   "));
    }

    @Test
    void emptyQuestionsReturnBadRequest() throws Exception {
        assertValidationError("""
                {
                  "quizTitle": "Security basics",
                  "passingScore": 80.00,
                  "maxAttemptCount": 3,
                  "questions": []
                }
                """);
    }

    @Test
    void invalidCorrectAnswerReturnsBadRequest() throws Exception {
        assertValidationError(
                validJson().replace(
                        "\"correctAnswer\": \"X\"",
                        "\"correctAnswer\": \"A\""));
    }

    @Test
    void questionScoreOverOneHundredReturnsBadRequest()
            throws Exception {
        assertValidationError(
                validJson().replace(
                        "\"score\": 50.00",
                        "\"score\": 101.00"));
    }

    @Test
    void zeroMaximumAttemptCountReturnsBadRequest()
            throws Exception {
        assertValidationError(
                validJson().replace(
                        "\"maxAttemptCount\": 3",
                        "\"maxAttemptCount\": 0"));
    }

    @Test
    void serviceUnauthorizedErrorReturnsUnauthorized()
            throws Exception {
        when(quizManagementService.createCourseQuiz(
                eq(10L),
                any(OxQuizCreateRequest.class)))
                .thenThrow(
                        new AuthenticationCredentialsNotFoundException(
                                "Authentication is required"));

        mockMvc.perform(post("/api/hr/courses/10/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("UNAUTHORIZED"));
    }

    @Test
    void serviceForbiddenErrorReturnsForbidden()
            throws Exception {
        when(quizManagementService.createCourseQuiz(
                eq(10L),
                any(OxQuizCreateRequest.class)))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/hr/courses/10/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("FORBIDDEN"));
    }

    @Test
    void missingCourseReturnsNotFound() throws Exception {
        when(quizManagementService.createCourseQuiz(
                eq(999L),
                any(OxQuizCreateRequest.class)))
                .thenThrow(
                        new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Course not found"));

        mockMvc.perform(post("/api/hr/courses/999/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Course not found"));
    }

        @Test
        void hrManagerGetsCourseQuizList() throws Exception {
        when(quizManagementService.getCourseQuizzes(10L))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/hr/courses/10/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quizId").value(200))
                .andExpect(jsonPath("$[0].courseId").value(10))
                .andExpect(jsonPath("$[0].quizTitle").exists())
                .andExpect(jsonPath("$[0].questions").isArray());

        verify(quizManagementService).getCourseQuizzes(10L);
        }

        @Test
        void hrManagerGetsCourseQuizDetail() throws Exception {
        when(quizManagementService.getCourseQuiz(10L, 200L))
                .thenReturn(response());

        mockMvc.perform(get("/api/hr/courses/10/quizzes/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(200))
                .andExpect(jsonPath("$.courseId").value(10))
                .andExpect(jsonPath("$.quizTitle").exists())
                .andExpect(jsonPath("$.passingScore").exists())
                .andExpect(jsonPath("$.questions").isArray());

        verify(quizManagementService).getCourseQuiz(10L, 200L);
        }

        @Test
        void invalidQuizIdOnDetailReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/hr/courses/10/quizzes/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Quiz ID must be a positive number"));

        verifyNoInteractions(quizManagementService);
        }

        @Test
        void hrManagerChangesCourseQuizActivation() throws Exception {
        when(quizManagementService.changeCourseQuizActivation(
                10L,
                200L,
                new QuizActivationUpdateRequest(false)))
                .thenReturn(response());

        mockMvc.perform(patch(
                        "/api/hr/courses/10/quizzes/200/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(200));

        verify(quizManagementService).changeCourseQuizActivation(
                10L,
                200L,
                new QuizActivationUpdateRequest(false));
        }

        @Test
        void missingActivationValueReturnsBadRequest() throws Exception {
        mockMvc.perform(patch(
                        "/api/hr/courses/10/quizzes/200/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verifyNoInteractions(quizManagementService);
        }

    private void assertValidationError(String json)
            throws Exception {
        mockMvc.perform(post("/api/hr/courses/10/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"));

        verify(quizManagementService, never())
                .createCourseQuiz(any(), any());
    }

    private String validJson() {
        return """
                {
                  "quizTitle": "Security basics",
                  "passingScore": 80.00,
                  "maxAttemptCount": 3,
                  "questions": [
                    {
                      "questionContent": "Passwords may be shared with coworkers.",
                      "score": 50.00,
                      "correctAnswer": "X"
                    },
                    {
                      "questionContent": "Suspicious emails must be reported.",
                      "score": 50.00,
                      "correctAnswer": "O"
                    }
                  ]
                }
                """;
    }

    private HrOxQuizResponse response() {
        return new HrOxQuizResponse(
                200L,
                10L,
                "Security basics",
                new BigDecimal("80.00"),
                3,
                true,
                7L,
                List.of(
                        new HrOxQuizResponse.Question(
                                300L,
                                "Passwords may be shared with coworkers.",
                                1,
                                new BigDecimal("50.00"),
                                "X"),
                        new HrOxQuizResponse.Question(
                                301L,
                                "Suspicious emails must be reported.",
                                2,
                                new BigDecimal("50.00"),
                                "O")));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            HrQuizController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        HrQuizManagementService quizManagementService() {
            return mock(HrQuizManagementService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder()
                    .findAndAddModules()
                    .build();
        }
    }
}