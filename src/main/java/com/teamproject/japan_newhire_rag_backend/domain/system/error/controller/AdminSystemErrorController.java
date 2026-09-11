package com.teamproject.japan_newhire_rag_backend.domain.system.error.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto.SystemErrorPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.service.SystemErrorQueryService;

@RestController @RequestMapping("/api/admin/system-errors") @PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminSystemErrorController {
    private final SystemErrorQueryService queryService;
    public AdminSystemErrorController(SystemErrorQueryService queryService) { this.queryService = queryService; }
    @GetMapping public SystemErrorPageResponse findAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return queryService.findAll(page, size); }
}
