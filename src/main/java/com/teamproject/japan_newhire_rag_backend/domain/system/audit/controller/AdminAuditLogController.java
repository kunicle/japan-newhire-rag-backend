package com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.service.AuditLogQueryService;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminAuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    public AdminAuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public AuditLogPageResponse findAll(
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditLogQueryService.findAll(
                actionType, actorUserId, targetType, targetId, from, to, page, size);
    }
}
