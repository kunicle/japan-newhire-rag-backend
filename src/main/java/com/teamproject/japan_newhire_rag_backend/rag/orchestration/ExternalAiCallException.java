package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

public class ExternalAiCallException extends RuntimeException {

    private final RuntimeException originalFailure;

    public ExternalAiCallException(RuntimeException originalFailure) {
        super(originalFailure);
        this.originalFailure = originalFailure;
    }

    public RuntimeException getOriginalFailure() {
        return originalFailure;
    }
}
