package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;

public interface ManagerRelationRepository extends JpaRepository<ManagerRelation, Long> {

    List<ManagerRelation> findByManagerEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
            Long managerEmployeeId,
            RelationStatus relationStatus
    );

    boolean existsByManagerEmployee_EmployeeIdAndEmployee_EmployeeIdAndRelationStatusAndEndedAtIsNull(
            Long managerEmployeeId,
            Long employeeId,
            RelationStatus relationStatus
    );
}
