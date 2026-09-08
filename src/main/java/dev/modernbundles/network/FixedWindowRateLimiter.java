package dev.modernbundles.network;

import java.util.HashMap;
import java.util.Map;

public final class FixedWindowRateLimiter<K> {
    private final int limit;
    private final long windowNanos;
    private final Map<K, Window> windows = new HashMap<>();

    public FixedWindowRateLimiter(int limit, long windowNanos) {
        if (limit < 1 || windowNanos < 1L) {
            throw new IllegalArgumentException("limit and window must be positive");
        }
        this.limit = limit;
        this.windowNanos = windowNanos;
    }

    public synchronized boolean tryAcquire(K key, long nowNanos) {
        Window current = this.windows.get(key);
        if (current == null || nowNanos < current.startedNanos || nowNanos - current.startedNanos >= this.windowNanos) {
            this.windows.put(key, new Window(nowNanos, 1));
            return true;
        }
        if (current.requests >= this.limit) {
            return false;
        }
        this.windows.put(key, new Window(current.startedNanos, current.requests + 1));
        return true;
    }

    public synchronized void clear(K key) {
        this.windows.remove(key);
    }

    public synchronized int trackedKeys() {
        return this.windows.size();
    }

    private record Window(long startedNanos, int requests) {
    }
}
