package com.teamproject.japan_newhire_rag_backend.domain.system.error.api;

public interface SystemErrorRecordService {
    void record(SystemErrorRecordCommand command);
}
