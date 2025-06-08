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

package org.openhab.io.homekit.api.accessory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Annotation used to define HomeKit accessory types and their metadata.
 * <p>
 * This annotation is used to mark classes that represent HomeKit accessories and provides
 * essential metadata for HomeKit integration. It defines the type, name, and tag information
 * that is used to identify and configure accessories in the HomeKit ecosystem.
 * </p>
 * <p>
 * The annotation is used to:
 * <ul>
 * <li>Define the HomeKit characteristic type UUID</li>
 * <li>Provide human-readable names for accessories</li>
 * <li>Specify tags for accessory identification</li>
 * <li>Enable binding-specific configurations</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Runtime retention for reflection-based processing</li>
 * <li>Type-level targeting for class annotations</li>
 * <li>Optional name and tag fields</li>
 * <li>Extensible for binding-specific configurations</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.characteristic.BindingMapping} for binding configurations</li>
 * <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory implementation</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@NonNullByDefault
public @interface HomekitAccessoryType {
    /**
     * The HomeKit characteristic type UUID that identifies this accessory type.
     * This UUID must be a valid HomeKit characteristic type as defined in the HomeKit protocol.
     *
     * @return the HomeKit characteristic type UUID
     * @since 1.0.0
     */
    String type();

    /**
     * Optional human-readable name for this accessory type.
     * If not specified, the name will be derived from the class name.
     *
     * @return the human-readable name, or empty string if not specified
     * @since 1.0.0
     */
    String name() default "";

    /**
     * Optional tag for this accessory type.
     * This tag can be used for additional identification or categorization.
     *
     * @return the tag, or empty string if not specified
     * @since 1.0.0
     */
    String tag() default "";

    // Binding-specific configurations
    // BindingMapping[] bindings() default {};

    // Fallback channel types if no binding matches
    // String[] channelTypes() default {};
}
