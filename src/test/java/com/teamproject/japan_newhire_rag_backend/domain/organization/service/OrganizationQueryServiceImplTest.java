package com.teamproject.japan_newhire_rag_backend.domain.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationQueryServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManagerRelationRepository managerRelationRepository;

    private OrganizationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganizationQueryServiceImpl(
                employeeRepository,
                managerRelationRepository);
    }

    @Test
    void isValidEmployeeReturnsTrueOnlyForEmployedNotDeletedEmployeeWithActiveAccount() {
        Employee valid = employee(1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee leave = employee(2L, EmploymentStatus.LEAVE, null, AccountStatus.ACTIVE);
        Employee retired = employee(3L, EmploymentStatus.RETIRED, null, AccountStatus.ACTIVE);
        Employee deleted = employee(
                4L, EmploymentStatus.EMPLOYED, LocalDateTime.now(), AccountStatus.ACTIVE);
        Employee inactive = employee(
                5L, EmploymentStatus.EMPLOYED, null, AccountStatus.INACTIVE);
        Employee locked = employee(
                6L, EmploymentStatus.EMPLOYED, null, AccountStatus.LOCKED);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(valid));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(retired));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(deleted));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(inactive));
        when(employeeRepository.findById(6L)).thenReturn(Optional.of(locked));
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertTrue(service.isValidEmployee(1L));
        assertFalse(service.isValidEmployee(2L));
        assertFalse(service.isValidEmployee(3L));
        assertFalse(service.isValidEmployee(4L));
        assertFalse(service.isValidEmployee(5L));
        assertFalse(service.isValidEmployee(6L));
        assertFalse(service.isValidEmployee(999L));
        assertFalse(service.isValidEmployee(null));
    }

    @Test
    void findValidEmployeeIdsByDepartmentIdsReturnsOnlyValidEmployees() {
        Employee valid1 = employee(1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee valid2 = employee(2L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee leave = employee(3L, EmploymentStatus.LEAVE, null, AccountStatus.ACTIVE);
        Employee deleted = employee(
                4L, EmploymentStatus.EMPLOYED, LocalDateTime.now(), AccountStatus.ACTIVE);
        Employee inactive = employee(
                5L, EmploymentStatus.EMPLOYED, null, AccountStatus.INACTIVE);

        when(employeeRepository.findByDepartment_DepartmentIdIn(Set.of(10L, 20L)))
                .thenReturn(List.of(valid1, leave, valid2, deleted, inactive));

        assertEquals(
                List.of(1L, 2L),
                service.findValidEmployeeIdsByDepartmentIds(List.of(10L, 20L, 10L)));
        verify(employeeRepository).findByDepartment_DepartmentIdIn(Set.of(10L, 20L));
    }

    @Test
    void findValidEmployeeIdsByDepartmentIdsReturnsEmptyWithoutQueryForNullOrEmptyInput() {
        assertTrue(service.findValidEmployeeIdsByDepartmentIds(null).isEmpty());
        assertTrue(service.findValidEmployeeIdsByDepartmentIds(List.of()).isEmpty());
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void findValidEmployeeIdsByJobGradeIdsReturnsOnlyValidEmployees() {
        Employee valid1 = employee(1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee valid2 = employee(2L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee retired = employee(3L, EmploymentStatus.RETIRED, null, AccountStatus.ACTIVE);
        Employee deleted = employee(
                4L, EmploymentStatus.EMPLOYED, LocalDateTime.now(), AccountStatus.ACTIVE);
        Employee locked = employee(
                5L, EmploymentStatus.EMPLOYED, null, AccountStatus.LOCKED);

        when(employeeRepository.findByJobGrade_JobGradeIdIn(Set.of(100L, 200L)))
                .thenReturn(List.of(valid1, retired, valid2, deleted, locked));

        assertEquals(
                List.of(1L, 2L),
                service.findValidEmployeeIdsByJobGradeIds(List.of(100L, 200L, 100L)));
        verify(employeeRepository).findByJobGrade_JobGradeIdIn(Set.of(100L, 200L));
    }

    @Test
    void findValidEmployeeIdsByJobGradeIdsReturnsEmptyWithoutQueryForNullOrEmptyInput() {
        assertTrue(service.findValidEmployeeIdsByJobGradeIds(null).isEmpty());
        assertTrue(service.findValidEmployeeIdsByJobGradeIds(List.of()).isEmpty());
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void findValidNewHireEmployeeIdsReturnsOnlyValidNewHiresAndDoesNotUseHireDate() {
        Employee validNewHire = employee(
                1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee leaveNewHire = employee(
                2L, EmploymentStatus.LEAVE, null, AccountStatus.ACTIVE);
        Employee inactiveNewHire = employee(
                3L, EmploymentStatus.EMPLOYED, null, AccountStatus.INACTIVE);

        when(employeeRepository.findByEmployeeType(EmployeeType.NEW_HIRE))
                .thenReturn(List.of(validNewHire, leaveNewHire, inactiveNewHire));

        assertEquals(List.of(1L), service.findValidNewHireEmployeeIds());
        verify(employeeRepository).findByEmployeeType(EmployeeType.NEW_HIRE);
        verify(validNewHire, never()).getHireDate();
        verify(leaveNewHire, never()).getHireDate();
        verify(inactiveNewHire, never()).getHireDate();
    }

    @Test
    void findManagedEmployeeIdsReturnsOnlyValidTargetsOfActiveCurrentRelations() {
        Employee valid = employee(2L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee invalid = employee(3L, EmploymentStatus.RETIRED, null, AccountStatus.ACTIVE);
        ManagerRelation validRelation = relation(valid);
        ManagerRelation invalidTargetRelation = relation(invalid);

        when(managerRelationRepository
                .findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, RelationStatus.ACTIVE))
                .thenReturn(List.of(validRelation, invalidTargetRelation));
        when(employeeRepository.findByEmployeeIdIn(Set.of(2L, 3L)))
                .thenReturn(List.of(valid, invalid));

        assertEquals(List.of(2L), service.findManagedEmployeeIds(1L));
        verify(managerRelationRepository)
                .findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, RelationStatus.ACTIVE);
    }

    @Test
    void findManagedEmployeeIdsReturnsEmptyWhenNoActiveCurrentRelationExists() {
        when(managerRelationRepository
                .findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, RelationStatus.ACTIVE))
                .thenReturn(List.of());

        assertTrue(service.findManagedEmployeeIds(1L).isEmpty());
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void findManagedEmployeeIdsReturnsEmptyWithoutQueryForNullManagerId() {
        assertTrue(service.findManagedEmployeeIds(null).isEmpty());
        verifyNoInteractions(managerRelationRepository, employeeRepository);
    }

    @Test
    void isManagedEmployeeReturnsTrueForActiveCurrentRelationAndValidTarget() {
        Employee valid = employee(2L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        when(managerRelationRepository
                .existsByManagerEmployee_EmployeeIdAndEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, 2L, RelationStatus.ACTIVE))
                .thenReturn(true);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(valid));

        assertTrue(service.isManagedEmployee(1L, 2L));
    }

    @Test
    void isManagedEmployeeReturnsFalseWhenRelationIsMissingOrEnded() {
        when(managerRelationRepository
                .existsByManagerEmployee_EmployeeIdAndEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, 2L, RelationStatus.ACTIVE))
                .thenReturn(false);

        assertFalse(service.isManagedEmployee(1L, 2L));
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void isManagedEmployeeReturnsFalseWhenTargetIsInvalid() {
        Employee retired = employee(2L, EmploymentStatus.RETIRED, null, AccountStatus.ACTIVE);
        when(managerRelationRepository
                .existsByManagerEmployee_EmployeeIdAndEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        1L, 2L, RelationStatus.ACTIVE))
                .thenReturn(true);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(retired));

        assertFalse(service.isManagedEmployee(1L, 2L));
    }

    @Test
    void findEmployeeSummariesReturnsFieldsForValidEmployeesOnly() {
        Employee valid = employee(1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee invalid = employee(2L, EmploymentStatus.LEAVE, null, AccountStatus.ACTIVE);
        stubSummary(valid, "Kim", 10L, "Engineering", 100L, "Junior");

        when(employeeRepository.findByEmployeeIdIn(Set.of(1L, 2L, 999L)))
                .thenReturn(List.of(valid, invalid));

        assertEquals(
                List.of(new EmployeeSummary(
                        1L, "Kim", 10L, "Engineering", 100L, "Junior")),
                service.findEmployeeSummaries(List.of(1L, 2L, 999L, 1L)));
    }

    @Test
    void findEmployeeSummariesReturnsEmptyWithoutQueryForNullOrEmptyInput() {
        assertTrue(service.findEmployeeSummaries(null).isEmpty());
        assertTrue(service.findEmployeeSummaries(List.of()).isEmpty());
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void findAppUserIdsByEmployeeIdsMapsValidEmployeesOnly() {
        Employee valid1 = employee(1L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee valid2 = employee(2L, EmploymentStatus.EMPLOYED, null, AccountStatus.ACTIVE);
        Employee inactive = employee(
                3L, EmploymentStatus.EMPLOYED, null, AccountStatus.INACTIVE);
        lenient().when(valid1.getAppUser().getAppUserId()).thenReturn(101L);
        lenient().when(valid2.getAppUser().getAppUserId()).thenReturn(102L);

        when(employeeRepository.findByEmployeeIdIn(Set.of(1L, 2L, 3L, 999L)))
                .thenReturn(List.of(valid1, inactive, valid2));

        assertEquals(
                Map.of(1L, 101L, 2L, 102L),
                service.findAppUserIdsByEmployeeIds(List.of(1L, 2L, 3L, 999L, 1L)));
    }

    @Test
    void findAppUserIdsByEmployeeIdsReturnsEmptyWithoutQueryForNullOrEmptyInput() {
        assertTrue(service.findAppUserIdsByEmployeeIds(null).isEmpty());
        assertTrue(service.findAppUserIdsByEmployeeIds(List.of()).isEmpty());
        verifyNoInteractions(employeeRepository);
    }

    private Employee employee(
            Long employeeId,
            EmploymentStatus employmentStatus,
            LocalDateTime deletedAt,
            AccountStatus accountStatus
    ) {
        Employee employee = mock(Employee.class);
        AppUser appUser = mock(AppUser.class);
        lenient().when(employee.getEmployeeId()).thenReturn(employeeId);
        lenient().when(employee.getEmploymentStatus()).thenReturn(employmentStatus);
        lenient().when(employee.getDeletedAt()).thenReturn(deletedAt);
        lenient().when(employee.getAppUser()).thenReturn(appUser);
        lenient().when(appUser.getAccountStatus()).thenReturn(accountStatus);
        return employee;
    }

    private ManagerRelation relation(Employee employee) {
        ManagerRelation relation = mock(ManagerRelation.class);
        when(relation.getEmployee()).thenReturn(employee);
        return relation;
    }

    private void stubSummary(
            Employee employee,
            String employeeName,
            Long departmentId,
            String departmentName,
            Long jobGradeId,
            String jobGradeName
    ) {
        Department department = mock(Department.class);
        JobGrade jobGrade = mock(JobGrade.class);
        when(employee.getEmployeeName()).thenReturn(employeeName);
        when(employee.getDepartment()).thenReturn(department);
        when(employee.getJobGrade()).thenReturn(jobGrade);
        when(department.getDepartmentId()).thenReturn(departmentId);
        when(department.getDepartmentName()).thenReturn(departmentName);
        when(jobGrade.getJobGradeId()).thenReturn(jobGradeId);
        when(jobGrade.getGradeName()).thenReturn(jobGradeName);
    }
}
