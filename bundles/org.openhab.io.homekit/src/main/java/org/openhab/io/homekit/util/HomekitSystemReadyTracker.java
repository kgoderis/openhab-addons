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
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the readiness of all HomeKit components and marks the system as ready.
 *
 * This component waits for all critical HomeKit components to be ready before
 * marking the system as ready. It serves as the final checkpoint in the startup
 * sequence, ensuring that all components are properly initialized and
 * operational.
 *
 * <p>
 * <b>Monitored Components by Level:</b>
 * </p>
 * <ul>
 * <li><b>Level 1:</b> Core Infrastructure
 * <ul>
 * <li>Event Manager</li>
 * <li>Configuration Manager</li>
 * </ul>
 * </li>
 * <li><b>Level 2:</b> Factories
 * <ul>
 * <li>Characteristic Factory</li>
 * <li>Service Factory</li>
 * <li>Accessory Factory</li>
 * </ul>
 * </li>
 * <li><b>Level 3:</b> Basic Registries and Providers
 * <ul>
 * <li>Accessory Registry</li>
 * <li>Managed Pairing Provider</li>
 * <li>Pairing Registry</li>
 * </ul>
 * </li>
 * <li><b>Level 4:</b> Advanced Providers and Registries
 * <ul>
 * <li>Managed Accessory Server Provider</li>
 * <li>Accessory Server Registry</li>
 * <li>Persisted Accessory Provider</li>
 * </ul>
 * </li>
 * <li><b>Level 5:</b> Type Providers
 * <ul>
 * <li>Thing Type Provider</li>
 * <li>Channel Type Provider</li>
 * <li>Channel Group Type Provider</li>
 * </ul>
 * </li>
 * <li><b>Level 6:</b> Bridges
 * <ul>
 * <li>Accessory Bridge</li>
 * <li>Item Bridge</li>
 * <li>Thing Bridge</li>
 * <li>Passthrough Bridge</li>
 * </ul>
 * </li>
 * <li><b>Level 7:</b> Discovery and Handlers
 * <ul>
 * <li>Discovery Service</li>
 * <li>Handler Factory</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@Component(service = HomekitSystemReadyTracker.class)
@NonNullByDefault
public class HomekitSystemReadyTracker implements ReadyService.ReadyTracker {

    private static final Logger logger = LoggerFactory.getLogger(HomekitSystemReadyTracker.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit SystemTracker: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final ReadyService readyService;
    private volatile boolean readyMarkerRegistered = false;

    /**
     * Initializes the HomeKit system ready tracker.
     *
     * This constructor sets up the tracker to monitor all critical HomeKit
     * components and mark the system as ready when all components are operational.
     *
     * Key implementation details:
     * - Registers as ready tracker for all components across all levels
     * - Monitors components in order of dependency
     * - Marks system as ready when all components are available
     * - Provides comprehensive logging of system startup
     *
     * @param readyService The service for tracking component readiness
     */
    @Activate
    public HomekitSystemReadyTracker(@Reference ReadyService readyService) {
        this.readyService = readyService;
        logger.debug("{}Initializing HomeKit system ready tracker", LOG_INIT);

        // Register to wait for all critical HomeKit components
        readyService.registerTracker(this, new ReadyMarkerFilter()
                // Level 1: Core Infrastructure
                .withType(HomekitReadyMarkers.HOMEKIT_EVENT_MANAGER)
                .withType(HomekitReadyMarkers.HOMEKIT_CONFIGURATION_MANAGER)
                // Level 2: Factories
                .withType(HomekitReadyMarkers.HOMEKIT_CHARACTERISTIC_FACTORY)
                .withType(HomekitReadyMarkers.HOMEKIT_SERVICE_FACTORY)
                .withType(HomekitReadyMarkers.HOMEKIT_ACCESSORY_FACTORY)
                // Level 3: Basic Registries and Providers
                .withType(HomekitReadyMarkers.HOMEKIT_ACCESSORY_REGISTRY)
                .withType(HomekitReadyMarkers.HOMEKIT_MANAGED_PAIRING_PROVIDER)
                .withType(HomekitReadyMarkers.HOMEKIT_PAIRING_REGISTRY)
                // Level 4: Advanced Providers and Registries
                .withType(HomekitReadyMarkers.HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER)
                .withType(HomekitReadyMarkers.HOMEKIT_ACCESSORY_SERVER_REGISTRY)
                .withType(HomekitReadyMarkers.HOMEKIT_PERSISTED_ACCESSORY_PROVIDER)
                // Level 5: Type Providers
                .withType(HomekitReadyMarkers.HOMEKIT_THING_TYPE_PROVIDER)
                .withType(HomekitReadyMarkers.HOMEKIT_CHANNEL_TYPE_PROVIDER)
                .withType(HomekitReadyMarkers.HOMEKIT_CHANNEL_GROUP_TYPE_PROVIDER)
                // Level 6: Bridges
                .withType(HomekitReadyMarkers.HOMEKIT_ACCESSORY_BRIDGE)
                .withType(HomekitReadyMarkers.HOMEKIT_ITEM_BRIDGE).withType(HomekitReadyMarkers.HOMEKIT_THING_BRIDGE)
                .withType(HomekitReadyMarkers.HOMEKIT_PASSTHROUGH_BRIDGE)
                // Level 7: Discovery and Handlers
                .withType(HomekitReadyMarkers.HOMEKIT_DISCOVERY_SERVICE)
                .withType(HomekitReadyMarkers.HOMEKIT_HANDLER_FACTORY));

        logger.debug("{}System ready tracker initialized", LOG_INIT);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added: {}", LOG_STATE, readyMarker.getType());

        // Check if all critical components are ready
        if (allCriticalComponentsReady() && !readyMarkerRegistered) {
            logger.info("{}All HomeKit components are ready, marking system as ready", LOG_STATE);
            readyService.markReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_SYSTEM_READY, "HomeKit System"));
            logger.info("{}HomeKit system fully initialized and ready", LOG_STATE);
            readyMarkerRegistered = true;
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.warn("{}Ready marker removed: {} - System may no longer be fully ready", LOG_STATE,
                readyMarker.getType());
    }

    /**
     * Checks if all critical HomeKit components are ready.
     *
     * This method verifies that all essential components have marked themselves as
     * ready, ensuring the system is fully operational before marking it as ready.
     *
     * @return true if all critical components are ready, false otherwise
     */
    private boolean allCriticalComponentsReady() {
        return
        // Level 1: Core Infrastructure
        readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_EVENT_MANAGER, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_CONFIGURATION_MANAGER, ""))
                // Level 2: Factories
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_CHARACTERISTIC_FACTORY, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_SERVICE_FACTORY, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ACCESSORY_FACTORY, ""))
                // Level 3: Basic Registries and Providers
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ACCESSORY_REGISTRY, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_MANAGED_PAIRING_PROVIDER, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_PAIRING_REGISTRY, ""))
                // Level 4: Advanced Providers and Registries
                && readyService
                        .isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ACCESSORY_SERVER_REGISTRY, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_PERSISTED_ACCESSORY_PROVIDER, ""))
                // Level 5: Type Providers
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_THING_TYPE_PROVIDER, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_CHANNEL_TYPE_PROVIDER, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_CHANNEL_GROUP_TYPE_PROVIDER, ""))
                // Level 6: Bridges
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ACCESSORY_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ITEM_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_THING_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_PASSTHROUGH_BRIDGE, ""))
                // Level 7: Discovery and Handlers
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_DISCOVERY_SERVICE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_HANDLER_FACTORY, ""));
    }
}
