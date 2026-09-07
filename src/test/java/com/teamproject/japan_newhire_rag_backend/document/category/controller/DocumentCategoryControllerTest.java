package com.teamproject.japan_newhire_rag_backend.document.category.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.document.category.entity.DocumentCategory;
import com.teamproject.japan_newhire_rag_backend.document.category.service.DocumentCategoryQueryService;
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

@SpringJUnitConfig(DocumentCategoryControllerTest.TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "auth.cookie.name=refresh_token",
        "auth.cookie.secure=false",
        "auth.cookie.same-site=Lax",
        "auth.cookie.path=/api/auth",
        "auth.cookie.max-age=14d",
        "auth.cors.allowed-origins=http://localhost:5173"
})
class DocumentCategoryControllerTest {

    private static final String ACCESS_TOKEN = "document-category-token";

    @Autowired WebApplicationContext applicationContext;
    @Autowired DocumentCategoryQueryService queryService;
    @Autowired AccessTokenService accessTokenService;
    @Autowired InternalJwtAuthenticationQueryService authenticationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(queryService, accessTokenService, authenticationQueryService);
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void hrManagerCanListActiveCategories() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        DocumentCategory category = category();
        when(queryService.getActiveCategories()).thenReturn(List.of(category));

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentCategoryId").value(1))
                .andExpect(jsonPath("$[0].categoryCode").value("HR_POLICY"))
                .andExpect(jsonPath("$[0].categoryName").value("인사 규정"))
                .andExpect(jsonPath("$[0].categoryDescription").doesNotExist())
                .andExpect(jsonPath("$[0].isActive").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());

        verify(queryService).getActiveCategories();
    }

    @Test
    void systemAdminCanListActiveCategories() throws Exception {
        authenticateAs(RoleType.SYSTEM_ADMIN);
        DocumentCategory category = category();
        when(queryService.getActiveCategories()).thenReturn(List.of(category));

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isOk());

        verify(queryService).getActiveCategories();
    }

    @Test
    void employeeCannotListActiveCategories() throws Exception {
        authenticateAs(RoleType.EMPLOYEE);

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(queryService);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(request())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(queryService);
    }

    @Test
    void returnsEmptyListWhenNoActiveCategoriesExist() throws Exception {
        authenticateAs(RoleType.HR_MANAGER);
        when(queryService.getActiveCategories()).thenReturn(List.of());

        mockMvc.perform(authenticatedRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(queryService).getActiveCategories();
    }

    private MockHttpServletRequestBuilder authenticatedRequest() {
        return request().header("Authorization", "Bearer " + ACCESS_TOKEN);
    }

    private MockHttpServletRequestBuilder request() {
        return get("/api/documents/categories");
    }

    private void authenticateAs(RoleType role) {
        when(accessTokenService.validateAndExtractAppUserId(ACCESS_TOKEN)).thenReturn(77L);
        when(authenticationQueryService.load(77L))
                .thenReturn(new JwtAuthenticationUser(77L, Set.of(role)));
    }

    private DocumentCategory category() {
        DocumentCategory category = mock(DocumentCategory.class);
        when(category.getDocumentCategoryId()).thenReturn(1L);
        when(category.getCategoryCode()).thenReturn("HR_POLICY");
        when(category.getCategoryName()).thenReturn("인사 규정");
        return category;
    }

    @Configuration
    @EnableWebMvc
    @Import({
            DocumentCategoryController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class
    })
    static class TestConfiguration {

        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean DocumentCategoryQueryService queryService() {
            return mock(DocumentCategoryQueryService.class);
        }
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
