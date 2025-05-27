/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.io.homekit.network.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Defines constants used in the HomeKit discovery process.
 *
 * <p>
 * This class provides a centralized location for all constants related to
 * HomeKit device discovery and configuration. These constants are used by
 * {@link HomekitAccessoryServerDiscoveryService} to identify and configure
 * HomeKit accessories during the discovery process.
 * </p>
 *
 * <p>
 * <b>Key features:</b>
 * </p>
 * <ul>
 *   <li>Device identification parameters</li>
 *   <li>Configuration properties</li>
 *   <li>Device categories</li>
 *   <li>Discovery-specific attributes</li>
 *   <li>Logging message prefixes</li>
 * </ul>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 *   <li>{@link HomekitAccessoryServerDiscoveryService} for device discovery and management</li>
 *   <li>{@link HomekitAccessoryServer} for device configuration and communication</li>
 *   <li>{@link HomekitAccessory} for device identification and state management</li>
 *   <li>{@link org.openhab.core.config.discovery.DiscoveryService} for integration with OpenHAB's discovery system</li>
 * </ul>
 *
 * <p>
 * <b>Usage Guidelines:</b>
 * </p>
 * <ul>
 *   <li>Use these constants for consistent device identification and configuration</li>
 *   <li>Follow the logging standards when implementing discovery-related features</li>
 *   <li>Maintain backward compatibility when modifying constants</li>
 *   <li>Document any changes to these constants in release notes</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitDiscoveryConstants {

    /** Logging prefixes */
    private static final String LOG_PREFIX = "HomeKit Discovery Constants: ";
    private static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    private static final String LOG_DEVICE = LOG_PREFIX + "Device - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /**
     * The device identifier property name.
     *
     * <p>
     * Used to uniquely identify a HomeKit accessory. This identifier is essential
     * for tracking and managing accessories in the OpenHAB system.
     * </p>
     *
     * <p>
     * <b>Format:</b> The device ID should be a unique string that follows the HomeKit
     * accessory identification format. It is used in conjunction with the category
     * identifier to create a complete device identification.
     * </p>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Device discovery process</li>
     *   <li>Accessory registration</li>
     *   <li>Thing creation</li>
     *   <li>Device state tracking</li>
     * </ul>
     */
    public static final String DEVICE_ID = "id";

    /**
     * The configuration number property name.
     *
     * <p>
     * Used to track configuration changes in HomeKit accessories. This number
     * is incremented whenever the accessory's configuration is modified.
     * </p>
     *
     * <p>
     * <b>Format:</b> The configuration number is a positive integer that
     * represents the current configuration state of the accessory.
     * </p>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Configuration change detection</li>
     *   <li>Accessory state synchronization</li>
     *   <li>Update management</li>
     * </ul>
     */
    public static final String CONFIGURATION_NUMBER_SHARP = "c#";

    /**
     * The configuration URI for HomeKit bridge bindings.
     *
     * <p>
     * Used to identify the binding type in the OpenHAB system. This URI is
     * used to register and manage HomeKit bridge bindings.
     * </p>
     *
     * <p>
     * <b>Format:</b> The URI follows the OpenHAB binding URI format:
     * "binding:homekit:bridge"
     * </p>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Binding registration</li>
     *   <li>Configuration management</li>
     *   <li>System integration</li>
     * </ul>
     */
    public static final String CONFIGURATION_URI = "binding:homekit:bridge";

    /**
     * The category identifier property name.
     *
     * <p>
     * Used to specify the type of HomeKit accessory. This identifier helps
     * in categorizing and managing different types of accessories.
     * </p>
     *
     * <p>
     * <b>Format:</b> The category ID is a string that represents the type
     * of HomeKit accessory. Common values include:
     * </p>
     * <ul>
     *   <li>"1" for standalone accessories</li>
     *   <li>"2" for bridge accessories</li>
     * </ul>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Device type identification</li>
     *   <li>Accessory categorization</li>
     *   <li>Thing type determination</li>
     * </ul>
     */
    public static final String CATEGORY_ID = "ci";

    /**
     * The category identifier for HomeKit bridges.
     *
     * <p>
     * Used to identify bridge-type accessories. Bridges act as intermediaries
     * between OpenHAB and HomeKit accessories.
     * </p>
     *
     * <p>
     * <b>Format:</b> The bridge category is represented by the string "2"
     * </p>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Bridge device identification</li>
     *   <li>Accessory hierarchy management</li>
     *   <li>Communication routing</li>
     * </ul>
     */
    public static final String BRIDGE_CATEGORY = "2";

    /**
     * The category identifier for standalone HomeKit accessories.
     *
     * <p>
     * Used to identify non-bridge accessories. These are direct HomeKit
     * accessories that don't require a bridge for communication.
     * </p>
     *
     * <p>
     * <b>Format:</b> The standalone category is represented by the string "1"
     * </p>
     *
     * <p>
     * <b>Usage:</b> This constant is used in:
     * </p>
     * <ul>
     *   <li>Standalone device identification</li>
     *   <li>Direct accessory management</li>
     *   <li>Communication handling</li>
     * </ul>
     */
    public static final String STANDALONE_CATEGORY = "1";
}
