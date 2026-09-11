package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAnswerRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptResultResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizAttemptSubmitRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
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

class QuizAttemptServiceTest {

    private QuizRepository quizRepository;
    private QuizQuestionRepository quizQuestionRepository;
    private QuizOptionRepository quizOptionRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private QuizResponseRepository quizResponseRepository;
    private QuizResponseOptionRepository quizResponseOptionRepository;
    private CourseEnrollmentRepository courseEnrollmentRepository;
    private CurrentUserProvider currentUserProvider;

    private QuizAttemptService service;

    private Course course;
    private CourseEnrollment enrollment;
    private Quiz quiz;
    private QuizQuestion question1;
    private QuizQuestion question2;
    private QuizOption correctOption1;
    private QuizOption correctOption2;
    private QuizOption wrongOption1;

    @BeforeEach
    void setUp() {
        quizRepository = mock(QuizRepository.class);
        quizQuestionRepository =
                mock(QuizQuestionRepository.class);
        quizOptionRepository =
                mock(QuizOptionRepository.class);
        quizAttemptRepository =
                mock(QuizAttemptRepository.class);
        quizResponseRepository =
                mock(QuizResponseRepository.class);
        quizResponseOptionRepository =
                mock(QuizResponseOptionRepository.class);
        courseEnrollmentRepository =
                mock(CourseEnrollmentRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-09-11T06:00:00Z"),
                ZoneId.of("Asia/Seoul"));

        service = new QuizAttemptService(
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository,
                quizAttemptRepository,
                quizResponseRepository,
                quizResponseOptionRepository,
                courseEnrollmentRepository,
                currentUserProvider,
                clock);

        course = mock(Course.class);
        enrollment = mock(CourseEnrollment.class);
        quiz = mock(Quiz.class);
        question1 = question(10L, "50.00");
        question2 = question(20L, "50.00");

        correctOption1 = option(101L, question1, true);
        correctOption2 = option(201L, question2, true);
        wrongOption1 = option(102L, question1, false);

        CurrentUserContext currentUser =
                mock(CurrentUserContext.class);

        when(currentUser.appUserId()).thenReturn(1L);
        when(currentUser.employeeId()).thenReturn(100L);
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser);

        when(course.getCourseId()).thenReturn(5L);
        when(enrollment.getEmployeeId()).thenReturn(100L);
        when(enrollment.getCourse()).thenReturn(course);

