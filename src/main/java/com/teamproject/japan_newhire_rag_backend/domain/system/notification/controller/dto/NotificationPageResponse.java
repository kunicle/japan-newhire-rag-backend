package com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record NotificationPageResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static NotificationPageResponse from(Page<NotificationResponse> result) {
        return new NotificationPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
