package com.teamproject.japan_newhire_rag_backend.rag.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
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
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(ragQueryExecutionService, accessTokenService, authenticationQueryService);
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
