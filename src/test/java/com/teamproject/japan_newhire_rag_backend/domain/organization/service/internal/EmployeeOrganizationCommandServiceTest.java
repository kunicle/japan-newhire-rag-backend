package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.*;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.*;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.*;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.*;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.*;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.api.*;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import org.mockito.ArgumentCaptor;

class EmployeeOrganizationCommandServiceTest {
    EmployeeRepository employees = mock(EmployeeRepository.class);
    DepartmentRepository departments = mock(DepartmentRepository.class);
    JobGradeRepository grades = mock(JobGradeRepository.class);
    DirectManagerCommandService managers = mock(DirectManagerCommandService.class);
    CurrentUserProvider user = mock(CurrentUserProvider.class);
    AuditLogRecordService audit = mock(AuditLogRecordService.class);
    ManagerRelationRepository relations = mock(ManagerRelationRepository.class);
    EmployeeOrganizationCommandService service = new EmployeeOrganizationCommandService(
            employees, departments, grades, managers, user, audit, relations);
    Employee employee;
    Department before, after;
    JobGrade junior, senior;

    @BeforeEach
    void setup() {
        before = Department.create("A", "A", null);
        after = Department.create("B", "B", null);
        ReflectionTestUtils.setField(before, "departmentId", 1L);
        ReflectionTestUtils.setField(after, "departmentId", 2L);
        junior = grade(1L, 5);
        senior = grade(2L, 2);
        employee = Employee.createEmployed(mock(AppUser.class), before, junior,
                "E10", "Employee", EmployeeType.GENERAL, LocalDate.of(2024, 1, 1));
        ReflectionTestUtils.setField(employee, "employeeId", 10L);
        when(employees.findForUpdateByEmployeeId(10L)).thenReturn(Optional.of(employee));
        when(departments.findById(1L)).thenReturn(Optional.of(before));
        when(departments.findById(2L)).thenReturn(Optional.of(after));
        when(grades.findById(1L)).thenReturn(Optional.of(junior));
        when(grades.findById(2L)).thenReturn(Optional.of(senior));
        when(user.getCurrentUser()).thenReturn(new CurrentUserContext(100L, 99L, Set.of(RoleType.HR_MANAGER), null, null, null));
    }

    @Test void changesDepartmentAndAudits() {
        service.changeOrganization(10L, new ChangeEmployeeOrganizationRequest(2L, 1L, 20L, EmploymentStatus.EMPLOYED));
        assertSame(after, employee.getDepartment());
        assertSame(junior, employee.getJobGrade());
        verify(managers).changeManager(10L, 20L);
        var captor = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(audit).record(captor.capture());
        assertEquals(AuditActionType.EMPLOYEE_DEPARTMENT_CHANGED, captor.getValue().actionType());
        assertEquals(Map.of("departmentId", 1L), captor.getValue().previousValue());
        assertEquals(Map.of("departmentId", 2L), captor.getValue().changedValue());
    }

    @Test void changesGradeBeforeValidatingManagerAndAudits() {
        doAnswer(invocation -> { assertSame(senior, employee.getJobGrade()); return null; })
                .when(managers).changeManager(10L, 20L);
        service.changeOrganization(10L, new ChangeEmployeeOrganizationRequest(1L, 2L, 20L, EmploymentStatus.EMPLOYED));
        assertSame(senior, employee.getJobGrade());
        var captor = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(audit).record(captor.capture());
        assertEquals(AuditActionType.EMPLOYEE_JOB_GRADE_CHANGED, captor.getValue().actionType());
    }

    @Test void delegatesManagerRemovalWithoutRedundantAudit() {
        service.changeOrganization(10L, new ChangeEmployeeOrganizationRequest(1L, 1L, null, EmploymentStatus.EMPLOYED));
        verify(managers).changeManager(10L, null);
        verifyNoInteractions(audit);
    }

    @Test void missingReferencesUseNotFoundPolicy() {
        assertCode(OrganizationErrorCode.EMPLOYEE_NOT_FOUND, 99L, 1L, 1L);
        assertCode(OrganizationErrorCode.DEPARTMENT_NOT_FOUND, 10L, 99L, 1L);
        assertCode(OrganizationErrorCode.JOB_GRADE_NOT_FOUND, 10L, 1L, 99L);
        verifyNoInteractions(managers, audit);
    }

