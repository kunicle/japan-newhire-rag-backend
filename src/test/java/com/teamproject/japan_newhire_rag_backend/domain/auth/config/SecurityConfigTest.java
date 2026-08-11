package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
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

        @GetMapping("/health")
        String health() {
            return "health";
        }

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }

        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        @GetMapping("/test/admin")
        String admin() {
            return "admin";
        }
    }
}
