package com.dazbones.service;

import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Single-instance login budget. Never trust client-supplied forwarding headers here. */
@Service
public class LoginAttemptLimiter {
    private static final int LIMIT = 10;
    private static final long WINDOW = Duration.ofMinutes(10).toMillis();
    private static final int MAX_ADDRESSES = 10000;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new HashMap<>();

    public LoginAttemptLimiter() { this(Clock.systemUTC()); }
    LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public synchronized Attempt acquire(String address) {
        long now = clock.millis();
        buckets.values().removeIf(b -> b.expires <= now);
        Bucket bucket = buckets.get(address);
        if (bucket == null) {
            if (buckets.size() >= MAX_ADDRESSES) return new Attempt(null, 60);
            bucket = new Bucket(now + WINDOW);
            buckets.put(address, bucket);
        }
        if (bucket.used >= LIMIT) return new Attempt(null, Math.max(1, (bucket.expires - now + 999) / 1000));
        bucket.used++;
        return new Attempt(bucket, 0);
    }

    public synchronized void complete(Attempt attempt, boolean success) {
        if (attempt.completed) return;
        attempt.completed = true;
        // Successful logins release only their own reservation, never other failures.
        if (success && attempt.bucket != null) attempt.bucket.used--;
    }

    private static final class Bucket {
        final long expires;
        int used;
        Bucket(long expires) { this.expires = expires; }
    }

    public static final class Attempt {
        private final Bucket bucket;
        private final long retryAfterSeconds;
        private boolean completed;
        private Attempt(Bucket bucket, long retryAfterSeconds) {
            this.bucket = bucket;
            this.retryAfterSeconds = retryAfterSeconds;
        }
        public boolean allowed() { return bucket != null; }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }
}
