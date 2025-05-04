package org.openhab.io.homekit.internal.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventSubscription;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the bridging of accessories between remote and local accessory servers.
 * This class handles the setup and teardown of event subscriptions to forward
 * events and commands between the remote and local servers.
 */
@NonNullByDefault
public class HomekitAccessoryBridge {
    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryBridge.class);
    private static final String LOG_PREFIX = "HomeKit Bridge: ";
    private final String subscriberUID = "bridge:" + UUID.randomUUID().toString();

    private final HomekitEventManager eventManager;
    private final Map<Accessory, BridgeContext> bridgedAccessories = new HashMap<>();

    /**
     * Creates a new AccessoryBridgeManager.
     *
     * @param eventManager The event manager to use for event handling
     */
    public HomekitAccessoryBridge(HomekitEventManager eventManager) {
        this.eventManager = eventManager;
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
    public void bridgeAccessory(Accessory remoteAccessory, AccessoryServer remoteServer, AccessoryServer localServer, Accessory localAccessory) throws HomekitAccessoryOperationException {
        logger.debug("{}Bridging accessory {} from remote server {} to local server {}", LOG_PREFIX,
                remoteAccessory.getUID(), remoteServer.getUID(), localServer.getUID());

        try {
            // Add the local accessory to the local server
            localServer.addAccessory(localAccessory);
            logger.debug("{}Added local accessory {} to local server {}", LOG_PREFIX,
                    localAccessory.getUID(), localServer.getUID());

            // Set up event forwarding from remote to local
            List<HomekitEventSubscription> remoteSubs = eventManager.subscribe(
                    Set.of(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED, HomekitEventType.SERVICE_ADDED,
                            HomekitEventType.SERVICE_REMOVED, HomekitEventType.ACCESSORY_STATE_CHANGED),
                    remoteAccessory.getUID().toString(), subscriberUID, event -> {
                        logger.debug("{}Forwarding event from remote to local: {}", LOG_PREFIX, event);
                        // The local server will handle the event through its event manager
                        eventManager.publishEvent(event);
                    });

            // Set up command forwarding from local to remote
            List<HomekitEventSubscription> localSubs = eventManager.subscribe(
                    Set.of(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED),
                    localAccessory.getUID().toString(), subscriberUID, event -> { // Use local accessory UID for local events
                        logger.debug("{}Forwarding command from local to remote: {}", LOG_PREFIX, event);
                            logger.debug("{}Forwarding command from local to remote: {}", LOG_PREFIX, event);
                            // The remote server will handle the event through its event manager
                            eventManager.publishEvent(event);
                    });

            bridgedAccessories.put(remoteAccessory, new BridgeContext(remoteSubs, localSubs, remoteServer, localServer, localAccessory));
        } catch (Exception e) {
            logger.error("{}Failed to bridge accessory {}: {}", LOG_PREFIX, remoteAccessory.getUID(), e.getMessage(), e);
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
    public void unbridgeAccessory(Accessory accessory) {
        BridgeContext ctx = bridgedAccessories.remove(accessory);
        if (ctx != null) {
            logger.debug("{}Unbridging accessory {} from remote server {} and local server {}", LOG_PREFIX,
                    accessory.getUID(), ctx.remoteServer.getUID(), ctx.localServer.getUID());
            ctx.remoteSubs.forEach(eventManager::unsubscribe);
            ctx.localSubs.forEach(eventManager::unsubscribe);
            try {
                ctx.localServer.removeAccessory(ctx.localAccessory);
                logger.debug("{}Removed local accessory {} from local server {}", LOG_PREFIX,
                        ctx.localAccessory.getUID(), ctx.localServer.getUID());
            } catch (Exception e) {
                logger.error("{}Failed to remove local accessory {}: {}", LOG_PREFIX, ctx.localAccessory.getUID(),
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
    public boolean isBridged(Accessory accessory) {
        return bridgedAccessories.containsKey(accessory);
    }

    /**
     * Gets the remote server for a bridged accessory.
     *
     * @param accessory The bridged accessory
     * @return The remote server, or null if the accessory is not bridged
     */
    public AccessoryServer getRemoteServer(Accessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? ctx.remoteServer : null;
    }

    /**
     * Gets the local server for a bridged accessory.
     *
     * @param accessory The bridged accessory
     * @return The local server, or null if the accessory is not bridged
     */
    public AccessoryServer getLocalServer(Accessory accessory) {
        BridgeContext ctx = bridgedAccessories.get(accessory);
        return ctx != null ? ctx.localServer : null;
    }

    /**
     * Gets the local accessory for a bridged remote accessory.
     *
     * @param remoteAccessory The remote accessory
     * @return The local accessory, or null if the remote accessory is not bridged
     */
    public Accessory getLocalAccessory(Accessory remoteAccessory) {
        BridgeContext ctx = bridgedAccessories.get(remoteAccessory);
        return ctx != null ? ctx.localAccessory : null;
    }

    /**
     * Context class to hold information about a bridged accessory.
     */
    private static class BridgeContext {
        final List<HomekitEventSubscription> remoteSubs;
        final List<HomekitEventSubscription> localSubs;
        final AccessoryServer remoteServer;
        final AccessoryServer localServer;
        final Accessory localAccessory;

        BridgeContext(List<HomekitEventSubscription> remoteSubs, List<HomekitEventSubscription> localSubs,
                AccessoryServer remoteServer, AccessoryServer localServer, Accessory localAccessory) {
            this.remoteSubs = remoteSubs;
            this.localSubs = localSubs;
            this.remoteServer = remoteServer;
            this.localServer = localServer;
            this.localAccessory = localAccessory;
        }
    }
} 