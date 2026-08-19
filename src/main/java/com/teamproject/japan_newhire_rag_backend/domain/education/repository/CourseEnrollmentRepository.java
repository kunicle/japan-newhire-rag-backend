package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;

public interface CourseEnrollmentRepository
        extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByCourse_CourseId(Long courseId);

    List<CourseEnrollment>
    findAllByCourse_CourseIdAndEmployeeIdInAndEnrollmentRound(
            Long courseId,
            Collection<Long> employeeIds,
            String enrollmentRound);

    @EntityGraph(attributePaths = "course")
    Page<CourseEnrollment> findAllByEmployeeId(
            Long employeeId,
            Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Optional<CourseEnrollment> findByCourseEnrollmentId(
            Long courseEnrollmentId);
}