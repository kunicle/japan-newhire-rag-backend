package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.HrOxQuizResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.OxQuizQuestionRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Quiz;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizOption;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.QuizQuestion;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizOptionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.QuizRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.QuizActivationUpdateRequest;

@ExtendWith(MockitoExtension.class)
class HrQuizManagementServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private QuizOptionRepository quizOptionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private HrQuizManagementService service;

    @BeforeEach
    void setUp() {
        service = new HrQuizManagementService(
                courseRepository,
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository,
                currentUserProvider);
    }

    @Test
    void hrManagerCreatesCourseOxQuiz() {
        Course course = course(10L);
        List<QuizOption> savedOptions = new ArrayList<>();
        AtomicLong questionId = new AtomicLong(300L);

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class)))
                .thenAnswer(invocation -> {
                    Quiz quiz = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            quiz,
                            "quizId",
                            200L);
                    return quiz;
                });
        when(quizQuestionRepository.save(any(QuizQuestion.class)))
                .thenAnswer(invocation -> {
                    QuizQuestion question = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            question,
                            "quizQuestionId",
                            questionId.getAndIncrement());
                    return question;
                });
        when(quizOptionRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    Iterable<QuizOption> options =
                            invocation.getArgument(0);

                    List<QuizOption> saved =
                            StreamSupport.stream(
                                    options.spliterator(),
                                    false)
                                    .toList();

                    savedOptions.addAll(saved);
                    return saved;
                });

        HrOxQuizResponse response =
                service.createCourseQuiz(10L, request());

        ArgumentCaptor<Quiz> quizCaptor =
                ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(quizCaptor.capture());

        Quiz savedQuiz = quizCaptor.getValue();

        assertThat(savedQuiz.getCourse()).isSameAs(course);
        assertThat(savedQuiz.getCourseModule()).isNull();
        assertThat(savedQuiz.getQuizTitle())
                .isEqualTo("Security basics");
        assertThat(savedQuiz.getPassingScore())
                .isEqualByComparingTo("80.00");
        assertThat(savedQuiz.getMaxAttemptCount()).isEqualTo(3);
        assertThat(savedQuiz.isActive()).isTrue();
        assertThat(savedQuiz.getCreatedBy()).isEqualTo(7L);

        ArgumentCaptor<QuizQuestion> questionCaptor =
                ArgumentCaptor.forClass(QuizQuestion.class);
        verify(quizQuestionRepository, times(2))
                .save(questionCaptor.capture());

        List<QuizQuestion> savedQuestions =
                questionCaptor.getAllValues();

        assertThat(savedQuestions)
                .extracting(QuizQuestion::getQuestionOrder)
                .containsExactly(1, 2);
        assertThat(savedQuestions)
                .extracting(QuizQuestion::getQuestionContent)
                .containsExactly(
                        "Passwords may be shared with coworkers.",
                        "Suspicious emails must be reported.");
        assertThat(savedQuestions)
                .extracting(QuizQuestion::getScore)
                .containsExactly(
                        new BigDecimal("50.00"),
                        new BigDecimal("50.00"));

        verify(quizOptionRepository, times(2))
                .saveAll(any());

        assertThat(savedOptions)
                .extracting(QuizOption::getOptionContent)
                .containsExactly("O", "X", "O", "X");
        assertThat(savedOptions)
                .filteredOn(QuizOption::isCorrect)
                .extracting(QuizOption::getOptionContent)
                .containsExactly("X", "O");
        assertThat(savedOptions)
                .extracting(QuizOption::getOptionOrder)
                .containsExactly(1, 2, 1, 2);

        assertThat(response.quizId()).isEqualTo(200L);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.createdBy()).isEqualTo(7L);
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions())
                .extracting(HrOxQuizResponse.Question::correctAnswer)
                .containsExactly("X", "O");
    }

    @Test
    void totalQuestionScoreMustEqualOneHundred() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L)));

        OxQuizCreateRequest invalidRequest =
                new OxQuizCreateRequest(
                        "Security basics",
                        new BigDecimal("80.00"),
                        3,
                        List.of(
                                question(
                                        "Question one",
                                        "40.00",
                                        "O"),
                                question(
                                        "Question two",
                                        "50.00",
                                        "X")));

        assertThatThrownBy(() ->
                service.createCourseQuiz(10L, invalidRequest))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.INVALID_REQUEST));

        verify(quizRepository, never()).save(any());
        verifyNoInteractions(
                quizQuestionRepository,
                quizOptionRepository);
    }

    @Test
    void missingCourseReturnsNotFound() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createCourseQuiz(999L, request()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.RESOURCE_NOT_FOUND));

        verify(quizRepository, never()).save(any());
        verifyNoInteractions(
                quizQuestionRepository,
                quizOptionRepository);
    }

    @Test
    void employeeCannotCreateQuiz() {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() ->
                service.createCourseQuiz(10L, request()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(
                courseRepository,
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository);
    }

    @Test
    void unauthenticatedUserCannotCreateQuiz() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(
                        new AuthenticationCredentialsNotFoundException(
                                "Authentication is required"));

        assertThatThrownBy(() ->
                service.createCourseQuiz(10L, request()))
                .isInstanceOf(
                        AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(
                courseRepository,
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository);
    }

    @Test
    void invalidCourseIdIsRejectedBeforeAuthentication() {
        assertThatThrownBy(() ->
                service.createCourseQuiz(0L, request()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(
                                exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(
                currentUserProvider,
                courseRepository,
                quizRepository,
                quizQuestionRepository,
                quizOptionRepository);
    }

        @Test
        void hrManagerReadsCourseQuizListInRepositoryOrder() {
        Course course = quizTestCourse(10L);
        Quiz newest = quizTestQuiz(course, 22L, "최신 퀴즈");
        Quiz older = quizTestQuiz(course, 21L, "이전 퀴즈");

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(quizRepository
                .findAllByCourse_CourseIdOrderByCreatedAtDescQuizIdDesc(10L))
                .thenReturn(List.of(newest, older));
        when(quizQuestionRepository
                .findAllByQuiz_QuizIdOrderByQuestionOrderAsc(22L))
                .thenReturn(List.of());
        when(quizQuestionRepository
                .findAllByQuiz_QuizIdOrderByQuestionOrderAsc(21L))
                .thenReturn(List.of());

        List<HrOxQuizResponse> responses =
                service.getCourseQuizzes(10L);

        assertThat(responses)
                .extracting(HrOxQuizResponse::quizId)
                .containsExactly(22L, 21L);
        assertThat(responses)
                .extracting(HrOxQuizResponse::quizTitle)
                .containsExactly("최신 퀴즈", "이전 퀴즈");
        }

        @Test
        void hrManagerReadsCourseQuizDetailWithCorrectAnswer() {
        Course course = quizTestCourse(10L);
        Quiz quiz = quizTestQuiz(course, 20L, "보안 교육 확인 퀴즈");
        QuizQuestion question = QuizQuestion.create(
                quiz,
                "비밀번호를 다른 사람과 공유해도 된다.",
                1,
                new BigDecimal("100.00"));
        ReflectionTestUtils.setField(
                question,
                "quizQuestionId",
                30L);

        QuizOption optionO = QuizOption.create(
                question,
                "O",
                1,
                false);
        QuizOption optionX = QuizOption.create(
                question,
                "X",
                2,
                true);

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(quizRepository.findByQuizIdAndCourse_CourseId(20L, 10L))
                .thenReturn(Optional.of(quiz));
        when(quizQuestionRepository
                .findAllByQuiz_QuizIdOrderByQuestionOrderAsc(20L))
                .thenReturn(List.of(question));
        when(quizOptionRepository
                .findAllByQuizQuestion_QuizQuestionIdInOrderByQuizQuestion_QuestionOrderAscOptionOrderAsc(
                        List.of(30L)))
                .thenReturn(List.of(optionO, optionX));

        HrOxQuizResponse response =
                service.getCourseQuiz(10L, 20L);

        assertThat(response.quizId()).isEqualTo(20L);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.quizTitle())
                .isEqualTo("보안 교육 확인 퀴즈");
        assertThat(response.questions()).hasSize(1);

        HrOxQuizResponse.Question responseQuestion =
                response.questions().get(0);

        assertThat(responseQuestion.questionId()).isEqualTo(30L);
        assertThat(responseQuestion.questionOrder()).isEqualTo(1);
        assertThat(responseQuestion.correctAnswer()).isEqualTo("X");
        assertThat(responseQuestion.score())
                .isEqualByComparingTo("100.00");
        }

        @Test
        void missingCourseQuizReturnsNotFound() {
        Course course = quizTestCourse(10L);

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(quizRepository.findByQuizIdAndCourse_CourseId(999L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getCourseQuiz(10L, 999L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                                assertThat(exception.getMessage())
                                        .isEqualTo("Quiz not found");
                        });
        }

@Test
void hrManagerDeactivatesCourseQuiz() {
    Course course = quizTestCourse(10L);
    Quiz quiz = quizTestQuiz(course, 20L, "보안 교육 확인 퀴즈");

    when(currentUserProvider.getCurrentUser())
            .thenReturn(currentUser(RoleType.HR_MANAGER));
    when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
            .thenReturn(Optional.of(course));
    when(quizRepository.findByQuizIdAndCourse_CourseId(20L, 10L))
            .thenReturn(Optional.of(quiz));
    when(quizQuestionRepository
            .findAllByQuiz_QuizIdOrderByQuestionOrderAsc(20L))
            .thenReturn(List.of());

    HrOxQuizResponse response =
            service.changeCourseQuizActivation(
                    10L,
                    20L,
                    new QuizActivationUpdateRequest(false));

    assertThat(quiz.isActive()).isFalse();
    assertThat(response.active()).isFalse();

    // JPA 변경 감지를 사용하므로 별도 save 호출이 없어야 한다.
    verify(quizRepository, never()).save(any(Quiz.class));
}

        @Test
        void activationOfMissingCourseQuizReturnsNotFound() {
        Course course = quizTestCourse(10L);

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(quizRepository.findByQuizIdAndCourse_CourseId(999L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.changeCourseQuizActivation(
                        10L,
                        999L,
                        new QuizActivationUpdateRequest(false)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                                assertThat(exception.getMessage())
                                        .isEqualTo("Quiz not found");
                        });
        }

    private OxQuizCreateRequest request() {
        return new OxQuizCreateRequest(
                " Security basics ",
                new BigDecimal("80.00"),
                3,
                List.of(
                        question(
                                " Passwords may be shared with coworkers. ",
                                "50.00",
                                "X"),
                        question(
                                " Suspicious emails must be reported. ",
                                "50.00",
                                "o")));
    }

    private OxQuizQuestionRequest question(
            String content,
            String score,
            String correctAnswer
    ) {
        return new OxQuizQuestionRequest(
                content,
                new BigDecimal(score),
                correctAnswer);
    }

    private Course course(Long courseId) {
        Course course = Course.create(
                "New hire fundamentals",
                "Company onboarding basics",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L);

        ReflectionTestUtils.setField(
                course,
                "courseId",
                courseId);

        return course;
    }

    private CurrentUserContext currentUser(RoleType role) {
        return new CurrentUserContext(
                7L,
                70L,
                Set.of(role),
                700L,
                1,
                EmployeeType.GENERAL);
    }

        private Course quizTestCourse(Long courseId) {
        Course course = Course.create(
                "신입사원 필수 교육",
                "신입사원 기본 교육 과정",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L);
        ReflectionTestUtils.setField(course, "courseId", courseId);
        return course;
        }

        private Quiz quizTestQuiz(
                Course course,
                Long quizId,
                String quizTitle
        ) {
        Quiz quiz = Quiz.create(
                course,
                null,
                quizTitle,
                new BigDecimal("80.00"),
                3,
                7L);
        ReflectionTestUtils.setField(quiz, "quizId", quizId);
        return quiz;
        }
}