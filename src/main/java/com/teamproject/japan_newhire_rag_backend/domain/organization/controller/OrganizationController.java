package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.OrganizationTreeQueryService;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationTreeQueryService organizationTreeQueryService;

    public OrganizationController(OrganizationTreeQueryService organizationTreeQueryService) {
        this.organizationTreeQueryService = organizationTreeQueryService;
    }

    @GetMapping
    public OrganizationResponse getOrganization() {
        return organizationTreeQueryService.getOrganizationTree();
    }
}
