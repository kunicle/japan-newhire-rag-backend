package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;

@Service
@Transactional(readOnly = true)
public class JobGradeQueryService {

    private final JobGradeRepository jobGradeRepository;

    public JobGradeQueryService(JobGradeRepository jobGradeRepository) {
        this.jobGradeRepository = jobGradeRepository;
    }

    public List<JobGrade> getActiveJobGrades() {
        return jobGradeRepository.findByIsActiveTrueOrderByGradeLevelAsc();
    }
}
