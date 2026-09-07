package com.teamproject.japan_newhire_rag_backend.document.access.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleCommand;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleManagementService;
import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentAccessRuleResult;
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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(DocumentAccessRuleControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentAccessRuleControllerTest {

    private static final String ACCESS_TOKEN = "access-rule-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentAccessRuleManagementService managementService;
    @Autowired CurrentUserProvider currentUserProvider;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                managementService,
                currentUserProvider,
                accessTokenService,
                authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void hrManagerCanReplaceAccessRule() throws Exception {
        arrangeSuccess(RoleType.HR_MANAGER);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isOk());
    }

    @Test
    void systemAdminCanReplaceAccessRule() throws Exception {
        arrangeSuccess(RoleType.SYSTEM_ADMIN);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotReplaceAccessRule() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(managementService);
    }

    @Test
    void managerCannotReplaceAccessRule() throws Exception {
        authenticateAs(RoleType.MANAGER);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(managementService);
    }

    @Test
    void unauthenticatedReplaceIsUnauthorized() throws Exception {
        mockMvc.perform(request(restrictedJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(managementService);
    }

    @Test
    void replaceUsesAppUserIdNotEmployeeId() throws Exception {
        arrangeSuccess(RoleType.HR_MANAGER);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isOk());

        verify(managementService).replace(eq(10L), eq(20L), any(), eq(77L));
    }

    @Test
    void restrictedRequestMapsExactlyToCommand() throws Exception {
        arrangeSuccess(RoleType.HR_MANAGER);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentAccessRuleCommand> captor =
                ArgumentCaptor.forClass(DocumentAccessRuleCommand.class);
        verify(managementService).replace(eq(10L), eq(20L), captor.capture(), eq(77L));
        assertThat(captor.getValue()).isEqualTo(new DocumentAccessRuleCommand(
                AccessScope.RESTRICTED,
                ConditionOperator.AND,
                Set.of(RoleType.EMPLOYEE, RoleType.MANAGER),
                Set.of(3L, 5L),
                7L,
                true));
    }

    @Test
    void allRequestOmittingOptionalFieldsMapsCorrectly() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(managementService.replace(eq(10L), eq(20L), any(), eq(77L)))
                .thenReturn(allResult());

        mockMvc.perform(authenticatedRequest("{\"accessScope\":\"ALL\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentAccessRuleCommand> captor =
                ArgumentCaptor.forClass(DocumentAccessRuleCommand.class);
        verify(managementService).replace(eq(10L), eq(20L), captor.capture(), eq(77L));
        assertThat(captor.getValue().roles()).isEmpty();
        assertThat(captor.getValue().departmentIds()).isEmpty();
        assertThat(captor.getValue().accessScope()).isEqualTo(AccessScope.ALL);
    }

    @Test
    void resourceNotFoundReturns404() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(managementService.replace(eq(10L), eq(20L), any(), eq(77L)))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "문서 버전을 찾을 수 없습니다."));

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void invalidBusinessRuleReturns400() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(managementService.replace(eq(10L), eq(20L), any(), eq(77L)))
                .thenThrow(new IllegalArgumentException("RESTRICTED 범위에는 접근 조건이 필요합니다."));

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void invalidEnumReturns400() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);

        mockMvc.perform(authenticatedRequest("{\"accessScope\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(managementService);
    }

    @Test
    void responseMapsResultExactly() throws Exception {
        arrangeSuccess(RoleType.HR_MANAGER);

        mockMvc.perform(authenticatedRequest(restrictedJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(10))
                .andExpect(jsonPath("$.documentVersionId").value(20))
                .andExpect(jsonPath("$.accessRuleId").value(30))
                .andExpect(jsonPath("$.accessScope").value("RESTRICTED"))
                .andExpect(jsonPath("$.conditionOperator").value("AND"))
                .andExpect(jsonPath("$.roleIds[0]").value(2))
                .andExpect(jsonPath("$.roleIds[1]").value(4))
                .andExpect(jsonPath("$.departmentIds[0]").value(3))
                .andExpect(jsonPath("$.departmentIds[1]").value(5))
                .andExpect(jsonPath("$.minimumJobGradeId").value(7))
                .andExpect(jsonPath("$.newEmployeeOnly").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value(77));
    }

    private void arrangeSuccess(RoleType role) {
        authenticateAs(role);
        stubCurrentUser(role);
        when(managementService.replace(eq(10L), eq(20L), any(), eq(77L)))
                .thenReturn(restrictedResult());
    }

    private MockHttpServletRequestBuilder authenticatedRequest(String json) {
        return request(json).header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder request(String json) {
        return put("/api/documents/10/versions/20/access-rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(77L);
        when(authenticationQueryService.load(77L))
                .thenReturn(new JwtAuthenticationUser(77L, Set.of(role)));
    }

    private void stubCurrentUser(RoleType role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        77L,
                        999L,
                        Set.of(role),
                        null,
                        null,
                        null));
    }

    private DocumentAccessRuleResult restrictedResult() {
        return new DocumentAccessRuleResult(
                10L,
                20L,
                30L,
                AccessScope.RESTRICTED,
                ConditionOperator.AND,
                List.of(2L, 4L),
                List.of(3L, 5L),
                7L,
                true,
                true,
                77L);
    }

    private DocumentAccessRuleResult allResult() {
        return new DocumentAccessRuleResult(
                10L,
                20L,
                30L,
                AccessScope.ALL,
                ConditionOperator.OR,
                List.of(),
                List.of(),
                null,
                false,
                true,
                77L);
    }

    private String restrictedJson() {
        return """
                {
                  "accessScope": "RESTRICTED",
                  "conditionOperator": "AND",
                  "roles": ["EMPLOYEE", "MANAGER"],
                  "departmentIds": [3, 5],
                  "minimumJobGradeId": 7,
                  "newEmployeeOnly": true
                }
                """;
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentAccessRuleController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentAccessRuleManagementService managementService() {
            return mock(DocumentAccessRuleManagementService.class);
        }
        @Bean CurrentUserProvider currentUserProvider() { return mock(CurrentUserProvider.class); }
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
