package com.teamproject.japan_newhire_rag_backend.rag.ai;

import java.time.Duration;

interface RetrySleeper {

    RetrySleeper THREAD_SLEEP = duration -> Thread.sleep(duration.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}
