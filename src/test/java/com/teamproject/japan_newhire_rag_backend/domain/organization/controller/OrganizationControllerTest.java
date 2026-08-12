package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationDepartmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.OrganizationTreeQueryService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(OrganizationControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class OrganizationControllerTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired OrganizationTreeQueryService organizationTreeQueryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(organizationTreeQueryService, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void authenticatedEmployeeCanReadOrganizationWithoutSensitiveFields() throws Exception {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(RoleType.EMPLOYEE)));
        when(organizationTreeQueryService.getOrganizationTree()).thenReturn(response());

        mockMvc.perform(get("/api/organization")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments[0].departmentCode").value("DEV"))
                .andExpect(jsonPath("$.departments[0].employees[0].employeeNumber").value("E-001"))
                .andExpect(jsonPath("$.departments[0].employees[0].jobGradeName").value("Junior"))
                .andExpect(jsonPath("$..passwordHash").doesNotExist())
                .andExpect(jsonPath("$..failedLoginCount").doesNotExist())
                .andExpect(jsonPath("$..lockedUntil").doesNotExist())
                .andExpect(jsonPath("$..refreshToken").doesNotExist())
                .andExpect(jsonPath("$..deletedAt").doesNotExist());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/organization"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private OrganizationResponse response() {
        OrganizationEmployeeResponse employee = new OrganizationEmployeeResponse(
                10L, "E-001", "Kim", 100L, 200L, "Junior", 1,
                LocalDate.of(2026, 1, 2));
        OrganizationDepartmentResponse department = new OrganizationDepartmentResponse(
                100L, "DEV", "Development", null, 1,
                List.of(employee), List.of());
        return new OrganizationResponse(List.of(department));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            OrganizationController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean OrganizationTreeQueryService organizationTreeQueryService() {
            return mock(OrganizationTreeQueryService.class);
        }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService accessTokenService,
                InternalJwtAuthenticationQueryService authenticationQueryService
        ) {
            return new JwtAuthenticationFilter(accessTokenService, authenticationQueryService);
        }
    }
}
