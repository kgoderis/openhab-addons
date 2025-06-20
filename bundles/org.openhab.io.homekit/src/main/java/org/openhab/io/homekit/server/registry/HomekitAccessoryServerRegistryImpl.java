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

package org.openhab.io.homekit.server.registry;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.provider.HomekitAccessoryServerProvider;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.openhab.io.homekit.util.HomekitUID;
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
 * Implementation of the HomeKit Accessory Server Registry.
 *
 * <p>
 * This class manages the lifecycle and state of HomeKit accessory servers in
 * the system. It provides:
 * <ul>
 * <li>Server registration and discovery</li>
 * <li>Bridge accessory management</li>
 * <li>Event subscription handling</li>
 * <li>Server state tracking</li>
 * <li>Port allocation and management</li>
 * </ul>
 *
 * <p>
 * The registry integrates with:
 * <ul>
 * <li>{@link ReadyService} for system readiness tracking</li>
 * <li>{@link NetworkAddressService} for network configuration</li>
 * <li>{@link HomekitAccessoryRegistry} for accessory management</li>
 * <li>{@link HomekitPairingRegistry} for pairing state management</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * <li>{@link HomekitAccessoryFactory} for accessory creation</li>
 * </ul>
 *
 * <p>
 * Security considerations:
 * <ul>
 * <li>Manages server authentication and pairing state</li>
 * <li>Controls server advertisement and discovery</li>
 * <li>Enforces maximum accessory limits per server</li>
 * <li>Handles secure port allocation</li>
 * </ul>
 *
 * <p>
 * Lifecycle management:
 * <ul>
 * <li>Activates on system startup</li>
 * <li>Registers with ReadyService for system readiness</li>
 * <li>Manages provider registration and removal</li>
 * <li>Handles graceful shutdown</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@Component(immediate = true, service = HomekitAccessoryServerRegistry.class)
