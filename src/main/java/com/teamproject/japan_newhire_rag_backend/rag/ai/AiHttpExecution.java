package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public record AiHttpExecution<T>(T result, List<AiHttpAttempt> attempts) {

    public AiHttpExecution {
        attempts = List.copyOf(attempts);
    }
}
