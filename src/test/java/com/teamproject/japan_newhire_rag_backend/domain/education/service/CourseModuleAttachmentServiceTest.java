package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.storage.DocumentStorageService;
import com.teamproject.japan_newhire_rag_backend.document.validation.InvalidTxtDocumentException;
import com.teamproject.japan_newhire_rag_backend.document.validation.TxtDocumentValidator;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleAttachmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class CourseModuleAttachmentServiceTest {

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private TxtDocumentValidator txtDocumentValidator;

    @Mock
    private DocumentStorageService documentStorageService;

    private CourseModuleAttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new CourseModuleAttachmentService(
                courseModuleRepository,
                courseEnrollmentRepository,
                currentUserProvider,
                txtDocumentValidator,
                documentStorageService);
    }

    @Test
    void hrManagerUploadsAttachment() {
        CourseModule module = module(true);
        byte[] content = content();
        allowUser(RoleType.HR_MANAGER);
        findModule(module);
        when(documentStorageService.store("교육자료.txt", content))
                .thenReturn("new-file.txt");

        CourseModuleAttachmentResponse response = attachmentService.upload(
                100L,
                "교육자료.txt",
                content);

        assertThat(response.moduleId()).isEqualTo(100L);
        assertThat(response.fileName()).isEqualTo("교육자료.txt");
        assertThat(response.fileSize()).isEqualTo((long) content.length);
        assertThat(module.getAttachmentOriginalFileName())
                .isEqualTo("교육자료.txt");
        assertThat(module.getAttachmentStoredFilePath())
                .isEqualTo("new-file.txt");
        assertThat(module.getAttachmentFileSize())
                .isEqualTo((long) content.length);

        verify(txtDocumentValidator).validate("교육자료.txt", content);
        verify(documentStorageService).store("교육자료.txt", content);
        verify(documentStorageService, never()).delete("new-file.txt");
    }

    @Test
    void uploadingReplacementDeletesPreviousStoredFile() {
        CourseModule module = module(true);
        module.replaceAttachment("이전자료.txt", "old-file.txt", 10L);
        byte[] content = content();
        allowUser(RoleType.HR_MANAGER);
        findModule(module);
        when(documentStorageService.store("새자료.txt", content))
                .thenReturn("new-file.txt");

        attachmentService.upload(100L, "새자료.txt", content);

        assertThat(module.getAttachmentOriginalFileName())
                .isEqualTo("새자료.txt");
        assertThat(module.getAttachmentStoredFilePath())
                .isEqualTo("new-file.txt");
        verify(documentStorageService).delete("old-file.txt");
    }

    @Test
    void invalidTxtDocumentIsNotStored() {
        CourseModule module = module(true);
        byte[] content = content();
        allowUser(RoleType.HR_MANAGER);
        findModule(module);
        doThrow(new InvalidTxtDocumentException("TXT 파일만 업로드할 수 있습니다."))
                .when(txtDocumentValidator)
                .validate("교육자료.pdf", content);

        assertThatThrownBy(() -> attachmentService.upload(
                100L,
                "교육자료.pdf",
                content))
                .isInstanceOf(InvalidTxtDocumentException.class);

        verify(documentStorageService, never()).store("교육자료.pdf", content);
    }

    @Test
    void fileNameLongerThanTwoHundredFiftyFiveCharactersIsNotStored() {
        CourseModule module = module(true);
        byte[] content = content();
        String longFileName = "가".repeat(252) + ".txt";
        allowUser(RoleType.HR_MANAGER);
        findModule(module);

        assertErrorCode(
                () -> attachmentService.upload(
                        100L,
                        longFileName,
                        content),
                ErrorCode.INVALID_REQUEST);

        verify(documentStorageService, never())
                .store(longFileName, content);
    }

    @Test
    void employeeEnrolledInCourseDownloadsActiveModuleAttachment() {
        CourseModule module = module(true);
        byte[] content = content();
        module.replaceAttachment("교육자료.txt", "stored-file.txt", content.length);
        allowUser(RoleType.EMPLOYEE);
        findModule(module);
        when(courseEnrollmentRepository
                .existsByCourse_CourseIdAndEmployeeId(10L, 70L))
                .thenReturn(true);
        when(documentStorageService.load("stored-file.txt"))
                .thenReturn(content);

        CourseModuleAttachmentDownload download =
                attachmentService.download(100L);

        assertThat(download.fileName()).isEqualTo("교육자료.txt");
        assertThat(download.content()).containsExactly(content);
        verify(documentStorageService).load("stored-file.txt");
    }

    @Test
    void hrManagerDownloadsWithoutEnrollmentCheck() {
        CourseModule module = module(true);
        byte[] content = content();
        module.replaceAttachment("교육자료.txt", "stored-file.txt", content.length);
        allowUser(RoleType.HR_MANAGER);
        findModule(module);
        when(documentStorageService.load("stored-file.txt"))
                .thenReturn(content);

        CourseModuleAttachmentDownload download =
                attachmentService.download(100L);

        assertThat(download.content()).containsExactly(content);
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void employeeNotEnrolledInCourseCannotDownload() {
        CourseModule module = module(true);
        module.replaceAttachment("교육자료.txt", "stored-file.txt", 10L);
        allowUser(RoleType.EMPLOYEE);
        findModule(module);
        when(courseEnrollmentRepository
                .existsByCourse_CourseIdAndEmployeeId(10L, 70L))
                .thenReturn(false);

        assertErrorCode(
                () -> attachmentService.download(100L),
                ErrorCode.FORBIDDEN);

        verify(documentStorageService, never()).load("stored-file.txt");
    }

    @Test
    void employeeCannotDownloadInactiveModuleAttachment() {
        CourseModule module = module(false);
        module.replaceAttachment("교육자료.txt", "stored-file.txt", 10L);
        allowUser(RoleType.EMPLOYEE);
        findModule(module);
        when(courseEnrollmentRepository
                .existsByCourse_CourseIdAndEmployeeId(10L, 70L))
                .thenReturn(true);

        assertErrorCode(
                () -> attachmentService.download(100L),
                ErrorCode.FORBIDDEN);

        verify(documentStorageService, never()).load("stored-file.txt");
    }

    @Test
    void downloadingModuleWithoutAttachmentReturnsNotFound() {
        CourseModule module = module(true);
        allowUser(RoleType.HR_MANAGER);
        findModule(module);

        assertErrorCode(
                () -> attachmentService.download(100L),
                ErrorCode.RESOURCE_NOT_FOUND);

        verifyNoInteractions(documentStorageService);
    }

    @Test
    void hrManagerDeletesAttachmentAndMetadata() {
        CourseModule module = module(true);
        module.replaceAttachment("교육자료.txt", "stored-file.txt", 10L);
        allowUser(RoleType.HR_MANAGER);
        findModule(module);

        attachmentService.delete(100L);

        verify(documentStorageService).delete("stored-file.txt");
        assertThat(module.hasAttachment()).isFalse();
        assertThat(module.getAttachmentOriginalFileName()).isNull();
        assertThat(module.getAttachmentStoredFilePath()).isNull();
        assertThat(module.getAttachmentFileSize()).isNull();
    }

    @Test
    void employeeCannotUploadOrDeleteAttachment() {
        allowUser(RoleType.EMPLOYEE);

        assertErrorCode(
                () -> attachmentService.upload(
                        100L,
                        "교육자료.txt",
                        content()),
                ErrorCode.FORBIDDEN);
        assertErrorCode(
                () -> attachmentService.delete(100L),
                ErrorCode.FORBIDDEN);

        verifyNoInteractions(
                courseModuleRepository,
                courseEnrollmentRepository,
                txtDocumentValidator,
                documentStorageService);
    }

    private void findModule(CourseModule module) {
        when(courseModuleRepository
                .findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));
    }

    private CourseModule module(boolean active) {
        Course course = Course.create(
                "신입사원 기본 교육",
                "교육 과정 설명",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L);
        ReflectionTestUtils.setField(course, "courseId", 10L);

        CourseModule module = CourseModule.create(
                course,
                "보안 교육",
                "보안 교육 내용",
                null,
                1,
                true);
        ReflectionTestUtils.setField(module, "courseModuleId", 100L);
        module.changeActive(active);
        return module;
    }

    private void allowUser(RoleType role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUserContext(
                        7L,
                        70L,
                        Set.of(role),
                        700L,
                        1,
                        EmployeeType.GENERAL));
    }

    private byte[] content() {
        return "교육 이수 단위 첨부 자료입니다."
                .getBytes(StandardCharsets.UTF_8);
    }

    private void assertErrorCode(
            Runnable operation,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode));
    }
}
