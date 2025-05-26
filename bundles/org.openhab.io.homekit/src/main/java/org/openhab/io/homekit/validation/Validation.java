package org.openhab.io.homekit.validation;

/**
 * Interface for all validation components in the HomeKit integration.
 * Validation components are responsible for checking specific aspects of the HomeKit configuration
 * and returning validation results.
 */
public interface Validation {
    /**
     * Performs validation on the given object using the provided context.
     * 
     * @param context The validation context containing the object to validate and settings
     * @return A ValidationResult containing the validation outcome
     */
    ValidationResult validate(ValidationContext context);

    /**
     * Returns the unique identifier for this validation component.
     * This is used for registration and lookup in the validation registry.
     * 
     * @return The validation component's unique identifier
     */
    String getId();

    /**
     * Returns the priority of this validation component.
     * Higher priority validations are executed first.
     * 
     * @return The validation component's priority
     */
    int getPriority();

    /**
     * Returns whether this validation component is enabled.
     * Disabled validations are skipped during the validation process.
     * 
     * @return true if the validation component is enabled, false otherwise
     */
    boolean isEnabled();
}
