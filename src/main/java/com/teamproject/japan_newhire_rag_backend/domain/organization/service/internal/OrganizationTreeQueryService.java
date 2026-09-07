package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationDepartmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationEmployeeResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.error.OrganizationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.DepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationTreeQueryService {

    private static final Comparator<Department> DEPARTMENT_ORDER = Comparator
            .comparingInt(Department::getDisplayOrder)
            .thenComparing(Department::getDepartmentId);

    private static final Comparator<OrganizationEmployeeResponse> EMPLOYEE_ORDER = Comparator
            .comparing(OrganizationEmployeeResponse::employeeNumber)
            .thenComparing(OrganizationEmployeeResponse::employeeId);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public OrganizationTreeQueryService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public OrganizationResponse getOrganizationTree() {
        List<Department> departments = departmentRepository.findByDeletedAtIsNull().stream()
                .filter(department -> department.getDeletedAt() == null)
                .toList();

        if (departments.isEmpty()) {
            return new OrganizationResponse(List.of());
        }

        Map<Long, Department> departmentsById = indexDepartments(departments);
        Map<Long, List<Department>> childrenByParentId = groupChildren(
                departments, departmentsById);
        Map<Long, List<OrganizationEmployeeResponse>> employeesByDepartmentId =
                groupEmployees(departmentsById);

        List<OrganizationDepartmentResponse> roots = departments.stream()
                .filter(department -> department.getParentDepartment() == null)
                .sorted(DEPARTMENT_ORDER)
                .map(department -> buildDepartment(
                        department,
                        childrenByParentId,
                        employeesByDepartmentId,
                        new HashSet<>()))
                .toList();

        if (countDepartments(roots) != departments.size()) {
            throw dataConflict();
        }

        return new OrganizationResponse(roots);
    }

    private Map<Long, Department> indexDepartments(List<Department> departments) {
        Map<Long, Department> departmentsById = new HashMap<>();
        for (Department department : departments) {
            if (department.getDepartmentId() == null
                    || departmentsById.put(department.getDepartmentId(), department) != null) {
                throw dataConflict();
            }
        }
        return departmentsById;
    }

    private Map<Long, List<Department>> groupChildren(
            List<Department> departments,
            Map<Long, Department> departmentsById
    ) {
        Map<Long, List<Department>> childrenByParentId = new HashMap<>();
        for (Department department : departments) {
            Department parent = department.getParentDepartment();
            if (parent == null) {
                continue;
            }
            Long parentId = parent.getDepartmentId();
            if (!departmentsById.containsKey(parentId)) {
                throw dataConflict();
            }
            childrenByParentId.computeIfAbsent(parentId, ignored -> new ArrayList<>())
                    .add(department);
        }
        childrenByParentId.values().forEach(children -> children.sort(DEPARTMENT_ORDER));
        return childrenByParentId;
    }

    private Map<Long, List<OrganizationEmployeeResponse>> groupEmployees(
            Map<Long, Department> departmentsById
    ) {
        Map<Long, List<OrganizationEmployeeResponse>> employeesByDepartmentId = new HashMap<>();
        employeeRepository.findByDeletedAtIsNullAndDepartment_DeletedAtIsNull().stream()
                .filter(employee -> employee.getDeletedAt() == null)
                .filter(employee -> departmentsById.containsKey(
                        employee.getDepartment().getDepartmentId()))
                .map(this::toEmployeeResponse)
                .forEach(employee -> employeesByDepartmentId
                        .computeIfAbsent(employee.departmentId(), ignored -> new ArrayList<>())
                        .add(employee));
        employeesByDepartmentId.values().forEach(employees -> employees.sort(EMPLOYEE_ORDER));
        return employeesByDepartmentId;
    }

    private OrganizationDepartmentResponse buildDepartment(
            Department department,
            Map<Long, List<Department>> childrenByParentId,
            Map<Long, List<OrganizationEmployeeResponse>> employeesByDepartmentId,
            Set<Long> path
    ) {
        Long departmentId = department.getDepartmentId();
        if (!path.add(departmentId)) {
            throw dataConflict();
        }

        List<OrganizationDepartmentResponse> children = childrenByParentId
                .getOrDefault(departmentId, List.of())
                .stream()
                .map(child -> buildDepartment(
                        child, childrenByParentId, employeesByDepartmentId, path))
                .toList();
        path.remove(departmentId);

        Department parent = department.getParentDepartment();
        return new OrganizationDepartmentResponse(
                departmentId,
                department.getDepartmentCode(),
                department.getDepartmentName(),
                parent == null ? null : parent.getDepartmentId(),
                department.getDisplayOrder(),
                employeesByDepartmentId.getOrDefault(departmentId, List.of()),
                children);
    }

    private OrganizationEmployeeResponse toEmployeeResponse(Employee employee) {
        return new OrganizationEmployeeResponse(
                employee.getEmployeeId(),
                employee.getEmployeeNumber(),
                employee.getEmployeeName(),
                employee.getDepartment().getDepartmentId(),
                employee.getJobGrade().getJobGradeId(),
                employee.getJobGrade().getGradeName(),
                employee.getJobGrade().getGradeLevel(),
                employee.getHireDate());
    }

    private int countDepartments(List<OrganizationDepartmentResponse> departments) {
        return departments.stream()
                .mapToInt(department -> 1 + countDepartments(department.children()))
                .sum();
    }

    private BusinessException dataConflict() {
        return new BusinessException(OrganizationErrorCode.ORGANIZATION_DATA_CONFLICT);
    }
}
