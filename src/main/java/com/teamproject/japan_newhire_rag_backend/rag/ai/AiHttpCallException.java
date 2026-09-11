package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.util.List;

public class AiHttpCallException extends RuntimeException {

    private final RuntimeException originalFailure;
    private final List<AiHttpAttempt> attempts;

    AiHttpCallException(RuntimeException originalFailure, List<AiHttpAttempt> attempts) {
        super(originalFailure);
        this.originalFailure = originalFailure;
        this.attempts = List.copyOf(attempts);
    }

    public RuntimeException getOriginalFailure() { return originalFailure; }

    public List<AiHttpAttempt> getAttempts() { return attempts; }
}
