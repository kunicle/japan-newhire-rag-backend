package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.MyProfileResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.MyProfileQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(MyProfileControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class MyProfileControllerTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired CurrentUserProvider currentUserProvider;
    @Autowired MyProfileQueryService myProfileQueryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(currentUserProvider, myProfileQueryService, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void returnsCurrentEmployeesProfileWithoutSensitiveFields() throws Exception {
        CurrentUserContext context = context(RoleType.EMPLOYEE);
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(RoleType.EMPLOYEE)));
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(myProfileQueryService.getMyProfile(context)).thenReturn(response(Set.of(RoleType.EMPLOYEE)));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appUserId").value(1))
                .andExpect(jsonPath("$.employeeNumber").value("E-001"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.jobGradeLevel").value(1))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
                .andExpect(jsonPath("$.managerName").value("Manager Lee"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.failedLoginCount").doesNotExist())
                .andExpect(jsonPath("$.lockedUntil").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(currentUserProvider).getCurrentUser();
        verify(myProfileQueryService).getMyProfile(context);
    }

    @Test
    void allowsManagerToReadOwnProfile() throws Exception {
        CurrentUserContext context = context(RoleType.MANAGER);
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(RoleType.MANAGER)));
        when(currentUserProvider.getCurrentUser()).thenReturn(context);
        when(myProfileQueryService.getMyProfile(context)).thenReturn(response(Set.of(RoleType.MANAGER)));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"));
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private CurrentUserContext context(RoleType role) {
        return new CurrentUserContext(1L, 10L, Set.of(role), 100L, 1, EmployeeType.GENERAL);
    }

    private MyProfileResponse response(Set<RoleType> roles) {
        return new MyProfileResponse(
                1L, 10L, "E-001", "Kim", "user@example.com",
                100L, "Engineering", 200L, "Junior", 1, roles,
                LocalDate.of(2026, 1, 2), 20L, "Manager Lee");
    }

    @Configuration
    @EnableWebMvc
    @Import({
            MyProfileController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean CurrentUserProvider currentUserProvider() { return mock(CurrentUserProvider.class); }
        @Bean MyProfileQueryService myProfileQueryService() { return mock(MyProfileQueryService.class); }
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
