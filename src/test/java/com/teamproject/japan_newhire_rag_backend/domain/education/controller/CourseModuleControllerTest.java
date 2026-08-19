package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(CourseModuleControllerTest.TestConfiguration.class)
@WebAppConfiguration
class CourseModuleControllerTest {
    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private CourseModuleService courseModuleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(courseModuleService);
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

        @Test
        void validModuleListReturnsModulesInOrder() throws Exception {
                CourseModuleResponse first = moduleResponse(100L, 1, true);
                CourseModuleResponse second = moduleResponse(101L, 2, false);
                CourseModuleResponse third = moduleResponse(102L, 3, true);

                when(courseModuleService.getModules(10L))
                        .thenReturn(List.of(first, second, third));

                mockMvc.perform(get("/api/hr/courses/10/modules"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(3))
                        .andExpect(jsonPath("$[0].courseModuleId").value(100))
                        .andExpect(jsonPath("$[0].moduleOrder").value(1))
                        .andExpect(jsonPath("$[1].courseModuleId").value(101))
                        .andExpect(jsonPath("$[1].moduleOrder").value(2))
                        .andExpect(jsonPath("$[1].active").value(false))
                        .andExpect(jsonPath("$[2].courseModuleId").value(102))
                        .andExpect(jsonPath("$[2].moduleOrder").value(3));

                verify(courseModuleService).getModules(10L);
        }

        @Test
        void invalidCourseIdForModuleListReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/hr/courses/not-a-number/modules"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(courseModuleService, never()).getModules(any());
        }

