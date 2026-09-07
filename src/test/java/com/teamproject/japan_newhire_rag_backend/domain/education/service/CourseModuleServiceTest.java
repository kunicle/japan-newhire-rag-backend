package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
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
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleActivationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseModuleUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseModule;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class CourseModuleServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private CourseModuleService courseModuleService;

    @BeforeEach
    void setUp() {
        courseModuleService = new CourseModuleService(
                courseRepository,
                courseModuleRepository,
                courseEnrollmentRepository,
                currentUserProvider);
    }

    @Test
    void hrManagerGetsAllModulesInRepositoryOrderIncludingInactive() {
        Course course = course(10L, CoursePublicationStatus.DRAFT);
        CourseModule first = module(100L, course, 1, true, true);
        CourseModule second = module(101L, course, 2, false, false);
        CourseModule third = module(102L, course, 3, false, true);

        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(courseModuleRepository
                .findAllByCourse_CourseIdOrderByModuleOrderAsc(10L))
                .thenReturn(List.of(first, second, third));

        List<CourseModuleResponse> responses =
                courseModuleService.getModules(10L);

        assertThat(responses)
                .extracting(CourseModuleResponse::moduleOrder)
                .containsExactly(1, 2, 3);
        assertThat(responses)
                .extracting(CourseModuleResponse::active)
                .containsExactly(true, false, true);

        verify(courseRepository)
                .findByCourseIdAndDeletedAtIsNull(10L);
        verify(courseModuleRepository)
                .findAllByCourse_CourseIdOrderByModuleOrderAsc(10L);
    }

    @Test
    void missingOrDeletedCourseCannotBeListed() {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.getModules(10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        verifyNoInteractions(
                courseModuleRepository,
                courseEnrollmentRepository);
    }

    @Test
    void hrManagerCreatesActiveModuleWithRequestedValues() {
        Course course = course(10L, CoursePublicationStatus.DRAFT);
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(invocation -> {
            CourseModule module = invocation.getArgument(0);
            ReflectionTestUtils.setField(module, "courseModuleId", 100L);
            ReflectionTestUtils.setField(module, "createdAt", LocalDateTime.of(2026, 8, 12, 10, 0));
            ReflectionTestUtils.setField(module, "updatedAt", LocalDateTime.of(2026, 8, 12, 10, 0));
            return module;
        });

        CourseModuleResponse response = courseModuleService.createModule(
                10L,
                createRequest("Content", null, 1, true));

        ArgumentCaptor<CourseModule> captor = ArgumentCaptor.forClass(CourseModule.class);
        verify(courseModuleRepository).save(captor.capture());
        CourseModule saved = captor.getValue();
        assertThat(saved.getCourse()).isSameAs(course);
        assertThat(saved.getModuleTitle()).isEqualTo("Security basics");
        assertThat(saved.getModuleContent()).isEqualTo("Content");
        assertThat(saved.getReferenceUrl()).isNull();
        assertThat(saved.getModuleOrder()).isEqualTo(1);
        assertThat(saved.isRequired()).isTrue();
        assertThat(saved.isActive()).isTrue();
        assertThat(response.courseModuleId()).isEqualTo(100L);
        assertThat(response.courseId()).isEqualTo(10L);
    }

    @Test
    void linkOnlyModuleCanBeCreatedAndBlankContentIsNormalized() {
        stubCreatableCourse(false);

        CourseModuleResponse response = courseModuleService.createModule(
                10L,
                createRequest("   ", "https://example.com", 1, false));

        assertThat(response.moduleContent()).isNull();
        assertThat(response.referenceUrl()).isEqualTo("https://example.com");
    }

    @Test
    void moduleWithContentAndLinkCanBeCreated() {
        stubCreatableCourse(false);

        CourseModuleResponse response = courseModuleService.createModule(
                10L,
                createRequest("Content", "https://example.com", 1, false));

        assertThat(response.moduleContent()).isEqualTo("Content");
        assertThat(response.referenceUrl()).isEqualTo("https://example.com");
    }

    @Test
    void moduleWithoutContentOrLinkIsRejected() {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, CoursePublicationStatus.DRAFT)));

        assertThatThrownBy(() -> courseModuleService.createModule(
                10L,
                createRequest(" ", "  ", 1, false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    void duplicateOrderInSameCourseIsRejectedIncludingInactiveModules() {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, CoursePublicationStatus.DRAFT)));
        when(courseModuleRepository.existsByCourse_CourseIdAndModuleOrder(10L, 2))
                .thenReturn(true);

        assertConflict(() -> courseModuleService.createModule(
                10L,
                createRequest("Content", null, 2, false)));
        verify(courseModuleRepository).existsByCourse_CourseIdAndModuleOrder(10L, 2);
        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    void sameOrderInDifferentCourseIsAllowed() {
        allowHrManager();
        Course course = course(20L, CoursePublicationStatus.DRAFT);
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(course));
        when(courseModuleRepository.existsByCourse_CourseIdAndModuleOrder(20L, 1))
                .thenReturn(false);
        when(courseModuleRepository.save(any(CourseModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseModuleResponse response = courseModuleService.createModule(
                20L,
                createRequest("Content", null, 1, false));

        assertThat(response.courseId()).isEqualTo(20L);
        verify(courseModuleRepository).existsByCourse_CourseIdAndModuleOrder(20L, 1);
    }

    @Test
    void missingOrDeletedCourseCannotReceiveModule() {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.createModule(
                10L,
                createRequest("Content", null, 1, false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verifyNoInteractions(courseModuleRepository, courseEnrollmentRepository);
    }

    @Test
    void requiredModuleIsRejectedAfterFirstEnrollment() {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, CoursePublicationStatus.DRAFT)));
        when(courseEnrollmentRepository.existsByCourse_CourseId(10L)).thenReturn(true);

        assertConflict(() -> courseModuleService.createModule(
                10L,
                createRequest("Content", null, 1, true)));
        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    void optionalModuleIsAllowedAfterFirstEnrollment() {
        stubCreatableCourse(false);

        CourseModuleResponse response = courseModuleService.createModule(
                10L,
                createRequest("Content", null, 1, false));

        assertThat(response.required()).isFalse();
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void unauthenticatedAndUnauthorizedUsersCannotCreateModule() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseModuleService.createModule(
                10L, createRequest("Content", null, 1, false)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> courseModuleService.createModule(
                10L, createRequest("Content", null, 1, false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository, courseModuleRepository, courseEnrollmentRepository);
    }

    @Test
    void moduleBasicInformationChangesWithoutMovingCourseOrChangingActivation() {
        Course originalCourse = course(10L, CoursePublicationStatus.DRAFT);
        CourseModule module = module(100L, originalCourse, 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        CourseModuleResponse response = courseModuleService.updateModule(
                100L,
                updateRequest("Updated", "https://example.com", 2, false));

        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.moduleTitle()).isEqualTo("Updated module");
        assertThat(response.moduleContent()).isEqualTo("Updated");
        assertThat(response.referenceUrl()).isEqualTo("https://example.com");
        assertThat(response.moduleOrder()).isEqualTo(2);
        assertThat(response.required()).isFalse();
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 10, 0));
        assertThat(module.getCourse()).isSameAs(originalCourse);
        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    void currentModuleIsExcludedFromOrderDuplicateCheck() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        courseModuleService.updateModule(
                100L,
                updateRequest("Content", null, 1, true));

        verify(courseModuleRepository)
                .existsByCourse_CourseIdAndModuleOrderAndCourseModuleIdNot(10L, 1, 100L);
    }

    @Test
    void anotherModuleWithRequestedOrderCausesConflict() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));
        when(courseModuleRepository
                .existsByCourse_CourseIdAndModuleOrderAndCourseModuleIdNot(10L, 2, 100L))
                .thenReturn(true);

        assertConflict(() -> courseModuleService.updateModule(
                100L,
                updateRequest("Content", null, 2, true)));
        assertThat(module.getModuleOrder()).isEqualTo(1);
    }

    @Test
    void missingModuleOrModuleOfDeletedCourseCannotBeUpdated() {
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.updateModule(
                100L, updateRequest("Content", null, 1, true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void requiredStatusCannotChangeAfterEnrollmentInEitherDirection() {
        Course course = course(10L, CoursePublicationStatus.DRAFT);
        CourseModule required = module(100L, course, 1, true, true);
        CourseModule optional = module(101L, course, 2, false, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(required));
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(101L))
                .thenReturn(Optional.of(optional));
        when(courseEnrollmentRepository.existsByCourse_CourseId(10L)).thenReturn(true);

        assertConflict(() -> courseModuleService.updateModule(
                100L, updateRequest("Content", null, 1, false)));
        assertConflict(() -> courseModuleService.updateModule(
                101L, updateRequest("Content", null, 2, true)));
        assertThat(required.isRequired()).isTrue();
        assertThat(optional.isRequired()).isFalse();
    }

    @Test
    void sameRequiredStatusAndTextChangesAreAllowedAfterEnrollment() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        CourseModuleResponse response = courseModuleService.updateModule(
                100L,
                updateRequest("Changed content", null, 1, true));

        assertThat(response.moduleContent()).isEqualTo("Changed content");
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void lastActiveRequiredModuleOfPublicCourseCannotBecomeOptional() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.PUBLIC), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        assertConflict(() -> courseModuleService.updateModule(
                100L, updateRequest("Content", null, 1, false)));
        assertThat(module.isRequired()).isTrue();
    }

    @Test
    void publicCourseRequiredModuleCanBecomeOptionalWhenAnotherRequiredModuleExists() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.PUBLIC), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));
        when(courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrueAndCourseModuleIdNot(10L, 100L))
                .thenReturn(true);

        CourseModuleResponse response = courseModuleService.updateModule(
                100L, updateRequest("Content", null, 1, false));

        assertThat(response.required()).isFalse();
    }

    @Test
    void optionalModuleCanBeDeactivatedWithoutPhysicalDelete() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, false, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        CourseModuleResponse response = courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(false));

        assertThat(response.active()).isFalse();
        assertThat(module.isActive()).isFalse();
        verify(courseModuleRepository, never()).delete(any());
        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    void inactiveModuleCanBeReactivatedAndSameStateIsIdempotent() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, false, false);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        CourseModuleResponse activated = courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(true));
        CourseModuleResponse unchanged = courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(true));

        assertThat(activated.active()).isTrue();
        assertThat(unchanged.active()).isTrue();
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void requiredModuleCannotBeDeactivatedAfterEnrollment() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.DRAFT), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));
        when(courseEnrollmentRepository.existsByCourse_CourseId(10L)).thenReturn(true);

        assertConflict(() -> courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(false)));
        assertThat(module.isActive()).isTrue();
    }

    @Test
    void lastActiveRequiredModuleOfPublicCourseCannotBeDeactivated() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.PUBLIC), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));

        assertConflict(() -> courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(false)));
        assertThat(module.isActive()).isTrue();
    }

    @Test
    void publicCourseRequiredModuleCanBeDeactivatedWhenAnotherExists() {
        CourseModule module = module(
                100L, course(10L, CoursePublicationStatus.PUBLIC), 1, true, true);
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.of(module));
        when(courseModuleRepository
                .existsByCourse_CourseIdAndRequiredTrueAndActiveTrueAndCourseModuleIdNot(10L, 100L))
                .thenReturn(true);

        CourseModuleResponse response = courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void missingModuleCannotChangeActivation() {
        allowHrManager();
        when(courseModuleRepository.findByCourseModuleIdAndCourse_DeletedAtIsNull(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseModuleService.changeActivation(
                100L,
                new CourseModuleActivationRequest(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void unauthenticatedAndUnauthorizedUsersCannotUpdateOrActivateModule() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseModuleService.updateModule(
                100L, updateRequest("Content", null, 1, true)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> courseModuleService.changeActivation(
                100L, new CourseModuleActivationRequest(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository, courseModuleRepository, courseEnrollmentRepository);
    }

    private void stubCreatableCourse(boolean required) {
        allowHrManager();
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, CoursePublicationStatus.DRAFT)));
        when(courseModuleRepository.save(any(CourseModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        if (required) {
            when(courseEnrollmentRepository.existsByCourse_CourseId(10L)).thenReturn(false);
        }
    }

    private Course course(Long courseId, CoursePublicationStatus status) {
        Course course = Course.create(
                "Course " + courseId,
                "Description",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L);
        ReflectionTestUtils.setField(course, "courseId", courseId);
        course.changePublicationStatus(status);
        return course;
    }

    private CourseModule module(
            Long moduleId,
            Course course,
            int order,
            boolean required,
            boolean active
    ) {
        CourseModule module = CourseModule.create(
                course,
                "Module " + moduleId,
                "Content",
                null,
                order,
                required);
        ReflectionTestUtils.setField(module, "courseModuleId", moduleId);
        ReflectionTestUtils.setField(module, "createdAt", LocalDateTime.of(2026, 8, 12, 10, 0));
        ReflectionTestUtils.setField(module, "updatedAt", LocalDateTime.of(2026, 8, 12, 10, 0));
        module.changeActive(active);
        return module;
    }

    private CourseModuleCreateRequest createRequest(
            String content,
            String referenceUrl,
            int order,
            boolean required
    ) {
        return new CourseModuleCreateRequest(
                "Security basics",
                content,
                referenceUrl,
                order,
                required);
    }

    private CourseModuleUpdateRequest updateRequest(
            String content,
            String referenceUrl,
            int order,
            boolean required
    ) {
        return new CourseModuleUpdateRequest(
                "Updated module",
                content,
                referenceUrl,
                order,
                required);
    }

    private void allowHrManager() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
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

    private void assertConflict(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
