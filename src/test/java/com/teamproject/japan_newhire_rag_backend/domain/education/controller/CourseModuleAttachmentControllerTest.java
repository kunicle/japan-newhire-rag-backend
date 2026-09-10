package com.teamproject.japan_newhire_rag_backend.domain.education.controller;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.common.exception.GlobalExceptionHandler;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleAttachmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleAttachmentDownload;
import com.teamproject.japan_newhire_rag_backend.domain.education.service.CourseModuleAttachmentService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(CourseModuleAttachmentControllerTest.TestConfiguration.class)
@WebAppConfiguration
class CourseModuleAttachmentControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private CourseModuleAttachmentService attachmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(attachmentService);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .build();
    }

    @Test
    void validTxtUploadReturnsCreatedAttachment() throws Exception {
        byte[] bytes = contentBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "교육자료.txt",
                MediaType.TEXT_PLAIN_VALUE,
                bytes);
        when(attachmentService.upload(
                eq(100L),
                eq("교육자료.txt"),
                any(byte[].class)))
                .thenReturn(new CourseModuleAttachmentResponse(
                        100L,
                        "교육자료.txt",
                        (long) bytes.length));

        mockMvc.perform(multipart(
                        "/api/hr/course-modules/100/attachment")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(100))
                .andExpect(jsonPath("$.fileName").value("교육자료.txt"))
                .andExpect(jsonPath("$.fileSize").value(bytes.length));

        verify(attachmentService).upload(
                eq(100L),
                eq("교육자료.txt"),
                any(byte[].class));
    }

    @Test
    void nonNumericModuleIdForUploadReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "교육자료.txt",
                MediaType.TEXT_PLAIN_VALUE,
                contentBytes());

        mockMvc.perform(multipart(
                        "/api/hr/course-modules/not-a-number/attachment")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(attachmentService, never()).upload(
                anyLong(),
                any(),
                any(byte[].class));
    }

    @Test
    void downloadReturnsTxtBytesAndAttachmentHeaders() throws Exception {
        byte[] bytes = contentBytes();
        when(attachmentService.download(100L))
                .thenReturn(new CourseModuleAttachmentDownload(
                        "교육자료.txt",
                        bytes));

        mockMvc.perform(get(
                        "/api/course-modules/100/attachment"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType(
                        "text/plain;charset=UTF-8"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_LENGTH,
                        String.valueOf(bytes.length)))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")));

        verify(attachmentService).download(100L);
    }

    @Test
    void missingAttachmentForDownloadReturnsNotFound() throws Exception {
        when(attachmentService.download(100L))
                .thenThrow(new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course module attachment not found"));

        mockMvc.perform(get(
                        "/api/course-modules/100/attachment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void forbiddenDownloadReturnsForbidden() throws Exception {
        when(attachmentService.download(100L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get(
                        "/api/course-modules/100/attachment"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validDeleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete(
                        "/api/hr/course-modules/100/attachment"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(attachmentService).delete(100L);
    }

    @Test
    void nonNumericModuleIdForDownloadAndDeleteReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get(
                        "/api/course-modules/not-a-number/attachment"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        mockMvc.perform(delete(
                        "/api/hr/course-modules/not-a-number/attachment"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"));

        verify(attachmentService, never()).download(anyLong());
        verify(attachmentService, never()).delete(anyLong());
    }

    private byte[] contentBytes() {
        return "교육 이수 단위 첨부 자료입니다."
                .getBytes(StandardCharsets.UTF_8);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            CourseModuleAttachmentController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        CourseModuleAttachmentService attachmentService() {
            return mock(CourseModuleAttachmentService.class);
        }
    }
}