@NonNullByDefault
public class HomekitAccessoryServerRegistryImpl
        extends AbstractRegistry<HomekitAccessoryServer, HomekitAccessoryServerUID, HomekitAccessoryServerProvider>
        implements HomekitAccessoryServerRegistry, ReadyService.ReadyTracker {

    private static final String HOMEKIT_ACCESSORY_SERVER_REGISTRY = "homekit.accessoryServerRegistry";

    private static final String HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER = "homekit.managedAccessoryServerProvider";

    private static final int MAX_ACCESSORIES_PER_SERVER = 150;

    private static final int LOWEST_PORT_NUMBER = 9000;

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Registry: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final HomekitUID subscriberUID = new HomekitUID("registry");

    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerRegistryImpl.class);

    private final ReadyService readyService;
    private final NetworkAddressService networkAddressService;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitPairingRegistry pairingRegistry;
    private final HomekitEventManager eventManager;
    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();
    private final HomekitAccessoryFactory accessoryFactory;
    private volatile boolean readyMarkerRegistered = false;
    private final Set<String> processedReadyMarkers = new HashSet<>();

    /**
     * Initializes the HomeKit accessory server registry.
     *
     * <p>
     * This constructor sets up the registry with its dependencies and prepares
     * it for managing accessory servers. It registers with the ready service to track
     * the initialization of required components.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes registry state</li>
     * <li>Sets up ready service tracking</li>
     * <li>Prepares for provider management</li>
     * <li>Registers ready markers for dependencies</li>
     * <li>Ensures thread safety</li>
     * </ul>
     *
     * @param readyService The service for tracking component readiness
     * @param networkAddressService The service for network address management
     * @param accessoryRegistry The registry for accessories
     * @param pairingRegistry The registry for pairings
     * @param eventManager The manager for events
     * @param accessoryFactory The factory for creating accessories
     */
    @Activate
    public HomekitAccessoryServerRegistryImpl(@Reference ReadyService readyService,
            @Reference NetworkAddressService networkAddressService,
            @Reference HomekitAccessoryRegistry accessoryRegistry, @Reference HomekitPairingRegistry pairingRegistry,
            @Reference HomekitEventManager eventManager, @Reference HomekitAccessoryFactory accessoryFactory) {
        super(HomekitAccessoryServerProvider.class);
        logger.debug("{}Initializing HomeKit accessory server registry", LOG_INIT);
        this.readyService = readyService;
        this.networkAddressService = networkAddressService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.eventManager = eventManager;
        this.accessoryFactory = accessoryFactory;

        logger.debug("{}HomeKit accessory server registry initialized successfully", LOG_INIT);
    }

    /**
     * Activates the registry.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Activates the base registry functionality</li>
     * <li>Registers with ReadyService for system readiness tracking</li>
     * <li>Initializes event handling</li>
     * </ul>
     *
     * @param context The bundle context for OSGi integration
     */
    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
        logger.debug("{}Activating HomekitAccessory Server Registry", LOG_INIT);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER));
    }

    /**
     * Deactivates the registry.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Deactivates the base registry functionality</li>
     * <li>Cleans up event subscriptions</li>
     * <li>Removes system readiness tracking</li>
     * <li>Unregisters the ready marker tracker</li>
     * </ul>
     */
    @Override
    @Deactivate
    protected void deactivate() {
        logger.debug("{}Deactivating HomekitAccessory Server Registry", LOG_INIT);

        // Unregister the tracker to prevent duplicate notifications
        readyService.unregisterTracker(this);

        // Clear processed ready markers to prevent memory leaks
        processedReadyMarkers.clear();

        super.deactivate();
    }

    /**
     * Sets the managed provider for the registry.
     *
     * <p>
     * This method is called by OSGi when a managed provider becomes available.
     * It ensures proper initialization and registration of the provider.
     *
     * @param provider The managed provider to set
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(HomekitManagedAccessoryServerProvider provider) {
        super.setManagedProvider(provider);
    }

    /**
     * Removes the managed provider from the registry.
     *
     * <p>
     * This method is called by OSGi when a managed provider becomes unavailable.
     * It ensures proper cleanup of the provider's resources.
     *
     * @param provider The managed provider to remove
     */
    protected void unsetManagedProvider(HomekitManagedAccessoryServerProvider provider) {
        super.unsetManagedProvider(provider);
    }

    /**
     * Adds a provider to the registry.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the provider type</li>
     * <li>Checks system readiness for managed providers</li>
     * <li>Adds the provider with appropriate initialization</li>
     * </ul>
     *
     * @param provider The provider to add
     */
    @Override
    protected void addProvider(Provider<HomekitAccessoryServer> provider) {
        logger.debug("{}Adding provider: {}", LOG_CONFIG, provider.toString());

        ReadyMarker newMarker = new ReadyMarker(HOMEKIT_MANAGED_ACCESSORY_SERVER_PROVIDER, provider.toString());

        if (provider instanceof HomekitManagedAccessoryServerProvider) {
            if (readyService.isReady(newMarker)) {
                addProviderWithReadyMarker(provider);
            }
        } else {
            super.addProvider(provider);
        }
    }

    /**
     * Gets an available bridge accessory server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Searches for an existing server with available capacity</li>
     * <li>Creates a new server if none is available</li>
     * <li>Ensures proper bridge accessory configuration</li>
     * <li>Manages port allocation</li>
     * </ul>
     *
     * <p>
     * Error handling:
     * <ul>
     * <li>Handles network configuration errors</li>
     * <li>Manages server creation failures</li>
     * <li>Handles accessory operation exceptions</li>
     * </ul>
     *
     * @return An available bridge accessory server, or empty if none can be created
     */
    @Override
    public synchronized Optional<HomekitAccessoryServer> getAvailableBridgeAccessoryServer() {
        HomekitAccessoryServer availableServer = null;
        int highestPortNumber = LOWEST_PORT_NUMBER;
        try {
            for (HomekitAccessoryServer server : getAll()) {
                if (server.getAccessories().size() < MAX_ACCESSORIES_PER_SERVER) {
                    availableServer = server;
                    break;
                }
                if (server.getPort() > highestPortNumber) {
                    highestPortNumber = server.getPort();
                }
            }
        } catch (HomekitAccessoryOperationException e) {
            logger.error("{}Error accessing server accessories: {}", LOG_ERROR, e.getMessage(), e);
            return Optional.empty();
        }

        logger.info("{}Found {} HomekitAccessory Servers, highest port: {}", LOG_STATE, getAll().size(),
                highestPortNumber);

        if (availableServer == null) {
            try {
                String serverId = "bridge-" + System.currentTimeMillis();
                availableServer = new HomekitRemoteAccessoryServer(HomekitAccessoryCategory.BRIDGES, serverId,
                        InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()), highestPortNumber++,
                        accessoryRegistry, pairingRegistry, eventManager, accessoryFactory);
            } catch (UnknownHostException | HomekitServerException e) {
                logger.error("{}Failed to create HomekitRemoteAccessoryServer: {}", LOG_ERROR, e.getMessage(), e);
                return Optional.empty();
            }
        } else {
            try {
                if (availableServer.getAccessory(1).isEmpty()) {
                    try {
                        logger.info(
                                "{}Adding Bridge HomekitAccessory to Server - UID: {}, Type: {}, Port: {}, Setup Code: {}",
                                LOG_ACCESSORY, availableServer.getUID(), availableServer.getClass().getSimpleName(),
                                availableServer.getPort(), availableServer.getSetupCode());
                        HomekitAccessory bridgeAccessory = accessoryFactory.createAccessoryFromTag("generic");
                        bridgeAccessory.assignToServer(availableServer);
                    } catch (Exception e) {
                        logger.error("{}Error adding bridge accessory: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }

                logger.info(
                        "{}Found HomekitAccessory Server - UID: {}, Type: {}, Port: {}, Setup Code: {}, Accessories: {}",
                        LOG_STATE, availableServer.getUID(), availableServer.getClass().getSimpleName(),
                        availableServer.getPort(), availableServer.getSetupCode(),
                        availableServer.getAccessories().size());
            } catch (HomekitAccessoryOperationException e) {
                logger.error("{}Error accessing server accessories: {}", LOG_ERROR, e.getMessage(), e);
            }
        }

        return Optional.ofNullable(availableServer);
    }

    /**
     * Handles the addition of a ready marker.
     *
     * <p>
     * This method is called when a component becomes ready. It:
     * <ul>
     * <li>Logs the ready marker addition</li>
     * <li>Adds the managed provider if available</li>
     * </ul>
     *
     * @param readyMarker The ready marker that was added
     */
    @Override
    public synchronized void onReadyMarkerAdded(ReadyMarker readyMarker) {
        String markerKey = readyMarker.getType() + ":" + readyMarker.getIdentifier();

        if (processedReadyMarkers.contains(markerKey)) {
            logger.debug("{}Duplicate ready marker ignored - Type: {}, Identifier: {}", LOG_STATE,
                    readyMarker.getType(), readyMarker.getIdentifier());
            return;
        }

        processedReadyMarkers.add(markerKey);
        logger.debug("{}Ready marker added - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());

        if (getManagedProvider().isPresent()) {
            @SuppressWarnings("null") // get() is safe after isPresent() check
            Provider<HomekitAccessoryServer> provider = getManagedProvider().get();
            addProviderWithReadyMarker(provider);
        }
    }

    /**
     * Handles the removal of a ready marker.
     *
     * <p>
     * This method is called when a component is no longer ready. It:
     * <ul>
     * <li>Logs the ready marker removal</li>
     * <li>Updates system state accordingly</li>
     * </ul>
     *
     * @param readyMarker The ready marker that was removed
     */
    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("{}Ready marker removed - Type: {}, Identifier: {}", LOG_STATE, readyMarker.getType(),
                readyMarker.getIdentifier());
    }

    /**
     * Handles accessory server events.
     *
     * <p>
     * This method processes events from accessory servers:
     * <ul>
     * <li>Updates server state changes</li>
     * <li>Manages server lifecycle events</li>
     * </ul>
     *
     * @param event The event to handle
     */
    public void handleAccessoryServerEvent(HomekitAccessoryServerEvent event) {
        switch (event.getType()) {
            case SERVER_STATE_CHANGED -> {
                if (event.getServer().isPresent()) {
                    @SuppressWarnings("null") // get() is safe after isPresent() check
                    HomekitAccessoryServer server = event.getServer().get();
                    this.update(server);
                }
            }
            default -> {
                // No Op
            }
        }
    }

    /**
     * Adds a provider with a ready marker.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Adds the provider to the registry</li>
     * <li>Advertises available servers</li>
     * <li>Marks the registry as ready</li>
     * </ul>
     *
     * @param provider The provider to add
     */
    public synchronized void addProviderWithReadyMarker(Provider<HomekitAccessoryServer> provider) {
        super.addProvider(provider);

        for (HomekitAccessoryServer aServer : getAll()) {
            logger.debug("{}HomekitAccessory Server available - UID: {}, Setup Code: {}", LOG_ACCESSORY,
                    aServer.getUID(), aServer.getSetupCode());
            if (aServer instanceof HomekitAccessoryServer accessoryServer) {
                accessoryServer.advertise();
            }
        }

        // Only register the ready marker once, even if multiple providers are added
        if (!readyMarkerRegistered) {
            logger.info("{}Marking HomekitAccessory Server Registry as ready", LOG_STATE);
            ReadyMarker newMarker = new ReadyMarker(HOMEKIT_ACCESSORY_SERVER_REGISTRY, this.toString());
            readyService.markReady(newMarker);
            readyMarkerRegistered = true;
        }
    }

    /**
     * Handles the addition of a server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Subscribes to server state changes</li>
     * <li>Adds the server to the registry</li>
     * </ul>
     *
     * @param provider The provider that added the server
     * @param element The server that was added
     */
    @Override
    public void added(Provider<HomekitAccessoryServer> provider, HomekitAccessoryServer element) {
        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVER_STATE_CHANGED, (UID) element.getUID(),
                subscriberUID, event -> handleAccessoryServerEvent((HomekitAccessoryServerEvent) event)));
        super.added(provider, element);
    }

    /**
     * Handles the removal of a server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Unsubscribes from server events</li>
     * <li>Removes the server from the registry</li>
     * <li>Cleans up associated resources</li>
     * </ul>
     *
     * <p>
     * Error handling:
     * <ul>
     * <li>Handles event unsubscription failures</li>
     * <li>Ensures proper cleanup on errors</li>
     * </ul>
     *
     * @param provider The provider that removed the server
     * @param element The server that was removed
     */
    @Override
    public void removed(Provider<HomekitAccessoryServer> provider, HomekitAccessoryServer element) {
        try {
            // get all the subscriptions for this accessory server
            List<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                    .filter(subscription -> subscription.getPublisherUID().equals((UID) element.getUID()))
                    .collect(Collectors.toList());

            // unsubscribe from the events
            subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription.getEventType(),
                    subscription.getPublisherUID(), subscription.getSubscriber()));

            super.removed(provider, element);
        } catch (Exception e) {
            logger.error("{}Error removing change listener: {}", LOG_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Gets the server for a specific accessory.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Searches all servers for the accessory</li>
     * <li>Handles accessory operation exceptions</li>
     * <li>Returns the first matching server</li>
     * </ul>
     *
     * @param accessoryUID The UID of the accessory to find
     * @return Optional containing the server with the accessory, or empty if not
     *         found
     */
    public Optional<HomekitAccessoryServer> getAccessoryServer(HomekitAccessoryUID accessoryUID) {
        return getAll().stream().filter(server -> {
            try {
                return server.getAccessories().stream().anyMatch(accessory -> accessory.getUID().equals(accessoryUID));
            } catch (HomekitAccessoryOperationException e) {
                return false;
            }
        }).findFirst();
    }
}
