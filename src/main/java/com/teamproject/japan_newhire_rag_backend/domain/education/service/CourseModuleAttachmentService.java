package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.storage.DocumentStorageService;
import com.teamproject.japan_newhire_rag_backend.document.validation.TxtDocumentValidator;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleAttachmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;

@Service
public class CourseModuleAttachmentService {

    private final CourseModuleRepository courseModuleRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TxtDocumentValidator txtDocumentValidator;
    private final DocumentStorageService documentStorageService;

    public CourseModuleAttachmentService(
            CourseModuleRepository courseModuleRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            CurrentUserProvider currentUserProvider,
            TxtDocumentValidator txtDocumentValidator,
            DocumentStorageService documentStorageService
    ) {
        this.courseModuleRepository = courseModuleRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.txtDocumentValidator = txtDocumentValidator;
        this.documentStorageService = documentStorageService;
    }

    @Transactional
    public CourseModuleAttachmentResponse upload(
            Long moduleId,
            String originalFileName,
            byte[] content
    ) {
        validateModuleId(moduleId);
        validateCurrentHrManager();

        CourseModule module = findAvailableModule(moduleId);
        txtDocumentValidator.validate(originalFileName, content);
        validateAttachmentFileNameLength(originalFileName);

        String previousStoredPath =
                module.getAttachmentStoredFilePath();
        String storedFilePath =
                documentStorageService.store(
                        originalFileName,
                        content);

        try {
            module.replaceAttachment(
                    originalFileName,
                    storedFilePath,
                    content.length);

            if (previousStoredPath != null
                    && !previousStoredPath.equals(storedFilePath)) {
                documentStorageService.delete(previousStoredPath);
            }

            return CourseModuleAttachmentResponse.from(module);
        } catch (RuntimeException exception) {
            try {
                documentStorageService.delete(storedFilePath);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public CourseModuleAttachmentDownload download(Long moduleId) {
        validateModuleId(moduleId);

        CourseModule module = findAvailableModule(moduleId);
        validateDownloadPermission(module);

        if (!module.hasAttachment()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Course module attachment not found");
        }

        byte[] content = documentStorageService.load(
                module.getAttachmentStoredFilePath());

        return new CourseModuleAttachmentDownload(
                module.getAttachmentOriginalFileName(),
                content);
    }

    @Transactional
    public void delete(Long moduleId) {
        validateModuleId(moduleId);
        validateCurrentHrManager();

        CourseModule module = findAvailableModule(moduleId);

        if (!module.hasAttachment()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Course module attachment not found");
        }

        String storedFilePath =
                module.getAttachmentStoredFilePath();

        documentStorageService.delete(storedFilePath);
        module.removeAttachment();
    }

    private CourseModule findAvailableModule(Long moduleId) {
        return courseModuleRepository
                .findByCourseModuleIdAndCourse_DeletedAtIsNull(moduleId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course module not found"));
    }

    private void validateDownloadPermission(CourseModule module) {
        CurrentUserContext currentUser =
                requireCurrentUser();

        if (currentUser.roles().contains(RoleType.HR_MANAGER)) {
            return;
        }

        if (currentUser.employeeId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        boolean enrolled = courseEnrollmentRepository
                .existsByCourse_CourseIdAndEmployeeId(
                        module.getCourse().getCourseId(),
                        currentUser.employeeId());

        if (!enrolled || !module.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateCurrentHrManager() {
        CurrentUserContext currentUser =
                requireCurrentUser();

        if (!currentUser.roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private CurrentUserContext requireCurrentUser() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return currentUser;
    }

    private void validateAttachmentFileNameLength(String originalFileName) {
        if (originalFileName != null
                && originalFileName.length() > 255) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Attachment file name must not exceed 255 characters");
        }
    }

    private void validateModuleId(Long moduleId) {
        if (moduleId == null || moduleId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Course module ID must be a positive number");
        }
    }
}
