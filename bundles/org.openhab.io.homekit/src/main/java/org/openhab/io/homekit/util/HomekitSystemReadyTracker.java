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
 * marking
 * the system as ready. It serves as the final checkpoint in the startup
 * sequence, ensuring that all components are properly initialized and
 * operational.
 *
 * <p>
 * <b>Monitored Components:</b>
 * </p>
 * <ul>
 * <li>Accessory Bridge - Core bridge for accessory management</li>
 * <li>Item Bridge - Bridge for OpenHAB item integration</li>
 * <li>Thing Bridge - Bridge for OpenHAB thing integration</li>
 * <li>Passthrough Bridge - Bridge for direct accessory passthrough</li>
 * <li>Discovery Service - Service for discovering HomeKit accessories</li>
 * <li>Handler Factory - Factory for creating thing handlers</li>
 * </ul>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link ReadyService} for tracking component readiness</li>
 * <li>{@link ReadyMarker} for signaling system readiness</li>
 * <li>{@link HomekitReadyMarkers} for ready marker constants</li>
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

    /**
     * Initializes the HomeKit system ready tracker.
     *
     * This constructor sets up the tracker to monitor all critical HomeKit
     * components
     * and mark the system as ready when all components are operational.
     *
     * Key implementation details:
     * - Registers as ready tracker for all bridge components
     * - Monitors discovery service and handler factory
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
        readyService.registerTracker(this,
                new ReadyMarkerFilter().withType(HomekitReadyMarkers.HOMEKIT_ACCESSORY_BRIDGE)
                        .withType(HomekitReadyMarkers.HOMEKIT_ITEM_BRIDGE)
                        .withType(HomekitReadyMarkers.HOMEKIT_THING_BRIDGE)
                        .withType(HomekitReadyMarkers.HOMEKIT_PASSTHROUGH_BRIDGE)
                        .withType(HomekitReadyMarkers.HOMEKIT_DISCOVERY_SERVICE)
                        .withType(HomekitReadyMarkers.HOMEKIT_HANDLER_FACTORY));

        logger.debug("{}System ready tracker initialized", LOG_INIT);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added: {}", LOG_STATE, readyMarker.getType());

        // Check if all critical components are ready
        if (allCriticalComponentsReady()) {
            logger.info("{}All HomeKit components are ready, marking system as ready", LOG_STATE);
            readyService.markReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_SYSTEM_READY, "HomeKit System"));
            logger.info("{}HomeKit system fully initialized and ready", LOG_STATE);
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
     * ready,
     * ensuring the system is fully operational before marking it as ready.
     *
     * @return true if all critical components are ready, false otherwise
     */
    private boolean allCriticalComponentsReady() {
        return readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ACCESSORY_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_ITEM_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_THING_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_PASSTHROUGH_BRIDGE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_DISCOVERY_SERVICE, ""))
                && readyService.isReady(new ReadyMarker(HomekitReadyMarkers.HOMEKIT_HANDLER_FACTORY, ""));
    }
}
