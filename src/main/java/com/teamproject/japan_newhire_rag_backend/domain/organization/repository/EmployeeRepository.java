package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByAppUser_AppUserId(Long appUserId);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByEmployeeNumber(String employeeNumber);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByDepartment_DepartmentIdIn(Collection<Long> departmentIds);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByJobGrade_JobGradeIdIn(Collection<Long> jobGradeIds);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByEmployeeType(EmployeeType employeeType);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByEmployeeIdIn(Collection<Long> employeeIds);
}
