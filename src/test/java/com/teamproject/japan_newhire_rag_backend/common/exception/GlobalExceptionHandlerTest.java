package com.teamproject.japan_newhire_rag_backend.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;
import com.teamproject.japan_newhire_rag_backend.document.validation.InvalidTxtDocumentException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void invalidRequestBusinessExceptionReturnsBadRequest() throws Exception {
        assertBusinessError(ErrorCode.INVALID_REQUEST, 400);
    }

    @Test
    void resourceNotFoundBusinessExceptionReturnsNotFound() throws Exception {
        assertBusinessError(ErrorCode.RESOURCE_NOT_FOUND, 404);
    }

    @Test
    void conflictBusinessExceptionReturnsConflict() throws Exception {
        assertBusinessError(ErrorCode.CONFLICT, 409);
    }

    @Test
    void domainSpecificErrorCodeIsHandledThroughCommonContract() throws Exception {
        mockMvc.perform(get("/test/errors/domain"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COURSE_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("Course is not available"));
    }

    @Test
    void beanValidationFailureReturnsFirstFieldError() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("email: must not be blank"));
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalMessage() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void unreadableJsonReturnsInvalidRequestWithoutExposingParserMessage() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request"));
    }

    @Test
    void invalidTxtDocumentExceptionReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/invalid-txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("TXT 파일만 업로드할 수 있습니다."));
    }

    private void assertBusinessError(ErrorCode errorCode, int status) throws Exception {
        mockMvc.perform(get("/test/errors/business/{code}", errorCode.name()))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(errorCode.name()))
                .andExpect(jsonPath("$.message").value(errorCode.defaultMessage()));
    }

    @RestController
    @RequestMapping("/test/errors")
    static class TestController {

        @GetMapping("/business/{code}")
        void business(@PathVariable ErrorCode code) {
            throw new BusinessException(code);
        }

        @GetMapping("/domain")
        void domain() {
            throw new BusinessException(TestDomainErrorCode.COURSE_NOT_AVAILABLE);
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException("must not be exposed");
        }

        @GetMapping("/invalid-txt")
        void invalidTxt() {
            throw new InvalidTxtDocumentException("TXT 파일만 업로드할 수 있습니다.");
        }
    }

    record ValidationRequest(@NotBlank String email) {
    }

    enum TestDomainErrorCode implements ErrorCodeSpec {

        COURSE_NOT_AVAILABLE;

        @Override
        public HttpStatus status() {
            return HttpStatus.CONFLICT;
        }

        @Override
        public String code() {
            return name();
        }

        @Override
        public String defaultMessage() {
            return "Course is not available";
        }
    }
}
