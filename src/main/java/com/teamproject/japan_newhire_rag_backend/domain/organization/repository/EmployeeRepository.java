package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.EmployeeType;

import jakarta.persistence.LockModeType;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Employee> findForUpdateByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    Optional<Employee> findByAppUser_AppUserId(Long appUserId);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByEmployeeNumber(String employeeNumber);

    @EntityGraph(attributePaths = {"department", "jobGrade"})
    List<Employee> findByDeletedAtIsNullAndDepartment_DeletedAtIsNull();

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByDepartment_DepartmentIdIn(Collection<Long> departmentIds);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByJobGrade_JobGradeIdIn(Collection<Long> jobGradeIds);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByEmployeeType(EmployeeType employeeType);

    @EntityGraph(attributePaths = {"appUser", "department", "jobGrade"})
    List<Employee> findByEmployeeIdIn(Collection<Long> employeeIds);
}
