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
 * This class provides a centralized location for all constants related to
 * HomeKit device discovery and configuration. These constants are used by
 * {@link HomekitAccessoryServerDiscoveryService} to identify and configure
 * HomeKit accessories during the discovery process.
 *
 * The constants define:
 * 1. Device identification parameters
 * 2. Configuration properties
 * 3. Device categories
 * 4. Discovery-specific attributes
 *
 * The class integrates with:
 * - {@link HomekitAccessoryServerDiscoveryService} for device discovery
 * - {@link HomekitAccessoryServer} for device configuration
 * - {@link HomekitAccessory} for device identification
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitDiscoveryConstants {

    /**
     * The device identifier property name.
     * Used to uniquely identify a HomeKit accessory.
     */
    public static final String DEVICE_ID = "id";

    /**
     * The configuration number property name.
     * Used to track configuration changes in HomeKit accessories.
     */
    public static final String CONFIGURATION_NUMBER_SHARP = "c#";

    /**
     * The configuration URI for HomeKit bridge bindings.
     * Used to identify the binding type in the OpenHAB system.
     */
    public static final String CONFIGURATION_URI = "binding:homekit:bridge";

    /**
     * The category identifier property name.
     * Used to specify the type of HomeKit accessory.
     */
    public static final String CATEGORY_ID = "ci";

    /**
     * The category identifier for HomeKit bridges.
     * Used to identify bridge-type accessories.
     */
    public static final String BRIDGE_CATEGORY = "2";

    /**
     * The category identifier for standalone HomeKit accessories.
     * Used to identify non-bridge accessories.
     */
    public static final String STANDALONE_CATEGORY = "1";
}
