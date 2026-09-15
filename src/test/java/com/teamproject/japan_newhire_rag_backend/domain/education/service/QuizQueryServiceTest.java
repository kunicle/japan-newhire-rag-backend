package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizDetailResponse;
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

class QuizQueryServiceTest {

    private QuizRepository quizRepository;
    private QuizQuestionRepository quizQuestionRepository;
    private QuizOptionRepository quizOptionRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private QuizResponseRepository quizResponseRepository;
    private QuizResponseOptionRepository quizResponseOptionRepository;
    private CourseEnrollmentRepository courseEnrollmentRepository;
    private CurrentUserProvider currentUserProvider;
    private QuizQueryService service;

    private Course course;
    private CourseEnrollment enrollment;
    private Quiz quiz;

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

        service = new QuizQueryService(
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository,
                quizAttemptRepository,
                quizResponseRepository,
                quizResponseOptionRepository,
                courseEnrollmentRepository,
                currentUserProvider);

        CurrentUserContext currentUser =
                mock(CurrentUserContext.class);
        when(currentUser.appUserId()).thenReturn(1L);
        when(currentUser.employeeId()).thenReturn(100L);
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser);

        course = mock(Course.class);
        enrollment = mock(CourseEnrollment.class);
        quiz = mock(Quiz.class);

        when(course.getCourseId()).thenReturn(5L);
        when(enrollment.getEmployeeId()).thenReturn(100L);
        when(enrollment.getCourse()).thenReturn(course);

        when(quiz.getQuizId()).thenReturn(1L);
        when(quiz.getCourse()).thenReturn(course);
        when(quiz.getQuizTitle()).thenReturn("Basic quiz");
        when(quiz.getPassingScore())
                .thenReturn(new BigDecimal("80.00"));
        when(quiz.getMaxAttemptCount()).thenReturn(3);

        when(courseEnrollmentRepository
                .findByCourseEnrollmentId(50L))
                .thenReturn(Optional.of(enrollment));
        when(quizRepository.findByQuizIdAndActiveTrue(1L))
                .thenReturn(Optional.of(quiz));
    }

    @Test
    void getsAssignedQuizWithQuestionsAndOptions() {
        QuizQuestion question = mock(QuizQuestion.class);
        QuizOption option = mock(QuizOption.class);
        QuizAttempt previous = mock(QuizAttempt.class);

        when(question.getQuizQuestionId()).thenReturn(10L);
        when(question.getQuestionContent())
                .thenReturn("Which option is correct?");
        when(question.getQuestionOrder()).thenReturn(1);
        when(question.getScore())
                .thenReturn(new BigDecimal("100.00"));

        when(option.getQuizOptionId()).thenReturn(101L);
        when(option.getQuizQuestion()).thenReturn(question);
        when(option.getOptionContent()).thenReturn("Option A");
        when(option.getOptionOrder()).thenReturn(1);

        when(previous.getAttemptNumber()).thenReturn(1);

        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of(question));

        when(quizOptionRepository
                .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                        List.of(10L)))
                .thenReturn(List.of(option));

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(previous));

        QuizDetailResponse result =
                service.getQuiz(1L, 50L);

        assertEquals(1L, result.quizId());
        assertEquals(5L, result.courseId());
        assertEquals("Basic quiz", result.quizTitle());
        assertEquals(1, result.attemptsUsed());
        assertEquals(1, result.questions().size());
        assertEquals(10L, result.questions().get(0).questionId());
        assertEquals(1, result.questions().get(0).options().size());
        assertEquals(
                101L,
                result.questions().get(0)
                        .options().get(0).optionId());
    }

    @Test
    void returnsQuizWithoutQuestions() {
        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of());

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.empty());

        QuizDetailResponse result =
                service.getQuiz(1L, 50L);

        assertTrue(result.questions().isEmpty());
        assertEquals(0, result.attemptsUsed());

        verify(quizOptionRepository, never())
                .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                        List.of());
    }

    @Test
    void hidesLatestAttemptReviewWhileRetryIsAvailable() {
        QuizAttempt attempt = mock(QuizAttempt.class);

        when(attempt.getAttemptNumber()).thenReturn(2);
        when(attempt.getPassed()).thenReturn(false);

        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of());

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(attempt));

        QuizDetailResponse result =
                service.getQuiz(1L, 50L);

        assertEquals(2, result.attemptsUsed());
        assertNull(result.latestAttemptReview());

        verify(quizResponseRepository, never())
                .findAllByQuizAttempt_QuizAttemptIdOrderByQuizQuestion_QuestionOrderAsc(
                        attempt.getQuizAttemptId());
        verify(quizResponseOptionRepository, never())
                .findAllByQuizResponse_QuizAttempt_QuizAttemptId(
                        attempt.getQuizAttemptId());
    }

    @Test
    void exposesLatestAttemptReviewAfterPassing() {
        QuizAttempt attempt = mock(QuizAttempt.class);

        when(attempt.getQuizAttemptId()).thenReturn(900L);
        when(attempt.getAttemptNumber()).thenReturn(1);
        when(attempt.getPassed()).thenReturn(true);
        when(attempt.getTotalScore())
                .thenReturn(new BigDecimal("100.00"));
        when(attempt.getSubmittedAt())
                .thenReturn(LocalDateTime.of(
                        2026, 9, 15, 10, 30));

        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of());

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(attempt));

        when(quizResponseRepository
                .findAllByQuizAttempt_QuizAttemptIdOrderByQuizQuestion_QuestionOrderAsc(
                        900L))
                .thenReturn(List.of());

        when(quizResponseOptionRepository
                .findAllByQuizResponse_QuizAttempt_QuizAttemptId(
                        900L))
                .thenReturn(List.of());

        QuizDetailResponse result =
                service.getQuiz(1L, 50L);

        assertNotNull(result.latestAttemptReview());
        assertTrue(result.latestAttemptReview().passed());
        assertEquals(
                new BigDecimal("100.00"),
                result.latestAttemptReview().totalScore());
        assertEquals(
                2,
                result.latestAttemptReview()
                        .remainingAttemptCount());
    }

    @Test
    void exposesQuestionAndCorrectAnswerAfterAttemptsAreExhausted() {
        QuizAttempt attempt = mock(QuizAttempt.class);
        QuizQuestion question = mock(QuizQuestion.class);
        QuizResponse response = mock(QuizResponse.class);
        QuizResponseOption responseOption =
                mock(QuizResponseOption.class);
        QuizOption selectedOption = mock(QuizOption.class);
        QuizOption correctOption = mock(QuizOption.class);

        when(attempt.getQuizAttemptId()).thenReturn(900L);
        when(attempt.getAttemptNumber()).thenReturn(3);
        when(attempt.getPassed()).thenReturn(false);
        when(attempt.getTotalScore())
                .thenReturn(new BigDecimal("0.00"));
        when(attempt.getSubmittedAt())
                .thenReturn(LocalDateTime.of(
                        2026, 9, 15, 10, 30));

        when(question.getQuizQuestionId()).thenReturn(10L);
        when(question.getQuestionContent())
                .thenReturn("Which option is correct?");
        when(question.getQuestionOrder()).thenReturn(1);
        when(question.getScore())
                .thenReturn(new BigDecimal("100.00"));

        when(response.getQuizResponseId()).thenReturn(700L);
        when(response.getQuizQuestion()).thenReturn(question);
        when(response.getCorrect()).thenReturn(false);
        when(response.getEarnedScore())
                .thenReturn(new BigDecimal("0.00"));

        when(responseOption.getQuizResponse())
                .thenReturn(response);
        when(responseOption.getQuizOption())
                .thenReturn(selectedOption);

        when(selectedOption.getQuizOptionId()).thenReturn(101L);
        when(selectedOption.getQuizQuestion()).thenReturn(question);
        when(selectedOption.getOptionContent())
                .thenReturn("Wrong answer");
        when(selectedOption.isCorrect()).thenReturn(false);

        when(correctOption.getQuizOptionId()).thenReturn(102L);
        when(correctOption.getQuizQuestion()).thenReturn(question);
        when(correctOption.getOptionContent())
                .thenReturn("Correct answer");
        when(correctOption.isCorrect()).thenReturn(true);

        when(quizQuestionRepository
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L))
                .thenReturn(List.of());

        when(quizAttemptRepository
                .findTopByQuiz_QuizIdAndEmployeeIdAndCourseEnrollment_CourseEnrollmentIdOrderByAttemptNumberDesc(
                        1L, 100L, 50L))
                .thenReturn(Optional.of(attempt));

        when(quizResponseRepository
                .findAllByQuizAttempt_QuizAttemptIdOrderByQuizQuestion_QuestionOrderAsc(
                        900L))
                .thenReturn(List.of(response));

        when(quizResponseOptionRepository
                .findAllByQuizResponse_QuizAttempt_QuizAttemptId(
                        900L))
                .thenReturn(List.of(responseOption));

        when(quizOptionRepository
                .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                        List.of(10L)))
                .thenReturn(List.of(
                        selectedOption,
                        correctOption));

        QuizDetailResponse result =
                service.getQuiz(1L, 50L);

        assertNotNull(result.latestAttemptReview());
        assertFalse(result.latestAttemptReview().passed());
        assertEquals(
                0,
                result.latestAttemptReview()
                        .remainingAttemptCount());
        assertEquals(
                1,
                result.latestAttemptReview().questions().size());

        var questionReview =
                result.latestAttemptReview().questions().get(0);

        assertEquals(
                "Which option is correct?",
                questionReview.questionContent());
        assertEquals(
                101L,
                questionReview.selectedOptionId());
        assertEquals(
                "Wrong answer",
                questionReview.selectedOptionContent());
        assertEquals(
                102L,
                questionReview.correctOptionId());
        assertEquals(
                "Correct answer",
                questionReview.correctOptionContent());
        assertFalse(questionReview.correct());
        assertEquals(
                new BigDecimal("0.00"),
                questionReview.earnedScore());
    }

    @Test
    void rejectsAnotherEmployeesEnrollment() {
        when(enrollment.getEmployeeId()).thenReturn(999L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getQuiz(1L, 50L));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

        verify(quizRepository, never())
                .findByQuizIdAndActiveTrue(1L);
    }

    @Test
    void rejectsQuizFromAnotherCourse() {
        Course anotherCourse = mock(Course.class);
        when(anotherCourse.getCourseId()).thenReturn(999L);
        when(quiz.getCourse()).thenReturn(anotherCourse);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getQuiz(1L, 50L));

        assertEquals(
                ErrorCode.RESOURCE_NOT_FOUND,
                exception.getErrorCode());

        verify(quizQuestionRepository, never())
                .findAllByQuiz_QuizIdAndActiveTrueOrderByQuestionOrderAsc(
                        1L);
    }
}