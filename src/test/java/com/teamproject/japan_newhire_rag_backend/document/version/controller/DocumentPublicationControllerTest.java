package com.teamproject.japan_newhire_rag_backend.document.version.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationResult;
import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentPublicationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.config.SecurityConfig;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.InternalJwtAuthenticationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.JwtAuthenticationUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(DocumentPublicationControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentPublicationControllerTest {

    private static final String ACCESS_TOKEN = "publication-token";
    private static final LocalDateTime PUBLISHED_AT =
            LocalDateTime.of(2026, 8, 18, 12, 30, 45);

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentPublicationService publicationService;
    @Autowired CurrentUserProvider currentUserProvider;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                publicationService,
                currentUserProvider,
                accessTokenService,
                authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void hrManagerCanPublishDocumentVersion() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(publicationService.publish(10L, 20L, 77L)).thenReturn(publicationResult());

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(10))
                .andExpect(jsonPath("$.documentVersionId").value(20))
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.publishedAt").value(PUBLISHED_AT.toString()))
                .andExpect(jsonPath("$.publishedBy").value(77));
    }

    @Test
    void systemAdminCanPublishDocumentVersion() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        stubCurrentUser(RoleType.SYSTEM_ADMIN);
        when(publicationService.publish(10L, 20L, 77L)).thenReturn(publicationResult());

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotPublishDocumentVersion() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(publicationService);
    }

    @Test
    void managerCannotPublishDocumentVersion() throws Exception {
        authenticateAs(RoleType.MANAGER);

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(publicationService);
    }

    @Test
    void unauthenticatedPublishIsUnauthorized() throws Exception {
        mockMvc.perform(publishRequest())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(publicationService);
    }

    @Test
    void publishUsesAppUserIdNotEmployeeId() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(publicationService.publish(10L, 20L, 77L)).thenReturn(publicationResult());

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isOk());

        verify(publicationService).publish(10L, 20L, 77L);
    }

    @Test
    void resourceNotFoundIsReturnedAsNotFound() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(publicationService.publish(10L, 20L, 77L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "문서 버전을 찾을 수 없습니다."));

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("문서 버전을 찾을 수 없습니다."));
    }

    @Test
    void alreadyPublishedVersionReturnsConflict() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(publicationService.publish(10L, 20L, 77L))
                .thenThrow(new BusinessException(
                        ErrorCode.CONFLICT,
                        "이미 공개된 버전입니다."));

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void successResponseMapsPublicationResultExactly() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(publicationService.publish(10L, 20L, 77L)).thenReturn(publicationResult());

        mockMvc.perform(authenticatedPublishRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(10))
                .andExpect(jsonPath("$.documentVersionId").value(20))
                .andExpect(jsonPath("$.publicationStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.publishedAt").value(PUBLISHED_AT.toString()))
                .andExpect(jsonPath("$.publishedBy").value(77));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            authenticatedPublishRequest() {
        return publishRequest().header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            publishRequest() {
        return patch("/api/documents/10/versions/20/publish");
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(77L);
        when(authenticationQueryService.load(77L))
                .thenReturn(new JwtAuthenticationUser(77L, Set.of(role)));
    }

    private void stubCurrentUser(RoleType role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        77L,
                        999L,
                        Set.of(role),
                        null,
                        null,
                        null));
    }

    private DocumentPublicationResult publicationResult() {
        return new DocumentPublicationResult(10L, 20L, "PUBLIC", true, PUBLISHED_AT, 77L);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentPublicationController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentPublicationService publicationService() {
            return mock(DocumentPublicationService.class);
        }
        @Bean CurrentUserProvider currentUserProvider() { return mock(CurrentUserProvider.class); }
        @Bean AccessTokenService accessTokenService() { return mock(AccessTokenService.class); }
        @Bean InternalJwtAuthenticationQueryService authenticationQueryService() {
            return mock(InternalJwtAuthenticationQueryService.class);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                AccessTokenService tokens,
                InternalJwtAuthenticationQueryService users) {
            return new JwtAuthenticationFilter(tokens, users);
        }
    }
}
