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

package org.openhab.io.homekit.util;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Central definition of ReadyService markers for HomeKit component startup
 * sequencing.
 *
 * This class defines the ready markers used to coordinate the startup sequence
 * of HomeKit components.
 * Components use these markers to signal their readiness and wait for
 * dependencies to be available.
 * The markers are organized in levels to ensure proper initialization order.
 *
 * <p>
 * <b>Startup Sequence Levels:</b>
 * </p>
 * <ul>
 * <li><b>Level 1:</b> Core Infrastructure (Event Manager, Configuration
 * Manager)</li>
 * <li><b>Level 2:</b> Factories (Characteristic, Service, Accessory
 * Factories)</li>
 * <li><b>Level 3:</b> Basic Registries and Providers</li>
 * <li><b>Level 4:</b> Advanced Providers and Registries</li>
 * <li><b>Level 5:</b> Type Providers</li>
 * <li><b>Level 6:</b> Bridges</li>
 * <li><b>Level 7:</b> Discovery and Handlers</li>
 * <li><b>Level 8:</b> System Ready</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public final class HomekitReadyMarkers {

    // ========== Level 1: Core Infrastructure ==========
    public static final String HOMEKIT_EVENT_MANAGER = "homekit.eventManager";

    public static final String HOMEKIT_CONFIGURATION_MANAGER = "homekit.configurationManager";

    // ========== Level 2: Factories ==========
    public static final String HOMEKIT_CHARACTERISTIC_FACTORY = "homekit.characteristicFactory";
    public static final String HOMEKIT_SERVICE_FACTORY = "homekit.serviceFactory";
    public static final String HOMEKIT_ACCESSORY_FACTORY = "homekit.accessoryFactory";

    // ========== Level 3: Basic Registries and Providers ==========
    public static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";

    public static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";

    public static final String HOMEKIT_PAIRING_REGISTRY = "homekit.pairingRegistry";

    // ========== Level 4: Advanced Providers and Registries ==========
    public static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    public static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    public static final String HOMEKIT_PERSISTED_ACCESSORY_PROVIDER = "homekit.persistedAccessoryProvider";

    // ========== Level 5: Type Providers ==========
    public static final String HOMEKIT_THING_TYPE_PROVIDER = "homekit.thingTypeProvider";

    public static final String HOMEKIT_CHANNEL_TYPE_PROVIDER = "homekit.channelTypeProvider";

    public static final String HOMEKIT_CHANNEL_GROUP_TYPE_PROVIDER = "homekit.channelGroupTypeProvider";

    // ========== Level 6: Bridges ==========
    public static final String HOMEKIT_ACCESSORY_BRIDGE = "homekit.accessoryBridge";

    public static final String HOMEKIT_ITEM_BRIDGE = "homekit.itemBridge";

    public static final String HOMEKIT_THING_BRIDGE = "homekit.thingBridge";

    public static final String HOMEKIT_PASSTHROUGH_BRIDGE = "homekit.passthroughBridge";

    // ========== Level 7: Discovery and Handlers ==========
    public static final String HOMEKIT_DISCOVERY_SERVICE = "homekit.discoveryService";

    public static final String HOMEKIT_HANDLER_FACTORY = "homekit.handlerFactory";

    // ========== Level 8: System Ready ==========
    public static final String HOMEKIT_SYSTEM_READY = "homekit.systemReady";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private HomekitReadyMarkers() {
        // Utility class - no instantiation
    }
}
