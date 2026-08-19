package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseEnrollmentCreateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseAssignment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.AssignmentTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseAssignmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@Service
public class CourseEnrollmentService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;

    public CourseEnrollmentService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseAssignmentRepository courseAssignmentRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            LearningProgressRepository learningProgressRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseAssignmentRepository = courseAssignmentRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public CourseEnrollmentCreateResponse createEnrollments(
            Long courseId,
            CourseEnrollmentCreateRequest request
    ) {
        CurrentUserContext currentUser = validateCurrentHrManager();
        Course course = findActiveCourse(courseId);

        validateCourseIsPublic(course);
        validateActiveRequiredModule(courseId);
        validateEnrollmentDates(
                request.enrollmentStartDate(),
                request.enrollmentDueDate());
        validateTargetCombination(request);

        List<Long> targetEmployeeIds = resolveTargetEmployeeIds(request)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (targetEmployeeIds.isEmpty()) {
            return new CourseEnrollmentCreateResponse(
                    0,
                    0,
                    List.of());
        }

        List<CourseEnrollment> existingEnrollments =
                courseEnrollmentRepository
                        .findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
                                courseId,
                                targetEmployeeIds,
                                request.enrollmentRound());

        Set<Long> duplicateEmployeeIdSet = existingEnrollments.stream()
                .map(CourseEnrollment::getEmployeeId)
                .collect(Collectors.toSet());

        List<Long> duplicateEmployeeIds = duplicateEmployeeIdSet.stream()
                .sorted()
                .toList();

        List<Long> newEmployeeIds = targetEmployeeIds.stream()
                .filter(employeeId -> !duplicateEmployeeIdSet.contains(employeeId))
                .toList();

        if (newEmployeeIds.isEmpty()) {
            return new CourseEnrollmentCreateResponse(
                    0,
                    duplicateEmployeeIds.size(),
                    duplicateEmployeeIds);
        }

        CourseAssignment assignment = CourseAssignment.create(
                course,
                request.targetType(),
                request.employeeId(),
                request.departmentId(),
                request.jobGradeId(),
                request.enrollmentRound(),
                request.enrollmentStartDate(),
                request.enrollmentDueDate(),
                currentUser.appUserId());

        courseAssignmentRepository.save(assignment);

        List<CourseEnrollment> newEnrollments = newEmployeeIds.stream()
                .map(employeeId -> CourseEnrollment.create(
                        course,
                        employeeId,
                        assignment,
                        request.enrollmentRound(),
                        request.enrollmentStartDate(),
                        request.enrollmentDueDate()))
                .toList();

        courseEnrollmentRepository.saveAll(newEnrollments);

        List<CourseModule> activeModules = courseModuleRepository
                .findAllByCourse_CourseIdAndActiveTrueOrderByModuleOrderAsc(
                        courseId);

        List<LearningProgress> learningProgresses = newEnrollments.stream()
                .flatMap(enrollment -> activeModules.stream()
                        .map(module -> LearningProgress.create(
                                enrollment,
                                module)))
                .toList();

        learningProgressRepository.saveAll(learningProgresses);

        return new CourseEnrollmentCreateResponse(
                newEmployeeIds.size(),
                duplicateEmployeeIds.size(),
                duplicateEmployeeIds);
    }

    private List<Long> resolveTargetEmployeeIds(
            CourseEnrollmentCreateRequest request
    ) {
        return switch (request.targetType()) {
            case EMPLOYEE -> organizationQueryService
                    .isValidEmployee(request.employeeId())
                            ? List.of(request.employeeId())
                            : List.of();

            case DEPARTMENT ->
                    organizationQueryService
                            .findValidEmployeeIdsByDepartmentIds(
                                    List.of(request.departmentId()));

            case JOB_GRADE ->
                    organizationQueryService
                            .findValidEmployeeIdsByJobGradeIds(
                                    List.of(request.jobGradeId()));

            case NEW_HIRE ->
                    organizationQueryService.findValidNewHireEmployeeIds();
        };
    }

    private void validateTargetCombination(
            CourseEnrollmentCreateRequest request
    ) {
        AssignmentTargetType targetType = request.targetType();

        if (targetType == null) {
            throw invalidTargetCombination();
        }

        boolean valid = switch (targetType) {
            case EMPLOYEE ->
                    isPositive(request.employeeId())
                            && request.departmentId() == null
                            && request.jobGradeId() == null;

            case DEPARTMENT ->
                    request.employeeId() == null
                            && isPositive(request.departmentId())
                            && request.jobGradeId() == null;

            case JOB_GRADE ->
                    request.employeeId() == null
                            && request.departmentId() == null
                            && isPositive(request.jobGradeId());

            case NEW_HIRE ->
                    request.employeeId() == null
                            && request.departmentId() == null
                            && request.jobGradeId() == null;
        };

        if (!valid) {
            throw invalidTargetCombination();
        }
    }

    private boolean isPositive(Long id) {
        return id != null && id > 0;
    }

    private BusinessException invalidTargetCombination() {
        return new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Target type and target ID combination is invalid");
    }

    private void validateEnrollmentDates(
            LocalDate startDate,
            LocalDate dueDate
    ) {
        if (startDate == null
                || dueDate == null
                || dueDate.isBefore(startDate)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Enrollment due date must not be before enrollment start date");
        }
    }

    private void validateCourseIsPublic(Course course) {
        if (course.getPublicationStatus()
                != CoursePublicationStatus.PUBLIC) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Only public courses can be assigned");
        }
    }

    private void validateActiveRequiredModule(Long courseId) {
        boolean exists = courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                        courseId);

        if (!exists) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "An active required module is required to assign the course");
        }
    }

    private Course findActiveCourse(Long courseId) {
        return courseRepository
                .findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));
    }

    private CurrentUserContext validateCurrentHrManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!currentUser.roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return currentUser;
    }
}