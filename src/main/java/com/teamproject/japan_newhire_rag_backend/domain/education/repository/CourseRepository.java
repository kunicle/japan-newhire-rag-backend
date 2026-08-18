package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Course> findByCourseIdAndDeletedAtIsNull(Long courseId);
}
