package com.teamproject.japan_newhire_rag_backend.domain.education.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.domain.education.entity.CourseEnrollment;
import com.teamproject.japan_newhire_rag_backend.domain.education.enums.EnrollmentStatus;
import java.time.LocalDate;

import jakarta.persistence.LockModeType;

public interface CourseEnrollmentRepository
        extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByCourse_CourseId(Long courseId);

    boolean existsByCourse_CourseIdAndEmployeeId(
        Long courseId,
        Long employeeId);

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
    Page<CourseEnrollment> findAllByEmployeeIdIn(
        Collection<Long> employeeIds,
        Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Optional<CourseEnrollment> findByCourseEnrollmentId(
            Long courseEnrollmentId);

    @EntityGraph(attributePaths = "course")
    List<CourseEnrollment> findAllByEnrollmentDueDateAndEnrollmentStatusNot(
            LocalDate enrollmentDueDate,
            EnrollmentStatus enrollmentStatus);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @EntityGraph(attributePaths = "course")
        @Query("""
                select enrollment
                from CourseEnrollment enrollment
                where enrollment.courseEnrollmentId = :enrollmentId
                """)
        Optional<CourseEnrollment> findByCourseEnrollmentIdForUpdate(
                @Param("enrollmentId") Long enrollmentId);
}