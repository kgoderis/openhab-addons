/**
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
package org.openhab.io.homekit.network.pairing;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.io.homekit.api.provider.HomekitPairingProvider;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;
import org.openhab.io.homekit.util.HomekitByte;
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
 * Implements the registry for HomeKit pairings.
 *
 * This class provides a central registry for managing HomeKit pairings, extending
 * the functionality of {@link AbstractRegistry} to handle pairing-specific operations.
 * It coordinates with various components to ensure proper initialization and
 * availability of pairings.
 *
 * The registry works in conjunction with:
 * - {@link HomekitPairingProvider} for pairing data sources
 * - {@link HomekitManagedPairingProvider} for managed pairings
 * - {@link HomekitPairingImpl} for pairing implementations
 * - {@link HomekitPairingUIDImpl} for unique identifiers
 *
 * Key responsibilities:
 * 1. Managing pairing lifecycle
 * 2. Coordinating with providers
 * 3. Ensuring proper initialization
 * 4. Supporting pairing queries
 * 5. Maintaining pairing state
 *
 * The implementation uses OSGi services for component lifecycle management and
 * integrates with the OpenHAB ready service to ensure proper initialization
 * order of components.
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@NonNullByDefault
@Component(immediate = true, service = HomekitPairingRegistry.class)
public class HomekitPairingRegistryImpl
        extends AbstractRegistry<HomekitPairing, HomekitPairingUID, HomekitPairingProvider>
        implements HomekitPairingRegistry, ReadyService.ReadyTracker {

    private final Logger logger = LoggerFactory.getLogger(HomekitPairingRegistry.class);

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit HomekitPairingRegistry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_PROVIDER = LOG_PREFIX + "Provider - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private static final String HOMEKIT_PAIRING_REGISTRY = "homekit.pairingRegistry";
    private static final String HOMEKIT_MANAGED_PAIRING_PROVIDER = "homekit.managedPairingProvider";
    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    private final ReadyService readyService;
    private boolean accessoryServerRegistryReady = false;
    private boolean managedPairingProviderReady = false;

    /**
     * Initializes the HomeKit pairing registry.
     *
     * This constructor sets up the registry with its dependencies and prepares
     * it for managing pairings. It registers with the ready service to track
     * the initialization of required components.
     *
     * Key implementation details:
     * - Initializes registry state
     * - Sets up ready service tracking
     * - Prepares for provider management
     *
     * @param readyService The service for tracking component readiness
     */
    @Activate
    public HomekitPairingRegistryImpl(@Reference ReadyService readyService) {
        super(HomekitPairingProvider.class);
        logger.debug("{}Initializing HomekitPairingRegistry", LOG_INIT);
        this.readyService = readyService;

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_PAIRING_PROVIDER)
                .withType(HOMEKIT_ACCESSORY_SERVER_REGISTRY));
        logger.debug("{}HomekitPairingRegistry initialized", LOG_INIT);
    }

    /**
     * Sets the managed provider for this registry.
     *
     * This method is called by OSGi to inject the managed provider, which
     * handles system-managed pairings.
     *
     * @param provider The managed provider to set
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(HomekitManagedPairingProvider provider) {
        logger.debug("{}Setting managed provider: {}", LOG_PROVIDER, provider);
        super.setManagedProvider(provider);
    }

    /**
     * Removes the managed provider from this registry.
     *
     * This method is called by OSGi when the managed provider is being removed.
     *
     * @param provider The managed provider to remove
     */
    protected void unsetManagedProvider(HomekitManagedPairingProvider provider) {
        logger.debug("{}Removing managed provider: {}", LOG_PROVIDER, provider);
        super.unsetManagedProvider(provider);
    }

    /**
     * Activates the registry.
     *
     * This method is called by OSGi when the component is being activated.
     * It initializes the registry and prepares it for use.
     *
     * @param context The bundle context
     */
    @Override
    @Activate
    protected void activate(final BundleContext context) {
        logger.debug("{}Activating HomekitPairingRegistry", LOG_INIT);
        super.activate(context);
        logger.debug("{}HomekitPairingRegistry activated", LOG_INIT);
    }

    /**
     * Deactivates the registry.
     *
     * This method is called by OSGi when the component is being deactivated.
     * It performs cleanup operations and releases resources.
     */
    @Override
    @Deactivate
    protected void deactivate() {
        logger.debug("{}Deactivating HomekitPairingRegistry", LOG_INIT);
        super.deactivate();
        logger.debug("{}HomekitPairingRegistry deactivated", LOG_INIT);
    }

    /**
     * Gets all pairings with the specified pairing ID.
     *
     * This method retrieves all pairings that match the given pairing ID,
     * which is used to identify specific pairing relationships.
     *
     * @param pairingId The pairing ID to search for
     * @return A collection of matching pairings
     */
    @Override
    public Collection<HomekitPairing> get(byte[] pairingId) {
        logger.debug("{}Getting pairings for ID: {}", LOG_STATE, HomekitByte.toHexString(pairingId));
        return getAll().stream().filter(p -> Arrays.equals(p.getUID().getSourcePairingId(), pairingId))
                .collect(Collectors.toList());
    }

    /**
     * Adds a provider to this registry.
     *
     * This method handles the addition of a new provider, ensuring proper
     * initialization and readiness tracking.
     *
     * Key implementation details:
     * - Validates provider type
     * - Checks provider readiness
     * - Updates registry state
     *
     * @param provider The provider to add
     */
    @Override
    protected void addProvider(Provider<HomekitPairing> provider) {
        logger.debug("{}Adding provider: {}", LOG_PROVIDER, provider);

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_PAIRING_PROVIDER, provider.toString());

        if (provider instanceof HomekitManagedPairingProvider) {
            if (readyService.isReady(newMarker)) {
                super.addProvider(provider);
                managedPairingProviderReady = true;
                logger.debug("{}Managed provider added and ready", LOG_PROVIDER);
            } else {
                logger.debug("{}Managed provider not ready yet", LOG_PROVIDER);
            }
        } else {
            super.addProvider(provider);
            logger.debug("{}Standard provider added", LOG_PROVIDER);
        }
    }

    /**
     * Handles the addition of a ready marker.
     *
     * This method is called when a component signals that it is ready.
     * It coordinates the initialization of the registry and its dependencies.
     *
     * Key implementation details:
     * - Tracks component readiness
     * - Coordinates initialization
     * - Updates registry state
     *
     * @param readyMarker The ready marker that was added
     */
    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker added: {}:{}", LOG_STATE, readyMarker.getType(), readyMarker.getIdentifier());

        if (readyMarker.getType() == HOMEKIT_ACCESSORY_SERVER_REGISTRY) {
            accessoryServerRegistryReady = true;
            logger.debug("{}Accessory server registry is ready", LOG_STATE);
        }

        if (readyMarker.getType() == HOMEKIT_MANAGED_PAIRING_PROVIDER) {
            if (getManagedProvider().isPresent()) {
                super.addProvider(getManagedProvider().get());
            }
            managedPairingProviderReady = true;
            logger.debug("{}Managed pairing provider is ready", LOG_STATE);
        }

        if (accessoryServerRegistryReady && managedPairingProviderReady) {
            for (HomekitPairing aPairing : getAll()) {
                logger.debug("{}Pairing {} with public key {} is available", LOG_STATE, aPairing.getUID(),
                        HomekitByte.toHexString(aPairing.getPublicKey()));
            }

            logger.info("{}Marking HomekitPairingRegistry as ready", LOG_STATE);
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_PAIRING_REGISTRY, this.toString());
            readyService.markReady(newMarker);
        }
    }

    /**
     * Handles the removal of a ready marker.
     *
     * This method is called when a component signals that it is no longer ready.
     * It updates the registry state accordingly.
     *
     * Key implementation details:
     * - Updates component readiness state
     * - Handles provider removal
     * - Maintains registry consistency
     *
     * @param readyMarker The ready marker that was removed
     */
    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed: {}:{}", LOG_STATE, readyMarker.getType(), readyMarker.getIdentifier());

        if (readyMarker.getType() == HOMEKIT_ACCESSORY_SERVER_REGISTRY) {
            accessoryServerRegistryReady = false;
            logger.debug("{}Accessory server registry is no longer ready", LOG_STATE);
        }

        if (readyMarker.getType() == HOMEKIT_MANAGED_PAIRING_PROVIDER) {
            managedPairingProviderReady = false;
            logger.debug("{}Managed pairing provider is no longer ready", LOG_STATE);
        }
    }
}
