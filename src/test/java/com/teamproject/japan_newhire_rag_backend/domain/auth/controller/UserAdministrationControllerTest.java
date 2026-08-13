package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.UserAdministrationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(UserAdministrationControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token", "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"
})
class UserAdministrationControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired UserAdministrationService service;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service, accessTokenService, authenticationQueryService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void systemAdminCanCreateUser() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        when(service.createUser(any())).thenReturn(new CreateUserResponse(
                1L, 2L, AccountStatus.ACTIVE, EmploymentStatus.EMPLOYED));
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appUserId").value(1));
    }

    @Test
    void nonAdminRolesAreForbidden() throws Exception {
        for (RoleType role : Set.of(RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.HR_MANAGER)) {
            authenticateAs(role);
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isUnauthorized());
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId("token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    private String requestJson() {
        return """
                {"email":"new@example.com","password":"raw-password",
                "employeeNumber":"E-100","employeeName":"New Hire",
                "departmentId":10,"jobGradeId":20,"employeeType":"NEW_HIRE",
                "hireDate":"2026-08-13"}
                """;
    }

    @Configuration
    @EnableWebMvc
    @Import({UserAdministrationController.class, GlobalExceptionHandler.class,
            SecurityConfig.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean UserAdministrationService userAdministrationService() {
            return mock(UserAdministrationService.class);
        }
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
