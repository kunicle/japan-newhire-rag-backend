package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.DirectManagerResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.DirectManagerCommandService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(HrEmployeeManagerControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token", "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"
})
class HrEmployeeManagerControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired DirectManagerCommandService service;
    @Autowired com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.EmployeeOrganizationCommandService organizationService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service, organizationService, accessTokenService, authenticationQueryService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void hrManagerCanChangeDirectManager() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(service.changeDirectManager(org.mockito.ArgumentMatchers.eq(10L), any()))
                .thenReturn(new DirectManagerResponse(10L, 20L));
        request("{\"managerEmployeeId\":20}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(10))
                .andExpect(jsonPath("$.managerEmployeeId").value(20));
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/hr/employees/10/manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerEmployeeId\":20}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everyNonHrRoleIsForbidden() throws Exception {
        for (RoleType role : Set.of(
                RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.SYSTEM_ADMIN)) {
            authenticateAs(role);
            request("{\"managerEmployeeId\":20}").andExpect(status().isForbidden());
            reset(accessTokenService, authenticationQueryService);
        }
    }

    @Test
    void invalidRequestIsBadRequest() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        request("{\"managerEmployeeId\":null}").andExpect(status().isBadRequest());
        request("{\"managerEmployeeId\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void hrManagerCanEditOrganizationAndRemoveManager() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        mockMvc.perform(patch("/api/hr/employees/10/organization")
                .header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"departmentId\":1,\"jobGradeId\":2,\"managerEmployeeId\":null}"))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(organizationService).changeOrganization(10L,
                new com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeEmployeeOrganizationRequest(1L, 2L, null));
    }

    @Test
    void organizationAndDepartmentWritesRequireHrRole() throws Exception {
        for (RoleType role : Set.of(RoleType.EMPLOYEE, RoleType.MANAGER, RoleType.SYSTEM_ADMIN)) {
            authenticateAs(role);
            mockMvc.perform(patch("/api/hr/employees/10/organization").header("Authorization", "Bearer token")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"departmentId\":1,\"jobGradeId\":2}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/hr/departments")
                    .header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"departmentCode\":\"DEV\",\"departmentName\":\"Development\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/hr/departments/1").header("Authorization", "Bearer token")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"departmentName\":\"Development\"}"))
                    .andExpect(status().isForbidden());
        }
        org.mockito.Mockito.verifyNoInteractions(organizationService);
    }

    @Test
    void anonymousOrganizationWriteIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/hr/employees/10/organization").contentType(MediaType.APPLICATION_JSON)
                .content("{\"departmentId\":1,\"jobGradeId\":2}")).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/hr/departments")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/hr/departments/1").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidOrganizationBodyIsBadRequest() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        mockMvc.perform(patch("/api/hr/employees/10/organization").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"departmentId\":0,\"jobGradeId\":2}"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions request(String body) throws Exception {
        return mockMvc.perform(patch("/api/hr/employees/10/manager")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId("token")).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(role)));
    }

    @Configuration
    @EnableWebMvc
    @Import({HrOrganizationController.class, HrEmployeeManagerController.class, GlobalExceptionHandler.class,
            SecurityConfig.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.EmployeeOrganizationCommandService organizationService() {
            return mock(com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.EmployeeOrganizationCommandService.class);
        }
        @Bean com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.DepartmentCommandService departmentService() {
            return mock(com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.DepartmentCommandService.class);
        }
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DirectManagerCommandService directManagerCommandService() {
            return mock(DirectManagerCommandService.class);
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
