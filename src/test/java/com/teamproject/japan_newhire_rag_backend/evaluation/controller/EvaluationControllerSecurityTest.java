package com.teamproject.japan_newhire_rag_backend.evaluation.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationAssignmentService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationCycleService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationItemService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationPublishService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationResultService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationSubmissionService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.EvaluationTemplateService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationProgressService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.ManagerEvaluationService;
import com.teamproject.japan_newhire_rag_backend.evaluation.service.SelfEvaluationService;
import com.teamproject.japan_newhire_rag_backend.evaluation.error.EvaluationErrorCode;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(EvaluationControllerSecurityTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token", "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"
})
class EvaluationControllerSecurityTest {

    @Autowired WebApplicationContext context;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;
    @Autowired EvaluationCycleService evaluationCycleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(accessTokenService, authenticationQueryService, evaluationCycleService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();
    }

    @Test
    void anonymousEvaluationRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me/evaluations/10/self"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestReachesHrEndpointAndServiceOwnsRoleDecision() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        request("/api/hr/evaluation-cycles/7").andExpect(status().isOk());
    }

    @Test
    void authenticatedRequestReachesManagerEndpointAndServiceOwnsRoleDecision() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        request("/api/manager/evaluations").andExpect(status().isOk());
    }

    @Test
    void serviceAccessDeniedIsReturnedAsForbidden() throws Exception {
        when(evaluationCycleService.getById(7L)).thenThrow(new BusinessException(
                EvaluationErrorCode.EVALUATION_ACCESS_DENIED));
        authenticateAs(RoleType.EMPLOYEE);

        request("/api/hr/evaluation-cycles/7").andExpect(status().isForbidden());
    }

    @Test
    void authenticatedEmployeeCanReachMyEvaluationEndpoint() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        request("/api/me/evaluations/10/self").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions request(String path)
            throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer token"));
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId("token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    @Configuration
    @EnableWebMvc
    @Import({HrEvaluationController.class, MyEvaluationController.class,
            ManagerEvaluationController.class, GlobalExceptionHandler.class,
            SecurityConfig.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService tokens, InternalJwtAuthenticationQueryService users) {
            return new JwtAuthenticationFilter(tokens, users);
        }
        @Bean EvaluationCycleService cycleService() { return mock(EvaluationCycleService.class); }
        @Bean EvaluationTemplateService templateService() {
            return mock(EvaluationTemplateService.class);
        }
        @Bean EvaluationItemService itemService() { return mock(EvaluationItemService.class); }
        @Bean EvaluationAssignmentService assignmentService() {
            return mock(EvaluationAssignmentService.class);
        }
        @Bean EvaluationProgressService evaluationProgressService() {
            return mock(EvaluationProgressService.class);
        }
        @Bean EvaluationPublishService publishService() {
            return mock(EvaluationPublishService.class);
        }
        @Bean SelfEvaluationService selfEvaluationService() {
            return mock(SelfEvaluationService.class);
        }
        @Bean EvaluationSubmissionService submissionService() {
            return mock(EvaluationSubmissionService.class);
        }
        @Bean EvaluationResultService resultService() {
            return mock(EvaluationResultService.class);
        }
        @Bean ManagerEvaluationService managerEvaluationService() {
            ManagerEvaluationService service = mock(ManagerEvaluationService.class);
            when(service.getMyAssignedEvaluations()).thenReturn(List.of());
            return service;
        }
        @Bean ManagerEvaluationProgressService managerProgressService() {
            return mock(ManagerEvaluationProgressService.class);
        }
    }
}
