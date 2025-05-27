package org.openhab.io.homekit.api.characteristic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define binding mappings between HomeKit characteristics and OpenHAB channels.
 * <p>
 * This annotation provides a way to map HomeKit characteristics to OpenHAB binding channels,
 * enabling automatic discovery and configuration of HomeKit accessories based on OpenHAB
 * binding configurations.
 * </p>
 * <p>
 * The annotation is used to:
 * <ul>
 *   <li>Map HomeKit characteristics to OpenHAB bindings</li>
 *   <li>Define supported channel types</li>
 *   <li>Specify channel properties</li>
 *   <li>Configure channel tags</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 *   <li>Runtime retention for reflection-based processing</li>
 *   <li>Type-level targeting for class annotations</li>
 *   <li>Flexible channel type configuration</li>
 *   <li>Support for multiple binding properties</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 *   <li>{@link org.openhab.io.homekit.api.characteristic.HomekitCharacteristic} for characteristic mapping</li>
 *   <li>{@link org.openhab.core.thing.Channel} for channel configuration</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BindingMapping {
    /**
     * The OpenHAB binding ID that this characteristic maps to.
     * This ID must match the binding ID in the OpenHAB binding registry.
     *
     * @return the OpenHAB binding ID
     * @since 1.0.0
     */
    String bindingId();

    /**
     * The channel types supported by this binding mapping.
     * These types define the kind of channels that can be used with this characteristic.
     *
     * @return array of supported channel types
     * @since 1.0.0
     */
    String[] channelTypes() default {};

    /**
     * The channel properties required by this binding mapping.
     * These properties define additional configuration needed for the channels.
     *
     * @return array of required channel properties
     * @since 1.0.0
     */
    String[] channelProperties() default {};

    /**
     * The channel tags associated with this binding mapping.
     * These tags are used for channel categorization and filtering.
     *
     * @return array of channel tags
     * @since 1.0.0
     */
    String[] channelTags() default {};
}
