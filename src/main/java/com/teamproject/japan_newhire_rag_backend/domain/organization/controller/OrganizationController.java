package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.JobGradeReferenceResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.JobGradeQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.OrganizationTreeQueryService;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationTreeQueryService organizationTreeQueryService;
    private final JobGradeQueryService jobGradeQueryService;

    public OrganizationController(
            OrganizationTreeQueryService organizationTreeQueryService,
            JobGradeQueryService jobGradeQueryService
    ) {
        this.organizationTreeQueryService = organizationTreeQueryService;
        this.jobGradeQueryService = jobGradeQueryService;
    }

    @GetMapping
    public OrganizationResponse getOrganization() {
        return organizationTreeQueryService.getOrganizationTree();
    }

    @GetMapping("/job-grades")
    public List<JobGradeReferenceResponse> getJobGrades() {
        return jobGradeQueryService.getActiveJobGrades().stream()
                .map(JobGradeReferenceResponse::from)
                .toList();
    }
}
