package com.teamproject.japan_newhire_rag_backend.domain.organization.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;

public interface JobGradeRepository extends JpaRepository<JobGrade, Long> {

    boolean existsByJobGradeId(Long jobGradeId);

    Optional<JobGrade> findByGradeCode(String gradeCode);

    List<JobGrade> findByIsActiveTrueOrderByGradeLevelAsc();
}
