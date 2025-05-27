package org.openhab.io.homekit.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.config.HomekitConfigurationManager;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.openhab.io.homekit.util.HomekitUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the bridging of accessories between remote and local accessory servers.
 *
 * This class implements the core functionality for bridging HomeKit accessories between remote and local servers.
 * It handles the setup and teardown of event subscriptions to forward events and commands between servers,
 * manages the lifecycle of bridged accessories, and provides mechanisms for orphaned accessory detection and restoration.
 *
 * The class integrates with:
 * - {@link HomekitEventManager} for event handling and subscription management
 * - {@link HomekitAccessoryServerRegistry} for server discovery and management
 * - {@link HomekitAccessoryFactory} for creating local accessory copies
 * - {@link HomekitConfigurationManager} for accessory configuration management
 * - {@link HomekitRemoteAccessoryServer} for remote server functionality
 * - {@link org.openhab.core.thing.UID OpenHAB's UID system} for unique identification
 * - {@link HomekitEventSubscription} for event subscription management
 * - {@link HomekitAccessoryServerEvent} for server event handling
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@Component(service = HomekitAccessoryBridge.class, immediate = true)
@NonNullByDefault
public class HomekitAccessoryBridge {
    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridge.class);
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit Bridge: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private static final String YAML_FILE_NAME = "homekit-accessory-bridge.yaml";
    private final HomekitUID bridgeUID = new HomekitUID("bridge");

    // Configuration key for orphan functionality
    private static final String CONFIG_ORPHAN_ENABLED = "orphanEnabled";
    private boolean orphanEnabled = true; // Default to true for backward compatibility

    private final HomekitEventManager eventManager;
    private final HomekitAccessoryServerRegistry serverRegistry;
    private final HomekitAccessoryFactory accessoryFactory;
    private final HomekitConfigurationManager configManager;
    private final Map<HomekitAccessory, BridgeContext> bridgedAccessories = new ConcurrentHashMap<>();
    private List<HomekitEventSubscription> eventSubscriptions;

    /**
     * Creates a new AccessoryBridgeManager.
     *
     * This method initializes the bridge manager with required dependencies and sets up event subscriptions
     * for accessory management. It also loads configuration settings for orphaned accessory handling.
     *
     * Key implementation details:
     * - Initializes event subscriptions for accessory added/removed events
     * - Loads orphan configuration from properties
     * - Sets up event handlers using lambda expressions
     * - Configures logging for initialization steps
     *
     * @param eventManager The {@link HomekitEventManager} to use for event handling
     * @param serverRegistry The {@link HomekitAccessoryServerRegistry} to use for accessing local servers
     * @param accessoryFactory The {@link HomekitAccessoryFactory} to use for creating local copies of accessories
     * @param configManager The {@link HomekitConfigurationManager} to use for fetching accessory configurations
     * @param properties The configuration properties for the bridge
     * @since 1.0.0
     */
    @Activate
    public HomekitAccessoryBridge(@Reference HomekitEventManager eventManager,
            @Reference HomekitAccessoryServerRegistry serverRegistry,
            @Reference HomekitAccessoryFactory accessoryFactory, @Reference HomekitConfigurationManager configManager,
            Map<String, Object> properties) {
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.accessoryFactory = accessoryFactory;
        this.configManager = configManager;

        // Load orphan configuration
        Object orphanConfig = properties.get(CONFIG_ORPHAN_ENABLED);
        if (orphanConfig != null) {
            this.orphanEnabled = Boolean.parseBoolean(orphanConfig.toString());
            logger.info("{}Orphan functionality is {}", LOG_PREFIX, orphanEnabled ? "enabled" : "disabled");
        }

        // Subscribe to accessory events using lambdas
        this.eventSubscriptions = List.of(
                eventManager.subscribe(HomekitEventType.ACCESSORY_ADDED, HomekitUID.WILDCARD_UID, bridgeUID, event -> {
                    if (event instanceof HomekitAccessoryServerEvent serverEvent) {
                        serverEvent.getAccessory().ifPresent(accessory -> {
                            serverEvent.getServer().ifPresent(server -> {
                                handleAccessoryAdded(accessory, server);
                            });
                        });
                    }
                }), eventManager.subscribe(HomekitEventType.ACCESSORY_REMOVED, HomekitUID.WILDCARD_UID, bridgeUID,
                        event -> {
                            if (event instanceof HomekitAccessoryServerEvent serverEvent) {
                                serverEvent.getAccessory()
                                        .ifPresent(HomekitAccessoryBridge.this::handleAccessoryRemoved);
                            }
                        }));
    }

    /**
     * Bridges an accessory between a remote and local server.
     *
     * This method sets up bidirectional event forwarding between remote and local servers for a given accessory.
     * It creates event subscriptions for characteristic value changes, service modifications, and accessory state changes.
     *
     * Key implementation details:
     * - Adds local accessory to local server
     * - Sets up event forwarding from remote to local
     * - Sets up command forwarding from local to remote
     * - Stores bridge context for future reference
     * - Handles cleanup on failure
     *
     * @param remoteAccessory The {@link HomekitAccessory} to bridge
     * @param remoteServer The {@link HomekitAccessoryServer} the accessory belongs to
     * @param localServer The {@link HomekitAccessoryServer} to expose the accessory on
     * @param localAccessory The {@link HomekitAccessory} to be added to the local server
     * @throws HomekitAccessoryOperationException if there is an error adding or removing the accessory
     * @since 1.0.0
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
                    (UID) remoteAccessory.getUID(), (UID) bridgeUID, event -> {
                        logger.debug("{}Forwarding event from remote to local: {}", LOG_PREFIX, event);
                        // The local server will handle the event through its event manager
                        eventManager.publishEvent(event);
                    });

            // Set up command forwarding from local to remote
            List<HomekitEventSubscription> localSubs = eventManager.subscribe(
                    Set.of(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED), (UID) localAccessory.getUID(),
                    (UID) bridgeUID, event -> { // Use local accessory UID for local events
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
     *
     * This method handles the cleanup of all resources associated with a bridged accessory,
     * including event subscriptions and local accessory removal.
     *
     * Key implementation details:
     * - Removes bridge context from storage
     * - Unsubscribes from all event subscriptions
     * - Removes local accessory from local server
     * - Handles cleanup errors gracefully
     *
     * @param accessory The {@link HomekitAccessory} to unbridge
     * @since 1.0.0
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
     * This method verifies whether a given accessory is currently being bridged
     * by checking its presence in the bridge context storage.
     *
     * @param accessory The {@link HomekitAccessory} to check
     * @return true if the accessory is bridged, false otherwise
     * @since 1.0.0
     */
    public boolean isBridged(HomekitAccessory accessory) {
        return bridgedAccessories.containsKey(accessory);
    }

    /**
     * Gets the remote server for a bridged accessory.
     *
     * This method retrieves the remote server associated with a bridged accessory
     * from the bridge context storage.
     *
     * @param accessory The {@link HomekitAccessory} to check
     * @return Optional containing the {@link HomekitAccessoryServer} if the accessory is bridged, empty otherwise
     * @since 1.0.0
     */
    public Optional<HomekitAccessoryServer> getRemoteServer(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? Optional.of(ctx.getRemoteServer()) : Optional.empty();
    }

    /**
     * Gets the local server for a bridged accessory.
     *
     * This method retrieves the local server associated with a bridged accessory
     * from the bridge context storage.
     *
     * @param accessory The {@link HomekitAccessory} to check
     * @return Optional containing the {@link HomekitAccessoryServer} if the accessory is bridged, empty otherwise
     * @since 1.0.0
     */
    public Optional<HomekitAccessoryServer> getLocalServer(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? Optional.of(ctx.getLocalServer()) : Optional.empty();
    }

    /**
     * Gets the local accessory copy for a remote accessory.
     *
     * This method retrieves the local copy of a remote accessory from the bridge context storage.
     *
     * @param remoteAccessory The {@link HomekitAccessory} to get the local copy for
     * @return The local {@link HomekitAccessory} copy
     * @throws IllegalStateException if the accessory is not bridged
     * @since 1.0.0
     */
    public HomekitAccessory getLocalAccessory(HomekitAccessory remoteAccessory) {
        BridgeContext ctx = bridgedAccessories.get(remoteAccessory);
        return ctx != null ? ctx.getLocalAccessory() : null;
    }

    /**
     * Handles the addition of a new accessory.
     *
     * This method processes accessory addition events, creating local copies and
     * setting up bridging when appropriate.
     *
     * Key implementation details:
     * - Checks if the server is remote
     * - Creates local accessory copy
     * - Sets up bridging between servers
     * - Handles orphaned accessory restoration
     *
     * @param accessory The {@link HomekitAccessory} that was added
     * @param remoteServer The {@link HomekitAccessoryServer} the accessory was added to
     * @since 1.0.0
     */
    public void handleAccessoryAdded(HomekitAccessory accessory, HomekitAccessoryServer remoteServer) {
        if (orphanEnabled) {
            // Check if this is a restoration of an orphaned accessory
            BridgeContext existingContext = bridgedAccessories.get(accessory);
            if (existingContext != null && existingContext.localAccessory.isOrphaned()) {
                if (restoreOrphanedAccessory(accessory, existingContext)) {
                    logger.info("{}Successfully restored orphaned accessory {}", LOG_PREFIX, accessory.getUID());
                    return;
                }
            }
        }
        // Continue with normal bridging process
        try {
            // Fetch config for the accessory
            Optional<Map<String, Object>> configOpt = configManager.getConfiguration((UID) accessory.getUID(),
                    HomekitConfigurationManager.ConfigurationType.ACCESSORY);
            if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().get("bridge"))) {
                logger.debug("{}Accessory {} not configured for bridging (missing or false 'bridge' parameter)",
                        LOG_PREFIX, accessory.getUID());
                return;
            }

            // Only bridge if accessory belongs to a remote accessory server (not a local server)
            if (remoteServer == null || isLocalServer(remoteServer)) {
                logger.debug("{}Accessory {} is not from a remote server, skipping bridging", LOG_PREFIX,
                        accessory.getUID());
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
     * Handles the removal of an accessory.
     *
     * This method processes accessory removal events, cleaning up bridging resources
     * and handling any necessary state updates.
     *
     * Key implementation details:
     * - Removes bridge context
     * - Cleans up event subscriptions
     * - Removes local accessory copy
     * - Handles cleanup errors
     *
     * @param accessory The {@link HomekitAccessory} that was removed
     * @since 1.0.0
     */
    public void handleAccessoryRemoved(HomekitAccessory accessory) {
        BridgeContext context = bridgedAccessories.get(accessory);
        if (context != null) {
            if (orphanEnabled) {
                // Mark the accessory as orphaned but keep it in the registry
                logger.info("{}Accessory {} was removed but keeping it to prevent controller deletion", LOG_PREFIX,
                        accessory.getUID());
                accessory.setOrphaned(true);

                // Update configuration to reflect orphaned state
                try {
                    Map<String, Object> config = new HashMap<>();
                    config.put("orphaned", true);
                    configManager.updateConfiguration((UID) accessory.getUID(),
                            HomekitConfigurationManager.ConfigurationType.ACCESSORY, config);
                } catch (Exception e) {
                    logger.error("{}Failed to update configuration for orphaned accessory {}: {}", LOG_PREFIX,
                            accessory.getUID(), e.getMessage(), e);
                }
            } else {
                // Completely remove the accessory and its characteristics
                try {
                    context.localServer.removeAccessory(context.localAccessory);
                    bridgedAccessories.remove(accessory);
                    logger.info("{}Successfully removed accessory {}", LOG_PREFIX, accessory.getUID());
                } catch (Exception e) {
                    logger.error("{}Failed to remove accessory {}: {}", LOG_PREFIX, accessory.getUID(), e.getMessage(),
                            e);
                }
            }
        }
    }

    /**
     * Checks if an accessory is orphaned.
     *
     * This method determines if a bridged accessory has become orphaned by checking
     * if its remote server is no longer available.
     *
     * @param accessory The {@link HomekitAccessory} to check
     * @return true if the accessory is orphaned, false otherwise
     * @since 1.0.0
     */
    public boolean isOrphaned(HomekitAccessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null && ctx.getLocalAccessory().isOrphaned();
    }

    /**
     * Restores an orphaned accessory if its remote counterpart becomes available again.
     *
     * @param remoteAccessory The remote accessory that was added
     * @param context The bridge context containing the orphaned accessory
     * @return true if the accessory was successfully restored, false otherwise
     */
    private boolean restoreOrphanedAccessory(HomekitAccessory remoteAccessory, BridgeContext context) {
        try {
            // Remove orphaned flag
            context.localAccessory.setOrphaned(false);

            // Update configuration to reflect restored state
            Map<String, Object> config = new HashMap<>();
            config.put("orphaned", false);
            configManager.updateConfiguration((UID) remoteAccessory.getUID(),
                    HomekitConfigurationManager.ConfigurationType.ACCESSORY, config);

            logger.info("{}Successfully restored orphaned accessory {}", LOG_PREFIX, remoteAccessory.getUID());
            return true;
        } catch (Exception e) {
            logger.error("{}Failed to restore orphaned accessory {}: {}", LOG_PREFIX, remoteAccessory.getUID(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determines if the given server is a local server.
     * This can be customized as needed to distinguish local from remote servers.
     *
     * @param server The server to check
     * @return true if the server is local, false if it's remote
     */
    private boolean isLocalServer(HomekitAccessoryServer server) {
        return !(server instanceof HomekitRemoteAccessoryServer);
    }

    /**
     * Gets an available local server for bridging.
     *
     * @return Optional containing an available local server, or empty if none found
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
    private HomekitAccessory createLocalAccessory(HomekitAccessory remoteAccessory,
            HomekitAccessoryServer localServer) {
        try {
            return accessoryFactory.createAccessoryWithArgs("bridged", remoteAccessory, localServer);
        } catch (Exception e) {
            logger.error("{}Failed to create local accessory: {}", LOG_PREFIX, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Internal class representing the context of a bridged accessory.
     *
     * This class maintains the state and resources associated with a bridged accessory,
     * including event subscriptions and server references.
     *
     * @since 1.0.0
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

    /**
     * Gets all bridged accessories.
     *
     * This method returns a set of all accessories currently being bridged.
     *
     * @return Set of {@link HomekitAccessory} instances
     * @since 1.0.0
     */
    public Set<HomekitAccessory> getAccessories() {
        return bridgedAccessories.keySet();
    }
}
