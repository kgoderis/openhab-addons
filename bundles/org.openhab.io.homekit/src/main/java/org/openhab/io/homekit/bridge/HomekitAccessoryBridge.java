package org.openhab.io.homekit.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.event.HomekitEventSubscriber;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.openhab.io.homekit.util.HomekitUID;
import org.openhab.io.homekit.event.model.accessory.HomekitAccessoryEvent;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.openhab.io.homekit.core.accessory.HomekitBridgedAccessory;

/**
 * Manages the bridging of accessories between remote and local accessory servers.
 * This class handles the setup and teardown of event subscriptions to forward
 * events and commands between the remote and local servers.
 */
@Component(service = HomekitAccessoryBridge.class, immediate = true)
@NonNullByDefault
public class HomekitAccessoryBridge {
    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridge.class);
    private static final String LOG_PREFIX = "Homekit Bridge: ";
    private final HomekitUID bridgeUID = new HomekitUID("bridge");

    private final HomekitEventManager eventManager;
    private final HomekitAccessoryServerRegistry serverRegistry;
    private final HomekitAccessoryFactory accessoryFactory;
    private final HomekitConfigurationManager configManager;
    private final Map<HomekitAccessory, BridgeContext> bridgedAccessories = new ConcurrentHashMap<>();
    private List<HomekitEventSubscription> eventSubscriptions;

    /**
     * Creates a new AccessoryBridgeManager.
     *
     * @param eventManager The event manager to use for event handling
     * @param serverRegistry The server registry to use for accessing local servers
     * @param accessoryFactory The accessory factory to use for creating local copies of accessories
     * @param configManager The configuration manager to use for fetching accessory configurations
     */
    @Activate
    public HomekitAccessoryBridge(
            @Reference HomekitEventManager eventManager,
            @Reference HomekitAccessoryServerRegistry serverRegistry,
            @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitConfigurationManager configManager) {
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.accessoryFactory = accessoryFactory;
        this.configManager = configManager;
        
        // Subscribe to accessory events using lambdas
        this.eventSubscriptions = List.of(
            eventManager.subscribe(
                HomekitEventType.ACCESSORY_ADDED,
                HomekitUID.WILDCARD_UID,
                bridgeUID,
                event -> {
                    if (event instanceof HomekitAccessoryServerEvent serverEvent) {
                        serverEvent.getAccessory().ifPresent(accessory -> {
                            serverEvent.getServer().ifPresent(server -> {
                                handleAccessoryAdded(accessory, server);
                            });
                        });
                    }
                }
            ),
            eventManager.subscribe(
                HomekitEventType.ACCESSORY_REMOVED,
                HomekitUID.WILDCARD_UID,
                bridgeUID,
                event -> {
                    if (event instanceof HomekitAccessoryServerEvent serverEvent) {
                        serverEvent.getAccessory().ifPresent(HomekitAccessoryBridge.this::handleAccessoryRemoved);
                    }
                }
            )
        );
    }

    /**
     * Bridges an accessory between a remote and local server.
     * Sets up event subscriptions to forward events and commands between the servers.
     *
     * @param remoteAccessory The accessory to bridge
     * @param remoteServer The remote server the accessory belongs to
     * @param localServer The local server to expose the accessory on
     * @param localAccessory The local accessory to be added to the local server
     * @throws HomekitAccessoryOperationException if there is an error adding or removing the accessory
     */
    public void bridgeAccessory(HomekitAccessory remoteAccessory, HomekitAccessoryServer remoteServer,
            HomekitAccessoryServer localServer, HomekitAccessory localAccessory)
            throws HomekitAccessoryOperationException {
        logger.debug("{}Bridging accessory {} from remote server {} to local server {}", LOG_PREFIX,
                remoteAccessory.getUID(), remoteServer.getUID(), localServer.getUID());

        try {
            // Add the local accessory to the local server
            localServer.addAccessory(localAccessory);
            logger.debug("{}Added local accessory {} to local server {}", LOG_PREFIX, localAccessory.getUID(),
                    localServer.getUID());

            // Set up event forwarding from remote to local
            List<HomekitEventSubscription> remoteSubs = eventManager.subscribe(
                    Set.of(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, HomekitEventType.SERVICE_ADDED,
                            HomekitEventType.SERVICE_REMOVED, HomekitEventType.ACCESSORY_STATE_CHANGED),
                    remoteAccessory.getUID(), bridgeUID, event -> {
                        logger.debug("{}Forwarding event from remote to local: {}", LOG_PREFIX, event);
                        // The local server will handle the event through its event manager
                        eventManager.publishEvent(event);
                    });

            // Set up command forwarding from local to remote
            List<HomekitEventSubscription> localSubs = eventManager.subscribe(
                    Set.of(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED), localAccessory.getUID(), bridgeUID,
                    event -> { // Use local accessory UID for local events
                        logger.debug("{}Forwarding command from local to remote: {}", LOG_PREFIX, event);
                        // The remote server will handle the event through its event manager
                        eventManager.publishEvent(event);
                    });

            // Store the bridge context with both servers
            bridgedAccessories.put(remoteAccessory,
                    new BridgeContext(remoteSubs, localSubs, localServer, remoteServer, localAccessory));
        } catch (Exception e) {
            logger.error("{}Failed to bridge accessory {}: {}", LOG_PREFIX, remoteAccessory.getUID(), e.getMessage(),
                    e);
            // Clean up if anything fails
            try {
                localServer.removeAccessory(localAccessory);
            } catch (Exception cleanupException) {
                logger.error("{}Failed to clean up local accessory {}: {}", LOG_PREFIX, localAccessory.getUID(),
                        cleanupException.getMessage(), cleanupException);
            }
            throw e;
        }
    }

    /**
     * Removes the bridging for an accessory.
     * Cleans up all event subscriptions associated with the bridge and removes the local accessory.
     *
     * @param accessory The accessory to unbridge
     */
    public void unbridgeAccessory(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.remove(accessory);
        if (ctx != null) {
            logger.debug("{}Unbridging accessory {} from remote server {} and local server {}", LOG_PREFIX,
                    accessory.getUID(), ctx.getRemoteServer().getUID(), ctx.getLocalServer().getUID());
            ctx.getRemoteSubscriptions().forEach(eventManager::unsubscribe);
            ctx.getLocalSubscriptions().forEach(eventManager::unsubscribe);
            try {
                ctx.getLocalServer().removeAccessory(ctx.getLocalAccessory());
                logger.debug("{}Removed local accessory {} from local server {}", LOG_PREFIX,
                        ctx.getLocalAccessory().getUID(), ctx.getLocalServer().getUID());
            } catch (Exception e) {
                logger.error("{}Failed to remove local accessory {}: {}", LOG_PREFIX, ctx.getLocalAccessory().getUID(),
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Checks if an accessory is currently bridged.
     *
     * @param accessory The accessory to check
     * @return true if the accessory is bridged, false otherwise
     */
    public boolean isBridged(HomekitAccessory accessory) {
        return bridgedAccessories.containsKey(accessory);
    }

    /**
     * Gets the remote server for a bridged accessory.
     *
     * @param accessory The bridged accessory
     * @return Optional containing the remote server if the accessory is bridged
     */
    public Optional<HomekitAccessoryServer> getRemoteServer(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? Optional.of(ctx.getRemoteServer()) : Optional.empty();
    }

    /**
     * Gets the local server for a bridged accessory.
     *
     * @param accessory The bridged accessory
     * @return Optional containing the local server if the accessory is bridged
     */
    public Optional<HomekitAccessoryServer> getLocalServer(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? Optional.of(ctx.getLocalServer()) : Optional.empty();
    }

    /**
     * Gets the local accessory for a bridged remote accessory.
     *
     * @param remoteAccessory The remote accessory
     * @return The local accessory, or null if the remote accessory is not bridged
     */
    public HomekitAccessory getLocalAccessory(HomekitAccessory remoteAccessory) {
        BridgeContext ctx = bridgedAccessories.get(remoteAccessory);
        return ctx != null ? ctx.getLocalAccessory() : null;
    }

    /**
     * Handles an accessory added event.
     * If the accessory has a "bridge" configuration parameter set to true,
     * it will be bridged to a local server.
     *
     * @param accessory The accessory that was added
     * @param remoteServer The remote server containing the accessory
     */
    private void handleAccessoryAdded(HomekitAccessory accessory, HomekitAccessoryServer remoteServer) {
        try {
            // Fetch config for the accessory
            Optional<Map<String, Object>> configOpt = configManager.getConfiguration(
                accessory.getUID().toString(), HomekitConfigurationManager.ConfigurationType.ACCESSORY);
            if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().get("bridge"))) {
                logger.debug("{}Accessory {} not configured for bridging (missing or false 'bridge' parameter)", LOG_PREFIX, accessory.getUID());
                return;
            }

            // Only bridge if accessory belongs to a remote accessory server (not a local server)
            if (remoteServer == null || isLocalServer(remoteServer)) {
                logger.debug("{}Accessory {} is not from a remote server, skipping bridging", LOG_PREFIX, accessory.getUID());
                return;
            }

            Optional<HomekitAccessoryServer> localServerOpt = getAvailableLocalServer();
            if (localServerOpt.isEmpty()) {
                logger.error("{}No available local server found for bridging", LOG_PREFIX);
                return;
            }

            HomekitAccessoryServer localServer = localServerOpt.get();
            HomekitAccessory localAccessory = createLocalAccessory(accessory, localServer);
            if (localAccessory == null) {
                logger.error("{}Failed to create local accessory for {}", LOG_PREFIX, accessory.getUID());
                return;
            }

            bridgeAccessory(accessory, remoteServer, localServer, localAccessory);
            logger.info("{}Successfully bridged accessory {} from remote server {} to local server {}", LOG_PREFIX,
                    accessory.getUID(), remoteServer.getUID(), localServer.getUID());
        } catch (Exception e) {
            logger.error("{}Failed to bridge accessory {}: {}", LOG_PREFIX, accessory.getUID(), e.getMessage(), e);
        }
    }

    /**
     * Determines if the given server is a local server.
     * This can be customized as needed to distinguish local from remote servers.
     */
    private boolean isLocalServer(HomekitAccessoryServer server) {
        if(server instanceof HomekitRemoteAccessoryServer) {
            return false;
        }
        return true;
    }

    /**
     * Handles an accessory removed event.
     * Removes the bridged accessory from the local server.
     *
     * @param accessory The accessory that was removed
     */
    private void handleAccessoryRemoved(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        if (ctx != null) {
            try {
                ctx.getLocalServer().removeAccessory(ctx.getLocalAccessory());
                logger.info("{}Successfully unbridged accessory {} from local server {}", LOG_PREFIX,
                        accessory.getUID(), ctx.getLocalServer().getUID());
            } catch (Exception e) {
                logger.error("{}Failed to unbridge accessory {}: {}", LOG_PREFIX, accessory.getUID(), e.getMessage(), e);
            }
        }
    }

    /**
     * Gets an available local server for bridging.
     * 
     * @return Optional containing an available local server, or empty if none are available
     */
    private Optional<HomekitAccessoryServer> getAvailableLocalServer() {
        return Optional.ofNullable(serverRegistry.getAvailableBridgeAccessoryServer());
    }

    /**
     * Creates a local copy of a remote accessory.
     * 
     * @param remoteAccessory The remote accessory to copy
     * @return The local copy of the accessory, or null if creation failed
     */
    private HomekitAccessory createLocalAccessory(HomekitAccessory remoteAccessory, HomekitAccessoryServer localServer) {
        try {
            return accessoryFactory.createAccessoryWithArgs("bridged", remoteAccessory, localServer);
        } catch (Exception e) {
            logger.error("{}Failed to create local accessory: {}", LOG_PREFIX, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Context class for bridged accessories.
     * Holds references to the local server, local accessory, and remote server.
     */
    private static class BridgeContext {
        private final List<HomekitEventSubscription> remoteSubs;
        private final List<HomekitEventSubscription> localSubs;
        private final HomekitAccessoryServer localServer;
        private final HomekitAccessoryServer remoteServer;
        private final HomekitAccessory localAccessory;

        public BridgeContext(List<HomekitEventSubscription> remoteSubs, List<HomekitEventSubscription> localSubs,
                HomekitAccessoryServer localServer, HomekitAccessoryServer remoteServer,
                HomekitAccessory localAccessory) {
            this.remoteSubs = remoteSubs;
            this.localSubs = localSubs;
            this.localServer = localServer;
            this.remoteServer = remoteServer;
            this.localAccessory = localAccessory;
        }

        public HomekitAccessoryServer getLocalServer() {
            return localServer;
        }

        public HomekitAccessory getLocalAccessory() {
            return localAccessory;
        }

        public HomekitAccessoryServer getRemoteServer() {
            return remoteServer;
        }

        public List<HomekitEventSubscription> getRemoteSubscriptions() {
            return remoteSubs;
        }

        public List<HomekitEventSubscription> getLocalSubscriptions() {
            return localSubs;
        }
    }
}
