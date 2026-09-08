package dev.modernbundles.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {
    private static final long SECOND = TimeUnit.SECONDS.toNanos(1L);

    @Test
    void permitsNormalTrafficAndRejectsAbuseUntilNextWindow() {
        FixedWindowRateLimiter<String> limiter = new FixedWindowRateLimiter<>(3, SECOND);
        assertTrue(limiter.tryAcquire("player", 10L));
        assertTrue(limiter.tryAcquire("player", 20L));
        assertTrue(limiter.tryAcquire("player", 30L));
        assertFalse(limiter.tryAcquire("player", 40L));
        assertTrue(limiter.tryAcquire("player", 10L + SECOND));
    }

    @Test
    void playersAreIndependentAndCleanupDropsState() {
        FixedWindowRateLimiter<String> limiter = new FixedWindowRateLimiter<>(1, SECOND);
        assertTrue(limiter.tryAcquire("one", 0L));
        assertFalse(limiter.tryAcquire("one", 1L));
        assertTrue(limiter.tryAcquire("two", 1L));
        assertEquals(2, limiter.trackedKeys());

        limiter.clear("one");
        assertEquals(1, limiter.trackedKeys());
        assertTrue(limiter.tryAcquire("one", 2L));
    }

    @Test
    void invalidLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter<>(0, SECOND));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindowRateLimiter<>(1, 0L));
    }
}
