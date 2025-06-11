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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents an individual validation issue.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class ValidationIssue {
    private final ValidationResult.Severity severity;
    private final String message;
    private final String code;
    private final String contextKey;
    private final boolean rootCause;
    private final boolean blocking;
    private final Map<String, Object> details;

    private ValidationIssue(Builder builder) {
        this.severity = builder.severity;
        this.message = Objects.requireNonNull(builder.message, "message cannot be null");
        this.code = Objects.requireNonNull(builder.code, "code cannot be null");
        this.contextKey = Objects.requireNonNull(builder.contextKey, "contextKey cannot be null");
        this.rootCause = builder.rootCause;
        this.blocking = builder.blocking;
        this.details = Objects.requireNonNullElse(builder.details, Collections.emptyMap());
    }

    public ValidationResult.Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getContextKey() {
        return contextKey;
    }

    public boolean isRootCause() {
        return rootCause;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ValidationResult.Severity severity = ValidationResult.Severity.INFO;
        private @Nullable String message;
        private @Nullable String code;
        private @Nullable String contextKey;
        private boolean rootCause = false;
        private boolean blocking = false;
        private @Nullable Map<String, Object> details;

        public Builder severity(ValidationResult.Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder contextKey(String contextKey) {
            this.contextKey = contextKey;
            return this;
        }

        public Builder rootCause(boolean rootCause) {
            this.rootCause = rootCause;
            return this;
        }

        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public ValidationIssue build() {
            return new ValidationIssue(this);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationIssue that = (ValidationIssue) o;
        return Objects.equals(code, that.code) && Objects.equals(contextKey, that.contextKey);
    }

    @Override
    @SuppressWarnings("null") // hashCode() return type compatibility with Object.hashCode()
    public int hashCode() {
        return Objects.hash(code, contextKey);
    }
}