        @Test
        void missingCourseForModuleListReturnsNotFound() throws Exception {
        when(courseModuleService.getModules(10L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));

        mockMvc.perform(get("/api/hr/courses/10/modules"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

    @Test
    void validCreateReturnsCreatedModuleResponse() throws Exception {
        when(courseModuleService.createModule(any(), any(CourseModuleCreateRequest.class)))
                .thenReturn(response(true));

        mockMvc.perform(post("/api/hr/courses/10/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseModuleId").value(100))
                .andExpect(jsonPath("$.courseId").value(10))
                .andExpect(jsonPath("$.moduleTitle").value("Security basics"))
                .andExpect(jsonPath("$.moduleOrder").value(1))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void blankTitleReturnsBadRequest() throws Exception {
        assertCreateValidationError(validJson().replace("Security basics", "   "));
    }

    @Test
    void titleOverTwoHundredCharactersReturnsBadRequest() throws Exception {
        assertCreateValidationError(validJson().replace("Security basics", "a".repeat(201)));
    }

    @Test
    void missingContentAndReferenceReturnsBadRequest() throws Exception {
        assertCreateValidationError("""
                {
                  "moduleTitle":"Security basics",
                  "moduleOrder":1,
                  "required":true
                }
                """);
    }

    @Test
    void blankContentAndReferenceReturnsBadRequest() throws Exception {
        assertCreateValidationError("""
                {
                  "moduleTitle":"Security basics",
                  "moduleContent":"   ",
                  "referenceUrl":"   ",
                  "moduleOrder":1,
                  "required":true
                }
                """);
    }

    @Test
    void missingZeroOrNegativeOrderReturnsBadRequest() throws Exception {
        assertCreateValidationError(validJson().replace("\"moduleOrder\":1,", ""));
        assertCreateValidationError(validJson().replace("\"moduleOrder\":1", "\"moduleOrder\":0"));
        assertCreateValidationError(validJson().replace("\"moduleOrder\":1", "\"moduleOrder\":-1"));
    }

    @Test
    void missingRequiredReturnsBadRequest() throws Exception {
        assertCreateValidationError("""
                {
                  "moduleTitle":"Security basics",
                  "moduleContent":"Security content",
                  "referenceUrl":null,
                  "moduleOrder":1
                }
                """);
    }

    @Test
    void referenceUrlOverFiveHundredCharactersReturnsBadRequest() throws Exception {
        assertCreateValidationError(validJson()
                .replace("\"referenceUrl\":null", "\"referenceUrl\":\"" + "a".repeat(501) + "\""));
    }

    @Test
    void invalidCourseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/hr/courses/not-a-number/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(courseModuleService, never()).createModule(any(), any());
    }

    @Test
    void missingCourseReturnsNotFoundAndDuplicateOrderReturnsConflict() throws Exception {
        when(courseModuleService.createModule(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Module order is already used"));

        mockMvc.perform(post("/api/hr/courses/10/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(post("/api/hr/courses/10/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void validUpdateReturnsOkModuleResponse() throws Exception {
        when(courseModuleService.updateModule(any(), any(CourseModuleUpdateRequest.class)))
                .thenReturn(response(true));

        mockMvc.perform(put("/api/hr/course-modules/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseModuleId").value(100))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateValidationRejectsBlankTitleBlankSourcesAndZeroOrder() throws Exception {
        assertUpdateValidationError(validJson().replace("Security basics", "  "));
        assertUpdateValidationError(validJson()
                .replace("\"moduleContent\":\"Security content\"", "\"moduleContent\":\" \"")
                .replace("\"referenceUrl\":null", "\"referenceUrl\":\" \""));
        assertUpdateValidationError(validJson().replace("\"moduleOrder\":1", "\"moduleOrder\":0"));
    }

    @Test
    void invalidModuleIdReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/hr/course-modules/not-a-number")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(courseModuleService, never()).updateModule(any(), any());
    }

    @Test
    void updateMissingModuleAndStructureConflictUseCommonResponses() throws Exception {
        when(courseModuleService.updateModule(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Course module not found"))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Module order is already used"));

        mockMvc.perform(put("/api/hr/course-modules/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/hr/course-modules/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void validActivationChangeReturnsUpdatedState() throws Exception {
        when(courseModuleService.changeActivation(
                any(), any(CourseModuleActivationRequest.class)))
                .thenReturn(response(false));

        mockMvc.perform(patch("/api/hr/course-modules/100/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void missingActiveReturnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/hr/course-modules/100/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(courseModuleService, never()).changeActivation(any(), any());
    }

    @Test
    void unreadableBooleanReturnsCommonInvalidRequest() throws Exception {
        mockMvc.perform(patch("/api/hr/course-modules/100/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(courseModuleService, never()).changeActivation(any(), any());
    }

    @Test
    void activationMissingModuleAndConflictUseCommonResponses() throws Exception {
        when(courseModuleService.changeActivation(any(), any()))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Course module not found"))
                .thenThrow(new BusinessException(
                        ErrorCode.CONFLICT, "Required module cannot be deactivated"));

        mockMvc.perform(patch("/api/hr/course-modules/100/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/hr/course-modules/100/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void authenticationAndAuthorizationErrorsUseCommonResponses() throws Exception {
        when(courseModuleService.createModule(any(), any()))
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"));
        when(courseModuleService.updateModule(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/hr/courses/10/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(put("/api/hr/course-modules/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void assertCreateValidationError(String body) throws Exception {
        mockMvc.perform(post("/api/hr/courses/10/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(courseModuleService, never()).createModule(any(), any());
    }

    private void assertUpdateValidationError(String body) throws Exception {
        mockMvc.perform(put("/api/hr/course-modules/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verify(courseModuleService, never()).updateModule(any(), any());
    }

    private String validJson() {
        return """
                {
                  "moduleTitle":"Security basics",
                  "moduleContent":"Security content",
                  "referenceUrl":null,
                  "moduleOrder":1,
                  "required":true
                }
                """;
    }

    private CourseModuleResponse response(boolean active) {
        return new CourseModuleResponse(
                100L,
                10L,
                "Security basics",
                "Security content",
                null,
                1,
                true,
                active,
                LocalDateTime.of(2026, 8, 12, 10, 0),
                LocalDateTime.of(2026, 8, 12, 10, 0));
    }

    private CourseModuleResponse moduleResponse(
        Long courseModuleId,
        int moduleOrder,
        boolean active
    ) {
        return new CourseModuleResponse(
                courseModuleId,
                10L,
                "Module " + moduleOrder,
                "Module content " + moduleOrder,
                null,
                moduleOrder,
                true,
                active,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                LocalDateTime.of(2026, 8, 18, 10, 0));
    }

    @Configuration
    @EnableWebMvc
    @Import({CourseModuleController.class, GlobalExceptionHandler.class})
    static class TestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        CourseModuleService courseModuleService() {
            return mock(CourseModuleService.class);
        }
    }
}
