package com.teamproject.japan_newhire_rag_backend.domain.organization.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.organization.enums.DepartmentStatus;

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
@Table(name = "department")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long departmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    @Column(name = "department_code", nullable = false, unique = true, length = 30)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_status", nullable = false, length = 20)
    private DepartmentStatus departmentStatus;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
