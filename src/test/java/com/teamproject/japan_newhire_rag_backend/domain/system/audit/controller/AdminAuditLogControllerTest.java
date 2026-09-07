package com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
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
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.AuditLogQueryService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(AdminAuditLogControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token", "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"
})
class AdminAuditLogControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired AuditLogQueryService service;
    @Autowired AccessTokenService tokens;
    @Autowired InternalJwtAuthenticationQueryService users;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service, tokens, users);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void systemAdminCanQueryWithDefaultPagination() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        when(service.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyInt(), anyInt())).thenReturn(new AuditLogPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void anonymousIsUnauthorizedAndApplicationRolesAreForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")).andExpect(status().isUnauthorized());
        for (RoleType role : Set.of(RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.HR_MANAGER)) {
            authenticateAs(role);
            mockMvc.perform(get("/api/admin/audit-logs")
                            .header("Authorization", "Bearer token"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void invalidEnumAndDateAreBadRequest() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        mockMvc.perform(get("/api/admin/audit-logs?actionType=UNKNOWN")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-logs?from=not-a-date")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSizeAndReversedDateRangeAreBadRequest() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        when(service.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyInt(), org.mockito.ArgumentMatchers.eq(0)))
                .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));
        doThrow(new IllegalArgumentException("from must not be after to"))
                .when(service).findAll(isNull(), isNull(), isNull(), isNull(), any(), any(),
                        anyInt(), anyInt());

        mockMvc.perform(get("/api/admin/audit-logs?size=0")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-logs?from=2026-08-02T00:00:00&to=2026-08-01T00:00:00")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }

    private void authenticateAs(RoleType role) {
        when(tokens.validateAndExtractAppUserId("token")).thenReturn(1L);
        when(users.load(1L)).thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    @Configuration
    @EnableWebMvc
    @Import({AdminAuditLogController.class, GlobalExceptionHandler.class, SecurityConfig.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean AuditLogQueryService auditLogQueryService() { return mock(AuditLogQueryService.class); }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService tokens, InternalJwtAuthenticationQueryService users) {
            return new JwtAuthenticationFilter(tokens, users);
        }
    }
}
