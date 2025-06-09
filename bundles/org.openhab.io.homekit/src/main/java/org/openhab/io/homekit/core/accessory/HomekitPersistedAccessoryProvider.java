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

package org.openhab.io.homekit.core.accessory;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the persistence and lifecycle of HomeKit accessories in the OpenHAB
 * system.
 *
 * This class acts as a bridge between the OpenHAB storage system and the
 * HomeKit accessory registry,
 * ensuring that accessories are properly persisted and restored across system
 * restarts. It operates
 * as an OSGi service that integrates with OpenHAB's storage and ready service
 * systems.
 *
 * Key responsibilities:
 * - Managing accessory persistence through storage service
 * - Restoring accessories using accessory factory
 * - Integrating with accessory registry for runtime management
 * - Coordinating with server registry for server assignments
 * - Handling accessory lifecycle events and state changes
 *
 * The class integrates with:
 * - {@link AbstractManagedProvider} for base provider functionality
 * - {@link StorageService} for persistent storage management
 * - {@link HomekitAccessoryFactory} for accessory creation and restoration
 * - {@link HomekitAccessoryRegistry} for runtime accessory management
 * - {@link HomekitAccessoryServerRegistry} for server coordination
 * - {@link ReadyService} for system readiness management
 * - {@link org.openhab.core.service.ReadyMarker OpenHAB's ready marker system}
 * for initialization coordination
 * - {@link org.openhab.core.storage.StorageService OpenHAB's storage system}
 * for persistence
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@Component(immediate = true, service = { HomekitPersistedAccessoryProvider.class,
        HomekitPersistedAccessoryProvider.class })
@NonNullByDefault
public class HomekitPersistedAccessoryProvider
        extends AbstractManagedProvider<HomekitAccessory, HomekitAccessoryUID, HomekitPersistedAccessory>
        implements HomekitAccessoryProvider, ReadyService.ReadyTracker {

    // ========== Constants ==========
    private static final Logger logger = LoggerFactory.getLogger(HomekitPersistedAccessoryProvider.class);

    // ========== Log HomekitMessage Prefixes ==========
    private static final String LOG_PREFIX = "Homekit HomekitAccessory Provider: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";
    static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit. HomekitAccessoryProvider";
    private static final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final HomekitAccessoryFactory homekitAccessoryFactory;
    private final ReadyService readyService;

    private volatile long lastUpdate = System.nanoTime();

    /**
     * Creates a new HomeKit persisted accessory provider.
     * This constructor initializes the provider with required services and
     * registers
     * it as a ready tracker for the HomeKit accessory server registry.
     *
     * @param storageService The storage service for persistence
     * @param accessoryServerRegistry The registry for HomeKit accessory servers
     * @param homekitAccessoryFactory The factory for creating accessories
     * @param readyService The service for managing system readiness
     */
    @Activate
    public HomekitPersistedAccessoryProvider(@Reference StorageService storageService,
            @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            @Reference HomekitAccessoryFactory homekitAccessoryFactory, @Reference ReadyService readyService) {
        super(storageService);
        this.homekitAccessoryFactory = homekitAccessoryFactory;
        this.readyService = readyService;

        final HomekitPersistedAccessoryProvider self = this;
        readyService.registerTracker(self, new ReadyMarkerFilter().withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
    }

    /**
     * Deactivates the provider and unregisters it from the ready service.
     * This method is called by the OSGi framework when the component is being
     * stopped.
     *
     * @param componentContext The OSGi component context
     */
    @Deactivate
    protected synchronized void deactivate(ComponentContext componentContext) {
        readyService.unregisterTracker(this);
    }

    /**
     * Performs delayed initialization of the provider.
     * This method ensures that all required services are available before marking
     * the provider as ready. It uses ThreadPoolManager to handle timing.
     */
    private synchronized void delayedInitialize() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        final long diff = System.nanoTime() - lastUpdate - INITIALIZATION_DELAY_NANOS;
        if (diff < 0) {
            ThreadPoolManager.getScheduledPool("homekit").schedule(() -> delayedInitialize(), -diff,
                    TimeUnit.NANOSECONDS);
        } else {
            logger.info("{}Marking the Managed HomekitAccessory Provider as ready", LOG_INIT);
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
            readyService.markReady(newMarker);
        }
    }

    /**
     * Gets the storage name for this provider.
     * This is used by the {@link StorageService} to identify the storage location.
     *
     * @return The storage name for HomeKit accessories
     */
    @Override
    protected String getStorageName() {
        return org.openhab.io.homekit.api.accessory.HomekitAccessory.class.getName();
    }

    /**
     * Converts a HomeKit accessory UID to a string key.
     * This method is used by the storage system to create unique keys.
     *
     * @param key The accessory UID to convert
     * @return The string representation of the UID
     */
    @Override
    protected @NonNull String keyToString(HomekitAccessoryUID key) {
        return key.toString();
    }

    /**
     * Converts a persisted accessory to a runtime accessory.
     * This method handles the restoration of accessories from storage,
     * using the appropriate factory to create the accessory instance.
     *
     * @param key The key identifying the accessory
     * @param persistableElement The persisted accessory data
     * @return The restored accessory, or null if restoration fails
     */
    @Override
    protected @Nullable HomekitAccessory toElement(String key, HomekitPersistedAccessory persistableElement) {
        HomekitAccessory accessory = null;

        JsonObject jsonObject = null;
        try (JsonReader jsonReader = Json.createReader(new StringReader(persistableElement.getJson()))) {
            jsonObject = jsonReader.readObject();
        }

        String accessoryType = persistableElement.getAccessoryType();
        if (homekitAccessoryFactory.supportsAccessoryType(accessoryType)) {
            try {
                accessory = homekitAccessoryFactory.createAccessoryWithArgs(accessoryType, (JsonValue) jsonObject);
            } catch (HomekitFactoryException e) {
                logger.error("{}Error creating accessory for type {}: {}", LOG_ERROR, accessoryType, e.getMessage());
            }
        } else {
            logger.warn("Accessory type {} is not supported by the factory", accessoryType);
        }

        return accessory;
    }

    /**
     * Converts a runtime accessory to a persisted accessory.
     * This method handles the serialization of accessories for storage.
     *
     * @param element The accessory to persist
     * @return The persisted accessory data
     */
    @Override
    protected @NonNull HomekitPersistedAccessory toPersistableElement(HomekitAccessory element) {
        return new HomekitPersistedAccessory(element.getClass().getName(), element.toJson().toString());
    }

    /**
     * Handles the addition of a ready marker.
     * This method is called by the {@link ReadyService} when a required service
     * becomes available, triggering the delayed initialization process.
     *
     * @param readyMarker The ready marker that was added
     */
    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added: {}", LOG_INIT, readyMarker);
        lastUpdate = System.nanoTime();
        delayedInitialize();
    }

    /**
     * Handles the removal of a ready marker.
     * This method is called by the {@link ReadyService} when a required service
     * becomes unavailable, unmarking this provider as ready.
     *
     * @param readyMarker The ready marker that was removed
     */
    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed: {}", LOG_INIT, readyMarker);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, this.toString());
        readyService.unmarkReady(newMarker);
    }
}
