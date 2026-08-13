package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.ManagerRelation;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;

public interface ManagerRelationRepository extends JpaRepository<ManagerRelation, Long> {

    @EntityGraph(attributePaths = {"employee", "managerEmployee"})
    List<ManagerRelation> findByEmployee_EmployeeIdAndRelationTypeAndRelationStatusAndEndedAtIsNull(
            Long employeeId,
            RelationType relationType,
            RelationStatus relationStatus
    );

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
