package com.teamproject.japan_newhire_rag_backend.rag.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryDetail;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryItem;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQuestionHistoryService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryExecutionService;
import com.teamproject.japan_newhire_rag_backend.rag.application.RagQueryResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(RagQueryControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class RagQueryControllerTest {

    private static final String ACCESS_TOKEN = "rag-query-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired RagQueryExecutionService ragQueryExecutionService;
    @Autowired CurrentUserProvider currentUserProvider;
    @Autowired RagQuestionHistoryService ragQuestionHistoryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                ragQueryExecutionService,
                currentUserProvider,
                ragQuestionHistoryService,
                accessTokenService,
                authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void employeeCanExecuteRagQuery() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        when(ragQueryExecutionService.execute("질문"))
                .thenReturn(new RagQueryResult(
                        true,
                        "답변",
                        List.of(101L, 102L),
                        List.of(
                                new RagCitationSnapshot(
                                        101L, "취업규칙", "v1", "제5조", "첫 번째 근거 문장"),
                                new RagCitationSnapshot(
                                        102L, "휴가규정", "v1", null, "두 번째 근거 문장"))));

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"질문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSufficientEvidence").value(true))
                .andExpect(jsonPath("$.answer").value("답변"))
                .andExpect(jsonPath("$.validCitedChunkIds").isArray())
                .andExpect(jsonPath("$.validCitedChunkIds.length()").value(2))
                .andExpect(jsonPath("$.validCitedChunkIds[0]").value(101))
                .andExpect(jsonPath("$.validCitedChunkIds[1]").value(102))
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations.length()").value(2))
                .andExpect(jsonPath("$.citations[0].documentChunkId").value(101))
                .andExpect(jsonPath("$.citations[0].documentName").value("취업규칙"))
                .andExpect(jsonPath("$.citations[0].versionName").value("v1"))
                .andExpect(jsonPath("$.citations[0].articleNumber").value("제5조"))
                .andExpect(jsonPath("$.citations[0].citedText").value("첫 번째 근거 문장"))
                .andExpect(jsonPath("$.citations[1].articleNumber").value(nullValue()));

        verify(ragQueryExecutionService).execute("질문");
    }

    @Test
    void hrManagerCanExecuteRagQuery() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(ragQueryExecutionService.execute("질문"))
                .thenReturn(new RagQueryResult(true, "답변", List.of(), List.of()));

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"질문\"}"))
                .andExpect(status().isOk());

        verify(ragQueryExecutionService).execute("질문");
    }

    @Test
    void insufficientEvidenceIsReturnedAsSuccessfulBusinessResult() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        when(ragQueryExecutionService.execute("질문"))
                .thenReturn(new RagQueryResult(false, null, List.of(), List.of()));

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"질문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSufficientEvidence").value(false))
                .andExpect(jsonPath("$.answer").value(nullValue()))
                .andExpect(jsonPath("$.validCitedChunkIds").isArray())
                .andExpect(jsonPath("$.validCitedChunkIds").isEmpty())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test
    void blankQuestionReturnsValidationError() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(ragQueryExecutionService);
    }

    @Test
    void missingQuestionReturnsValidationError() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedQuestionRequest("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(ragQueryExecutionService);
    }

    @Test
    void questionOfLengthOneReturnsValidationError() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"가\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(ragQueryExecutionService);
    }

    @Test
    void questionOfMaxLengthIsAccepted() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        String question = "가".repeat(500);
        when(ragQueryExecutionService.execute(question))
                .thenReturn(new RagQueryResult(true, "답변", List.of(), List.of()));

        mockMvc.perform(authenticatedQuestionRequest(
                        "{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk());

        verify(ragQueryExecutionService).execute(question);
    }

    @Test
    void questionExceedingMaxLengthReturnsValidationError() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        String question = "가".repeat(501);

        mockMvc.perform(authenticatedQuestionRequest(
                        "{\"question\":\"" + question + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(ragQueryExecutionService);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(questionRequest("{\"question\":\"질문\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ragQueryExecutionService);
    }

    @Test
    void serviceFailureIsReturnedAsServerError() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        when(ragQueryExecutionService.execute("질문"))
                .thenThrow(new RuntimeException("test"));

        mockMvc.perform(authenticatedQuestionRequest("{\"question\":\"질문\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsAuthenticatedUsersQuestionHistory() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        CurrentUserContext currentUser = currentUser();
        LocalDateTime askedAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(ragQuestionHistoryService.getQuestionHistory(currentUser))
                .thenReturn(List.of(new RagQuestionHistoryItem(
                        10L, "연차는 몇 일인가요?", "ANSWERED", askedAt)));

        mockMvc.perform(get("/api/rag/questions/me")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(10))
                .andExpect(jsonPath("$[0].question").value("연차는 몇 일인가요?"))
                .andExpect(jsonPath("$[0].status").value("ANSWERED"))
                .andExpect(jsonPath("$[0].askedAt").value("2026-08-19T10:00:00"));

        verify(currentUserProvider).getCurrentUser();
        verify(ragQuestionHistoryService).getQuestionHistory(currentUser);
    }

    @Test
    void returnsEmptyQuestionHistory() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        CurrentUserContext currentUser = currentUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(ragQuestionHistoryService.getQuestionHistory(currentUser)).thenReturn(List.of());

        mockMvc.perform(get("/api/rag/questions/me")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(ragQuestionHistoryService).getQuestionHistory(currentUser);
    }

    @Test
    void returnsOwnedQuestionDetailWithCitationSnapshots() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        CurrentUserContext currentUser = currentUser();
        LocalDateTime askedAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(ragQuestionHistoryService.getQuestionDetail(currentUser, 10L))
                .thenReturn(new RagQuestionHistoryDetail(
                        10L,
                        "연차는 몇 일인가요?",
                        "ANSWERED",
                        askedAt,
                        "연차는 연 15일입니다.",
                        List.of(
                                new RagCitationSnapshot(
                                        101L, "취업규칙", "v1", "제5조", "첫 번째 근거"),
                                new RagCitationSnapshot(
                                        102L, "휴가규정", "v2", null, "두 번째 근거"))));

        mockMvc.perform(get("/api/rag/questions/10")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(10))
                .andExpect(jsonPath("$.question").value("연차는 몇 일인가요?"))
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.askedAt").value("2026-08-19T10:00:00"))
                .andExpect(jsonPath("$.answer").value("연차는 연 15일입니다."))
                .andExpect(jsonPath("$.citations[0].documentChunkId").value(101))
                .andExpect(jsonPath("$.citations[0].documentName").value("취업규칙"))
                .andExpect(jsonPath("$.citations[0].versionName").value("v1"))
                .andExpect(jsonPath("$.citations[0].articleNumber").value("제5조"))
                .andExpect(jsonPath("$.citations[0].citedText").value("첫 번째 근거"))
                .andExpect(jsonPath("$.citations[1].articleNumber").value(nullValue()));

        verify(ragQuestionHistoryService).getQuestionDetail(currentUser, 10L);
    }

    @Test
    void returnsNotFoundForUnavailableQuestionDetail() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        CurrentUserContext currentUser = currentUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(ragQuestionHistoryService.getQuestionDetail(currentUser, 99L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "질문을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/rag/questions/99")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unauthenticatedHistoryRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/rag/questions/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(currentUserProvider, ragQuestionHistoryService);
    }

    private MockHttpServletRequestBuilder authenticatedQuestionRequest(String content) {
        return questionRequest(content).header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder questionRequest(String content) {
        return post("/api/rag/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    private CurrentUserContext currentUser() {
        return new CurrentUserContext(
                1001L, 2001L, Set.of(RoleType.EMPLOYEE), 10L, 1, null);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            RagQueryController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean RagQueryExecutionService ragQueryExecutionService() {
            return mock(RagQueryExecutionService.class);
        }
        @Bean CurrentUserProvider currentUserProvider() {
            return mock(CurrentUserProvider.class);
        }
        @Bean RagQuestionHistoryService ragQuestionHistoryService() {
            return mock(RagQuestionHistoryService.class);
        }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService accessTokenService,
                InternalJwtAuthenticationQueryService authenticationQueryService) {
            return new JwtAuthenticationFilter(accessTokenService, authenticationQueryService);
        }
    }
}
