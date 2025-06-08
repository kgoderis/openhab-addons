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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents the result of a validation check.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class ValidationResult {
    private final boolean valid;
    private final Severity severity;
    private final String message;
    private final String code;
    private final String contextKey; // e.g., thingUID+serviceType+characteristicTypeUid
    private final boolean rootCause;
    private final boolean blocking;
    private final List<ValidationIssue> issues;

    private ValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.severity = builder.severity;
        this.message = Objects.requireNonNull(builder.message, "message cannot be null");
        this.code = Objects.requireNonNull(builder.code, "code cannot be null");
        this.contextKey = Objects.requireNonNull(builder.contextKey, "contextKey cannot be null");
        this.rootCause = builder.rootCause;
        this.blocking = builder.blocking;
        this.issues = builder.issues;
    }

    public boolean isValid() {
        return valid;
    }

    public Severity getSeverity() {
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

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid = true;
        private Severity severity = Severity.INFO;
        private @Nullable String message;
        private @Nullable String code;
        private @Nullable String contextKey;
        private boolean rootCause = false;
        private boolean blocking = false;
        private List<ValidationIssue> issues = new ArrayList<>();

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder severity(Severity severity) {
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

        public Builder issues(List<ValidationIssue> issues) {
            this.issues = issues;
            return this;
        }

        public Builder addIssue(ValidationIssue issue) {
            this.issues.add(issue);
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ValidationResult that = (ValidationResult) o;
        return Objects.equals(code, that.code) && Objects.equals(contextKey, that.contextKey);
    }

    @Override
    @SuppressWarnings("null") // hashCode() return type compatibility with Object.hashCode()
    public int hashCode() {
        return Objects.hash(code, contextKey);
    }
}
