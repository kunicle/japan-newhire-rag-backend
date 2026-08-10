package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByDepartmentId(Long departmentId);

    Optional<Department> findByDepartmentCode(String departmentCode);
}
