package com.teamproject.japan_newhire_rag_backend.domain.organization.entity;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "job_grade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_grade_id")
    private Long jobGradeId;

    @Column(name = "grade_code", nullable = false, unique = true, length = 30)
    private String gradeCode;

    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName;

    @Column(name = "grade_level", nullable = false)
    private int gradeLevel;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
