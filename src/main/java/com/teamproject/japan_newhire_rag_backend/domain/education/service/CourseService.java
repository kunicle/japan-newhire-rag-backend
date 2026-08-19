package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePublicationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;

@Service
public class CourseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort COURSE_LIST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("courseId"));

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public CourseService(
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

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        validateAuthenticatedUser(currentUser);
        validateHrManager(currentUser);
        validateTrainingDates(request);

        Course course = Course.create(
                request.courseName(),
                request.courseDescription(),
                request.required(),
                request.trainingStartDate(),
                request.trainingEndDate(),
                currentUser.appUserId());

        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CoursePageResponse getCourses(int page, int size) {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        validateAuthenticatedUser(currentUser);
        validateHrManager(currentUser);
        validatePageRequest(page, size);

        PageRequest pageRequest = PageRequest.of(page, size, COURSE_LIST_SORT);
        Page<CourseResponse> coursePage = courseRepository
                .findAllByDeletedAtIsNull(pageRequest)
                .map(CourseResponse::from);
        return CoursePageResponse.from(coursePage);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId) {
        validateCurrentHrManager();

        return CourseResponse.from(findActiveCourse(courseId));
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        validateCurrentHrManager();
        validateTrainingDates(request.trainingStartDate(), request.trainingEndDate());

        Course course = findActiveCourse(courseId);
        course.updateBasicInformation(
                request.courseName(),
                request.courseDescription(),
                request.required(),
                request.trainingStartDate(),
                request.trainingEndDate());
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse changePublicationStatus(
            Long courseId,
            CoursePublicationUpdateRequest request
    ) {
        validateCurrentHrManager();

        Course course = findActiveCourse(courseId);
        CoursePublicationStatus currentStatus =
                course.getPublicationStatus();
        CoursePublicationStatus requestedStatus =
                request.publicationStatus();

        // 같은 상태 요청은 기존 상태를 그대로 반환
        if (currentStatus == requestedStatus) {
            return CourseResponse.from(course);
        }

        boolean hasOperationHistory =
                requestedStatus == CoursePublicationStatus.DRAFT
                        && courseEnrollmentRepository
                                .existsByCourse_CourseId(courseId);

        validatePublicationTransition(
                currentStatus,
                requestedStatus,
                hasOperationHistory);

        if (requestedStatus == CoursePublicationStatus.PUBLIC
                && !courseModuleRepository
                        .existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                                courseId)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "An active required module is required to publish the course");
        }

        course.changePublicationStatus(requestedStatus);
        return CourseResponse.from(course);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        validateCurrentHrManager();
        Course course = findActiveCourse(courseId);
        course.softDelete(LocalDateTime.now());
    }

    private Course findActiveCourse(Long courseId) {
        return courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));
    }

    private CurrentUserContext validateCurrentHrManager() {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        validateAuthenticatedUser(currentUser);
        validateHrManager(currentUser);
        return currentUser;
    }

    private void validateAuthenticatedUser(CurrentUserContext currentUser) {
        if (currentUser == null || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateHrManager(CurrentUserContext currentUser) {
        if (!currentUser.roles().contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateTrainingDates(CourseCreateRequest request) {
        validateTrainingDates(request.trainingStartDate(), request.trainingEndDate());
    }

    private void validateTrainingDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Training end date must not be before training start date");
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private void validatePublicationTransition(
            CoursePublicationStatus currentStatus,
            CoursePublicationStatus requestedStatus,
            boolean hasOperationHistory
    ) {
        if (currentStatus == CoursePublicationStatus.DRAFT
                && requestedStatus == CoursePublicationStatus.PRIVATE) {
            throw invalidPublicationTransition(
                    currentStatus,
                    requestedStatus);
        }

        if (requestedStatus == CoursePublicationStatus.DRAFT
                && hasOperationHistory) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "A course with operation history cannot return to DRAFT");
        }

        boolean allowedTransition =
                (currentStatus == CoursePublicationStatus.DRAFT
                        && requestedStatus == CoursePublicationStatus.PUBLIC)
                || (currentStatus == CoursePublicationStatus.PUBLIC
                        && requestedStatus == CoursePublicationStatus.PRIVATE)
                || (currentStatus == CoursePublicationStatus.PRIVATE
                        && requestedStatus == CoursePublicationStatus.PUBLIC)
                || ((currentStatus == CoursePublicationStatus.PUBLIC
                        || currentStatus == CoursePublicationStatus.PRIVATE)
                        && requestedStatus == CoursePublicationStatus.DRAFT);

        if (!allowedTransition) {
            throw invalidPublicationTransition(
                    currentStatus,
                    requestedStatus);
        }
    }

    private BusinessException invalidPublicationTransition(
            CoursePublicationStatus currentStatus,
            CoursePublicationStatus requestedStatus
    ) {
        return new BusinessException(
                ErrorCode.CONFLICT,
                "Course publication status cannot transition from "
                        + currentStatus
                        + " to "
                        + requestedStatus);
    }
}
