package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(SecurityConfigTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(accessTokenService, authenticationQueryService);
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
    void publicLoginEndpointAllowsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("login"));
    }

    @Test
    void healthEndpointAllowsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("health"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void validJwtWithAllowedRolePassesProtectedEndpoint() throws Exception {
        stubJwt("valid-token", Set.of(RoleType.EMPLOYEE));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    @Test
    void validJwtWithoutRoleIsForbidden() throws Exception {
        stubJwt("roleless-token", Set.of());

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer roleless-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void authenticatedEmployeeWithoutAdminRoleIsForbiddenByMethodSecurity() throws Exception {
        stubJwt("employee-token", Set.of(RoleType.EMPLOYEE));

        mockMvc.perform(get("/test/admin")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void expiredJwtOnProtectedEndpointReturnsUnauthorized() throws Exception {
        when(accessTokenService.validateAndExtractAppUserId("expired-token"))
                .thenThrow(new BadJwtException("JWT expired"));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedJwtOnProtectedEndpointReturnsUnauthorized() throws Exception {
        when(accessTokenService.validateAndExtractAppUserId("tampered-token"))
                .thenThrow(new BadJwtException("Invalid signature"));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer tampered-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestDoesNotCreateHttpSession() throws Exception {
        stubJwt("valid-token", Set.of(RoleType.EMPLOYEE));

        MvcResult result = mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void allowedCorsPreflightSupportsCredentialsAndRequiredHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/refresh")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void unapprovedCorsOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/auth/refresh")
                        .header("Origin", "https://unapproved.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void refreshWithCsrfTokenReachesPublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("refresh"));
    }

    @Test
    void bearerOnlyBusinessPostDoesNotRequireCsrfToken() throws Exception {
        stubJwt("valid-token", Set.of(RoleType.EMPLOYEE));

        mockMvc.perform(post("/test/protected")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected-post"));
    }

    @Test
    void employeeIsForbiddenByDocumentManagementGetMatchers() throws Exception {
        stubJwt("employee-token", Set.of(RoleType.EMPLOYEE));

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/documents/1")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrManagerPassesDocumentManagementGetMatchers() throws Exception {
        stubJwt("hr-token", Set.of(RoleType.HR_MANAGER));

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer hr-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/documents/1")
                        .header("Authorization", "Bearer hr-token"))
                .andExpect(status().isOk());
    }

    private void stubJwt(String token, Set<RoleType> roles) {
        when(accessTokenService.validateAndExtractAppUserId(token)).thenReturn(1L);
        when(authenticationQueryService.load(1L))
                .thenReturn(new JwtAuthenticationUser(1L, roles));
    }

    @Configuration
    @EnableWebMvc
    @Import({
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

        @Bean
        TestEndpointController testEndpointController() {
            return new TestEndpointController();
        }
    }

    @RestController
    static class TestEndpointController {

        @PostMapping("/api/auth/login")
        String login() {
            return "login";
        }

        @PostMapping("/api/auth/refresh")
        String refresh() {
            return "refresh";
        }

        @GetMapping("/health")
        String health() {
            return "health";
        }

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @GetMapping({"/api/documents", "/api/documents/{documentId}"})
        String documentManagement() {
            return "documents";
        }

        @PostMapping("/test/protected")
        String protectedPost() {
            return "protected-post";
        }

        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        @GetMapping("/test/admin")
        String admin() {
            return "admin";
        }
    }
}
