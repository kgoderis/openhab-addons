package org.openhab.io.homekit.validation;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an individual validation issue.
 */
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
        this.message = builder.message;
        this.code = builder.code;
        this.contextKey = builder.contextKey;
        this.rootCause = builder.rootCause;
        this.blocking = builder.blocking;
        this.details = builder.details;
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
        private String message;
        private String code;
        private String contextKey;
        private boolean rootCause = false;
        private boolean blocking = false;
        private Map<String, Object> details;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationIssue that = (ValidationIssue) o;
        return Objects.equals(code, that.code) && 
               Objects.equals(contextKey, that.contextKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, contextKey);
    }
} 