package com.teamproject.japan_newhire_rag_backend.domain.education.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.common.entity.BaseEntity;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.CoursePublicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false, length = 100)
    private String courseName;

    @Column(name = "course_description", nullable = false, length = 2000)
    private String courseDescription;

    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    @Column(name = "training_start_date", nullable = false)
    private LocalDate trainingStartDate;

    @Column(name = "training_end_date", nullable = false)
    private LocalDate trainingEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private CoursePublicationStatus publicationStatus = CoursePublicationStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
