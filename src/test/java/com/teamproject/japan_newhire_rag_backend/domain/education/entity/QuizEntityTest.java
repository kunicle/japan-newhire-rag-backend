package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.education.enums.QuizAttemptStatus;

class QuizEntityTest {

    @Test
    void createsActiveQuiz() {
        Course course = mock(Course.class);
        CourseModule module = mock(CourseModule.class);

        Quiz quiz = Quiz.create(
                course,
                module,
                "Basic rules quiz",
                new BigDecimal("80.00"),
                3,
                10L);

        assertSame(course, quiz.getCourse());
        assertSame(module, quiz.getCourseModule());
        assertEquals("Basic rules quiz", quiz.getQuizTitle());
        assertEquals(
                new BigDecimal("80.00"),
                quiz.getPassingScore());
        assertEquals(3, quiz.getMaxAttemptCount());
        assertEquals(10L, quiz.getCreatedBy());
        assertTrue(quiz.isActive());
    }

    @Test
    void changesQuizActiveStatus() {
        Quiz quiz = Quiz.create(
                mock(Course.class),
                null,
                "Quiz",
                new BigDecimal("60.00"),
                null,
                10L);

        quiz.changeActive(false);

        assertFalse(quiz.isActive());
    }

    @Test
    void createsQuestionAndOption() {
        Quiz quiz = mock(Quiz.class);

        QuizQuestion question = QuizQuestion.create(
                quiz,
                "Which option is correct?",
                1,
                new BigDecimal("20.00"));

        QuizOption option = QuizOption.create(
                question,
                "Option A",
                1,
                true);

        assertSame(quiz, question.getQuiz());
        assertEquals(1, question.getQuestionOrder());
        assertTrue(question.isActive());

        assertSame(question, option.getQuizQuestion());
        assertEquals("Option A", option.getOptionContent());
        assertTrue(option.isCorrect());
    }

    @Test
    void startsQuizAttempt() {
        Quiz quiz = mock(Quiz.class);
        CourseEnrollment enrollment =
                mock(CourseEnrollment.class);
        LocalDateTime startedAt =
                LocalDateTime.of(2026, 9, 11, 15, 0);

        QuizAttempt attempt = QuizAttempt.start(
                quiz,
                100L,
                enrollment,
                1,
                startedAt);

        assertSame(quiz, attempt.getQuiz());
        assertSame(enrollment, attempt.getCourseEnrollment());
        assertEquals(100L, attempt.getEmployeeId());
        assertEquals(1, attempt.getAttemptNumber());
        assertEquals(
                QuizAttemptStatus.IN_PROGRESS,
                attempt.getAttemptStatus());
        assertEquals(startedAt, attempt.getStartedAt());
        assertNull(attempt.getTotalScore());
        assertNull(attempt.getPassed());
        assertNull(attempt.getSubmittedAt());
    }

    @Test
    void gradesQuizAttempt() {
        QuizAttempt attempt = QuizAttempt.start(
                mock(Quiz.class),
                100L,
                mock(CourseEnrollment.class),
                1,
                LocalDateTime.of(2026, 9, 11, 15, 0));

        LocalDateTime submittedAt =
                LocalDateTime.of(2026, 9, 11, 15, 10);

        attempt.grade(
                new BigDecimal("80.00"),
                true,
                submittedAt);

        assertEquals(
                new BigDecimal("80.00"),
                attempt.getTotalScore());
        assertTrue(attempt.getPassed());
        assertEquals(
                QuizAttemptStatus.GRADED,
                attempt.getAttemptStatus());
        assertEquals(submittedAt, attempt.getSubmittedAt());
    }

    @Test
    void createsGradedResponseAndSelectedOption() {
        QuizAttempt attempt = mock(QuizAttempt.class);
        QuizQuestion question = mock(QuizQuestion.class);
        QuizOption option = mock(QuizOption.class);

        QuizResponse response = QuizResponse.createGraded(
                attempt,
                question,
                true,
                new BigDecimal("20.00"));

        QuizResponseOption selected =
                QuizResponseOption.create(response, option);

        assertSame(attempt, response.getQuizAttempt());
        assertSame(question, response.getQuizQuestion());
        assertTrue(response.getCorrect());
        assertEquals(
                new BigDecimal("20.00"),
                response.getEarnedScore());

        assertSame(response, selected.getQuizResponse());
        assertSame(option, selected.getQuizOption());
    }
}