package com.teamproject.japan_newhire_rag_backend.domain.system.notification.api;

public record NotificationSendCommand(
        Long recipientAppUserId,
        String notificationType,
        String title,
        String message,
        String targetType,
        Long targetId
) {
    public NotificationSendCommand {
        require(recipientAppUserId != null, "recipientAppUserId is required");
        requireText(notificationType, 50, "notificationType");
        requireText(title, 200, "title");
        requireText(message, null, "message");
        if (targetType != null) {
            requireText(targetType, 50, "targetType");
        }
    }

    private static void requireText(String value, Integer maxLength, String field) {
        require(value != null && !value.isBlank(), field + " is required");
        if (maxLength != null) {
            require(value.length() <= maxLength, field + " must be at most " + maxLength + " characters");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
