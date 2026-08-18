package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;

@Service
public class CourseModuleService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public CourseModuleService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<CourseModuleResponse> getModules(Long courseId) {
        validateCurrentHrManager();
        findActiveCourse(courseId);

        return courseModuleRepository
                .findAllByCourse_CourseIdOrderByModuleOrderAsc(courseId)
                .stream()
                .map(CourseModuleResponse::from)
                .toList();
    }

    @Transactional
    public CourseModuleResponse createModule(Long courseId, CourseModuleCreateRequest request) {
        validateCurrentHrManager();
        Course course = findActiveCourse(courseId);
        String moduleContent = normalizeOptional(request.moduleContent());
        String referenceUrl = normalizeOptional(request.referenceUrl());
        validateContentOrReference(moduleContent, referenceUrl);
        validateModuleOrder(request.moduleOrder());
        validateOrderAvailable(courseId, request.moduleOrder());
        validateRequiredModuleCanBeAdded(courseId, request.required());

        CourseModule module = CourseModule.create(
                course,
                request.moduleTitle(),
                moduleContent,
                referenceUrl,
                request.moduleOrder(),
                request.required());
        return CourseModuleResponse.from(courseModuleRepository.save(module));
    }

    @Transactional
    public CourseModuleResponse updateModule(Long moduleId, CourseModuleUpdateRequest request) {
        validateCurrentHrManager();
        CourseModule module = findAvailableModule(moduleId);
        Long courseId = module.getCourse().getCourseId();
        String moduleContent = normalizeOptional(request.moduleContent());
        String referenceUrl = normalizeOptional(request.referenceUrl());

        validateContentOrReference(moduleContent, referenceUrl);
        validateModuleOrder(request.moduleOrder());
        validateOrderAvailableForUpdate(courseId, request.moduleOrder(), moduleId);
        validateRequiredChange(module, request.required());

        module.updateBasicInformation(
                request.moduleTitle(),
                moduleContent,
                referenceUrl,
                request.moduleOrder(),
                request.required());
        return CourseModuleResponse.from(module);
    }

    @Transactional
    public CourseModuleResponse changeActivation(
            Long moduleId,
            CourseModuleActivationRequest request
    ) {
        validateCurrentHrManager();
        CourseModule module = findAvailableModule(moduleId);
        boolean requestedActive = request.active();

        if (module.isActive() == requestedActive) {
            return CourseModuleResponse.from(module);
        }

        if (!requestedActive && module.isRequired()) {
            Long courseId = module.getCourse().getCourseId();
            if (courseEnrollmentRepository.existsByCourse_CourseId(courseId)) {
                throw structureConflict("Required modules cannot be deactivated after enrollment");
            }
            validatePublicCourseKeepsRequiredModule(module);
        }

        module.changeActive(requestedActive);
        return CourseModuleResponse.from(module);
    }

    private Course findActiveCourse(Long courseId) {
        return courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));
    }

    private CourseModule findAvailableModule(Long moduleId) {
        return courseModuleRepository
                .findByCourseModuleIdAndCourse_DeletedAtIsNull(moduleId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course module not found"));
    }

    private void validateOrderAvailable(Long courseId, int moduleOrder) {
        if (courseModuleRepository.existsByCourse_CourseIdAndModuleOrder(
                courseId,
                moduleOrder)) {
            throw structureConflict("Module order is already used in this course");
        }
    }

    private void validateOrderAvailableForUpdate(
            Long courseId,
            int moduleOrder,
            Long moduleId
    ) {
        if (courseModuleRepository
                .existsByCourse_CourseIdAndModuleOrderAndCourseModuleIdNot(
                        courseId,
                        moduleOrder,
                        moduleId)) {
            throw structureConflict("Module order is already used in this course");
        }
    }

    private void validateRequiredModuleCanBeAdded(Long courseId, boolean required) {
        if (required && courseEnrollmentRepository.existsByCourse_CourseId(courseId)) {
            throw structureConflict("Required modules cannot be added after enrollment");
        }
    }

    private void validateRequiredChange(CourseModule module, boolean requestedRequired) {
        if (module.isRequired() == requestedRequired) {
            return;
        }

        Long courseId = module.getCourse().getCourseId();
        if (courseEnrollmentRepository.existsByCourse_CourseId(courseId)) {
            throw structureConflict("Required status cannot be changed after enrollment");
        }

        if (module.isRequired() && module.isActive() && !requestedRequired) {
            validatePublicCourseKeepsRequiredModule(module);
        }
    }

    private void validatePublicCourseKeepsRequiredModule(CourseModule module) {
        Course course = module.getCourse();
        if (course.getPublicationStatus() != CoursePublicationStatus.PUBLIC) {
            return;
        }

        boolean anotherRequiredModuleExists = courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrueAndCourseModuleIdNot(
                        course.getCourseId(),
                        module.getCourseModuleId());
        if (!anotherRequiredModuleExists) {
            throw structureConflict(
                    "A public course must keep at least one active required module");
        }
    }

    private void validateCurrentHrManager() {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        if (currentUser == null || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!currentUser.roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateContentOrReference(String moduleContent, String referenceUrl) {
        if (moduleContent == null && referenceUrl == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Module content or reference URL must be provided");
        }
    }

    private void validateModuleOrder(Integer moduleOrder) {
        if (moduleOrder == null || moduleOrder < 1) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Module order must be at least 1");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private BusinessException structureConflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
