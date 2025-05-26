package org.openhab.io.homekit.validation;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all validation components.
 * Provides common functionality and default implementations for validation operations.
 */
public abstract class AbstractValidation implements Validation {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final String id;
    private final int priority;
    private boolean enabled = true;

    protected AbstractValidation(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        if (!isEnabled()) {
            logger.debug("Validation {} is disabled, skipping validation", id);
            return ValidationResult.builder().valid(true).build();
        }

        try {
            return doValidate(context);
        } catch (Exception e) {
            logger.error("Error in validation {}: {}", id, e.getMessage(), e);
            return ValidationResult.builder().valid(false).severity(ValidationResult.Severity.ERROR)
                    .message("Validation failed: " + e.getMessage()).code("VALIDATION_ERROR")
                    .contextKey(getContextKey(context.getTarget())).rootCause(true).blocking(true).build();
        }
    }

    /**
     * Performs the actual validation.
     * Subclasses must implement this method.
     */
    protected abstract ValidationResult doValidate(ValidationContext context);

    /**
     * Generates a context key for the given object.
     * Subclasses should override this method to provide meaningful context keys.
     */
    protected String getContextKey(Object object) {
        return object != null ? object.getClass().getSimpleName() : "null";
    }

    /**
     * Creates a validation issue with the given parameters.
     */
    protected ValidationIssue createIssue(ValidationResult.Severity severity, String message, String code,
            String contextKey, boolean rootCause, boolean blocking) {
        return ValidationIssue.builder().severity(severity).message(message).code(code).contextKey(contextKey)
                .rootCause(rootCause).blocking(blocking).build();
    }

    /**
     * Creates a validation issue with the given parameters and additional details.
     */
    protected ValidationIssue createIssue(ValidationResult.Severity severity, String message, String code,
            String contextKey, boolean rootCause, boolean blocking, Map<String, Object> details) {
        return ValidationIssue.builder().severity(severity).message(message).code(code).contextKey(contextKey)
                .rootCause(rootCause).blocking(blocking).details(details).build();
    }

    /**
     * Creates a validation result from a list of issues.
     */
    protected ValidationResult createResult(List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return ValidationResult.builder().valid(true).build();
        }

        boolean isValid = false;
        ValidationResult.Severity maxSeverity = issues.stream().map(ValidationIssue::getSeverity).max(Enum::compareTo)
                .orElse(ValidationResult.Severity.INFO);

        return ValidationResult.builder().valid(isValid).severity(maxSeverity).issues(issues).build();
    }
}
