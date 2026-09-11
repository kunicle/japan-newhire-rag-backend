package com.teamproject.japan_newhire_rag_backend.rag.orchestration;

import java.util.List;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpAttempt;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiHttpCallException;

public class ExternalAiCallException extends RuntimeException {

    private final RuntimeException originalFailure;

    public ExternalAiCallException(RuntimeException originalFailure) {
        super(originalFailure);
        this.originalFailure = originalFailure;
    }

    public RuntimeException getOriginalFailure() {
        return originalFailure instanceof AiHttpCallException aiException
                ? aiException.getOriginalFailure() : originalFailure;
    }

    public List<AiHttpAttempt> getAttempts() {
        return originalFailure instanceof AiHttpCallException aiException
                ? aiException.getAttempts() : List.of();
    }
}
