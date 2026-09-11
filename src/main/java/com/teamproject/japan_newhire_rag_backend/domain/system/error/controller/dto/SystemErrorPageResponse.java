package com.teamproject.japan_newhire_rag_backend.domain.system.error.controller.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record SystemErrorPageResponse(List<SystemErrorResponse> content, int page, int size,
        long totalElements, int totalPages) {
    public static SystemErrorPageResponse from(Page<SystemErrorResponse> page) {
        return new SystemErrorPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
