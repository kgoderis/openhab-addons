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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and orchestrates the validation process.
 * Handles validation component registration, validation execution, and result caching.
 * 
 * @author Karel Goderis - Initial contribution
 */
@Component(service = ValidationManager.class)
@NonNullByDefault
public class ValidationManager {
    private final Logger logger = LoggerFactory.getLogger(ValidationManager.class);
    private final Map<String, Validation> validations = new ConcurrentHashMap<>();
    private final ValidationCache cache;
    private final ValidationContext.ValidationSettings defaultSettings;

    public ValidationManager() {
        // Default cache duration: 5 minutes
        this.cache = new ValidationCache(5 * 60 * 1000);
        this.defaultSettings = new ValidationContext.ValidationSettings(true, // caching enabled
                5 * 60 * 1000, // 5 minutes cache duration
                false, // don't fail fast
                true // suppress cascading errors
        );
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addValidation(Validation validation) {
        validations.put(validation.getId(), validation);
        logger.debug("Registered validation: {}", validation.getId());
    }

    public void removeValidation(Validation validation) {
        validations.remove(validation.getId());
        logger.debug("Unregistered validation: {}", validation.getId());
    }

    public ValidationResult validate(Object target) {
        return validate(target, defaultSettings);
    }

    public ValidationResult validate(Object target, ValidationContext.ValidationSettings settings) {
        ValidationContext context = new ValidationContext(target, settings);
        List<ValidationResult> results = new ArrayList<>();

        // Sort validations by priority
        List<Validation> sortedValidations = validations.values().stream().filter(Validation::isEnabled)
                .sorted((v1, v2) -> Integer.compare(v2.getPriority(), v1.getPriority())).collect(Collectors.toList());

        for (Validation validation : sortedValidations) {
            String cacheKey = generateCacheKey(target, validation);
            Optional<ValidationResult> result = Optional.empty();

            if (settings.isCachingEnabled()) {
                result = cache.get(cacheKey);
            }

            if (result.isEmpty()) {
                try {
                    Optional<ValidationResult> validationResult = validation.validate(context);
                    result = validationResult;
                    if (result.isPresent() && settings.isCachingEnabled()) {
                        cache.put(cacheKey, result.get(), settings.getCacheDuration());
                    }
                } catch (Exception e) {
                    logger.error("Error executing validation {}: {}", validation.getId(), e.getMessage(), e);
                    ValidationResult errorResult = ValidationResult.builder().valid(false)
                            .severity(ValidationResult.Severity.ERROR)
                            .message("Validation execution failed: " + e.getMessage()).build();
                    result = Optional.of(errorResult);
                }
            }

            if (result.isPresent()) {
                results.add(result.get());

                if (!result.get().isValid() && settings.isFailFast()) {
                    break;
                }
            }
        }

        return combineResults(results, settings.isSuppressCascadingErrors());
    }

    private String generateCacheKey(Object target, Validation validation) {
        return target.getClass().getName() + ":" + target.hashCode() + ":" + validation.getId();
    }

    private ValidationResult combineResults(List<ValidationResult> results, boolean suppressCascading) {
        if (results.isEmpty()) {
            return ValidationResult.builder().valid(true).build();
        }

        List<ValidationResult> filteredResults = results;
        if (suppressCascading) {
            // Remove results that are likely cascading from previous errors
            filteredResults = results.stream().filter(result -> !isLikelyCascading(result, results))
                    .collect(Collectors.toList());
        }

        boolean isValid = filteredResults.stream().allMatch(ValidationResult::isValid);
        ValidationResult.Severity maxSeverity = Objects
                .requireNonNull(
                        filteredResults.stream().map(ValidationResult::getSeverity).filter(Objects::nonNull)
                                .max(Enum::compareTo).orElse(ValidationResult.Severity.INFO),
                        "Severity cannot be null");

        List<ValidationIssue> allIssues = filteredResults.stream().flatMap(r -> r.getIssues().stream())
                .collect(Collectors.toList());

        return ValidationResult.builder().valid(isValid).severity(maxSeverity).issues(allIssues).build();
    }

    private boolean isLikelyCascading(ValidationResult result, List<ValidationResult> allResults) {
        if (result.isValid()) {
            return false;
        }

        // Check if this result's issues are likely caused by previous validation failures
        return allResults.stream().filter(r -> r != result).filter(r -> !r.isValid())
                .anyMatch(r -> r.getIssues().stream().anyMatch(issue -> result.getIssues().stream()
                        .anyMatch(resultIssue -> resultIssue.getContextKey().startsWith(issue.getContextKey()))));
    }

    public void clearCache() {
        cache.clear();
    }

    public void shutdown() {
        cache.shutdown();
    }
}
