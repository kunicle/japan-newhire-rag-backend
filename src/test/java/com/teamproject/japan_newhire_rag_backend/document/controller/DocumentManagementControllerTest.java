package com.teamproject.japan_newhire_rag_backend.document.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.AccessScope;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule.ConditionOperator;
import com.teamproject.japan_newhire_rag_backend.document.access.controller.dto.DocumentAccessRuleReadResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementDetailResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementListItemResponse;
import com.teamproject.japan_newhire_rag_backend.document.controller.dto.DocumentManagementVersionResponse;
import com.teamproject.japan_newhire_rag_backend.document.service.DocumentManagementQueryService;
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

@SpringJUnitConfig(DocumentManagementControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentManagementControllerTest {

    private static final String ACCESS_TOKEN = "document-management-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentManagementQueryService queryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(queryService, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity()).build();
    }

    @Test
    void hrManagerCanReadDocumentListWithoutInternalFields() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getDocuments()).thenReturn(List.of(listItem()));

        mockMvc.perform(authorizedGet("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentName").value("Policy"))
                .andExpect(jsonPath("$[0].latestVersionName").value("v2"))
                .andExpect(jsonPath("$..createdBy").doesNotExist())
                .andExpect(jsonPath("$..storedFilePath").doesNotExist())
                .andExpect(jsonPath("$..publishedBy").doesNotExist());
    }

    @Test
    void systemAdminCanReadDocumentList() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        when(queryService.getDocuments()).thenReturn(List.of());
        mockMvc.perform(authorizedGet("/api/documents")).andExpect(status().isOk());
    }

    @Test
    void employeeAndManagerCannotReadDocumentList() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        mockMvc.perform(authorizedGet("/api/documents")).andExpect(status().isForbidden());
        authenticateAs(RoleType.MANAGER);
        mockMvc.perform(authorizedGet("/api/documents")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousDocumentListRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
    }

    @Test
    void hrManagerCanReadDetailWithNullAllAndRestrictedRules() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getDocument(1L)).thenReturn(detail());

        mockMvc.perform(authorizedGet("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[0].publicationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.versions[0].accessRule").doesNotExist())
                .andExpect(jsonPath("$.versions[1].publicationStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.versions[1].isActive").value(true))
                .andExpect(jsonPath("$.versions[1].accessRule.accessScope").value("ALL"))
                .andExpect(jsonPath("$.versions[2].accessRule.accessScope").value("RESTRICTED"))
                .andExpect(jsonPath("$.versions[2].accessRule.roles[0]").value("HR_MANAGER"))
                .andExpect(jsonPath("$..documentAccessRuleId").doesNotExist())
                .andExpect(jsonPath("$..roleIds").doesNotExist())
                .andExpect(jsonPath("$..createdBy").doesNotExist())
                .andExpect(jsonPath("$..storedFilePath").doesNotExist())
                .andExpect(jsonPath("$..publishedBy").doesNotExist());
    }

    @Test
    void systemAdminCanReadDocumentDetail() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        when(queryService.getDocument(1L)).thenReturn(detail());
        mockMvc.perform(authorizedGet("/api/documents/1")).andExpect(status().isOk());
    }

    @Test
    void employeeCannotReadDocumentDetail() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);
        mockMvc.perform(authorizedGet("/api/documents/1")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousDocumentDetailRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/documents/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingDocumentReturnsNotFound() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getDocument(99L)).thenThrow(new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND, "문서를 찾을 수 없습니다."));
        mockMvc.perform(authorizedGet("/api/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("문서를 찾을 수 없습니다."));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorizedGet(
            String path) {
        return get(path).header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    private DocumentManagementListItemResponse listItem() {
        return new DocumentManagementListItemResponse(
                1L, "Policy", 10L, "POLICY", "Policy", "ACTIVE",
                2L, "v2", "PUBLIC", true, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private DocumentManagementDetailResponse detail() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        DocumentAccessRuleReadResponse all = new DocumentAccessRuleReadResponse(
                AccessScope.ALL, ConditionOperator.OR, List.of(), List.of(), null, false);
        DocumentAccessRuleReadResponse restricted = new DocumentAccessRuleReadResponse(
                AccessScope.RESTRICTED, ConditionOperator.AND,
                List.of(RoleType.HR_MANAGER), List.of(9L), 7L, true);
        return new DocumentManagementDetailResponse(
                1L, "Policy", "Description", 10L, "POLICY", "Policy", "ACTIVE", now,
                List.of(
                        version(3L, "v3", "DRAFT", false, null, now),
                        version(2L, "v2", "PUBLIC", true, all, now),
                        version(1L, "v1", "DRAFT", false, restricted, now)));
    }

    private DocumentManagementVersionResponse version(
            Long id,
            String name,
            String status,
            boolean active,
            DocumentAccessRuleReadResponse rule,
            LocalDateTime createdAt) {
        return new DocumentManagementVersionResponse(
                id, name, status, active, "policy.txt",
                LocalDate.of(2026, 1, 1), null, active ? createdAt : null, createdAt, rule);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentManagementController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentManagementQueryService queryService() {
            return mock(DocumentManagementQueryService.class);
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
