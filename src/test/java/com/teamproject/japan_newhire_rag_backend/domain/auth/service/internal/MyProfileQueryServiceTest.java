package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.MyProfileResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.auth.error.ProfileErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;

class MyProfileQueryServiceTest {

    private EmployeeRepository employeeRepository;
    private ManagerRelationRepository managerRelationRepository;
    private MyProfileQueryService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        managerRelationRepository = mock(ManagerRelationRepository.class);
        service = new MyProfileQueryService(employeeRepository, managerRelationRepository);
    }

    @Test
    void returnsEmployeeProfileWithLatestRolesAndDirectManager() {
        CurrentUserContext context = context(Set.of(RoleType.EMPLOYEE, RoleType.MANAGER));
        Employee employee = employee();
        Employee manager = mock(Employee.class);
        ManagerRelation relation = mock(ManagerRelation.class);
        when(manager.getEmployeeId()).thenReturn(20L);
        when(manager.getEmployeeName()).thenReturn("Manager Lee");
        when(manager.getDeletedAt()).thenReturn(null);
        when(relation.getManagerEmployee()).thenReturn(manager);
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(managerRepositoryCall()).thenReturn(List.of(relation));

        MyProfileResponse response = service.getMyProfile(context);

        assertEquals(1L, response.appUserId());
        assertEquals(10L, response.employeeId());
        assertEquals("E-001", response.employeeNumber());
        assertEquals("Kim", response.employeeName());
        assertEquals("user@example.com", response.email());
        assertEquals(100L, response.departmentId());
        assertEquals("Engineering", response.departmentName());
        assertEquals(200L, response.jobGradeId());
        assertEquals("Junior", response.jobGradeName());
        assertEquals(1, response.jobGradeLevel());
        assertEquals(Set.of(RoleType.EMPLOYEE, RoleType.MANAGER), response.roles());
        assertEquals(LocalDate.of(2026, 1, 2), response.hireDate());
        assertEquals(20L, response.managerEmployeeId());
        assertEquals("Manager Lee", response.managerName());
    }

    @Test
    void returnsNullManagerFieldsWhenNoDirectManagerExists() {
        Employee employee = employee();
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(managerRepositoryCall()).thenReturn(List.of());

        MyProfileResponse response = service.getMyProfile(context(Set.of(RoleType.EMPLOYEE)));

        assertNull(response.managerEmployeeId());
        assertNull(response.managerName());
    }

    @Test
    void rejectsMissingEmployeeAsProfileNotFound() {
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getMyProfile(context(Set.of(RoleType.EMPLOYEE))));

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsEmployeeThatDoesNotMatchCurrentContext() {
        Employee employee = mock(Employee.class);
        when(employee.getEmployeeId()).thenReturn(99L);
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getMyProfile(context(Set.of(RoleType.EMPLOYEE))));

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsSoftDeletedCurrentEmployeeAsProfileNotFound() {
        Employee employee = mock(Employee.class);
        when(employee.getEmployeeId()).thenReturn(10L);
        when(employee.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 8, 13, 10, 0));
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getMyProfile(context(Set.of(RoleType.EMPLOYEE))));

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void excludesSoftDeletedDirectManagerFromProfile() {
        Employee employee = employee();
        Employee manager = mock(Employee.class);
        ManagerRelation relation = mock(ManagerRelation.class);
        when(manager.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 8, 13, 10, 0));
        when(relation.getManagerEmployee()).thenReturn(manager);
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(managerRepositoryCall()).thenReturn(List.of(relation));

        MyProfileResponse response = service.getMyProfile(context(Set.of(RoleType.EMPLOYEE)));

        assertNull(response.managerEmployeeId());
        assertNull(response.managerName());
    }

    @Test
    void teamRelationIsNotReturnedAsDirectManager() {
        Employee employee = employee();
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(managerRepositoryCall()).thenReturn(List.of());

        MyProfileResponse response = service.getMyProfile(context(Set.of(RoleType.EMPLOYEE)));

        assertNull(response.managerEmployeeId());
        assertNull(response.managerName());
        org.mockito.Mockito.verify(managerRelationRepository)
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE);
    }

    @Test
    void rejectsMultipleActiveDirectManagersAsDataConflict() {
        Employee employee = employee();
        when(employeeRepository.findByAppUser_AppUserId(1L)).thenReturn(Optional.of(employee));
        when(managerRepositoryCall()).thenReturn(List.of(
                mock(ManagerRelation.class), mock(ManagerRelation.class)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getMyProfile(context(Set.of(RoleType.MANAGER))));

        assertEquals(ProfileErrorCode.PROFILE_DATA_CONFLICT, exception.getErrorCode());
    }

    private List<ManagerRelation> managerRepositoryCall() {
        return managerRelationRepository
                .findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
                        10L, RelationType.DIRECT, RelationStatus.ACTIVE);
    }

    private CurrentUserContext context(Set<RoleType> roles) {
        return new CurrentUserContext(1L, 10L, roles, 100L, 1, EmployeeType.GENERAL);
    }

    private Employee employee() {
        Employee employee = mock(Employee.class);
        AppUser appUser = mock(AppUser.class);
        Department department = mock(Department.class);
        JobGrade jobGrade = mock(JobGrade.class);
        when(employee.getEmployeeId()).thenReturn(10L);
        when(employee.getDeletedAt()).thenReturn(null);
        when(employee.getEmployeeNumber()).thenReturn("E-001");
        when(employee.getEmployeeName()).thenReturn("Kim");
        when(employee.getAppUser()).thenReturn(appUser);
        when(employee.getDepartment()).thenReturn(department);
        when(employee.getJobGrade()).thenReturn(jobGrade);
        when(employee.getHireDate()).thenReturn(LocalDate.of(2026, 1, 2));
        when(appUser.getEmail()).thenReturn("user@example.com");
        when(department.getDepartmentId()).thenReturn(100L);
        when(department.getDepartmentName()).thenReturn("Engineering");
        when(jobGrade.getJobGradeId()).thenReturn(200L);
        when(jobGrade.getGradeName()).thenReturn("Junior");
        when(jobGrade.getGradeLevel()).thenReturn(1);
        return employee;
    }
}
