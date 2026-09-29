package com.dazbones.service;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptLimiterTest {
    static class TestClock extends Clock {
        long now;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
        public long millis() { return now; }
    }
    @Test void failuresExpireAndOtherAddressesStayAvailable() {
        var clock = new TestClock();
        var limiter = new LoginAttemptLimiter(clock);
        for (int i = 0; i < 10; i++) assertThat(limiter.acquire("a").allowed()).isTrue();
        assertThat(limiter.acquire("a").retryAfterSeconds()).isEqualTo(600);
        assertThat(limiter.acquire("b").allowed()).isTrue();
        clock.now = 599999;
        assertThat(limiter.acquire("a").retryAfterSeconds()).isEqualTo(1);
        clock.now = 600000;
        assertThat(limiter.acquire("a").allowed()).isTrue();
    }
    @Test void successDoesNotEraseOtherFailuresAndCompletionIsIdempotent() {
        var limiter = new LoginAttemptLimiter(new TestClock());
        for (int i = 0; i < 9; i++) limiter.complete(limiter.acquire("a"), false);
        for (int i = 0; i < 20; i++) {
            var attempt = limiter.acquire("a");
            assertThat(attempt.allowed()).isTrue();
            limiter.complete(attempt, true);
            limiter.complete(attempt, true);
        }
        limiter.complete(limiter.acquire("a"), false);
        assertThat(limiter.acquire("a").allowed()).isFalse();
    }
    @Test void concurrentAttemptsCannotExceedBudget() throws Exception {
        var limiter = new LoginAttemptLimiter(new TestClock());
        var pool = Executors.newFixedThreadPool(20);
        try {
            var results = pool.invokeAll(IntStream.range(0, 100)
                    .<java.util.concurrent.Callable<Boolean>>mapToObj(i -> () -> limiter.acquire("a").allowed()).toList());
            int allowed = 0;
            for (var result : results) if (result.get()) allowed++;
            assertThat(allowed).isEqualTo(10);
        } finally { pool.shutdownNow(); }
    }
}
