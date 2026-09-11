package com.teamproject.japan_newhire_rag_backend.domain.system.error.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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
import com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto.SystemErrorPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto.SystemErrorResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.service.SystemErrorQueryService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(AdminSystemErrorControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {"auth.cookie.name=refresh_token", "auth.cookie.secure=false", "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth", "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"})
class AdminSystemErrorControllerTest {
    @Autowired WebApplicationContext context; @Autowired SystemErrorQueryService service; @Autowired AccessTokenService tokens; @Autowired InternalJwtAuthenticationQueryService users;
    private MockMvc mockMvc;
    @BeforeEach void setUp() { reset(service, tokens, users); mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void systemAdminGetsDefaultPageAndResponseFields() throws Exception {
        authenticate(RoleType.SYSTEM_ADMIN);
        var entry = new SystemErrorResponse(1L, null, 2L, "LLM_API", "HTTP_5XX", "503", "OPEN", 2, "HttpServerErrorException", LocalDateTime.of(2026, 9, 11, 9, 0), null);
        when(service.findAll(anyInt(), anyInt())).thenReturn(new SystemErrorPageResponse(List.of(entry), 0, 20, 1, 1));
        mockMvc.perform(get("/api/admin/system-errors").header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].systemErrorLogId").value(1))
                .andExpect(jsonPath("$.content[0].errorSource").value("LLM_API"))
                .andExpect(jsonPath("$.content[0].retryCount").value(2))
                .andExpect(jsonPath("$.content[0].externalApiCallLogId").value(2));
    }

    @Test
    void anonymousIsUnauthorizedAndOtherApplicationRolesAreForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/system-errors")).andExpect(status().isUnauthorized());
        for (RoleType role : Set.of(RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.HR_MANAGER)) {
            authenticate(role);
            mockMvc.perform(get("/api/admin/system-errors").header("Authorization", "Bearer token")).andExpect(status().isForbidden());
        }
    }

    private void authenticate(RoleType role) { when(tokens.validateAndExtractAppUserId("token")).thenReturn(1L); when(users.load(1L)).thenReturn(new JwtAuthenticationUser(1L, Set.of(role))); }
    @Configuration @EnableWebMvc @Import({AdminSystemErrorController.class, GlobalExceptionHandler.class, SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean SystemErrorQueryService systemErrorQueryService() { return mock(SystemErrorQueryService.class); }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() { return mock(InternalJwtAuthenticationQueryService.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenService tokens, InternalJwtAuthenticationQueryService users) { return new JwtAuthenticationFilter(tokens, users); }
    }
}
