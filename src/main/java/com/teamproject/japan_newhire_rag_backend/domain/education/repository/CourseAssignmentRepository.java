package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseAssignment;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {
}
