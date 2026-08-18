package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseCreateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CoursePublicationUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.CourseUpdateRequest;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseModuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseModuleRepository,
                currentUserProvider);
    }

    @Test
    void hrManagerCreatesDraftCourseWithCurrentAppUserId() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "courseId", 100L);
            ReflectionTestUtils.setField(course, "createdAt", LocalDateTime.of(2026, 8, 12, 10, 0));
            ReflectionTestUtils.setField(course, "updatedAt", LocalDateTime.of(2026, 8, 12, 10, 0));
            return course;
        });

        CourseResponse response = courseService.createCourse(request(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)));

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course saved = courseCaptor.getValue();

        assertThat(saved.getCourseName()).isEqualTo("New hire fundamentals");
        assertThat(saved.getCourseDescription()).isEqualTo("Company onboarding basics");
        assertThat(saved.isRequired()).isTrue();
        assertThat(saved.getTrainingStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(saved.getTrainingEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(saved.getPublicationStatus()).isEqualTo(CoursePublicationStatus.DRAFT);
        assertThat(saved.getCreatedBy()).isEqualTo(7L);
        assertThat(saved.getDeletedAt()).isNull();
        assertThat(response.courseId()).isEqualTo(100L);
        assertThat(response.publicationStatus()).isEqualTo(CoursePublicationStatus.DRAFT);
        assertThat(response.createdBy()).isEqualTo(7L);
    }

    @Test
    void employeeCannotCreateCourse() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseService.createCourse(request(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void endDateBeforeStartDateIsRejected() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));

        assertThatThrownBy(() -> courseService.createCourse(request(
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void equalStartAndEndDatesAreAllowed() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate trainingDate = LocalDate.of(2026, 9, 1);

        CourseResponse response = courseService.createCourse(request(trainingDate, trainingDate));

        assertThat(response.trainingStartDate()).isEqualTo(trainingDate);
        assertThat(response.trainingEndDate()).isEqualTo(trainingDate);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void courseListUsesNotDeletedQueryAndReturnsRepositoryOrder() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        Course newest = course(2L, LocalDateTime.of(2026, 8, 12, 11, 0));
        Course older = course(1L, LocalDateTime.of(2026, 8, 12, 10, 0));
        PageRequest expectedPageRequest = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("courseId")));
        when(courseRepository.findAllByDeletedAtIsNull(expectedPageRequest))
                .thenReturn(new PageImpl<>(List.of(newest, older), expectedPageRequest, 2));

        CoursePageResponse response = courseService.getCourses(0, 20);

        assertThat(response.content()).extracting(CourseResponse::courseId)
                .containsExactly(2L, 1L);
        verify(courseRepository).findAllByDeletedAtIsNull(expectedPageRequest);
    }

    @Test
    void courseListUsesTwentyAsRequestedDefaultPageSize() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        courseService.getCourses(0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findAllByDeletedAtIsNull(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void emptyCourseListReturnsEmptyPage() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        CoursePageResponse response = courseService.getCourses(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void existingNotDeletedCourseDetailIsReturned() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, LocalDateTime.of(2026, 8, 12, 10, 0))));

        CourseResponse response = courseService.getCourse(10L);

        assertThat(response.courseId()).isEqualTo(10L);
        verify(courseRepository).findByCourseIdAndDeletedAtIsNull(10L);
    }

    @Test
    void missingCourseDetailReturnsNotFound() {
        assertCourseNotFound(999L);
    }

    @Test
    void deletedCourseDetailReturnsNotFound() {
        assertCourseNotFound(10L);
    }

    @Test
    void unauthenticatedUserCannotReadCourses() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"));

        assertThatThrownBy(() -> courseService.getCourses(0, 20))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(courseRepository);
    }

    @Test
    void employeeCannotReadCourses() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseService.getCourses(0, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void negativePageIsRejected() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));

        assertThatThrownBy(() -> courseService.getCourses(-1, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void pageSizeOverOneHundredIsRejected() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));

        assertThatThrownBy(() -> courseService.getCourses(0, 101))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void hrManagerUpdatesBasicInformationWithoutChangingProtectedFields() {
        Course course = course(10L, LocalDateTime.of(2026, 8, 12, 10, 0));
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));

        CourseResponse response = courseService.updateCourse(10L, updateRequest(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)));

        assertThat(response.courseName()).isEqualTo("Updated course");
        assertThat(response.courseDescription()).isEqualTo("Updated description");
        assertThat(response.required()).isFalse();
        assertThat(response.trainingStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.trainingEndDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(response.publicationStatus()).isEqualTo(CoursePublicationStatus.DRAFT);
        assertThat(response.createdBy()).isEqualTo(7L);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 10, 0));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void equalDatesAreAllowedWhenUpdatingCourse() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, LocalDateTime.now())));

        CourseResponse response = courseService.updateCourse(10L, updateRequest(date, date));

        assertThat(response.trainingStartDate()).isEqualTo(date);
        assertThat(response.trainingEndDate()).isEqualTo(date);
    }

    @Test
    void endDateBeforeStartDateIsRejectedWhenUpdatingCourse() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));

        assertThatThrownBy(() -> courseService.updateCourse(10L, updateRequest(
                LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 10, 1))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(courseRepository);
    }

    @Test
    void missingOrDeletedCourseCannotBeUpdated() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(10L, updateRequest(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void unauthenticatedAndUnauthorizedUsersCannotUpdateCourse() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseService.updateCourse(10L, updateRequest(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> courseService.updateCourse(10L, updateRequest(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void courseBecomesPublicWhenActiveRequiredModuleExists() {
        Course course = course(10L, LocalDateTime.now());
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));
        when(courseModuleRepository.existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(10L))
                .thenReturn(true);

        CourseResponse response = courseService.changePublicationStatus(
                10L,
                new CoursePublicationUpdateRequest(CoursePublicationStatus.PUBLIC));

        assertThat(response.publicationStatus()).isEqualTo(CoursePublicationStatus.PUBLIC);
        verify(courseModuleRepository).existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(10L);
    }

    @Test
    void courseCannotBecomePublicWithoutActiveRequiredModule() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, LocalDateTime.now())));
        when(courseModuleRepository.existsByCourse_CourseIdAndRequiredTrueAndActiveTrue(10L))
                .thenReturn(false);

        assertThatThrownBy(() -> courseService.changePublicationStatus(
                10L,
                new CoursePublicationUpdateRequest(CoursePublicationStatus.PUBLIC)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void courseCanBecomePrivateWithoutModuleCheck() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, LocalDateTime.now())));

        CourseResponse response = courseService.changePublicationStatus(
                10L,
                new CoursePublicationUpdateRequest(CoursePublicationStatus.PRIVATE));

        assertThat(response.publicationStatus()).isEqualTo(CoursePublicationStatus.PRIVATE);
        verifyNoInteractions(courseModuleRepository);
    }

    @Test
    void requestingSamePublicationStatusIsIdempotent() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course(10L, LocalDateTime.now())));

        CourseResponse response = courseService.changePublicationStatus(
                10L,
                new CoursePublicationUpdateRequest(CoursePublicationStatus.DRAFT));

        assertThat(response.publicationStatus()).isEqualTo(CoursePublicationStatus.DRAFT);
        verifyNoInteractions(courseModuleRepository);
    }

    @Test
    void missingOrDeletedCoursePublicationCannotBeChanged() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.changePublicationStatus(
                10L,
                new CoursePublicationUpdateRequest(CoursePublicationStatus.PUBLIC)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verifyNoInteractions(courseModuleRepository);
    }

    @Test
    void unauthenticatedAndUnauthorizedUsersCannotChangePublicationStatus() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseService.changePublicationStatus(
                10L, new CoursePublicationUpdateRequest(CoursePublicationStatus.PUBLIC)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> courseService.changePublicationStatus(
                10L, new CoursePublicationUpdateRequest(CoursePublicationStatus.PUBLIC)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository, courseModuleRepository);
    }

    @Test
    void courseIsSoftDeletedWithoutRepositoryDelete() {
        Course course = course(10L, LocalDateTime.now());
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(course));

        courseService.deleteCourse(10L);

        assertThat(course.getDeletedAt()).isNotNull();
        verify(courseRepository, never()).delete(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void missingOrAlreadyDeletedCourseCannotBeDeleted() {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void unauthenticatedAndUnauthorizedUsersCannotDeleteCourse() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Authentication is required"))
                .thenReturn(currentUser(RoleType.EMPLOYEE));

        assertThatThrownBy(() -> courseService.deleteCourse(10L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> courseService.deleteCourse(10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(courseRepository);
    }

    private void assertCourseNotFound(Long courseId) {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser(RoleType.HR_MANAGER));
        when(courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(courseId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Course not found");
                });
    }

    private Course course(Long courseId, LocalDateTime createdAt) {
        Course course = Course.create(
                "Course " + courseId,
                "Course description",
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L);
        ReflectionTestUtils.setField(course, "courseId", courseId);
        ReflectionTestUtils.setField(course, "createdAt", createdAt);
        ReflectionTestUtils.setField(course, "updatedAt", createdAt);
        return course;
    }

    private CourseCreateRequest request(LocalDate startDate, LocalDate endDate) {
        return new CourseCreateRequest(
                "New hire fundamentals",
                "Company onboarding basics",
                true,
                startDate,
                endDate);
    }

    private CourseUpdateRequest updateRequest(LocalDate startDate, LocalDate endDate) {
        return new CourseUpdateRequest(
                "Updated course",
                "Updated description",
                false,
                startDate,
                endDate);
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
}
