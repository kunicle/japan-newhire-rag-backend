package com.teamproject.japan_newhire_rag_backend.domain.system.audit.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.controller.dto.AuditLogResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository.AuditLogRepository;

@Service
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LATEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("auditLogId"));

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLogPageResponse findAll(
            AuditActionType actionType,
            Long actorUserId,
            AuditTargetType targetType,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        validate(from, to, page, size);
        Page<AuditLogResponse> result = auditLogRepository
                .findAllByFilters(actionType, actorUserId, targetType, targetId, from, to,
                        PageRequest.of(page, size, LATEST_FIRST))
                .map(AuditLogResponse::from);
        return AuditLogPageResponse.from(result);
    }

    private void validate(LocalDateTime from, LocalDateTime to, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be at least 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
