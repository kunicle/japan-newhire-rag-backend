package com.teamproject.japan_newhire_rag_backend.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentIngestionOrchestrationService;
import com.teamproject.japan_newhire_rag_backend.document.processing.service.DocumentIngestionResult;
import com.teamproject.japan_newhire_rag_backend.document.validation.InvalidTxtDocumentException;
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

@SpringJUnitConfig(DocumentUploadControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentUploadControllerTest {

    private static final String ACCESS_TOKEN = "upload-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentIngestionOrchestrationService ingestionService;
    @Autowired CurrentUserProvider currentUserProvider;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(ingestionService, currentUserProvider, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void hrManagerUploadSucceeds() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(completedResult());

        mockMvc.perform(uploadRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(101))
                .andExpect(jsonPath("$.documentVersionId").value(201))
                .andExpect(jsonPath("$.documentProcessingJobId").value(301))
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"));
    }

    @Test
    void systemAdminUploadSucceeds() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        stubCurrentUser(RoleType.SYSTEM_ADMIN);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(completedResult());

        mockMvc.perform(uploadRequest())
                .andExpect(status().isCreated());
    }

    @Test
    void employeeUploadIsForbidden() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(uploadRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void managerUploadIsForbidden() throws Exception {
        authenticateAs(RoleType.MANAGER);

        mockMvc.perform(uploadRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void unauthenticatedUploadIsUnauthorized() throws Exception {
        mockMvc.perform(uploadRequestWithoutAuthorization())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ingestionService);
    }

    @Test
    void uploadPassesMultipartFieldsAndCurrentUserToIngestionService() throws Exception {
        byte[] content = "신입사원 온보딩 문서".getBytes(StandardCharsets.UTF_8);
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(completedResult());

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile(
                                "file", "onboarding.txt", MediaType.TEXT_PLAIN_VALUE, content))
                        .param("documentCategoryId", "10")
                        .param("title", "신입사원 온보딩 안내")
                        .param("description", "2026년 온보딩 문서")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isCreated());

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(ingestionService).ingest(
                eq(10L),
                eq("신입사원 온보딩 안내"),
                eq("2026년 온보딩 문서"),
                eq("v1"),
                eq("onboarding.txt"),
                contentCaptor.capture(),
                eq(77L));
        assertThat(contentCaptor.getValue()).containsExactly(content);
    }

    @Test
    void invalidTxtUploadReturnsBadRequest() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidTxtDocumentException("TXT 파일만 업로드할 수 있습니다."));

        mockMvc.perform(uploadRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("TXT 파일만 업로드할 수 있습니다."));
    }

    @Test
    void inactiveCategoryReturnsConflict() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(
                        ErrorCode.CONFLICT,
                        "비활성 문서 카테고리는 사용할 수 없습니다."));

        mockMvc.perform(uploadRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void failedProcessingStillReturnsCreated() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        stubCurrentUser(RoleType.HR_MANAGER);
        when(ingestionService.ingest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new DocumentIngestionResult(101L, 201L, 301L, "FAILED"));

        mockMvc.perform(uploadRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processingStatus").value("FAILED"));
    }

    private MockMultipartHttpServletRequestBuilder uploadRequest() {
        return uploadRequestWithoutAuthorization()
                .header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private MockMultipartHttpServletRequestBuilder uploadRequestWithoutAuthorization() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "onboarding.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "온보딩 안내".getBytes(StandardCharsets.UTF_8));
        return multipart("/api/documents")
                .file(file)
                .param("documentCategoryId", "10")
                .param("title", "신입사원 온보딩 안내")
                .param("description", "2026년 온보딩 문서");
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(77L);
        when(authenticationQueryService.load(77L))
                .thenReturn(new JwtAuthenticationUser(77L, Set.of(role)));
    }

    private void stubCurrentUser(RoleType role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(77L, 999L, Set.of(role), null, null, null));
    }

    private DocumentIngestionResult completedResult() {
        return new DocumentIngestionResult(101L, 201L, 301L, "COMPLETED");
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentUploadController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentIngestionOrchestrationService ingestionService() {
            return mock(DocumentIngestionOrchestrationService.class);
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
