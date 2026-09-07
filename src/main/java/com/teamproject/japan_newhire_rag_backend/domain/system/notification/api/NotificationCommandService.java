package com.teamproject.japan_newhire_rag_backend.domain.system.notification.api;

public interface NotificationCommandService {

    void send(NotificationSendCommand command);
}
