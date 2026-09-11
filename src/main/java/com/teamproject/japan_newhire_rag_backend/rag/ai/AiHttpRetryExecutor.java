package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class AiHttpRetryExecutor {

    private static final int MAX_ATTEMPTS = 3;

    private final RetrySleeper sleeper;
    private final Clock clock;

    AiHttpRetryExecutor() {
        this(RetrySleeper.THREAD_SLEEP, Clock.systemDefaultZone());
    }

    AiHttpRetryExecutor(RetrySleeper sleeper) {
        this(sleeper, Clock.systemDefaultZone());
    }

    AiHttpRetryExecutor(RetrySleeper sleeper, Clock clock) {
        this.sleeper = sleeper;
        this.clock = clock;
    }

    <T> T execute(Supplier<T> call) {
        try {
            return executeWithMetadata(call).result();
        } catch (AiHttpCallException exception) {
            throw exception.getOriginalFailure();
        }
    }

    <T> AiHttpExecution<T> executeWithMetadata(Supplier<T> call) {
        List<AiHttpAttempt> attempts = new ArrayList<>();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            LocalDateTime requestedAt = LocalDateTime.now(clock);
            try {
                T result = call.get();
                LocalDateTime completedAt = LocalDateTime.now(clock);
                attempts.add(successAttempt(attempt, requestedAt, completedAt));
                return new AiHttpExecution<>(result, attempts);
            } catch (RuntimeException exception) {
                LocalDateTime completedAt = LocalDateTime.now(clock);
                attempts.add(failedAttempt(attempt, exception, requestedAt, completedAt));
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    throw new AiHttpCallException(exception, attempts);
                }

                try {
                    sleeper.sleep(retryDelay(exception, attempt));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AiHttpCallException(exception, attempts);
                }
            }
        }
        throw new IllegalStateException("AI HTTP retry 실행 상태가 올바르지 않습니다.");
    }

    private AiHttpAttempt successAttempt(
            int attempt, LocalDateTime requestedAt, LocalDateTime completedAt) {
        return new AiHttpAttempt(attempt, "COMPLETED", null, null, null, null,
                durationMs(requestedAt, completedAt), requestedAt, completedAt);
    }

    private AiHttpAttempt failedAttempt(
            int attempt, RuntimeException exception,
            LocalDateTime requestedAt, LocalDateTime completedAt) {
        Integer httpStatusCode = null;
        String errorType = "UNEXPECTED_ERROR";
        String errorCode = null;
        if (exception instanceof RestClientResponseException responseException) {
            httpStatusCode = responseException.getStatusCode().value();
            errorCode = String.valueOf(httpStatusCode);
            errorType = httpStatusCode == 429 ? "RATE_LIMIT"
                    : responseException.getStatusCode().is5xxServerError() ? "HTTP_5XX" : "HTTP_4XX";
        } else if (exception instanceof ResourceAccessException) {
            errorType = "NETWORK_ERROR";
        }
        return new AiHttpAttempt(attempt, "FAILED", httpStatusCode, errorType, errorCode,
                exception.getClass().getSimpleName(), durationMs(requestedAt, completedAt),
                requestedAt, completedAt);
    }

    private int durationMs(LocalDateTime requestedAt, LocalDateTime completedAt) {
        long duration = Math.max(0, Duration.between(requestedAt, completedAt).toMillis());
        return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
    }

    private boolean isRetryable(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        if (exception instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || responseException.getStatusCode().is5xxServerError();
        }
        return false;
    }

    private Duration retryDelay(RuntimeException exception, int attempt) {
        if (exception instanceof RestClientResponseException responseException
                && responseException.getStatusCode().value() == 429) {
            Duration retryAfter = parseRetryAfter(responseException.getResponseHeaders());
            if (retryAfter != null) {
                return retryAfter;
            }
        }
        return Duration.ofSeconds(attempt);
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String headerValue = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(headerValue);
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
