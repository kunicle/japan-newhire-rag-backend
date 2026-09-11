package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.ExternalApiCallLog;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.ExternalApiCallLogRepository;

@Service
public class ExternalApiCallLogService {
    private final ExternalApiCallLogRepository repository;
    public ExternalApiCallLogService(ExternalApiCallLogRepository repository) { this.repository = repository; }
    @Transactional
    public Long record(ExternalApiCallLogCommand command) {
        validate(command);
        return repository.save(ExternalApiCallLog.record(command.aiModelId(), command.ragQuestionId(),
                command.documentProcessingJobId(), command.apiType(), command.callStatus(),
                command.attemptNumber(), command.httpStatusCode(), command.errorType(),
                command.errorMessage(), command.durationMs(), command.requestedAt(), command.completedAt()))
                .getExternalApiCallLogId();
    }
    private void validate(ExternalApiCallLogCommand command) {
        if (command == null || command.aiModelId() == null || command.aiModelId() <= 0
                || (command.ragQuestionId() == null && command.documentProcessingJobId() == null)
                || command.apiType() == null || command.callStatus() == null
                || command.attemptNumber() < 1 || command.attemptNumber() > 3
                || command.requestedAt() == null || command.durationMs() == null || command.durationMs() < 0) {
            throw new IllegalArgumentException("Invalid external API call log command");
        }
    }
}