        when(quiz.getQuizId()).thenReturn(1L);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getPassingScore())
                .thenReturn(new BigDecimal("80.00"));
        when(quiz.getMaxAttemptCount()).thenReturn(3);

        when(courseEnrollmentRepository
                .findByCourseEnrollmentIdForUpdate(50L))
                .thenReturn(Optional.of(enrollment));

        when(quizRepository.findByQuizIdAndActiveTrue(1L))
                .thenReturn(Optional.of(quiz));

        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of(question1, question2));

        when(quizAttemptRepository.save(
                any(QuizAttempt.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(quizResponseRepository.save(
                any(QuizResponse.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(quizResponseOptionRepository.save(
                any(QuizResponseOption.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    @Test
    void gradesPassingAttempt() {
        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        correctOption1,
                        correctOption2));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.empty());

        QuizAttemptResultResponse result = service.submit(
                1L,
                request(101L, 201L));

        assertEquals(1, result.attemptNumber());
        assertEquals(
                new BigDecimal("100.00"),
                result.totalScore());
        assertTrue(result.passed());
        assertEquals(2, result.remainingAttemptCount());

        verify(quizResponseRepository, times(2))
                .save(any(QuizResponse.class));
        verify(quizResponseOptionRepository, times(2))
                .save(any(QuizResponseOption.class));
    }

    @Test
    void gradesFailingAttempt() {
        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        wrongOption1,
                        correctOption2));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.empty());

        QuizAttemptResultResponse result = service.submit(
                1L,
                request(102L, 201L));

        assertEquals(
                new BigDecimal("50.00"),
                result.totalScore());
        assertFalse(result.passed());
        assertEquals(2, result.remainingAttemptCount());
    }

    @Test
    void createsNextAttemptAfterFailure() {
        QuizAttempt previous = mock(QuizAttempt.class);

        when(previous.getAttemptNumber()).thenReturn(1);
        when(previous.getPassed()).thenReturn(false);

        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        correctOption1,
                        correctOption2));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(previous));

        QuizAttemptResultResponse result = service.submit(
                1L,
                request(101L, 201L));

        assertEquals(2, result.attemptNumber());
        assertEquals(1, result.remainingAttemptCount());
    }

    @Test
    void rejectsRetryAfterPassing() {
        QuizAttempt previous = mock(QuizAttempt.class);

        when(previous.getAttemptNumber()).thenReturn(1);
        when(previous.getPassed()).thenReturn(true);

        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        correctOption1,
                        correctOption2));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(previous));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(
                        1L,
                        request(101L, 201L)));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());

        verify(quizAttemptRepository, never())
                .save(any(QuizAttempt.class));
    }

    @Test
    void rejectsAttemptBeyondMaximum() {
        QuizAttempt previous = mock(QuizAttempt.class);

        when(previous.getAttemptNumber()).thenReturn(3);
        when(previous.getPassed()).thenReturn(false);

        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        correctOption1,
                        correctOption2));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(previous));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(
                        1L,
                        request(101L, 201L)));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void rejectsAnotherEmployeesEnrollment() {
        when(enrollment.getEmployeeId()).thenReturn(999L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(
                        1L,
                        request(101L, 201L)));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

        verify(quizRepository, never())
                .findByQuizIdAndActiveTrue(any());
    }

    @Test
    void rejectsMissingQuestionAnswer() {
        QuizAttemptSubmitRequest incomplete =
                new QuizAttemptSubmitRequest(
                        50L,
                        List.of(new QuizAnswerRequest(
                                10L,
                                101L)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(1L, incomplete));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                exception.getErrorCode());

        verify(quizAttemptRepository, never())
                .save(any(QuizAttempt.class));
    }

    @Test
    void rejectsOptionBelongingToAnotherQuestion() {
        when(quizOptionRepository.findAllByQuizOptionIdIn(any()))
                .thenReturn(List.of(
                        correctOption2,
                        correctOption1));

        QuizAttemptSubmitRequest request =
                new QuizAttemptSubmitRequest(
                        50L,
                        List.of(
                                new QuizAnswerRequest(10L, 201L),
                                new QuizAnswerRequest(20L, 101L)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(1L, request));

        assertEquals(
                ErrorCode.INVALID_REQUEST,
                exception.getErrorCode());

        verify(quizAttemptRepository, never())
                .save(any(QuizAttempt.class));
    }

    private QuizAttemptSubmitRequest request(
            Long firstOptionId,
            Long secondOptionId
    ) {
        return new QuizAttemptSubmitRequest(
                50L,
                List.of(
                        new QuizAnswerRequest(
                                10L,
                                firstOptionId),
                        new QuizAnswerRequest(
                                20L,
                                secondOptionId)));
    }

    private QuizQuestion question(
            Long questionId,
            String score
    ) {
        QuizQuestion question = mock(QuizQuestion.class);

        when(question.getQuizQuestionId())
                .thenReturn(questionId);
        when(question.getScore())
                .thenReturn(new BigDecimal(score));

        return question;
    }

    private QuizOption option(
            Long optionId,
            QuizQuestion question,
            boolean correct
    ) {
        QuizOption option = mock(QuizOption.class);

        when(option.getQuizOptionId()).thenReturn(optionId);
        when(option.getQuizQuestion()).thenReturn(question);
        when(option.isCorrect()).thenReturn(correct);

        return option;
    }
}