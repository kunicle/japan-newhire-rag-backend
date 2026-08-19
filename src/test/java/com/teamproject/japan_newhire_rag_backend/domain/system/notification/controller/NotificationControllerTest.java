package com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto.NotificationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.error.NotificationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.service.NotificationService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(NotificationControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token", "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax", "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d", "auth.cors.allowed-origins=http://localhost:5173"
})
class NotificationControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired NotificationService service;
    @Autowired AccessTokenService tokens;
    @Autowired InternalJwtAuthenticationQueryService users;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service, tokens, users);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void authenticatedUserCanListOwnNotificationsWithDefaults() throws Exception {
        authenticate();
        when(service.findMine(isNull(), anyInt(), anyInt()))
                .thenReturn(new NotificationPageResponse(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void anonymousCannotListOrMarkRead() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/1/read")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanMarkOwnNotificationRead() throws Exception {
        authenticate();
        mockMvc.perform(patch("/api/notifications/1/read")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void otherUsersNotificationIsReportedAsNotFound() throws Exception {
        authenticate();
        when(service.markMineAsRead(99L))
                .thenThrow(new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        mockMvc.perform(patch("/api/notifications/99/read")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    private void authenticate() {
        when(tokens.validateAndExtractAppUserId("token")).thenReturn(1L);
        when(users.load(1L)).thenReturn(new JwtAuthenticationUser(1L, Set.of(RoleType.EMPLOYEE)));
    }

    @Configuration
    @EnableWebMvc
    @Import({NotificationController.class, GlobalExceptionHandler.class, SecurityConfig.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
    static class TestConfiguration {
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean NotificationService notificationService() { return mock(NotificationService.class); }
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
