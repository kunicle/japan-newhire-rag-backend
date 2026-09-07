package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
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

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.ManagerEducationQueryService;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(
        ManagerEducationControllerSecurityTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class ManagerEducationControllerSecurityTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private InternalJwtAuthenticationQueryService
            authenticationQueryService;

    @Autowired
    private ManagerEducationQueryService
            managerEducationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                accessTokenService,
                authenticationQueryService,
                managerEducationQueryService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymousRequestIsUnauthorized()
            throws Exception {
        mockMvc.perform(get(
                        "/api/manager/team-education"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(managerEducationQueryService);
    }

    @Test
    void regularEmployeeRequestIsForbidden()
            throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        when(managerEducationQueryService
            .getTeamEducation(0, 20))
            .thenThrow(new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Manager role is required"));

        request("/api/manager/team-education")
                .andExpect(status().isForbidden());

        verify(managerEducationQueryService)
            .getTeamEducation(0, 20);
    }

    @Test
    void managerCanReachTeamEducationEndpoint()
            throws Exception {
        authenticateAs(RoleType.MANAGER);

        when(managerEducationQueryService
                .getTeamEducation(0, 20))
                .thenReturn(emptyPage());

        request("/api/manager/team-education")
                .andExpect(status().isOk());
    }

    @Test
    void managerCanReachEmployeeCoursesEndpoint()
            throws Exception {
        authenticateAs(RoleType.MANAGER);

        when(managerEducationQueryService
                .getEmployeeCourses(10L, 0, 20))
                .thenReturn(emptyPage());

        request("/api/manager/employees/10/courses")
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions
    request(String path) throws Exception {
        return mockMvc.perform(
                get(path).header(
                        "Authorization",
                        "Bearer token"));
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService
                .validateAndExtractAppUserId("token"))
                .thenReturn(1L);

        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(
                        1L,
                        Set.of(role)));
    }

    private ManagerEducationPageResponse emptyPage() {
        return new ManagerEducationPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            ManagerEducationController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AccessTokenService accessTokenService() {
            return mock(AccessTokenService.class);
        }

        @Bean
        InternalJwtAuthenticationQueryService
        authenticationQueryService() {
            return mock(
                    InternalJwtAuthenticationQueryService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService accessTokenService,
                InternalJwtAuthenticationQueryService
                        authenticationQueryService
        ) {
            return new JwtAuthenticationFilter(
                    accessTokenService,
                    authenticationQueryService);
        }

        @Bean
        ManagerEducationQueryService
        managerEducationQueryService() {
            return mock(ManagerEducationQueryService.class);
        }
    }
}