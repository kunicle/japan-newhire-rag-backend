package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationDepartmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;

class OrganizationTreeQueryServiceTest {

    private DepartmentRepository departmentRepository;
    private EmployeeRepository employeeRepository;
    private OrganizationTreeQueryService service;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new OrganizationTreeQueryService(departmentRepository, employeeRepository);
    }

    @Test
    void buildsSortedHierarchyAndPlacesEmployeesWithJobGrades() {
        Department rootSecond = department(2L, "ROOT-2", "Second", null, 2, null);
        Department rootFirst = department(1L, "ROOT-1", "First", null, 1, null);
        Department childSecond = department(12L, "CHILD-2", "Child 2", rootFirst, 5, null);
        Department childFirst = department(11L, "CHILD-1", "Child 1", rootFirst, 5, null);
        Employee employeeB = employee(102L, "E-002", "Lee", childFirst, 202L, "Senior", 3);
        Employee employeeA = employee(101L, "E-001", "Kim", childFirst, 201L, "Junior", 1);
        when(departmentRepository.findByDeletedAtIsNull())
                .thenReturn(List.of(rootSecond, childSecond, rootFirst, childFirst));
        when(employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull())
                .thenReturn(List.of(employeeB, employeeA));

        OrganizationResponse response = service.getOrganizationTree();

        assertEquals(List.of(1L, 2L), response.departments().stream()
                .map(OrganizationDepartmentResponse::departmentId).toList());
        assertEquals(List.of(11L, 12L), response.departments().get(0).children().stream()
                .map(OrganizationDepartmentResponse::departmentId).toList());
        List<OrganizationEmployeeResponse> employees = response.departments().get(0)
                .children().get(0).employees();
        assertEquals(List.of("E-001", "E-002"), employees.stream()
                .map(OrganizationEmployeeResponse::employeeNumber).toList());
        assertEquals(201L, employees.get(0).jobGradeId());
        assertEquals("Junior", employees.get(0).jobGradeName());
        assertEquals(1, employees.get(0).jobGradeLevel());
        assertEquals(LocalDate.of(2026, 1, 2), employees.get(0).hireDate());
    }

    @Test
    void returnsEmptyEmployeesForDepartmentWithoutEmployees() {
        Department root = department(1L, "ROOT", "Root", null, 0, null);
        when(departmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(root));
        when(employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull())
                .thenReturn(List.of());

        OrganizationResponse response = service.getOrganizationTree();

        assertTrue(response.departments().get(0).employees().isEmpty());
    }

    @Test
    void returnsEmptyResponseWithoutDepartmentsAndSkipsEmployeeQuery() {
        when(departmentRepository.findByDeletedAtIsNull()).thenReturn(List.of());

        OrganizationResponse response = service.getOrganizationTree();

        assertTrue(response.departments().isEmpty());
        verify(departmentRepository).findByDeletedAtIsNull();
    }

    @Test
    void defensivelyExcludesDeletedDepartmentsAndTheirEmployees() {
        Department active = department(1L, "ACTIVE", "Active", null, 0, null);
        Department deleted = department(
                2L, "DELETED", "Deleted", null, 1, LocalDateTime.of(2026, 1, 1, 0, 0));
        Employee deletedDepartmentEmployee = employee(
                10L, "E-001", "Kim", deleted, 100L, "Junior", 1);
        when(departmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(active, deleted));
        when(employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull())
                .thenReturn(List.of(deletedDepartmentEmployee));

        OrganizationResponse response = service.getOrganizationTree();

        assertEquals(List.of(1L), response.departments().stream()
                .map(OrganizationDepartmentResponse::departmentId).toList());
        assertTrue(response.departments().get(0).employees().isEmpty());
    }

    @Test
    void defensivelyExcludesDeletedEmployees() {
        Department root = department(1L, "ROOT", "Root", null, 0, null);
        Employee deleted = employee(10L, "E-001", "Kim", root, 100L, "Junior", 1);
        when(deleted.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(departmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(root));
        when(employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull())
                .thenReturn(List.of(deleted));

        assertTrue(service.getOrganizationTree().departments().get(0).employees().isEmpty());
    }

    @Test
    void rejectsDepartmentCycleWithoutRecursingForever() {
        Department first = mock(Department.class);
        Department second = mock(Department.class);
        stubDepartment(first, 1L, "FIRST", "First", second, 0, null);
        stubDepartment(second, 2L, "SECOND", "Second", first, 0, null);
        when(departmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(first, second));
        when(employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull())
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, service::getOrganizationTree);

        assertEquals(OrganizationErrorCode.ORGANIZATION_DATA_CONFLICT,
                exception.getErrorCode());
    }

    private Department department(
            Long id,
            String code,
            String name,
            Department parent,
            int displayOrder,
            LocalDateTime deletedAt
    ) {
        Department department = mock(Department.class);
        stubDepartment(department, id, code, name, parent, displayOrder, deletedAt);
        return department;
    }

    private void stubDepartment(
            Department department,
            Long id,
            String code,
            String name,
            Department parent,
            int displayOrder,
            LocalDateTime deletedAt
    ) {
        when(department.getDepartmentId()).thenReturn(id);
        when(department.getDepartmentCode()).thenReturn(code);
        when(department.getDepartmentName()).thenReturn(name);
        when(department.getParentDepartment()).thenReturn(parent);
        when(department.getDisplayOrder()).thenReturn(displayOrder);
        when(department.getDeletedAt()).thenReturn(deletedAt);
    }

    private Employee employee(
            Long id,
            String number,
            String name,
            Department department,
            Long gradeId,
            String gradeName,
            int gradeLevel
    ) {
        Employee employee = mock(Employee.class);
        JobGrade jobGrade = mock(JobGrade.class);
        when(employee.getEmployeeId()).thenReturn(id);
        when(employee.getEmployeeNumber()).thenReturn(number);
        when(employee.getEmployeeName()).thenReturn(name);
        when(employee.getDepartment()).thenReturn(department);
        when(employee.getJobGrade()).thenReturn(jobGrade);
        when(employee.getHireDate()).thenReturn(LocalDate.of(2026, 1, 2));
        when(jobGrade.getJobGradeId()).thenReturn(gradeId);
        when(jobGrade.getGradeName()).thenReturn(gradeName);
        when(jobGrade.getGradeLevel()).thenReturn(gradeLevel);
        return employee;
    }
}
