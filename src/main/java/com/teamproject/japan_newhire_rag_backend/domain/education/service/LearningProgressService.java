package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.LearningProgressUpdateResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.LearningProgress;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.LearningCompletionStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.LearningProgressRepository;

@Service
public class LearningProgressService {

    private final LearningProgressRepository learningProgressRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public LearningProgressService(
            LearningProgressRepository learningProgressRepository,
            CourseModuleRepository courseModuleRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.learningProgressRepository = learningProgressRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public LearningProgressUpdateResponse startProgress(
            Long progressId
    ) {
        validateProgressId(progressId);

        Long employeeId = getCurrentEmployeeId();

        LearningProgress progress = findProgress(progressId);
        CourseEnrollment enrollment =
                progress.getCourseEnrollment();

        validateOwnership(enrollment, employeeId);

        LocalDate today = LocalDate.now(clock);
        LocalDateTime currentTime = LocalDateTime.now(clock);

        progress.start(currentTime);
        enrollment.startLearning(today);

        return LearningProgressUpdateResponse.from(
                progress,
                today);
    }

    @Transactional
    public LearningProgressUpdateResponse completeProgress(
            Long progressId
    ) {
        validateProgressId(progressId);

        Long employeeId = getCurrentEmployeeId();

        LearningProgress progress = findProgress(progressId);
        CourseEnrollment enrollment =
                progress.getCourseEnrollment();

        validateOwnership(enrollment, employeeId);

        LocalDate today = LocalDate.now(clock);

        // 이미 완료된 요청은 완료 시각과 진행률을 다시 변경하지 않는다.
        if (progress.getCompletionStatus()
                == LearningCompletionStatus.COMPLETED) {
            return LearningProgressUpdateResponse.from(
                    progress,
                    today);
        }

        LocalDateTime currentTime = LocalDateTime.now(clock);

        // 직접 완료 요청이 들어와도 수강 시작/연체 상태를 먼저 동기화한다.
        enrollment.startLearning(today);
        progress.complete(currentTime);

        Long courseId = enrollment
                .getCourse()
                .getCourseId();

        long totalRequiredModuleCount =
                courseModuleRepository
                        .countByCourse_CourseIdAndRequiredTrueAndActiveTrue(
                                courseId);

        if (totalRequiredModuleCount <= 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Active required module is required");
        }

        long completedRequiredModuleCount =
                learningProgressRepository
                        .countByCourseEnrollment_CourseEnrollmentIdAndCourseModule_RequiredTrueAndCourseModule_ActiveTrueAndCompletionStatus(
                                enrollment.getCourseEnrollmentId(),
                                LearningCompletionStatus.COMPLETED);

        BigDecimal progressRate = calculateProgressRate(
                completedRequiredModuleCount,
                totalRequiredModuleCount);

        enrollment.updateProgressRate(progressRate);

        if (completedRequiredModuleCount
                >= totalRequiredModuleCount) {
            enrollment.complete(currentTime);
        }

        return LearningProgressUpdateResponse.from(
                progress,
                today);
    }

    private LearningProgress findProgress(Long progressId) {
        return learningProgressRepository
                .findByLearningProgressId(progressId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Learning progress not found"));
    }

    private BigDecimal calculateProgressRate(
            long completedCount,
            long totalCount
    ) {
        return BigDecimal.valueOf(completedCount)
                .multiply(new BigDecimal("100"))
                .divide(
                        BigDecimal.valueOf(totalCount),
                        2,
                        RoundingMode.HALF_UP);
    }

    private void validateOwnership(
            CourseEnrollment enrollment,
            Long employeeId
    ) {
        if (!Objects.equals(
                enrollment.getEmployeeId(),
                employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Learning progress belongs to another employee");
        }
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

    private void validateProgressId(Long progressId) {
        if (progressId == null || progressId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Progress ID must be a positive number");
        }
    }
}