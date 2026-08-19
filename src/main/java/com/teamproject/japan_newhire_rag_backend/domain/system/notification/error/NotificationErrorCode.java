package com.teamproject.japan_newhire_rag_backend.domain.system.notification.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum NotificationErrorCode implements ErrorCodeSpec {
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification recipient not found"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found");

    private final HttpStatus status;
    private final String defaultMessage;

    NotificationErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String code() { return name(); }
    public String defaultMessage() { return defaultMessage; }
}
