package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.HrOxQuizResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizQuestionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizActivationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizOptionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizRepository;

@Service
public class HrQuizManagementService {

    private static final BigDecimal TOTAL_SCORE =
            new BigDecimal("100.00");

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final CurrentUserProvider currentUserProvider;

    public HrQuizManagementService(
            CourseRepository courseRepository,
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository,
            QuizOptionRepository quizOptionRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public HrOxQuizResponse createCourseQuiz(
            Long courseId,
            OxQuizCreateRequest request
    ) {
        validatePositiveId(courseId, "Course");
        CurrentUserContext currentUser = validateCurrentHrManager();

        Course course = courseRepository
                .findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));

        validateTotalScore(request.questions());

        Quiz quiz = quizRepository.save(
                Quiz.create(
                        course,
                        null,
                        request.quizTitle().trim(),
                        request.passingScore(),
                        request.maxAttemptCount(),
                        currentUser.appUserId()));

        List<HrOxQuizResponse.Question> questionResponses =
                new ArrayList<>();

        for (int index = 0;
                index < request.questions().size();
                index++) {
            OxQuizQuestionRequest questionRequest =
                    request.questions().get(index);

            String correctAnswer =
                    validateAndNormalizeAnswer(
                            questionRequest.correctAnswer());

            QuizQuestion question = quizQuestionRepository.save(
                    QuizQuestion.create(
                            quiz,
                            questionRequest.questionContent().trim(),
                            index + 1,
                            questionRequest.score()));

            quizOptionRepository.saveAll(List.of(
                    QuizOption.create(
                            question,
                            "O",
                            1,
                            "O".equals(correctAnswer)),
                    QuizOption.create(
                            question,
                            "X",
                            2,
                            "X".equals(correctAnswer))));

            questionResponses.add(
                    new HrOxQuizResponse.Question(
                            question.getQuizQuestionId(),
                            question.getQuestionContent(),
                            question.getQuestionOrder(),
                            question.getScore(),
                            correctAnswer));
        }

        return HrOxQuizResponse.from(
                quiz,
                questionResponses);
    }

        @Transactional(readOnly = true)
        public List<HrOxQuizResponse> getCourseQuizzes(
                Long courseId
        ) {
        validatePositiveId(courseId, "Course");
        validateCurrentHrManager();
        findCourse(courseId);

        return quizRepository
                .findAllByCourse_CourseIdOrderByCreatedAtDescQuizIdDesc(
                        courseId)
                .stream()
                .map(this::toResponse)
                .toList();
        }

                @Transactional(readOnly = true)
                public HrOxQuizResponse getCourseQuiz(
                        Long courseId,
                        Long quizId
                ) {
                validatePositiveId(courseId, "Course");
                validatePositiveId(quizId, "Quiz");
                validateCurrentHrManager();
                findCourse(courseId);

                Quiz quiz = quizRepository
                        .findByQuizIdAndCourse_CourseId(
                                quizId,
                                courseId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Quiz not found"));

                return toResponse(quiz);
                }


        @Transactional
        public HrOxQuizResponse changeCourseQuizActivation(
                Long courseId,
                Long quizId,
                QuizActivationUpdateRequest request
        ) {
        validatePositiveId(courseId, "Course");
        validatePositiveId(quizId, "Quiz");
        validateCurrentHrManager();
        findCourse(courseId);

        Quiz quiz = quizRepository
                .findByQuizIdAndCourse_CourseId(quizId, courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Quiz not found"));

        quiz.changeActive(request.active());

        return toResponse(quiz);
        }

    private void validateTotalScore(
            List<OxQuizQuestionRequest> questions
    ) {
        BigDecimal total = questions.stream()
                .map(OxQuizQuestionRequest::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(TOTAL_SCORE) != 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "The total question score must be 100");
        }
    }

    private String validateAndNormalizeAnswer(String answer) {
        if (answer == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Correct answer must be O or X");
        }

        String normalized = answer.trim().toUpperCase();

        if (!"O".equals(normalized)
                && !"X".equals(normalized)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Correct answer must be O or X");
        }

        return normalized;
    }

    private CurrentUserContext validateCurrentHrManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!currentUser.roles()
                .contains(RoleType.HR_MANAGER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return currentUser;
    }

        private void validatePositiveId(
                Long id,
                String name
        ) {
        if (id == null || id <= 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        name + " ID must be a positive number");
        }
        }

        private Course findCourse(Long courseId) {
        return courseRepository
                .findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Course not found"));
        }

        private HrOxQuizResponse toResponse(Quiz quiz) {
        List<QuizQuestion> questions =
                quizQuestionRepository
                        .findAllByQuiz_QuizIdOrderByQuestionOrderAsc(
                                quiz.getQuizId());

        List<Long> questionIds = questions.stream()
                .map(QuizQuestion::getQuizQuestionId)
                .toList();

        List<QuizOption> options = questionIds.isEmpty()
                ? List.of()
                : quizOptionRepository
                        .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                                questionIds);

        List<HrOxQuizResponse.Question> questionResponses =
                questions.stream()
                        .map(question -> {
                                List<QuizOption> correctOptions =
                                        options.stream()
                                                .filter(option ->
                                                        option.isCorrect()
                                                        && option
                                                                .getQuizQuestion()
                                                                .getQuizQuestionId()
                                                                .equals(question
                                                                        .getQuizQuestionId()))
                                                .toList();

                                if (correctOptions.size() != 1) {
                                throw new BusinessException(
                                        ErrorCode.CONFLICT,
                                        "Quiz question must have exactly one correct option");
                                }

                                return new HrOxQuizResponse.Question(
                                        question.getQuizQuestionId(),
                                        question.getQuestionContent(),
                                        question.getQuestionOrder(),
                                        question.getScore(),
                                        correctOptions.get(0)
                                                .getOptionContent());
                        })
                        .toList();

        return HrOxQuizResponse.from(
                quiz,
                questionResponses);
        }

}