package com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.organization.entity.JobGrade;

public record JobGradeReferenceResponse(
        Long jobGradeId,
        String jobGradeCode,
        String jobGradeName,
        Integer jobGradeLevel) {

    public static JobGradeReferenceResponse from(JobGrade jobGrade) {
        return new JobGradeReferenceResponse(
                jobGrade.getJobGradeId(),
                jobGrade.getGradeCode(),
                jobGrade.getGradeName(),
                jobGrade.getGradeLevel());
    }
}
