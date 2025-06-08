/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.validation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Context object that holds the state and configuration for validation.
 * This includes the object being validated, any cached results, and validation settings.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
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

    public Optional<Object> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public void cacheResult(String key, ValidationResult result) {
        if (settings.isCachingEnabled()) {
            cachedResults.put(key, result);
        }
    }

    public Optional<ValidationResult> getCachedResult(String key) {
        return Optional.ofNullable(settings.isCachingEnabled() ? cachedResults.get(key) : null);
    }

    public ValidationSettings getSettings() {
        return settings;
    }

    public static class ValidationSettings {
        private final boolean cachingEnabled;
        private final long cacheDuration;
        private final boolean failFast;
        private final boolean suppressCascadingErrors;

        public ValidationSettings(boolean cachingEnabled, long cacheDuration, boolean failFast,
                boolean suppressCascadingErrors) {
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
