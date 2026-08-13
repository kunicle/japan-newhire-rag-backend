package com.teamproject.japan_newhire_rag_backend.domain.organization.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationStatus;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.RelationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "manager_relation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagerRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_relation_id")
    private Long managerRelationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_employee_id", nullable = false)
    private Employee managerEmployee;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_status", nullable = false, length = 20)
    private RelationStatus relationStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    public static ManagerRelation createDirect(
            Employee employee,
            Employee managerEmployee,
            AppUser createdBy,
            LocalDateTime startedAt
    ) {
        ManagerRelation relation = new ManagerRelation();
        relation.employee = employee;
        relation.managerEmployee = managerEmployee;
        relation.relationType = RelationType.DIRECT;
        relation.startedAt = startedAt;
        relation.relationStatus = RelationStatus.ACTIVE;
        relation.createdBy = createdBy;
        return relation;
    }

    public void end(LocalDateTime endedAt) {
        if (relationStatus != RelationStatus.ACTIVE || this.endedAt != null) {
            throw new IllegalStateException("Only an active current relation can be ended");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not be before startedAt");
        }
        relationStatus = RelationStatus.ENDED;
        this.endedAt = endedAt;
    }
}
