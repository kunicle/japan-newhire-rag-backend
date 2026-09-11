package com.teamproject.japan_newhire_rag_backend.domain.system.error.service.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordService;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.entity.SystemErrorLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.repository.SystemErrorLogRepository;

@Service
public class SystemErrorRecordServiceImpl implements SystemErrorRecordService {
    private final SystemErrorLogRepository repository;
    public SystemErrorRecordServiceImpl(SystemErrorLogRepository repository) { this.repository = repository; }
    @Override @Transactional
    public void record(SystemErrorRecordCommand command) {
        if (command == null || command.errorSource() == null || command.errorSource().isBlank()
                || command.errorType() == null || command.errorType().isBlank()
                || command.errorMessage() == null || command.errorMessage().isBlank()
                || command.occurredAt() == null || command.retryCount() < 0 || command.retryCount() > 2) {
            throw new IllegalArgumentException("Invalid system error record command");
        }
        repository.save(SystemErrorLog.open(command.appUserId(), command.externalApiCallLogId(),
                command.errorSource(), command.errorType(), command.errorCode(), command.retryCount(),
                command.errorMessage(), command.occurredAt()));
    }
}
