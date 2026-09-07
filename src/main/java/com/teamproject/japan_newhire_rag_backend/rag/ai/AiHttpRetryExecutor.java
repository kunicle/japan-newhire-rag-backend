package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class AiHttpRetryExecutor {

    private static final int MAX_ATTEMPTS = 3;

    private final RetrySleeper sleeper;

    AiHttpRetryExecutor() {
        this(RetrySleeper.THREAD_SLEEP);
    }

    AiHttpRetryExecutor(RetrySleeper sleeper) {
        this.sleeper = sleeper;
    }

    <T> T execute(Supplier<T> call) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException exception) {
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }

                try {
                    sleeper.sleep(retryDelay(exception, attempt));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("AI HTTP retry 실행 상태가 올바르지 않습니다.");
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
