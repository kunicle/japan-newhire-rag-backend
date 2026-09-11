package com.teamproject.japan_newhire_rag_backend.domain.system.error.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto.SystemErrorPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto.SystemErrorResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.repository.SystemErrorLogRepository;

@Service @Transactional(readOnly = true)
public class SystemErrorQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private final SystemErrorLogRepository repository;
    public SystemErrorQueryService(SystemErrorLogRepository repository) { this.repository = repository; }
    public SystemErrorPageResponse findAll(int page, int size) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("Invalid page or size");
        Page<SystemErrorResponse> result = repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("systemErrorLogId"))))
                .map(SystemErrorResponse::from);
        return SystemErrorPageResponse.from(result);
    }
}
