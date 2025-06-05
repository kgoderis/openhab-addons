package org.openhab.io.homekit.validation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages caching of validation results with expiration.
 */
public class ValidationCache {
    private final Map<String, CacheEntry> cache;
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultExpirationMillis;

    public ValidationCache(long defaultExpirationMillis) {
        this.cache = new ConcurrentHashMap<>();
        this.defaultExpirationMillis = defaultExpirationMillis;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

        // Schedule periodic cleanup of expired entries
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, defaultExpirationMillis, defaultExpirationMillis,
                TimeUnit.MILLISECONDS);
    }

    public void put(String key, ValidationResult result, long expirationMillis) {
        long expirationTime = System.currentTimeMillis() + expirationMillis;
        cache.put(key, new CacheEntry(result, expirationTime));
    }

    public void put(String key, ValidationResult result) {
        put(key, result, defaultExpirationMillis);
    }

    public ValidationResult get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getResult();
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class CacheEntry {
        private final ValidationResult result;
        private final long expirationTime;

        public CacheEntry(ValidationResult result, long expirationTime) {
            this.result = result;
            this.expirationTime = expirationTime;
        }

        public ValidationResult getResult() {
            return result;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return currentTime >= expirationTime;
        }
    }
}
