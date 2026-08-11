package com.teamproject.japan_newhire_rag_backend.domain.organization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.EmployeeSummary;
import com.teamproject.japan_newhire_rag_backend.domain.organization.api.OrganizationQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmploymentStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.EmployeeRepository;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.ManagerRelationRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationQueryServiceImpl implements OrganizationQueryService {

    private final EmployeeRepository employeeRepository;
    private final ManagerRelationRepository managerRelationRepository;

    public OrganizationQueryServiceImpl(
            EmployeeRepository employeeRepository,
            ManagerRelationRepository managerRelationRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.managerRelationRepository = managerRelationRepository;
    }

    @Override
    public boolean isValidEmployee(Long employeeId) {
        if (employeeId == null) {
            return false;
        }

        return employeeRepository.findById(employeeId)
                .filter(this::isValid)
                .isPresent();
    }

    @Override
    public List<Long> findValidEmployeeIdsByDepartmentIds(Collection<Long> departmentIds) {
        Set<Long> normalizedIds = normalizeIds(departmentIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        return validEmployeeIds(
                employeeRepository.findByDepartment_DepartmentIdIn(normalizedIds));
    }

    @Override
    public List<Long> findValidEmployeeIdsByJobGradeIds(Collection<Long> jobGradeIds) {
        Set<Long> normalizedIds = normalizeIds(jobGradeIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        return validEmployeeIds(
                employeeRepository.findByJobGrade_JobGradeIdIn(normalizedIds));
    }

    @Override
    public List<Long> findValidNewHireEmployeeIds() {
        return validEmployeeIds(employeeRepository.findByEmployeeType(EmployeeType.NEW_HIRE));
    }

    @Override
    public List<Long> findManagedEmployeeIds(Long managerEmployeeId) {
        if (managerEmployeeId == null) {
            return List.of();
        }

        Set<Long> managedEmployeeIds = managerRelationRepository
                .findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        managerEmployeeId,
                        RelationStatus.ACTIVE)
                .stream()
                .map(ManagerRelation::getEmployee)
                .map(Employee::getEmployeeId)
                .collect(Collectors.toUnmodifiableSet());

        if (managedEmployeeIds.isEmpty()) {
            return List.of();
        }

        return validEmployeeIds(employeeRepository.findByEmployeeIdIn(managedEmployeeIds));
    }

    @Override
    public boolean isManagedEmployee(Long managerEmployeeId, Long employeeId) {
        if (managerEmployeeId == null || employeeId == null) {
            return false;
        }

        boolean hasActiveRelation = managerRelationRepository
                .existsByManagerEmployee_EmployeeIdAndEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
                        managerEmployeeId,
                        employeeId,
                        RelationStatus.ACTIVE);

        return hasActiveRelation && isValidEmployee(employeeId);
    }

    @Override
    public List<EmployeeSummary> findEmployeeSummaries(Collection<Long> employeeIds) {
        Set<Long> normalizedIds = normalizeIds(employeeIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        return employeeRepository.findByEmployeeIdIn(normalizedIds)
                .stream()
                .filter(this::isValid)
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Map<Long, Long> findAppUserIdsByEmployeeIds(Collection<Long> employeeIds) {
        Set<Long> normalizedIds = normalizeIds(employeeIds);
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        return employeeRepository.findByEmployeeIdIn(normalizedIds)
                .stream()
                .filter(this::isValid)
                .collect(Collectors.toUnmodifiableMap(
                        Employee::getEmployeeId,
                        employee -> employee.getAppUser().getAppUserId()));
    }

    private List<Long> validEmployeeIds(Collection<Employee> employees) {
        return employees.stream()
                .filter(this::isValid)
                .map(Employee::getEmployeeId)
                .distinct()
                .toList();
    }

    private boolean isValid(Employee employee) {
        return employee.getEmploymentStatus() == EmploymentStatus.EMPLOYED
                && employee.getDeletedAt() == null
                && employee.getAppUser() != null
                && employee.getAppUser().getAccountStatus() == AccountStatus.ACTIVE;
    }

    private EmployeeSummary toSummary(Employee employee) {
        return new EmployeeSummary(
                employee.getEmployeeId(),
                employee.getEmployeeName(),
                employee.getDepartment().getDepartmentId(),
                employee.getDepartment().getDepartmentName(),
                employee.getJobGrade().getJobGradeId(),
                employee.getJobGrade().getGradeName());
    }

    private Set<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
