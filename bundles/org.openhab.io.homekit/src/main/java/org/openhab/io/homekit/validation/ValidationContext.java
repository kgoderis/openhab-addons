package org.openhab.io.homekit.validation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object that holds the state and configuration for validation.
 * This includes the object being validated, any cached results, and validation settings.
 */
public class ValidationContext {
    private final Object target;
    private final Map<String, Object> attributes;
    private final Map<String, ValidationResult> cachedResults;
    private final ValidationSettings settings;

    public ValidationContext(Object target, ValidationSettings settings) {
        this.target = target;
        this.attributes = new ConcurrentHashMap<>();
        this.cachedResults = new ConcurrentHashMap<>();
        this.settings = settings;
    }

    public Object getTarget() {
        return target;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void cacheResult(String key, ValidationResult result) {
        if (settings.isCachingEnabled()) {
            cachedResults.put(key, result);
        }
    }

    public ValidationResult getCachedResult(String key) {
        return settings.isCachingEnabled() ? cachedResults.get(key) : null;
    }

    public ValidationSettings getSettings() {
        return settings;
    }

    public static class ValidationSettings {
        private final boolean cachingEnabled;
        private final long cacheDuration;
        private final boolean failFast;
        private final boolean suppressCascadingErrors;

        public ValidationSettings(boolean cachingEnabled, long cacheDuration, 
                                boolean failFast, boolean suppressCascadingErrors) {
            this.cachingEnabled = cachingEnabled;
            this.cacheDuration = cacheDuration;
            this.failFast = failFast;
            this.suppressCascadingErrors = suppressCascadingErrors;
        }

        public boolean isCachingEnabled() {
            return cachingEnabled;
        }

        public long getCacheDuration() {
            return cacheDuration;
        }

        public boolean isFailFast() {
            return failFast;
        }

        public boolean isSuppressCascadingErrors() {
            return suppressCascadingErrors;
        }
    }
} 