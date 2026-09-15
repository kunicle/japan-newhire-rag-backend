package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptReviewResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizDetailResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizQuestionResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizQuestionReviewResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizAttempt;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizResponseOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizAttemptRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizOptionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizResponseOptionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizResponseRepository;

@Service
@Transactional(readOnly = true)
public class QuizQueryService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final QuizResponseOptionRepository quizResponseOptionRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public QuizQueryService(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizOptionRepository quizOptionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizResponseRepository quizResponseRepository,
            QuizResponseOptionRepository quizResponseOptionRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizResponseRepository = quizResponseRepository;
        this.quizResponseOptionRepository = quizResponseOptionRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public QuizDetailResponse getQuiz(
            Long quizId,
            Long enrollmentId
    ) {
        validatePositiveId(quizId, "Quiz");
        validatePositiveId(enrollmentId, "Enrollment");

        Long employeeId = getCurrentEmployeeId();

        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByCourseEnrollmentId(enrollmentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course enrollment not found"));

        if (!Objects.equals(enrollment.getEmployeeId(), employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Course enrollment belongs to another employee");
        }

        Quiz quiz = quizRepository
                .findByQuizIdAndActiveTrue(quizId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Active quiz not found"));

        if (!Objects.equals(
                quiz.getCourse().getCourseId(),
                enrollment.getCourse().getCourseId())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Quiz is not available for this enrollment");
        }

        List<QuizQuestion> questions = quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        quizId);

        List<Long> questionIds = questions.stream()
                .map(QuizQuestion::getQuizQuestionId)
                .toList();

        List<QuizOption> options = questionIds.isEmpty()
                ? List.of()
                : quizOptionRepository
                        .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                                questionIds);

        Map<Long, List<QuizOption>> optionsByQuestion =
                options.stream().collect(Collectors.groupingBy(
                        option -> option.getQuizQuestion()
                                .getQuizQuestionId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<QuizQuestionResponse> questionResponses =
                questions.stream()
                        .map(question -> QuizQuestionResponse.from(
                                question,
                                optionsByQuestion.getOrDefault(
                                        question.getQuizQuestionId(),
                                        List.of())))
                        .toList();

        Optional<QuizAttempt> latestAttempt = quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        quizId,
                        employeeId,
                        enrollmentId);

        int attemptsUsed = latestAttempt
                .map(QuizAttempt::getAttemptNumber)
                .orElse(0);

        QuizAttemptReviewResponse latestAttemptReview = latestAttempt
                .filter(attempt -> isReviewAvailable(quiz, attempt))
                .map(attempt -> buildAttemptReview(quiz, attempt))
                .orElse(null);

        return QuizDetailResponse.from(
                quiz,
                attemptsUsed,
                questionResponses,
                latestAttemptReview);
    }

    private boolean isReviewAvailable(
            Quiz quiz,
            QuizAttempt attempt
    ) {
        if (attempt.getPassed() == null) {
            return false;
        }

        if (Boolean.TRUE.equals(attempt.getPassed())) {
            return true;
        }

        Integer maxAttemptCount = quiz.getMaxAttemptCount();

        return maxAttemptCount != null
                && attempt.getAttemptNumber() >= maxAttemptCount;
    }

    private QuizAttemptReviewResponse buildAttemptReview(
            Quiz quiz,
            QuizAttempt attempt
    ) {
        Long attemptId = attempt.getQuizAttemptId();

        List<QuizResponse> responses = quizResponseRepository
                .findAllByQuizAttempt_QuizAttemptIdOrderByQuizQuestion_QuestionOrderAsc(
                        attemptId);

        Map<Long, QuizOption> selectedOptionByResponseId =
                quizResponseOptionRepository
                        .findAllByQuizResponse_QuizAttempt_QuizAttemptId(
                                attemptId)
                        .stream()
                        .collect(Collectors.toMap(
                                responseOption -> responseOption
                                        .getQuizResponse()
                                        .getQuizResponseId(),
                                QuizResponseOption::getQuizOption,
                                (first, ignored) -> first,
                                LinkedHashMap::new));

        List<Long> reviewQuestionIds = responses.stream()
                .map(response -> response.getQuizQuestion()
                        .getQuizQuestionId())
                .toList();

        Map<Long, List<QuizOption>> reviewOptionsByQuestion =
                reviewQuestionIds.isEmpty()
                        ? Map.of()
                        : quizOptionRepository
                                .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                                        reviewQuestionIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        option -> option.getQuizQuestion()
                                                .getQuizQuestionId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<QuizQuestionReviewResponse> questionReviews =
                responses.stream()
                        .map(response -> {
                            Long questionId = response
                                    .getQuizQuestion()
                                    .getQuizQuestionId();

                            QuizOption selectedOption = Optional
                                    .ofNullable(
                                            selectedOptionByResponseId.get(
                                                    response.getQuizResponseId()))
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Selected quiz option is missing"));

                            QuizOption correctOption =
                                    reviewOptionsByQuestion
                                            .getOrDefault(
                                                    questionId,
                                                    List.of())
                                            .stream()
                                            .filter(QuizOption::isCorrect)
                                            .findFirst()
                                            .orElseThrow(() ->
                                                    new IllegalStateException(
                                                            "Correct quiz option is missing"));

                            return QuizQuestionReviewResponse.from(
                                    response,
                                    selectedOption,
                                    correctOption);
                        })
                        .toList();

        Integer remainingAttemptCount =
                quiz.getMaxAttemptCount() == null
                        ? null
                        : Math.max(
                                quiz.getMaxAttemptCount()
                                        - attempt.getAttemptNumber(),
                                0);

        return QuizAttemptReviewResponse.from(
                attempt,
                remainingAttemptCount,
                questionReviews);
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

    private void validatePositiveId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    name + " ID must be a positive number");
        }
    }
}