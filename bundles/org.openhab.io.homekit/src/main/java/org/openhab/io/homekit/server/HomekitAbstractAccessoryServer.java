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

package org.openhab.io.homekit.server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.listener.HomekitAccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerState;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerUIDImpl;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitInvalidStateTransitionException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.network.pairing.HomekitPairingImpl;
import org.openhab.io.homekit.network.pairing.HomekitPairingUIDImpl;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for HomeKit accessory servers that provides core functionality for managing
 * HomeKit accessories, pairings, and server lifecycle.
 *
 * <p>
 * This class implements the core server functionality for HomeKit accessories, providing
 * the bridge between OpenHAB and HomeKit clients. It manages the lifecycle and communication
 * for a single HomeKit accessory server instance.
 *
 * <p>
 * The server operates as a central hub for {@link HomekitAccessory HomeKit accessories}. When initialized, it sets up
 * network listeners and security settings to accept incoming HomeKit client connections. As
 * accessories are registered through the {@link #addAccessory(HomekitAccessory) addAccessory} method, the server
 * creates the
 * necessary handlers and prepares them for client access.
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitAccessory} for accessory lifecycle and state management</li>
 * <li>{@link HomekitCharacteristic} for value conversion and validation</li>
 * <li>{@link org.openhab.core.items.Item OpenHAB's item system} for state synchronization</li>
 * <li>{@link HomekitAccessoryRegistry} for accessory registration and discovery</li>
 * <li>{@link HomekitPairingRegistry} for secure pairing management</li>
 * <li>{@link HomekitEventManager} for event handling and notifications</li>
 * </ul>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>State management with valid state transitions</li>
 * <li>Accessory lifecycle management</li>
 * <li>Secure pairing and authentication</li>
 * <li>Event handling and notifications</li>
 * <li>Resource cleanup and shutdown</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 */
@NonNullByDefault
public abstract class HomekitAbstractAccessoryServer implements HomekitAccessoryServer, AutoCloseable {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitAbstractAccessoryServer.class);
    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Server";
    protected static final String LOG_INIT = "Init";
    protected static final String LOG_STATE = "State";
    protected static final String LOG_CONFIG = "Config";
    protected static final String LOG_ACCESSORY = "Accessory";
    protected static final String LOG_PAIRING = "Pairing";
    protected static final String LOG_EVENT = "Event";
    protected static final String LOG_ERROR = "Error";
    protected static final String LOG_SERVER = "Server";
    protected static final String LOG_WARN = "Warning";

    // ========== State Management ==========
    protected final Object stateLock = new Object();
    private final Object accessoryLock = new Object();
    protected volatile HomekitAccessoryServerState currentState = HomekitAccessoryServerState.UNKNOWN;
    private volatile int configurationIndex = 1;
    private volatile boolean isShutdown = false;

    private static final Map<HomekitAccessoryServerState, Set<HomekitAccessoryServerState>> VALID_STATE_TRANSITIONS = Map
            .of(HomekitAccessoryServerState.UNKNOWN, Set.of(HomekitAccessoryServerState.READY),
                    HomekitAccessoryServerState.READY,
                    Set.of(HomekitAccessoryServerState.STOPPED, HomekitAccessoryServerState.CONNECTED),
                    HomekitAccessoryServerState.CONNECTED,
                    Set.of(HomekitAccessoryServerState.DISCONNECTED, HomekitAccessoryServerState.PAIR_SETUP_INITIAL),
                    HomekitAccessoryServerState.DISCONNECTED,
                    Set.of(HomekitAccessoryServerState.CONNECTED, HomekitAccessoryServerState.STOPPED,
                            HomekitAccessoryServerState.PAIR_SETUP_INITIAL,
                            HomekitAccessoryServerState.MISSING_SETUP_CODE),
                    HomekitAccessoryServerState.PAIR_SETUP_INITIAL,
                    Set.of(HomekitAccessoryServerState.PAIRED, HomekitAccessoryServerState.DISCONNECTED,
                            HomekitAccessoryServerState.UNPAIRED, HomekitAccessoryServerState.MISSING_SETUP_CODE),
                    HomekitAccessoryServerState.PAIRED,
                    Set.of(HomekitAccessoryServerState.PAIR_UNVERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.PAIR_UNVERIFIED,
                    Set.of(HomekitAccessoryServerState.PAIR_VERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.PAIR_VERIFIED,
                    Set.of(HomekitAccessoryServerState.PAIR_UNVERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.STOPPED, Set.of(HomekitAccessoryServerState.READY));

    // ========== Server Configuration ==========
    private final HomekitAccessoryCategory category;
    private final String serverId;
    protected final InetAddress address;
    protected final int port;
    private final byte[] pairingIdentifier;
    protected final byte[] secretKey;
    protected volatile String setupCode;

    // ========== Collections and Executors ==========
    private final Collection<HomekitAccessory> accessories = new CopyOnWriteArraySet<>();

    // ========== Dependencies ==========
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitPairingRegistry pairingRegistry;
    protected final HomekitEventManager eventManager;

    // ========== Constructor ==========
    /**
     * Creates a new HomeKit accessory server with the specified configuration.
     *
     * <p>
     * This constructor initializes the server with the required configuration parameters
     * and validates them before proceeding with initialization.
     *
     * <p>
     * Key implementation details:
     * <ul>
     * <li>Validates all constructor parameters</li>
     * <li>Initializes server state and configuration</li>
     * <li>Sets up logging and event management</li>
     * <li>Prepares for accessory registration</li>
     * </ul>
     *
     * @param category The category of accessories this server will host
     * @param serverId The unique identifier for this server
     * @param address The network address to bind to
     * @param port The port to listen on
     * @param pairingId The unique identifier for this server
     * @param privateKey The private key for secure communication
     * @param accessoryRegistry The registry for managing accessories
     * @param pairingRegistry The registry for managing pairings
     * @param eventManager The manager for handling events
     * @throws HomekitConfigurationException if any configuration parameter is invalid
     */
    public HomekitAbstractAccessoryServer(HomekitAccessoryCategory category, String serverId, InetAddress address,
            int port, byte[] pairingId, byte[] privateKey, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry, HomekitEventManager eventManager)
            throws HomekitConfigurationException {
        super();
        validateConstructorParameters(category, serverId, address, port, pairingId, privateKey, accessoryRegistry,
                pairingRegistry);

        this.category = category;
        this.serverId = serverId;
        this.address = address;
        this.port = port;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.secretKey = privateKey;
        this.pairingIdentifier = pairingId;
        this.setupCode = "";
        this.eventManager = eventManager;

        logger.debug("{} [{}] : {} - Initializing Homekit server - Category: {}, ServerId: {}, Address: {}, Port: {}",
                LOG_PREFIX, getUID(), LOG_INIT, category, serverId, address, port);
    }

    /**
     * Validates the constructor parameters to ensure they meet the requirements.
     *
     * <p>
     * This method checks:
     * <ul>
     * <li>Server ID is not null or empty</li>
     * <li>Port number is within valid range (1-65535)</li>
     * <li>Pairing ID is not empty</li>
     * <li>Private key is not empty</li>
     * </ul>
     *
     * @param category The accessory category
     * @param serverId The server identifier
     * @param address The network address
     * @param port The port number
     * @param pairingId The pairing identifier
     * @param privateKey The private key
     * @param accessoryRegistry The accessory registry
     * @param pairingRegistry The pairing registry
     * @throws HomekitConfigurationException if any parameter is invalid
     */
    private void validateConstructorParameters(HomekitAccessoryCategory category, String serverId, InetAddress address,
            int port, byte[] pairingId, byte[] privateKey, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry) throws HomekitConfigurationException {
        if (serverId == null || serverId.trim().isEmpty()) {
            throw new HomekitConfigurationException(
                    "Homekit server ID cannot be null or empty - required for unique identification");
        }
        if (port <= 0 || port > 65535) {
            throw new HomekitConfigurationException(
                    String.format("Homekit server port %d is invalid - must be between 1 and 65535", port));
        }
        if (pairingId.length == 0) {
            throw new HomekitConfigurationException(
                    "Homekit server pairing ID cannot be empty - required for secure pairing");
        }
        if (privateKey.length == 0) {
            throw new HomekitConfigurationException(
                    "Homekit server private key cannot be empty - required for secure pairing");
        }
    }

    // ========== Lifecycle Methods ==========
    /**
     * Initializes any resources needed by the server.
     *
     * <p>
     * This method should be overridden by subclasses to initialize their specific resources.
     * The base implementation does nothing.
     *
     * @throws HomekitServerException if initialization fails
     */
    protected void initializeResources() throws HomekitServerException {
        // Base implementation does nothing
    }

    /**
     * Cleans up any resources held by the server.
     *
     * <p>
     * This method should be overridden by subclasses to cleanup their specific resources.
     * The base implementation does nothing.
     *
     * @throws HomekitServerException if cleanup fails
     */
    protected void cleanupResources() throws HomekitServerException {
        // Base implementation does nothing
    }

    /**
     * Removes all registered accessories during server shutdown.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Creates a copy of the accessories list to avoid concurrent modification</li>
     * <li>Attempts to remove each accessory</li>
     * <li>Logs any errors that occur during removal</li>
     * </ul>
     */
    private void cleanupAccessories() {
        List<HomekitAccessory> accessoriesToRemove;
        synchronized (accessoryLock) {
            accessoriesToRemove = new ArrayList<>(accessories);
        }

        for (HomekitAccessory accessory : accessoriesToRemove) {
            try {
                removeAccessory(accessory);
            } catch (HomekitAccessoryOperationException e) {
                logger.error("{} [{}] : {} - Failed to remove accessory during shutdown: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, e.getMessage(), e);
            }
        }
    }

    /**
     * Starts the HomeKit accessory server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the current server state</li>
     * <li>Initializes required resources</li>
     * <li>Transitions to the READY state</li>
     * <li>Handles any initialization errors</li>
     * </ul>
     *
     * @throws HomekitServerException if the server cannot be started
     */
    @Override
    public void start() throws HomekitServerException {
        synchronized (stateLock) {
            validateLifecycleOperation("start server");

            if (currentState == HomekitAccessoryServerState.READY) {
                logger.warn("{} [{}] : {} - Server is already running", LOG_PREFIX, getUID(), LOG_SERVER);
                return;
            }
            if (currentState == HomekitAccessoryServerState.STOPPED) {
                logger.warn("{} [{}] : {} - Server is in stopped state, attempting to restart", LOG_PREFIX, getUID(),
                        LOG_SERVER);
            }

            try {
                initializeResources();
                setState(HomekitAccessoryServerState.READY);
                logger.info("{} [{}] : {} - Server started successfully", LOG_PREFIX, getUID(), LOG_SERVER);
            } catch (Exception e) {
                try {
                    cleanupResources();
                } catch (Exception cleanupEx) {
                    logger.error("{} [{}] : {} - Failed to cleanup resources after failed start: {}", LOG_PREFIX,
                            getUID(), LOG_ERROR, cleanupEx.getMessage(), cleanupEx);
                }
                throw new HomekitServerException("Failed to start server", e);
            }
        }
    }

    /**
     * Stops the HomeKit accessory server gracefully.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the current server state</li>
     * <li>Transitions to the STOPPED state</li>
     * <li>Cleans up resources</li>
     * <li>Handles any shutdown errors</li>
     * </ul>
     *
     * @throws HomekitServerException if the server cannot be stopped gracefully
     */
    @Override
    public void stop() throws HomekitServerException {
        synchronized (stateLock) {
            if (currentState == HomekitAccessoryServerState.STOPPED) {
                logger.warn("{} [{}] : {} - Server is already stopped", LOG_PREFIX, getUID(), LOG_SERVER);
                return;
            }
            try {
                try {
                    setState(HomekitAccessoryServerState.STOPPED);
                } catch (HomekitServerException e) {
                    logger.error("{} [{}] : {} - Failed to set stopped state during shutdown: {}", LOG_PREFIX, getUID(),
                            LOG_ERROR, e.getMessage(), e);
                    throw new HomekitServerException("Failed to set stopped state during shutdown", e);
                }
            } catch (HomekitServerException e) {
                logger.error("{} [{}] : {} - Failed to stop server gracefully: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                        e.getMessage(), e);
                forceStop();
            }
        }
    }

    /**
     * Forces the server to stop immediately, bypassing normal shutdown procedures.
     *
     * <p>
     * This method is called when normal shutdown fails and attempts to:
     * <ul>
     * <li>Clean up all resources</li>
     * <li>Set the server state to STOPPED</li>
     * <li>Log any errors that occur</li>
     * </ul>
     */
    private void forceStop() {
        try {
            cleanupResources();
            setState(HomekitAccessoryServerState.STOPPED);
            logger.warn("{} [{}] : {} - Server force stopped", LOG_PREFIX, getUID(), LOG_WARN);
        } catch (Exception e) {
            logger.error("{} [{}] : {} - Failed to force stop server: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage(), e);
        }
    }

    /**
     * Validates that a lifecycle operation can be performed in the current server state.
     *
     * <p>
     * This method checks:
     * <ul>
     * <li>The server is not in a shutdown state</li>
     * <li>The operation is valid for the current state</li>
     * </ul>
     *
     * @param operation A description of the operation being performed
     * @throws HomekitServerException if the operation is not valid
     */
    protected void validateLifecycleOperation(String operation) throws HomekitServerException {
        if (isShutdown) {
            throw new HomekitServerException(String.format("Cannot %s - server is shut down", operation));
        }
    }

    // ========== State Management Methods ==========
    /**
     * Gets the current state of the server.
     *
     * @return The current server state
     */
    public synchronized HomekitAccessoryServerState getCurrentState() {
        return currentState;
    }

    /**
     * Validates that a state transition is allowed.
     *
     * <p>
     * This method checks if the transition from the current state to the new state
     * is valid according to the state transition rules.
     *
     * @param newState The desired new state
     * @throws HomekitInvalidStateTransitionException if the transition is not allowed
     */
    private void validateStateTransition(HomekitAccessoryServerState newState)
            throws HomekitInvalidStateTransitionException {
        if (!isValidStateTransition(currentState, newState)) {
            throw new HomekitInvalidStateTransitionException(currentState, newState);
        }
    }

    /**
     * Checks if a state transition is valid.
     *
     * @param current The current state
     * @param next The next state
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidStateTransition(HomekitAccessoryServerState current, HomekitAccessoryServerState next) {
        // Allow same-state transitions (no-op)
        if (current == next) {
            return true;
        }

        @SuppressWarnings("null") // Map.get() return type interpretation
        Set<HomekitAccessoryServerState> validNextStates = VALID_STATE_TRANSITIONS.get(current);
        return validNextStates != null && validNextStates.contains(next);
    }

    /**
     * Sets the server state and notifies listeners of the change.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the state transition</li>
     * <li>Updates the current state</li>
     * <li>Notifies state change listeners</li>
     * <li>Logs the state change</li>
     * </ul>
     *
     * @param newState The new state to set
     * @throws HomekitServerException if the state transition is invalid
     */
    protected synchronized void setState(HomekitAccessoryServerState newState) throws HomekitServerException {
        try {
            validateStateTransition(newState);
            HomekitAccessoryServerState oldState = currentState;
            currentState = newState;
            logger.debug("{} [{}] : {} - Server state changed from {} to {}", LOG_PREFIX, getUID(), LOG_STATE, oldState,
                    newState);
            eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.SERVER_STATE_CHANGED, this,
                    (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
        } catch (HomekitInvalidStateTransitionException e) {
            logger.error("{} [{}] : {} - Invalid state transition: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            throw new HomekitServerException("Invalid state transition", e);
        }
    }

    // ========== Configuration Management Methods ==========
    /**
     * Gets the current configuration index.
     *
     * <p>
     * The configuration index is used to track changes to the server's configuration
     * and notify clients when updates are needed.
     *
     * @return The current configuration index
     */
    @Override
    public int getConfigurationIndex() {
        return configurationIndex;
    }

    /**
     * Sets a new configuration index.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the new index</li>
     * <li>Updates the configuration index</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param newIndex The new configuration index
     * @throws HomekitConfigurationException if the new index is invalid
     */
    @Override
    public void setConfigurationIndex(int newIndex) throws HomekitConfigurationException {
        validateConfigurationIndex(newIndex);
        configurationIndex = newIndex;
        logger.debug("{} [{}] : {} - Configuration index updated to {}", LOG_PREFIX, getUID(), LOG_CONFIG, newIndex);
        eventManager.publishEvent(
                new HomekitAccessoryServerEvent(HomekitEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED, this,
                        (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
    }

    /**
     * Validates a new configuration index.
     *
     * <p>
     * This method ensures that:
     * <ul>
     * <li>The new index is greater than the current index</li>
     * <li>The new index is not unreasonably large</li>
     * </ul>
     *
     * @param newIndex The new configuration index to validate
     * @throws HomekitConfigurationException if the new index is invalid
     */
    private void validateConfigurationIndex(int newIndex) throws HomekitConfigurationException {
        if (newIndex <= configurationIndex) {
            throw new HomekitConfigurationException(String.format(
                    "New configuration index %d must be greater than current index %d", newIndex, configurationIndex));
        }
        if (newIndex > configurationIndex + 1000) {
            throw new HomekitConfigurationException(
                    String.format("New configuration index %d is unreasonably large", newIndex));
        }
    }

    /**
     * Increments the configuration index.
     *
     * <p>
     * This method is called when the server's configuration changes and
     * clients need to be notified of the update.
     */
    protected synchronized void incrementConfigurationIndex() {
        configurationIndex++;
        logger.debug("{} [{}] : {} - Configuration index incremented to {}", LOG_PREFIX, getUID(), LOG_CONFIG,
                configurationIndex);
        eventManager.publishEvent(
                new HomekitAccessoryServerEvent(HomekitEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED, this,
                        (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
    }

    /**
     * Performs a factory reset of the server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Removes all pairings</li>
     * <li>Resets the configuration index</li>
     * <li>Clears the setup code</li>
     * <li>Notifies clients of the reset</li>
     * </ul>
     */
    @Override
    public void factoryReset() {
        logger.info("{} [{}] : {} - Performing factory reset", LOG_PREFIX, getUID(), LOG_CONFIG);
        try {
            Collection<HomekitPairing> pairings = pairingRegistry.get(getPairingId());
            for (HomekitPairing pairing : pairings) {
                pairingRegistry.remove(pairing.getUID());
            }
            configurationIndex = 1;
            setupCode = "";
            eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.SERVER_STATE_UNPAIRED, this,
                    (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
            logger.info("{} [{}] : {} - Factory reset completed successfully", LOG_PREFIX, getUID(), LOG_CONFIG);
        } catch (Exception e) {
            logger.error("{} [{}] : {} - Failed to perform factory reset: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage(), e);
        }
    }

    // ========== Server Identity and Network Methods ==========
    /**
     * Gets the network address of the server.
     *
     * @return The server's network address
     */
    @Override
    public InetAddress getAddress() {
        logger.trace("{} [{}] : {} - Getting server address: {}", LOG_PREFIX, getUID(), LOG_CONFIG, address);
        return address;
    }

    /**
     * Gets the port number the server is listening on.
     *
     * @return The server's port number
     */
    @Override
    public int getPort() {
        logger.trace("{} [{}] : {} - Getting server port: {}", LOG_PREFIX, getUID(), LOG_CONFIG, port);
        return port;
    }

    /**
     * Gets the pairing identifier for this server.
     *
     * @return The server's pairing identifier
     */
    @Override
    public byte[] getPairingId() {
        logger.trace("{} [{}] : {} - Getting pairing ID", LOG_PREFIX, getUID(), LOG_CONFIG);
        return pairingIdentifier;
    }

    /**
     * Gets the unique identifier for this server.
     *
     * @return The server's unique identifier
     */
    @Override
    public HomekitAccessoryServerUID getUID() {
        logger.trace("{} [{}] : {} - Getting server UID", LOG_PREFIX, getServerId(), LOG_CONFIG);
        return new HomekitAccessoryServerUIDImpl(getServerId());
    }

    /**
     * Gets the server's secret key.
     *
     * @return The server's secret key
     */
    @Override
    public byte[] getSecretKey() {
        logger.trace("{} [{}] : {} - Getting secret key", LOG_PREFIX, getUID(), LOG_CONFIG);
        return secretKey;
    }

    /**
     * Gets the public key for a specific pairing.
     *
     * @param destinationPairingId The pairing identifier to get the public key for
     * @return The public key for the specified pairing
     */
    @Override
    public Optional<byte[]> getPublicKey(byte[] destinationPairingId) {
        logger.debug("{} [{}] : {} - Getting public key for destination pairing ID: {}", LOG_PREFIX, getUID(),
                LOG_CONFIG, HomekitByte.toHexString(destinationPairingId));
        HomekitPairing hp = pairingRegistry.get(new HomekitPairingUIDImpl(getPairingId(), destinationPairingId));
        return hp != null ? Optional.of(hp.getPublicKey()) : Optional.empty();
    }

    // ========== Setup Code Management Methods ==========
    /**
     * Gets the setup code for this server.
     *
     * @return The server's setup code
     */
    @Override
    public String getSetupCode() {
        logger.trace("{} [{}] : {} - Getting setup code", LOG_PREFIX, getUID(), LOG_CONFIG);
        return setupCode;
    }

    /**
     * Sets the setup code for this server.
     *
     * @param setupCode The new setup code
     */
    @Override
    public void setSetupCode(String setupCode) {
        if (setupCode.length() != 8 || !setupCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("Setup code must be 8 digits");
        }
        this.setupCode = setupCode;
    }

    // ========== HomekitPairing Management Methods ==========
    /**
     * Adds a new pairing to the server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the pairing parameters</li>
     * <li>Creates a new pairing</li>
     * <li>Registers it with the pairing registry</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param pairingId The identifier for the new pairing
     * @param publicKey The public key for the new pairing
     * @throws HomekitServerException if the pairing cannot be added
     */
    @Override
    public void addPairing(byte[] pairingId, byte[] publicKey) throws HomekitServerException {
        if (pairingId.length == 0) {
            throw new HomekitServerException("HomekitPairing ID cannot be empty");
        }
        if (publicKey.length == 0) {
            throw new HomekitServerException("Public key cannot be empty");
        }

        // Validate pairing ID uniqueness
        if (pairingRegistry.get(new HomekitPairingUIDImpl(getPairingId(), pairingId)) != null) {
            String error = String.format("HomekitPairing ID %s already exists", HomekitByte.toHexString(pairingId));
            logger.error("{} [{}] : {} - HomekitPairing validation error: {}", LOG_PREFIX, getUID(), LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{} [{}] : {} - Adding pairing - ID: {}", LOG_PREFIX, getUID(), LOG_PAIRING,
                HomekitByte.toHexString(pairingId));

        try {
            HomekitPairing newPairing = new HomekitPairingImpl(getPairingId(), pairingId, publicKey);
            HomekitPairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("{} [{}] : {} - Removed existing pairing - Destination: {}, Public Key: {}", LOG_PREFIX,
                        getUID(), LOG_PAIRING, HomekitByte.toHexString(oldPairing.getDestinationId()),
                        HomekitByte.toHexString(oldPairing.getPublicKey()));
                setState(HomekitAccessoryServerState.DISCONNECTED);
            }

            pairingRegistry.add(newPairing);
            logger.debug("{} [{}] : {} - HomekitPairing added successfully", LOG_PREFIX, getUID(), LOG_PAIRING);
            setState(HomekitAccessoryServerState.PAIRED);
            logger.info("{} [{}] : {} - HomekitPairing added successfully - ID: {}", LOG_PREFIX, getUID(), LOG_PAIRING,
                    HomekitByte.toHexString(pairingId));
        } catch (HomekitServerException e) {
            String error = String.format("Failed to add pairing %s: %s", HomekitByte.toHexString(pairingId),
                    e.getMessage());
            logger.error("{} [{}] : {} - HomekitPairing addition error: {}", LOG_PREFIX, getUID(), LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    /**
     * Gets a pairing by its identifier.
     *
     * @param pairingId The identifier of the pairing to get
     * @return The pairing, or null if not found
     */
    @Override
    public Optional<HomekitPairing> getPairing(byte[] pairingId) {
        if (pairingId.length == 0) {
            throw new IllegalArgumentException("HomekitPairing ID cannot be empty");
        }
        Collection<HomekitPairing> pairings = pairingRegistry.get(pairingId);
        if (pairings.isEmpty()) {
            return Optional.empty();
        } else {
            @SuppressWarnings("null") // iterator().next() is safe after isEmpty() check
            HomekitPairing firstPairing = pairings.iterator().next();
            return Optional.of(firstPairing);
        }
    }

    /**
     * Gets all pairings for this server.
     *
     * @return A collection of all pairings
     */
    @Override
    public Collection<HomekitPairing> getPairings() {
        logger.debug("{} [{}] : {} - Getting all pairings", LOG_PREFIX, getUID(), LOG_PAIRING);
        return pairingRegistry.get(getPairingId());
    }

    /**
     * Removes a pairing from the server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the pairing exists</li>
     * <li>Removes it from the pairing registry</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param pairingId The identifier of the pairing to remove
     * @throws HomekitServerException if the pairing cannot be removed
     */
    @Override
    public void removePairing(byte[] pairingId) throws HomekitServerException {
        if (pairingId.length == 0) {
            String error = "HomekitPairing ID cannot be empty";
            logger.error("{} [{}] : {} - HomekitPairing validation error: {}", LOG_PREFIX, getUID(), LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{} [{}] : {} - Removing pairing - ID: {}", LOG_PREFIX, getUID(), LOG_PAIRING,
                HomekitByte.toHexString(pairingId));

        try {
            HomekitPairingUID uid = new HomekitPairingUIDImpl(getPairingId(), pairingId);
            if (pairingRegistry.remove(uid) != null) {
                setState(HomekitAccessoryServerState.UNPAIRED);
                logger.info("{} [{}] : {} - HomekitPairing removed successfully - ID: {}", LOG_PREFIX, getUID(),
                        LOG_PAIRING, HomekitByte.toHexString(pairingId));
            }
        } catch (HomekitServerException e) {
            String error = String.format("Failed to remove pairing %s: %s", HomekitByte.toHexString(pairingId),
                    e.getMessage());
            logger.error("{} [{}] : {} - HomekitPairing removal error: {}", LOG_PREFIX, getUID(), LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    /**
     * Handles the result of a pairing verification attempt.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Updates the server state based on verification result</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param verified Whether the pairing was verified successfully
     * @throws HomekitServerException if the state transition fails
     */
    protected void handlePairingVerification(boolean verified) throws HomekitServerException {
        logger.debug("{} [{}] : {} - Handling pairing verification - Verified: {}", LOG_PREFIX, getUID(), LOG_STATE,
                verified);
        synchronized (stateLock) {
            if (verified) {
                setState(HomekitAccessoryServerState.PAIR_VERIFIED);
                logger.info("{} [{}] : {} - HomekitPairing verified successfully", LOG_PREFIX, getUID(), LOG_STATE);
            } else {
                setState(HomekitAccessoryServerState.PAIRED);
                logger.info("{} [{}] : {} - HomekitPairing verification failed", LOG_PREFIX, getUID(), LOG_STATE);
            }
        }
    }

    /**
     * Checks if the server has any active pairings.
     *
     * @return true if the server has at least one pairing, false otherwise
     */
    @Override
    public boolean isPaired() {
        synchronized (stateLock) {
            boolean paired = !pairingRegistry.get(getPairingId()).isEmpty();
            try {
                if (paired) {
                    setState(HomekitAccessoryServerState.PAIRED);
                } else {
                    setState(HomekitAccessoryServerState.UNPAIRED);
                }
                return paired;
            } catch (HomekitServerException e) {
                logger.error("{} [{}] : {} - Failed to update pairing state: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                        e.getMessage(), e);
                // Return the current paired state without updating the server state
                return paired;
            }
        }
    }

    // ========== HomekitAccessory Management Methods ==========
    /**
     * Gets all accessories registered with this server.
     *
     * @return A collection of all registered accessories
     */
    @Override
    public Collection<HomekitAccessory> getAccessories() {
        synchronized (accessoryLock) {
            return Collections.unmodifiableCollection(new ArrayList<>(accessories));
        }
    }

    /**
     * Gets an accessory by its identifier.
     *
     * @param accessoryId The identifier of the accessory to get
     * @return The accessory, or null if not found
     */
    @Override
    public Optional<HomekitAccessory> getAccessory(int accessoryId) {
        synchronized (accessoryLock) {
            Optional<HomekitAccessory> result = accessories.stream().filter(a -> a.getAccessoryId() == accessoryId)
                    .findFirst();
            return result.isPresent() ? result : Optional.empty();
        }
    }

    /**
     * Adds a new accessory to the server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the accessory</li>
     * <li>Checks for ID and UID uniqueness</li>
     * <li>Registers the accessory</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param accessory The accessory to add
     * @throws HomekitAccessoryOperationException if the accessory cannot be added
     */
    @Override
    public void addAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        validateAccessory(accessory);
        try {
            validateLifecycleOperation("add accessory");
        } catch (HomekitServerException e) {
            throw new HomekitAccessoryOperationException("Failed to validate lifecycle operation", e);
        }
        validateAccessoryExists(accessory);

        synchronized (accessoryLock) {
            logger.debug("{} [{}] : {} - Adding accessory - UID: {}, Type: {}, Server: {}", LOG_PREFIX, getUID(),
                    LOG_ACCESSORY, accessory.getUID(), accessory.getClass().getSimpleName(), this.getUID());

            // Double-check uniqueness after acquiring lock
            validateAccessoryIdUniqueness(accessory);
            validateAccessoryUidUniqueness(accessory);

            if (accessories.add(accessory)) {
                try {
                    if (accessoryRegistry.update(accessory) == null) {
                        accessoryRegistry.add(accessory);
                    }
                    incrementConfigurationIndex();
                    advertise();
                    eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.ACCESSORY_ADDED, this,
                            accessory, (HomekitService) null, (HomekitCharacteristic<?>) null));
                    logger.info("{} [{}] : {} - HomekitAccessory added successfully - UID: {}", LOG_PREFIX, getUID(),
                            LOG_ACCESSORY, accessory.getUID());

                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{} [{}] : {} - Failed to add accessory due to runtime error - UID: {}, Error: {}",
                            LOG_PREFIX, getUID(), LOG_ERROR, accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to add accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                } catch (Exception e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{} [{}] : {} - Failed to add accessory - UID: {}, Error: {}", LOG_PREFIX, getUID(),
                            LOG_ERROR, accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to add accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                }
            } else {
                logger.warn("{} [{}] : {} - HomekitAccessory already exists - UID: {}", LOG_PREFIX, getUID(), LOG_WARN,
                        accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("HomekitAccessory with UID %s already exists", accessory.getUID()));
            }
        }
    }

    /**
     * Removes an accessory from the server.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the accessory exists</li>
     * <li>Unregisters the accessory</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param accessory The accessory to remove
     * @throws HomekitAccessoryOperationException if the accessory cannot be removed
     */
    @Override
    public void removeAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        validateAccessory(accessory);
        try {
            validateLifecycleOperation("remove accessory");
        } catch (HomekitServerException e) {
            String message = e.getMessage();
            throw new HomekitAccessoryOperationException(message != null ? message : "Server exception occurred", e);
        }
        validateAccessoryExists(accessory);

        synchronized (accessoryLock) {
            logger.debug("{} [{}] : {} - Removing accessory - UID: {}, Type: {}, Server: {}", LOG_PREFIX, getUID(),
                    LOG_ACCESSORY, accessory.getUID(), accessory.getClass().getSimpleName(), this.getUID());

            if (accessories.remove(accessory)) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    incrementConfigurationIndex();
                    advertise();
                    eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.ACCESSORY_REMOVED, this,
                            accessory, (HomekitService) null, (HomekitCharacteristic<?>) null));
                    logger.info("{} [{}] : {} - HomekitAccessory removed successfully - UID: {}", LOG_PREFIX, getUID(),
                            LOG_ACCESSORY, accessory.getUID());
                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{} [{}] : {} - Failed to remove accessory due to runtime error - UID: {}, Error: {}",
                            LOG_PREFIX, getUID(), LOG_ERROR, accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to remove accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                } catch (Exception e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{} [{}] : {} - Failed to remove accessory - UID: {}, Error: {}", LOG_PREFIX, getUID(),
                            LOG_ERROR, accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to remove accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                }

            } else {
                logger.warn("{} [{}] : {} - HomekitAccessory not found - UID: {}", LOG_PREFIX, getUID(), LOG_WARN,
                        accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("HomekitAccessory with UID %s not found", accessory.getUID()));
            }
        }
    }

    /**
     * Validates that an accessory's ID is unique.
     *
     * @param accessory The accessory to validate
     * @throws HomekitAccessoryOperationException if the ID is not unique
     */
    private void validateAccessoryIdUniqueness(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        synchronized (accessoryLock) {
            for (HomekitAccessory existing : accessories) {
                if (existing.getAccessoryId() == accessory.getAccessoryId()) {
                    throw new HomekitAccessoryOperationException(String.format(
                            "Cannot add accessory with ID %d - an accessory with this ID already exists (UID: %s, Type: %s)",
                            accessory.getAccessoryId(), existing.getUID(), existing.getClass().getSimpleName()));
                }
            }
        }
    }

    /**
     * Validates that an accessory's UID is unique.
     *
     * @param accessory The accessory to validate
     * @throws HomekitAccessoryOperationException if the UID is not unique
     */
    private void validateAccessoryUidUniqueness(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        synchronized (accessoryLock) {
            for (HomekitAccessory existing : accessories) {
                if (existing.getUID().equals(accessory.getUID())) {
                    throw new HomekitAccessoryOperationException(String.format(
                            "Cannot add accessory with UID %s - an accessory with this UID already exists (ID: %d, Type: %s)",
                            accessory.getUID(), existing.getAccessoryId(), existing.getClass().getSimpleName()));
                }
            }
        }
    }

    /**
     * Validates an accessory's configuration.
     *
     * @param accessory The accessory to validate
     * @throws HomekitAccessoryOperationException if the accessory is invalid
     */
    private void validateAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {

        if (accessory.getAccessoryId() < 0) {
            throw new HomekitAccessoryOperationException(String
                    .format("Invalid accessory ID %d - must be a non-negative number", accessory.getAccessoryId()));
        }

        validateAccessoryIdUniqueness(accessory);
        validateAccessoryUidUniqueness(accessory);
    }

    /**
     * Validates that an accessory exists in the server.
     *
     * @param accessory The accessory to validate
     * @throws HomekitAccessoryOperationException if the accessory is not found
     */
    private void validateAccessoryExists(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        synchronized (stateLock) {
            if (!accessories.contains(accessory)) {
                throw new HomekitAccessoryOperationException(String.format(
                        "Cannot remove accessory with ID %d - accessory not found", accessory.getAccessoryId()));
            }
        }
    }

    // ========== Interfaces ==========
    /**
     * Interface for listeners that need to be prioritized.
     */
    public interface PrioritizedListener extends HomekitAccessoryServerChangeListener {
        /**
         * Gets the priority of this listener.
         *
         * @return The listener's priority
         */
        int getPriority();
    }

    // ========== Utility Methods ==========
    /**
     * Generates a new secret key for the server.
     *
     * @return A new secret key
     * @throws HomekitServerException if key generation fails
     */
    protected static byte[] generateSecretKey() throws HomekitServerException {
        try {
            logger.debug("{} : {} - Generating new secret key", LOG_PREFIX, LOG_CONFIG);
            return HomekitKeyGenerator.generateSecretKey();
        } catch (Exception e) {
            logger.error("{} : {} - Failed to generate secret key: {}", LOG_PREFIX, LOG_ERROR, e.getMessage(), e);
            throw new HomekitServerException("Failed to generate secret key", e);
        }
    }

    /**
     * Generates a new pairing identifier for the server.
     *
     * @return A new pairing identifier
     * @throws HomekitServerException if ID generation fails
     */
    protected static byte[] generatePairingId() throws HomekitServerException {
        try {
            logger.debug("{} : {} - Generating new pairing ID", LOG_PREFIX, LOG_CONFIG);
            return HomekitKeyGenerator.generateHexidecimalId();
        } catch (Exception e) {
            logger.error("{} : {} - Failed to generate pairing ID: {}", LOG_PREFIX, LOG_ERROR, e.getMessage(), e);
            throw new HomekitServerException("Failed to generate pairing ID", e);
        }
    }

    /**
     * Checks if this server is a bridge.
     *
     * @return true if this server is a bridge, false otherwise
     */
    public boolean isBridge() {
        logger.debug("{} [{}] : {} - Checking if server is a bridge: {}", LOG_PREFIX, getUID(), LOG_CONFIG,
                category == HomekitAccessoryCategory.BRIDGES);
        return category == HomekitAccessoryCategory.BRIDGES;
    }

    @Override
    public abstract void advertise();

    /**
     * Handles a connection state change.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Updates the server state based on connection status</li>
     * <li>Notifies clients of the change</li>
     * </ul>
     *
     * @param connected Whether the server is connected
     * @throws HomekitServerException if the state transition fails
     */
    protected void handleConnection(boolean connected) throws HomekitServerException {
        logger.debug("{} [{}] : {} - Handling connection - Connected: {}", LOG_PREFIX, getUID(), LOG_STATE, connected);
        synchronized (stateLock) {
            if (connected) {
                setState(HomekitAccessoryServerState.CONNECTED);
                logger.info("{} [{}] : {} - Connection established", LOG_PREFIX, getUID(), LOG_STATE);
            } else {
                setState(HomekitAccessoryServerState.DISCONNECTED);
                logger.info("{} [{}] : {} - Connection terminated", LOG_PREFIX, getUID(), LOG_STATE);
            }
        }
    }

    /**
     * Closes the HomeKit accessory server and releases all resources.
     *
     * <p>
     * This method performs a graceful shutdown of the server, ensuring that all
     * resources are properly released and all registered accessories are removed.
     * It is called automatically when the server is used in a try-with-resources block
     * or when explicitly closed.
     *
     * <p>
     * Key implementation details:
     * <ul>
     * <li>Stops the server if it is running</li>
     * <li>Cleans up all resources and registered accessories</li>
     * <li>Logs any errors encountered during shutdown</li>
     * <li>Ensures idempotent behavior (multiple calls are safe)</li>
     * </ul>
     *
     * <p>
     * The method integrates with:
     * <ul>
     * <li>{@link #stop()} for graceful server shutdown</li>
     * <li>{@link #cleanupAccessories()} for accessory cleanup</li>
     * <li>{@link #cleanupResources()} for resource cleanup</li>
     * </ul>
     *
     * @throws Exception if an error occurs during shutdown
     */
    @Override
    public void close() throws Exception {
        logger.info("{} [{}] : {} - Closing Homekit accessory server", LOG_PREFIX, getUID(), LOG_INIT);
        synchronized (stateLock) {
            if (!isShutdown) {
                try {
                    stop();
                    cleanupAccessories();
                    cleanupResources();
                    isShutdown = true;
                    logger.info("{} [{}] : {} - Homekit accessory server closed successfully", LOG_PREFIX, getUID(),
                            LOG_INIT);
                } catch (Exception e) {
                    logger.error("{} [{}] : {} - Error while closing Homekit accessory server: {}", LOG_PREFIX,
                            getUID(), LOG_ERROR, e.getMessage(), e);
                    throw e;
                }
            } else {
                logger.warn("{} [{}] : {} - Homekit accessory server is already closed", LOG_PREFIX, getUID(),
                        LOG_WARN);
            }
        }
    }

    /**
     * Gets the server ID.
     *
     * @return The server ID
     */
    protected String getServerId() {
        return serverId;
    }
}
