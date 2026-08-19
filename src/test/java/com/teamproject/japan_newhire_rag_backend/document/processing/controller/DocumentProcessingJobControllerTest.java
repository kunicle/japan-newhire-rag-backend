package com.teamproject.japan_newhire_rag_backend.document.processing.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobQueryService;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentProcessingJobStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(DocumentProcessingJobControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentProcessingJobControllerTest {

    private static final String ACCESS_TOKEN = "document-processing-job-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentProcessingJobQueryService queryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(queryService, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void hrManagerCanListProcessingJobs() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getProcessingJobs()).thenReturn(List.of(
                new DocumentProcessingJobStatus(
                        301L,
                        201L,
                        "FAILED",
                        "Embedding processing failed",
                        LocalDateTime.of(2026, 8, 19, 10, 0)),
                new DocumentProcessingJobStatus(
                        302L,
                        205L,
                        "COMPLETED",
                        null,
                        LocalDateTime.of(2026, 8, 19, 9, 0))));

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentProcessingJobId").value(301))
                .andExpect(jsonPath("$[0].documentVersionId").value(201))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].failureReason")
                        .value("Embedding processing failed"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-08-19T10:00:00"))
                .andExpect(jsonPath("$[1].documentProcessingJobId").value(302))
                .andExpect(jsonPath("$[1].documentVersionId").value(205))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].failureReason").value(nullValue()))
                .andExpect(jsonPath("$[1].createdAt").value("2026-08-19T09:00:00"));

        verify(queryService).getProcessingJobs();
    }

    @Test
    void returnsEmptyListWhenNoJobs() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getProcessingJobs()).thenReturn(List.of());

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(queryService).getProcessingJobs();
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(request())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(queryService);
    }

    @Test
    void nonHrManagerRoleIsForbidden() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(queryService);
    }

    private MockHttpServletRequestBuilder authenticatedRequest() {
        return request().header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder request() {
        return get("/api/hr/document-processing-jobs");
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(77L);
        when(authenticationQueryService.load(77L))
                .thenReturn(new JwtAuthenticationUser(77L, Set.of(role)));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentProcessingJobController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentProcessingJobQueryService queryService() {
            return mock(DocumentProcessingJobQueryService.class);
        }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService tokens,
                InternalJwtAuthenticationQueryService users) {
            return new JwtAuthenticationFilter(tokens, users);
        }
    }
}