    @Test void rejectsDemotionBelowDirectReport() {
        Employee report = Employee.createEmployed(mock(AppUser.class), before, grade(3L, 1),
                "R", "Report", EmployeeType.GENERAL, LocalDate.now());
        var relation = ManagerRelation.createDirect(report, employee, mock(AppUser.class), LocalDateTime.now());
        when(relations.findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(10L, RelationStatus.ACTIVE))
                .thenReturn(List.of(relation));
        assertCode(OrganizationErrorCode.MANAGER_GRADE_NOT_ALLOWED, 10L, 1L, 2L);
        assertSame(junior, employee.getJobGrade());
        verifyNoInteractions(managers, audit);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "EMPLOYED,LEAVE", "LEAVE,EMPLOYED", "EMPLOYED,RETIRED"
    })
    void changesEmploymentStatusAndAuditsWithoutDeletingEmployee(
            EmploymentStatus beforeStatus, EmploymentStatus afterStatus) {
        employee.changeEmploymentStatus(beforeStatus);
        AppUser appUser = employee.getAppUser();
        service.changeOrganization(10L,
                new ChangeEmployeeOrganizationRequest(1L, 1L, null, afterStatus));

        assertEquals(afterStatus, employee.getEmploymentStatus());
        assertNull(employee.getDeletedAt());
        assertSame(employee, employees.findForUpdateByEmployeeId(10L).orElseThrow());
        assertSame(appUser, employee.getAppUser());
        verifyNoInteractions(appUser);
        verify(employees, never()).delete(any(Employee.class));
        verify(employees, never()).deleteById(any());
        var captor = ArgumentCaptor.forClass(AuditLogRecordCommand.class);
        verify(audit).record(captor.capture());
        var record = captor.getValue();
        assertEquals(AuditActionType.EMPLOYEE_EMPLOYMENT_STATUS_CHANGED, record.actionType());
        assertEquals(10L, record.targetId());
        assertEquals(100L, record.actorUserId());
        assertEquals(Map.of("employmentStatus", beforeStatus.name()), record.previousValue());
        assertEquals(Map.of("employmentStatus", afterStatus.name()), record.changedValue());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(EmploymentStatus.class)
    void unchangedEmploymentStatusDoesNotAudit(EmploymentStatus status) {
        employee.changeEmploymentStatus(status);
        service.changeOrganization(10L,
                new ChangeEmployeeOrganizationRequest(1L, 1L, null, status));
        assertEquals(status, employee.getEmploymentStatus());
        verifyNoInteractions(audit);
    }

    @Test
    void entityRejectsNullEmploymentStatus() {
        assertThrows(IllegalArgumentException.class, () -> employee.changeEmploymentStatus(null));
        assertEquals(EmploymentStatus.EMPLOYED, employee.getEmploymentStatus());
    }

    @Test
    void changesOrganizationAndStatusTogether() {
        service.changeOrganization(10L,
                new ChangeEmployeeOrganizationRequest(2L, 2L, 20L, EmploymentStatus.LEAVE));
        assertSame(after, employee.getDepartment());
        assertSame(senior, employee.getJobGrade());
        assertEquals(EmploymentStatus.LEAVE, employee.getEmploymentStatus());
        verify(managers).changeManager(10L, 20L);
        verify(audit, times(3)).record(any());
    }

    private void assertCode(OrganizationErrorCode code, Long employeeId, Long departmentId, Long gradeId) {
        assertEquals(code, assertThrows(BusinessException.class, () -> service.changeOrganization(
                employeeId, new ChangeEmployeeOrganizationRequest(departmentId, gradeId, null, EmploymentStatus.EMPLOYED))).getErrorCode());
    }

    private JobGrade grade(Long id, int level) {
        JobGrade grade = mock(JobGrade.class);
        when(grade.getJobGradeId()).thenReturn(id);
        when(grade.getGradeLevel()).thenReturn(level);
        when(grade.isActive()).thenReturn(true);
        return grade;
    }
}
