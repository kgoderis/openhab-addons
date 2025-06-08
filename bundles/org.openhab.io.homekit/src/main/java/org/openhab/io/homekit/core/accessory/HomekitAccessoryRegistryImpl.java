/*
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the HomeKit accessory registry that manages the lifecycle and availability of HomeKit accessories.
 *
 * This class serves as a central repository for all HomeKit accessories in the OpenHAB system, providing
 * registration, discovery, and management capabilities. It operates as an OSGi service that integrates
 * with OpenHAB's registry and ready service systems.
 *
 * Key responsibilities:
 * - Managing accessory registration and discovery
 * - Coordinating with accessory providers
 * - Handling accessory lifecycle events
 * - Ensuring proper initialization order
 * - Maintaining accessory availability state
 *
 * The class integrates with:
 * - {@link AbstractRegistry} for base registry functionality
 * - {@link HomekitAccessoryProvider} for accessory source management
 * - {@link HomekitAccessory} for accessory lifecycle
 * - {@link ReadyService} for system readiness management
 * - {@link ManagedProvider} for persistent storage integration
 * - {@link org.openhab.core.common.registry.Provider OpenHAB's provider system} for accessory management
 * - {@link org.openhab.core.service.ReadyMarker OpenHAB's ready marker system} for initialization coordination
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@Component(immediate = true, service = HomekitAccessoryRegistry.class)
@NonNullByDefault
public class HomekitAccessoryRegistryImpl
        extends AbstractRegistry<HomekitAccessory, HomekitAccessoryUID, HomekitAccessoryProvider>
        implements HomekitAccessoryRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryRegistryImpl.class);

    private static final String HOMEKIT_MANAGED_ACCESSORY_PROVIDER = "homekit.managedAccessoryProvider";
    private static final String HOMEKIT_ACCESSORY_REGISTRY = "homekit.accessoryRegistry";

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Registry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final ReadyService readyService;

    /**
     * Creates a new HomeKit accessory registry instance.
     * This constructor initializes the registry with the required ready service
     * for managing system initialization.
     *
     * @param readyService The service for managing system readiness
     * @see ReadyService
     */
    @Activate
    public HomekitAccessoryRegistryImpl(@Reference ReadyService readyService) {
        super(HomekitAccessoryProvider.class);
        this.readyService = readyService;
    }

    /**
     * Sets the managed provider for this registry.
     * This method is called by the OSGi framework when a managed provider becomes available.
     *
     * @param provider The managed provider to set
     * @see ManagedProvider
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setManagedProvider(ManagedProvider<HomekitAccessory, HomekitAccessoryUID> provider) {
        super.setManagedProvider(provider);
    }

    /**
     * Removes the managed provider from this registry.
     * This method is called by the OSGi framework when a managed provider becomes unavailable.
     *
     * @param provider The managed provider to remove
     * @see ManagedProvider
     */
    @Override
    protected void unsetManagedProvider(ManagedProvider<HomekitAccessory, HomekitAccessoryUID> provider) {
        super.unsetManagedProvider(provider);
    }

    /**
     * Activates the registry and registers it with the ready service.
     * This method is called by the OSGi framework when the component is being started.
     *
     * @param context The OSGi bundle context
     * @see BundleContext
     */
    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
        logger.debug("{}Activating HomekitAccessory Registry", LOG_INIT);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_PROVIDER));
    }

    /**
     * Deactivates the registry and unregisters it from the ready service.
     * This method is called by the OSGi framework when the component is being stopped.
     */
    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
        logger.debug("{}Deactivating HomekitAccessory Registry", LOG_INIT);
        readyService.unregisterTracker(this);
    }

    /**
     * Handles the addition of a ready marker.
     * This method is called when a component signals that it is ready to operate.
     *
     * @param readyMarker The ready marker that was added
     * @see ReadyMarker
     */
    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());

        if (getManagedProvider().isPresent()) {
            addProviderWithReadyMarker(getManagedProvider().get());
        }
    }

    /**
     * Handles the removal of a ready marker.
     * This method is called when a component signals that it is no longer ready to operate.
     *
     * @param readyMarker The ready marker that was removed
     * @see ReadyMarker
     */
    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());
    }

    /**
     * Adds a provider to the registry.
     * This method ensures that providers are only added when the system is ready.
     *
     * @param provider The provider to add
     * @see Provider
     */
    @Override
    protected void addProvider(Provider<HomekitAccessory> provider) {
        logger.debug("{}Adding provider: {}", LOG_CONFIG, provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_PROVIDER, provider.toString());

        if (provider instanceof Provider<HomekitAccessory>) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    /**
     * Adds a provider to the registry and marks the registry as ready.
     * This method is called when a provider is ready to be added to the registry.
     *
     * @param provider The provider to add
     * @see Provider
     */
    public synchronized void addProviderWithReadyMarker(Provider<HomekitAccessory> provider) {
        super.addProvider(provider);

        for (HomekitAccessory accessory : getAll()) {
            logger.debug("{}HomekitAccessory available in registry - UID: {}", LOG_ACCESSORY, accessory.getUID());
        }

        logger.info("{}Marking HomekitAccessory Registry as ready", LOG_STATE);
        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_REGISTRY, this.toString());
        readyService.markReady(newMarker);
    }
}
