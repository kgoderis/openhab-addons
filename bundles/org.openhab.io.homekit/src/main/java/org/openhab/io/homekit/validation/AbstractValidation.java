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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all validation components.
 * Provides common functionality and default implementations for validation operations.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
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
    public Optional<ValidationResult> validate(ValidationContext context) {
        if (!isEnabled()) {
            logger.debug("Validation {} is disabled, skipping validation", id);
            return Optional.of(ValidationResult.builder().valid(true).build());
        }

        try {
            return doValidate(context);
        } catch (Exception e) {
            logger.error("Error in validation {}: {}", id, e.getMessage(), e);
            return Optional.of(ValidationResult.builder().valid(false).severity(ValidationResult.Severity.ERROR)
                    .message("Validation failed: " + e.getMessage()).code("VALIDATION_ERROR")
                    .contextKey(getContextKey(context.getTarget())).rootCause(true).blocking(true).build());
        }
    }

    /**
     * Performs the actual validation.
     * Subclasses must implement this method.
     */
    protected abstract Optional<ValidationResult> doValidate(ValidationContext context);

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
        ValidationResult.Severity maxSeverity = Objects.requireNonNull(issues.stream().map(ValidationIssue::getSeverity)
                .filter(Objects::nonNull).max(Enum::compareTo).orElse(ValidationResult.Severity.INFO),
                "Severity cannot be null");

        return ValidationResult.builder().valid(isValid).severity(maxSeverity).issues(issues).build();
    }
}
