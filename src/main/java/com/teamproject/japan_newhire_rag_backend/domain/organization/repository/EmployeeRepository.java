package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByAppUser_AppUserId(Long appUserId);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByEmployeeNumber(String employeeNumber);
}
