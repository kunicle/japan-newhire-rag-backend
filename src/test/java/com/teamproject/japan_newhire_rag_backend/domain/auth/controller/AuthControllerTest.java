package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import jakarta.servlet.http.Cookie;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenOrchestrationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenPair;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenLogoutService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenRotationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(AuthControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class AuthControllerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private LoginTokenOrchestrationService loginService;

    @Autowired
    private RefreshTokenRotationService refreshService;

    @Autowired
    private RefreshTokenLogoutService logoutService;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                loginService,
                refreshService,
                logoutService,
                currentUserProvider,
                accessTokenService,
                authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        when(loginService.login("user@example.com", "password", "browser"))
                .thenReturn(new LoginTokenPair(ACCESS_TOKEN, REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password","deviceInfo":"browser"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("refresh_token=" + REFRESH_TOKEN),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("Path=/api/auth"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("XSRF-TOKEN="))));
    }

    @Test
    void loginRejectsBlankEmail() throws Exception {
        assertValidationError(
                "/api/auth/login",
                "{\"email\":\"\",\"password\":\"password\"}",
                "email: must not be blank");
        verify(loginService, never()).login(anyString(), anyString(), org.mockito.ArgumentMatchers.nullable(String.class));
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        assertValidationError(
                "/api/auth/login",
                "{\"email\":\"user@example.com\",\"password\":\"\"}",
                "password: must not be blank");
    }

    @Test
    void badCredentialsReturnCommonUnauthorizedResponse() throws Exception {
        when(loginService.login("user@example.com", "wrong", null))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        assertLoginUnauthorized("wrong");
    }

    @Test
    void disabledAccountReturnsCommonUnauthorizedResponse() throws Exception {
        when(loginService.login("user@example.com", "password", null))
                .thenThrow(new DisabledException("Account is inactive"));

        assertLoginUnauthorized("password");
    }

    @Test
    void lockedAccountReturnsCommonUnauthorizedResponse() throws Exception {
        when(loginService.login("user@example.com", "password", null))
                .thenThrow(new LockedException("Account is locked"));

        assertLoginUnauthorized("password");
    }

    @Test
    void refreshReturnsRotatedTokenPair() throws Exception {
        when(refreshService.rotate(REFRESH_TOKEN))
                .thenReturn(new LoginTokenPair(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(refreshCookie(REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(NEW_ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                                "refresh_token=" + NEW_REFRESH_TOKEN)));
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidRefreshTokenReturnsUnauthorized() throws Exception {
        assertRefreshUnauthorized("invalid-token");
    }

    @Test
    void revokedRefreshTokenReturnsUnauthorized() throws Exception {
        assertRefreshUnauthorized("revoked-token");
    }

    @Test
    void expiredRefreshTokenReturnsUnauthorized() throws Exception {
        assertRefreshUnauthorized("expired-token");
    }

    @Test
    void logoutWithoutAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(refreshCookie(REFRESH_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoLogoutCall();
    }

    @Test
    void authenticatedLogoutRevokesCurrentUsersRefreshToken() throws Exception {
        stubAuthenticatedUser();
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUserContext());

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .cookie(refreshCookie(REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("refresh_token="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"))));

        verify(logoutService).logout(1L, REFRESH_TOKEN);
    }

    @Test
    void authenticatedLogoutWithoutCookieReturnsUnauthorizedAndClearsCookie() throws Exception {
        stubAuthenticatedUser();

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.containsString(
                                "Max-Age=0")));

        verifyNoLogoutCall();
    }

    private void assertLoginUnauthorized(String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\""
                                + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    private void assertRefreshUnauthorized(String token) throws Exception {
        when(refreshService.rotate(token))
                .thenThrow(new BadCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(refreshCookie(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void assertValidationError(String endpoint, String body, String message)
            throws Exception {
        mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(message));
    }

    private void stubAuthenticatedUser() {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, Set.of(RoleType.EMPLOYEE)));
    }

    private CurrentUserContext currentUserContext() {
        return new CurrentUserContext(
                1L,
                10L,
                Set.of(RoleType.EMPLOYEE),
                100L,
                1,
                EmployeeType.GENERAL);
    }

    private Cookie refreshCookie(String token) {
        return new Cookie("refresh_token", token);
    }

    private void verifyNoLogoutCall() {
        verify(logoutService, never()).logout(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Configuration
    @EnableWebMvc
    @Import({
            AuthController.class,
            RefreshTokenCookieManager.class,
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
        LoginTokenOrchestrationService loginService() {
            return mock(LoginTokenOrchestrationService.class);
        }

        @Bean
        RefreshTokenRotationService refreshService() {
            return mock(RefreshTokenRotationService.class);
        }

        @Bean
        RefreshTokenLogoutService logoutService() {
            return mock(RefreshTokenLogoutService.class);
        }

        @Bean
        CurrentUserProvider currentUserProvider() {
            return mock(CurrentUserProvider.class);
        }

        @Bean
        AccessTokenService accessTokenService() {
            return mock(AccessTokenService.class);
        }

        @Bean
        InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService accessTokenService,
                InternalJwtAuthenticationQueryService authenticationQueryService
        ) {
            return new JwtAuthenticationFilter(accessTokenService, authenticationQueryService);
        }
    }
}
