package com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;
import com.teamproject.japan_newhire_rag_backend.domain.organization.repository.JobGradeRepository;

class JobGradeQueryServiceTest {

    private JobGradeRepository jobGradeRepository;
    private JobGradeQueryService service;

    @BeforeEach
    void setUp() {
        jobGradeRepository = mock(JobGradeRepository.class);
        service = new JobGradeQueryService(jobGradeRepository);
    }

    @Test
    void returnsActiveJobGradesInRepositoryOrder() {
        JobGrade level1 = mock(JobGrade.class);
        JobGrade level2 = mock(JobGrade.class);
        JobGrade level3 = mock(JobGrade.class);
        List<JobGrade> grades = List.of(level1, level2, level3);
        when(jobGradeRepository.findByIsActiveTrueOrderByGradeLevelAsc())
                .thenReturn(grades);

        List<JobGrade> result = service.getActiveJobGrades();

        assertEquals(grades, result);
        verify(jobGradeRepository).findByIsActiveTrueOrderByGradeLevelAsc();
    }

    @Test
    void returnsEmptyListWhenNoActiveJobGradesExist() {
        when(jobGradeRepository.findByIsActiveTrueOrderByGradeLevelAsc())
                .thenReturn(List.of());

        List<JobGrade> result = service.getActiveJobGrades();

        assertTrue(result.isEmpty());
        verify(jobGradeRepository).findByIsActiveTrueOrderByGradeLevelAsc();
    }
}
