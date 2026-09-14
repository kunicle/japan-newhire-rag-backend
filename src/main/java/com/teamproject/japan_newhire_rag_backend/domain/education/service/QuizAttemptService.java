package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAnswerRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptResultResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptSubmitRequest;
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
@Transactional
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final QuizResponseOptionRepository quizResponseOptionRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public QuizAttemptService(
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizOptionRepository quizOptionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizResponseRepository quizResponseRepository,
            QuizResponseOptionRepository quizResponseOptionRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizResponseRepository = quizResponseRepository;
        this.quizResponseOptionRepository =
                quizResponseOptionRepository;
        this.courseEnrollmentRepository =
                courseEnrollmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public QuizAttemptResultResponse submit(
            Long quizId,
            QuizAttemptSubmitRequest request
    ) {
        validatePositiveId(quizId, "Quiz");
        validateRequest(request);

        Long employeeId = getCurrentEmployeeId();

        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByCourseEnrollmentIdForUpdate(
                        request.enrollmentId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course enrollment not found"));

        validateEnrollmentOwner(enrollment, employeeId);

        Quiz quiz = quizRepository
                .findByQuizIdAndActiveTrue(quizId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Active quiz not found"));

        validateQuizCourse(quiz, enrollment);

        List<QuizQuestion> questions = quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        quizId);

        if (questions.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Quiz has no active questions");
        }

        Map<Long, Long> selectedOptionByQuestion =
                validateAndMapAnswers(request.answers(), questions);

        Map<Long, QuizOption> selectedOptions =
                loadAndValidateOptions(selectedOptionByQuestion);

        Optional<QuizAttempt> previousAttempt =
                quizAttemptRepository
                        .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                                quizId,
                                employeeId,
                                request.enrollmentId());

        int nextAttemptNumber = determineNextAttemptNumber(
                quiz,
                previousAttempt);

        BigDecimal totalScore = calculateScore(
                questions,
                selectedOptionByQuestion,
                selectedOptions);

        boolean passed =
                totalScore.compareTo(quiz.getPassingScore()) >= 0;

        LocalDateTime now = LocalDateTime.now(clock);

        QuizAttempt attempt = QuizAttempt.start(
                quiz,
                employeeId,
                enrollment,
                nextAttemptNumber,
                now);

        attempt.grade(totalScore, passed, now);
        QuizAttempt savedAttempt =
                quizAttemptRepository.save(attempt);

        saveResponses(
                savedAttempt,
                questions,
                selectedOptionByQuestion,
                selectedOptions);

        return QuizAttemptResultResponse.from(
                savedAttempt,
                calculateRemainingAttempts(
                        quiz,
                        nextAttemptNumber));
    }

    private Map<Long, Long> validateAndMapAnswers(
            List<QuizAnswerRequest> answers,
            List<QuizQuestion> questions
    ) {
        Set<Long> expectedQuestionIds = new HashSet<>();

        for (QuizQuestion question : questions) {
            expectedQuestionIds.add(
                    question.getQuizQuestionId());
        }

        Map<Long, Long> selectedOptionByQuestion =
                new LinkedHashMap<>();

        for (QuizAnswerRequest answer : answers) {
            if (answer == null
                    || answer.questionId() == null
                    || answer.questionId() <= 0
                    || answer.optionId() == null
                    || answer.optionId() <= 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Question and option IDs must be positive");
            }

            Long previous = selectedOptionByQuestion.put(
                    answer.questionId(),
                    answer.optionId());

            if (previous != null) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Each question must have exactly one answer");
            }
        }

        if (!selectedOptionByQuestion.keySet()
                .equals(expectedQuestionIds)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Answers must cover every active question exactly once");
        }

        return selectedOptionByQuestion;
    }

    private Map<Long, QuizOption> loadAndValidateOptions(
            Map<Long, Long> selectedOptionByQuestion
    ) {
        List<QuizOption> options = quizOptionRepository
                .findAllByQuizOptionIdIn(
                        new HashSet<>(
                                selectedOptionByQuestion.values()));

        Map<Long, QuizOption> optionsById = new HashMap<>();

        for (QuizOption option : options) {
            optionsById.put(option.getQuizOptionId(), option);
        }

        if (optionsById.size()
                != new HashSet<>(
                        selectedOptionByQuestion.values()).size()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "One or more selected options do not exist");
        }

        for (Map.Entry<Long, Long> answer
                : selectedOptionByQuestion.entrySet()) {
            QuizOption option = optionsById.get(answer.getValue());

            if (option == null
                    || !Objects.equals(
                            option.getQuizQuestion()
                                    .getQuizQuestionId(),
                            answer.getKey())) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Selected option does not belong to the question");
            }
        }

        return optionsById;
    }

    private BigDecimal calculateScore(
            List<QuizQuestion> questions,
            Map<Long, Long> selectedOptionByQuestion,
            Map<Long, QuizOption> optionsById
    ) {
        BigDecimal totalScore = BigDecimal.ZERO;

        for (QuizQuestion question : questions) {
            Long optionId = selectedOptionByQuestion.get(
                    question.getQuizQuestionId());
            QuizOption option = optionsById.get(optionId);

            if (option.isCorrect()) {
                totalScore = totalScore.add(
                        question.getScore());
            }
        }

        return totalScore.setScale(2, RoundingMode.HALF_UP);
    }

    private void saveResponses(
            QuizAttempt attempt,
            List<QuizQuestion> questions,
            Map<Long, Long> selectedOptionByQuestion,
            Map<Long, QuizOption> optionsById
    ) {
        for (QuizQuestion question : questions) {
            QuizOption selectedOption = optionsById.get(
                    selectedOptionByQuestion.get(
                            question.getQuizQuestionId()));

            boolean correct = selectedOption.isCorrect();

            BigDecimal earnedScore = correct
                    ? question.getScore()
                            .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP);

            QuizResponse response =
                    quizResponseRepository.save(
                            QuizResponse.createGraded(
                                    attempt,
                                    question,
                                    correct,
                                    earnedScore));

            quizResponseOptionRepository.save(
                    QuizResponseOption.create(
                            response,
                            selectedOption));
        }
    }

    private int determineNextAttemptNumber(
            Quiz quiz,
            Optional<QuizAttempt> previousAttempt
    ) {
        if (previousAttempt.isEmpty()) {
            return 1;
        }

        QuizAttempt previous = previousAttempt.get();

        if (Boolean.TRUE.equals(previous.getPassed())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Quiz has already been passed");
        }

        int nextAttemptNumber =
                previous.getAttemptNumber() + 1;

        if (quiz.getMaxAttemptCount() != null
                && nextAttemptNumber
                        > quiz.getMaxAttemptCount()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Maximum quiz attempt count exceeded");
        }

        return nextAttemptNumber;
    }

    private Integer calculateRemainingAttempts(
            Quiz quiz,
            int attemptNumber
    ) {
        if (quiz.getMaxAttemptCount() == null) {
            return null;
        }

        return Math.max(
                quiz.getMaxAttemptCount() - attemptNumber,
                0);
    }

    private void validateEnrollmentOwner(
            CourseEnrollment enrollment,
            Long employeeId
    ) {
        if (!Objects.equals(
                enrollment.getEmployeeId(),
                employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Course enrollment belongs to another employee");
        }
    }

    private void validateQuizCourse(
            Quiz quiz,
            CourseEnrollment enrollment
    ) {
        if (!Objects.equals(
                quiz.getCourse().getCourseId(),
                enrollment.getCourse().getCourseId())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Quiz is not available for this enrollment");
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

    private void validateRequest(
            QuizAttemptSubmitRequest request
    ) {
        if (request == null
                || request.enrollmentId() == null
                || request.enrollmentId() <= 0
                || request.answers() == null
                || request.answers().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Enrollment and answers are required");
        }
    }

    private void validatePositiveId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    name + " ID must be a positive number");
        }
    }
}