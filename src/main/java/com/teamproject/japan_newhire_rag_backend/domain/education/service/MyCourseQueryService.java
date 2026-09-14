package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.MyCourseSummaryResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizRepository;

@Service
@Transactional(readOnly = true)
public class MyCourseQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private final QuizRepository quizRepository;

    private static final Sort COURSE_LIST_SORT =
            Sort.by(Sort.Direction.DESC, "courseEnrollmentId");

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public MyCourseQueryService(
            CourseEnrollmentRepository courseEnrollmentRepository,
            LearningProgressRepository learningProgressRepository,
            CurrentUserProvider currentUserProvider,
            QuizRepository quizRepository,
            Clock clock
    ) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.currentUserProvider = currentUserProvider;
        this.quizRepository = quizRepository;
        this.clock = clock;
    }

    public MyCoursePageResponse getMyCourses(
            int page,
            int size
    ) {
        validatePageRequest(page, size);

        Long employeeId = getCurrentEmployeeId();
        LocalDate today = LocalDate.now(clock);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                COURSE_LIST_SORT);

        Page<MyCourseSummaryResponse> responsePage =
                courseEnrollmentRepository
                        .findAllByEmployeeId(employeeId, pageRequest)
                        .map(enrollment ->
                                MyCourseSummaryResponse.from(
                                        enrollment,
                                        today));

        return MyCoursePageResponse.from(responsePage);
    }

    public MyCourseDetailResponse getMyCourse(
            Long enrollmentId
    ) {
        if (enrollmentId == null || enrollmentId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Enrollment ID must be a positive number");
        }

        Long employeeId = getCurrentEmployeeId();

        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByCourseEnrollmentId(enrollmentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course enrollment not found"));

        if (!Objects.equals(
                enrollment.getEmployeeId(),
                employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Course enrollment belongs to another employee");
        }

        List<LearningProgress> progresses =
                learningProgressRepository
                        .findAllByCourseEnrollment_CourseEnrollmentIdAndCourseModule_ActiveTrueOrderByCourseModule_ModuleOrderAsc(
                                enrollmentId);

        List<Quiz> quizzes = quizRepository
        .findAllByCourse_CourseIdAndActiveTrueOrderByQuizIdAsc(
                enrollment.getCourse().getCourseId());

        return MyCourseDetailResponse.from(
                enrollment,
                progresses,
                quizzes,
                LocalDate.now(clock));
    }

    private Long getCurrentEmployeeId() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (currentUser.employeeId() == null) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Current user is not linked to an employee");
        }

        return currentUser.employeeId();
    }

    private void validatePageRequest(
            int page,
            int size
    ) {
        if (page < 0
                || size < 1
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100");
        }
    }
}