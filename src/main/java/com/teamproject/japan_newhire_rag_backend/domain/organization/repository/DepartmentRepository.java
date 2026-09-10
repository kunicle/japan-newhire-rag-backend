package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.Department;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Department d order by d.departmentId")
    List<Department> lockDepartments();

    @EntityGraph(attributePaths = "parentDepartment")
    List<Department> findByDeletedAtIsNull();

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByDepartmentIdAndDepartmentStatusAndDeletedAtIsNull(
            Long departmentId,
            DepartmentStatus departmentStatus);

    Optional<Department> findByDepartmentCode(String departmentCode);
}
