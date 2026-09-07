package com.teamproject.japan_newhire_rag_backend.domain.education.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationItemResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.controller.dto.ManagerEducationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.repository.CourseEnrollmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;

@Service
@Transactional(readOnly = true)
public class ManagerEducationQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort EDUCATION_LIST_SORT =
            Sort.by(Sort.Direction.DESC, "courseEnrollmentId");

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final OrganizationQueryService organizationQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ManagerEducationQueryService(
            CourseEnrollmentRepository courseEnrollmentRepository,
            OrganizationQueryService organizationQueryService,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.organizationQueryService = organizationQueryService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public ManagerEducationPageResponse getTeamEducation(
            int page,
            int size
    ) {
        validatePageRequest(page, size);

        CurrentUserContext manager = getCurrentManager();
        List<Long> managedEmployeeIds =
                organizationQueryService.findManagedEmployeeIds(
                        manager.employeeId());

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                EDUCATION_LIST_SORT);

        if (managedEmployeeIds.isEmpty()) {
            return ManagerEducationPageResponse.from(
                    Page.empty(pageRequest));
        }

        Page<CourseEnrollment> enrollmentPage =
                courseEnrollmentRepository.findAllByEmployeeIdIn(
                        managedEmployeeIds,
                        pageRequest);

        return toResponse(enrollmentPage);
    }

    public ManagerEducationPageResponse getEmployeeCourses(
            Long employeeId,
            int page,
            int size
    ) {
        validateEmployeeId(employeeId);
        validatePageRequest(page, size);

        CurrentUserContext manager = getCurrentManager();

        if (!organizationQueryService.isManagedEmployee(
                manager.employeeId(),
                employeeId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Employee is outside the manager's scope");
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                EDUCATION_LIST_SORT);

        Page<CourseEnrollment> enrollmentPage =
                courseEnrollmentRepository.findAllByEmployeeId(
                        employeeId,
                        pageRequest);

        return toResponse(enrollmentPage);
    }

    private ManagerEducationPageResponse toResponse(
            Page<CourseEnrollment> enrollmentPage
    ) {
        if (enrollmentPage.isEmpty()) {
            return ManagerEducationPageResponse.from(
                    Page.empty(enrollmentPage.getPageable()));
        }

        List<Long> employeeIds = enrollmentPage.getContent()
                .stream()
                .map(CourseEnrollment::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, EmployeeSummary> employeesById =
                organizationQueryService
                        .findEmployeeSummaries(employeeIds)
                        .stream()
                        .collect(Collectors.toMap(
                                EmployeeSummary::employeeId,
                                Function.identity()));

        LocalDate today = LocalDate.now(clock);

        Page<ManagerEducationItemResponse> responsePage =
                enrollmentPage.map(enrollment -> {
                    EmployeeSummary employee =
                            employeesById.get(
                                    enrollment.getEmployeeId());

                    if (employee == null) {
                        throw new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Employee information not found");
                    }

                    return ManagerEducationItemResponse.from(
                            enrollment,
                            employee,
                            today);
                });

        return ManagerEducationPageResponse.from(responsePage);
    }

    private CurrentUserContext getCurrentManager() {
        CurrentUserContext currentUser =
                currentUserProvider.getCurrentUser();

        if (currentUser == null
                || currentUser.appUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (currentUser.employeeId() == null
                || !currentUser.roles().contains(
                        RoleType.MANAGER)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Manager role is required");
        }

        return currentUser;
    }

    private void validateEmployeeId(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Employee ID must be a positive number");
        }
    }

    private void validatePageRequest(
            int page,
            int size
    ) {
        if (page < 0
                || size < 1
                || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100");
        }
    }
}