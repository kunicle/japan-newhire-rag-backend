package com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto.NotificationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto.NotificationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse findMine(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.findMine(read, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markMineAsRead(@PathVariable Long notificationId) {
        return notificationService.markMineAsRead(notificationId);
    }
}
