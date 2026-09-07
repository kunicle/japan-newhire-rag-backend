package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class AiHttpRetryExecutorTest {

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void retriesResourceAccessExceptionThreeTimes() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        ResourceAccessException failure = new ResourceAccessException("connection failed");
        AtomicInteger attempts = new AtomicInteger();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(3, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 502, 503, 504})
    void retriesConfiguredHttpStatusesThreeTimes(int status) {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        RestClientResponseException failure = responseException(status, new HttpHeaders());
        AtomicInteger attempts = new AtomicInteger();

        RestClientResponseException thrown = assertThrows(
                RestClientResponseException.class,
                () -> executor.execute(() -> {
                    attempts.incrementAndGet();
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(3, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 408})
    void doesNotRetryOtherClientErrors(int status) {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        RestClientResponseException failure = responseException(status, new HttpHeaders());
        AtomicInteger attempts = new AtomicInteger();

        RestClientResponseException thrown = assertThrows(
                RestClientResponseException.class,
                () -> executor.execute(() -> {
                    attempts.incrementAndGet();
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(1, attempts.get());
        assertEquals(List.of(), sleeper.durations);
    }

    @Test
    void doesNotRetryUnrelatedRuntimeException() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        IllegalStateException failure = new IllegalStateException("malformed response");
        AtomicInteger attempts = new AtomicInteger();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(1, attempts.get());
        assertEquals(List.of(), sleeper.durations);
    }

    @Test
    void returnsSuccessFromSecondAttempt() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw responseException(500, new HttpHeaders());
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(2, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1)), sleeper.durations);
    }

    @Test
    void returnsSuccessFromThirdAttempt() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw responseException(503, new HttpHeaders());
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
    }

    @Test
    void usesRetryAfterSecondsForTooManyRequests() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        HttpHeaders headers = retryAfter("5");
        AtomicInteger attempts = new AtomicInteger();

        executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw responseException(429, headers);
            }
            return "success";
        });

        assertEquals(List.of(Duration.ofSeconds(5)), sleeper.durations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "-1"})
    void usesDefaultDelayForInvalidRetryAfter(String headerValue) {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        HttpHeaders headers = retryAfter(headerValue);
        AtomicInteger attempts = new AtomicInteger();

        executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw responseException(429, headers);
            }
            return "success";
        });

        assertEquals(List.of(Duration.ofSeconds(1)), sleeper.durations);
    }

    @Test
    void usesDefaultDelayWhenRetryAfterIsAbsent() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        AtomicInteger attempts = new AtomicInteger();

        executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw responseException(429, new HttpHeaders());
            }
            return "success";
        });

        assertEquals(List.of(Duration.ofSeconds(1)), sleeper.durations);
    }

    @Test
    void usesRetryAfterOnEveryRetry() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        AtomicInteger attempts = new AtomicInteger();

        executor.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                throw responseException(429, retryAfter("3"));
            }
            if (attempt == 2) {
                throw responseException(429, retryAfter("5"));
            }
            return "success";
        });

        assertEquals(List.of(Duration.ofSeconds(3), Duration.ofSeconds(5)), sleeper.durations);
    }

    @Test
    void stopsRetryingWhenSleepIsInterrupted() {
        RecordingSleeper sleeper = new RecordingSleeper();
        sleeper.interrupt = true;
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        ResourceAccessException failure = new ResourceAccessException("connection failed");
        AtomicInteger attempts = new AtomicInteger();

        ResourceAccessException thrown = assertThrows(
                ResourceAccessException.class,
                () -> executor.execute(() -> {
                    attempts.incrementAndGet();
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(1, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1)), sleeper.durations);
        assertEquals(true, Thread.currentThread().isInterrupted());
    }

    @Test
    void propagatesTheLastFailureAfterExhaustion() {
        RecordingSleeper sleeper = new RecordingSleeper();
        AiHttpRetryExecutor executor = new AiHttpRetryExecutor(sleeper);
        List<RestClientResponseException> failures = List.of(
                responseException(500, new HttpHeaders()),
                responseException(502, new HttpHeaders()),
                responseException(503, new HttpHeaders()));
        AtomicInteger attempts = new AtomicInteger();

        RestClientResponseException thrown = assertThrows(
                RestClientResponseException.class,
                () -> executor.execute(() -> {
                    throw failures.get(attempts.getAndIncrement());
                }));

        assertSame(failures.get(2), thrown);
        assertEquals(3, attempts.get());
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeper.durations);
    }

    private RestClientResponseException responseException(int status, HttpHeaders headers) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(status);
        if (status >= 500) {
            return HttpServerErrorException.create(
                    statusCode, "failure", headers, new byte[0], UTF_8);
        }
        return HttpClientErrorException.create(
                statusCode, "failure", headers, new byte[0], UTF_8);
    }

    private HttpHeaders retryAfter(String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, value);
        return headers;
    }

    private static class RecordingSleeper implements RetrySleeper {

        private final List<Duration> durations = new ArrayList<>();
        private boolean interrupt;

        @Override
        public void sleep(Duration duration) throws InterruptedException {
            durations.add(duration);
            if (interrupt) {
                throw new InterruptedException("interrupted");
            }
        }
    }
}
