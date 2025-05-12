package org.openhab.io.homekit.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.listener.HomekitAccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryServerState;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitInvalidStateTransitionException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.network.pairing.HomekitPairingImpl;
import org.openhab.io.homekit.network.pairing.HomekitPairingUID;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class HomekitAbstractAccessoryServer implements HomekitAccessoryServer, AutoCloseable {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitAbstractAccessoryServer.class);
    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Server: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // ========== State Management ==========
    protected final Object stateLock = new Object();
    private final Object accessoryLock = new Object();
    protected volatile HomekitAccessoryServerState currentState = HomekitAccessoryServerState.UNKNOWN;
    private volatile int configurationIndex = 1;
    private volatile boolean isShutdown = false;

    /** Server state management */
    private static final Map<HomekitAccessoryServerState, Set<HomekitAccessoryServerState>> VALID_STATE_TRANSITIONS = Map
            .of(HomekitAccessoryServerState.UNKNOWN, Set.of(HomekitAccessoryServerState.READY),
                    HomekitAccessoryServerState.READY,
                    Set.of(HomekitAccessoryServerState.STOPPED, HomekitAccessoryServerState.CONNECTED),
                    HomekitAccessoryServerState.CONNECTED,
                    Set.of(HomekitAccessoryServerState.DISCONNECTED, HomekitAccessoryServerState.PAIR_SETUP_INITIAL),
                    HomekitAccessoryServerState.DISCONNECTED,
                    Set.of(HomekitAccessoryServerState.CONNECTED, HomekitAccessoryServerState.STOPPED),
                    HomekitAccessoryServerState.PAIR_SETUP_INITIAL,
                    Set.of(HomekitAccessoryServerState.PAIRED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.PAIRED,
                    Set.of(HomekitAccessoryServerState.PAIR_UNVERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.PAIR_UNVERIFIED,
                    Set.of(HomekitAccessoryServerState.PAIR_VERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.PAIR_VERIFIED,
                    Set.of(HomekitAccessoryServerState.PAIR_UNVERIFIED, HomekitAccessoryServerState.DISCONNECTED),
                    HomekitAccessoryServerState.STOPPED, Set.of(HomekitAccessoryServerState.READY));

    // ========== Server Configuration ==========
    private final HomekitAccessoryCategory category;
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
    protected final Set<HomekitFactory> homekitFactories;

    // ========== Constructor ==========
    public HomekitAbstractAccessoryServer(HomekitAccessoryCategory category, InetAddress address, int port,
            byte[] pairingId, byte[] privateKey, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry, HomekitEventManager eventManager,
            Set<HomekitFactory> homekitFactories) throws HomekitConfigurationException {
        super();
        validateConstructorParameters(category, address, port, pairingId, privateKey, accessoryRegistry,
                pairingRegistry);

        logger.debug("{}Initializing Homekit server - Category: {}, Address: {}, Port: {}", LOG_INIT, category, address,
                port);
        this.category = category;
        this.address = address;
        this.port = port;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.secretKey = privateKey;
        this.pairingIdentifier = pairingId;
        this.setupCode = "";
        this.eventManager = eventManager;
        this.homekitFactories = homekitFactories;
        logger.debug("{}Homekit server initialization completed", LOG_INIT);
    }

    private void validateConstructorParameters(HomekitAccessoryCategory category, InetAddress address, int port,
            byte[] pairingId, byte[] privateKey, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry) throws HomekitConfigurationException {
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
     * Initialize any resources needed by the server.
     * Subclasses should override this method to initialize their specific resources.
     * 
     * @throws Exception if initialization fails
     */
    protected void initializeResources() throws HomekitServerException {
        // Base implementation does nothing
    }

    /**
     * Cleanup any resources held by the server.
     * Subclasses should override this method to cleanup their specific resources.
     * 
     * @throws Exception if cleanup fails
     */
    protected void cleanupResources() throws HomekitServerException {
        // Base implementation does nothing
    }

    private void cleanupAccessories() {
        List<HomekitAccessory> accessoriesToRemove;
        synchronized (accessoryLock) {
            accessoriesToRemove = new ArrayList<>(accessories);
        }

        for (HomekitAccessory accessory : accessoriesToRemove) {
            try {
                removeAccessory(accessory);
            } catch (HomekitAccessoryOperationException e) {
                logger.error("{}Failed to remove accessory during shutdown: {}", LOG_ERROR, e.getMessage(), e);
            }
        }
    }

    @Override
    public void start() throws HomekitServerException {
        synchronized (stateLock) {
            validateLifecycleOperation("start server");

            if (currentState == HomekitAccessoryServerState.READY) {
                logger.warn("{}Server is already running", LOG_SERVER);
                return;
            }
            if (currentState == HomekitAccessoryServerState.STOPPED) {
                logger.warn("{}Server is in stopped state, attempting to restart", LOG_SERVER);
            }

            try {
                initializeResources();
                setState(HomekitAccessoryServerState.READY);
                logger.info("{}Server started successfully", LOG_SERVER);
            } catch (Exception e) {
                try {
                    cleanupResources();
                } catch (Exception cleanupEx) {
                    logger.error("{}Failed to cleanup resources after failed start: {}", LOG_ERROR,
                            cleanupEx.getMessage(), cleanupEx);
                }
                throw new HomekitServerException("Failed to start server", e);
            }
        }
    }

    @Override
    public void stop() throws HomekitServerException {
        synchronized (stateLock) {
            if (currentState == HomekitAccessoryServerState.STOPPED) {
                logger.warn("{}Server is already stopped", LOG_SERVER);
                return;
            }
            try {
                try {
                    setState(HomekitAccessoryServerState.STOPPED);
                } catch (HomekitServerException e) {
                    logger.error("{}Failed to set stopped state during shutdown: {}", LOG_ERROR, e.getMessage(), e);
                    throw new HomekitServerException("Failed to set stopped state during shutdown", e);
                }
            } catch (HomekitServerException e) {
                logger.error("{}Failed to stop server gracefully: {}", LOG_ERROR, e.getMessage(), e);
                forceStop();
            }
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (stateLock) {
            if (isShutdown) {
                logger.debug("{}Server already shut down", LOG_SERVER);
                return;
            }
            isShutdown = true;
            logger.info("{}Initiating server shutdown", LOG_SERVER);

            Exception firstException = null;

            // Cleanup in specific order, capturing first exception but continuing cleanup
            try {
                cleanupAccessories();
            } catch (Exception e) {
                if (firstException == null)
                    firstException = e;
                logger.error("{}Error during accessory cleanup: {}", LOG_ERROR, e.getMessage(), e);
            }

            try {
                cleanupResources();
            } catch (Exception e) {
                if (firstException == null)
                    firstException = e;
                logger.error("{}Error during resource cleanup: {}", LOG_ERROR, e.getMessage(), e);
            }

            try {
                setState(HomekitAccessoryServerState.STOPPED);
            } catch (Exception e) {
                if (firstException == null)
                    firstException = e;
                logger.error("{}Failed to set stopped state during shutdown: {}", LOG_ERROR, e.getMessage(), e);
            }

            if (firstException != null) {
                throw new Exception("Server shutdown completed with errors", firstException);
            }

            logger.info("{}Server shutdown completed successfully", LOG_SERVER);
        }
    }

    private void forceStop() {
        try {
            cleanupResources();
            setState(HomekitAccessoryServerState.STOPPED);
            logger.warn("{}Server force stopped", LOG_WARN);
        } catch (Exception e) {
            logger.error("{}Failed to force stop server: {}", LOG_ERROR, e.getMessage(), e);
        }
    }

    protected void validateLifecycleOperation(String operation) throws HomekitServerException {
        if (isShutdown) {
            throw new HomekitServerException(String.format("Cannot %s - server is shut down", operation));
        }
    }

    // ========== State Management Methods ==========
    public synchronized HomekitAccessoryServerState getCurrentState() {
        return currentState;
    }

    private void validateStateTransition(HomekitAccessoryServerState newState)
            throws HomekitInvalidStateTransitionException {
        if (!isValidStateTransition(currentState, newState)) {
            throw new HomekitInvalidStateTransitionException(currentState, newState);
        }
    }

    private boolean isValidStateTransition(HomekitAccessoryServerState current, HomekitAccessoryServerState next) {
        @Nullable
        Set<HomekitAccessoryServerState> validNextStates = VALID_STATE_TRANSITIONS.get(current);
        return validNextStates != null && validNextStates.contains(next);
    }

    protected synchronized void setState(HomekitAccessoryServerState newState) throws HomekitServerException {
        HomekitAccessoryServerState previousState = currentState;
        try {
            validateStateTransition(newState);

            synchronized (stateLock) {
                if (!isValidStateTransition(currentState, newState)) {
                    throw new HomekitInvalidStateTransitionException(
                            String.format("Invalid state transition from %s to %s - Valid transitions from %s are: %s",
                                    currentState, newState, currentState, VALID_STATE_TRANSITIONS.get(currentState)));
                }
                currentState = newState;
            }

            eventManager.publishEvent(new HomekitAccessoryServerEvent(newState.getEventType(), this,
                    (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
            logger.debug("{}State change completed", LOG_STATE);
        } catch (HomekitServerException e) {
            // Rollback on failure
            synchronized (stateLock) {
                currentState = previousState;
            }
            logger.error("{}Failed to set state to {}: {}", LOG_ERROR, newState, e.getMessage());
            throw e;
        }
    }

    // ========== Configuration Management Methods ==========
    @Override
    public int getConfigurationIndex() {
        synchronized (stateLock) {
            return configurationIndex;
        }
    }

    @Override
    public void setConfigurationIndex(int newIndex) throws HomekitConfigurationException {
        synchronized (stateLock) {
            validateConfigurationIndex(newIndex);
            int oldIndex = configurationIndex;
            try {
                configurationIndex = newIndex;
                eventManager.publishEvent(
                        new HomekitAccessoryServerEvent(HomekitEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED,
                                this, (HomekitAccessory) null, (HomekitService) null, (HomekitCharacteristic<?>) null));
                logger.info("{}Configuration index updated from {} to {}", LOG_CONFIG, oldIndex, newIndex);
            } catch (Exception e) {
                // Rollback on failure
                configurationIndex = oldIndex;
                throw new HomekitConfigurationException(
                        String.format("Failed to update configuration index from %d to %d", oldIndex, newIndex), e);
            }
        }
    }

    private void validateConfigurationIndex(int newIndex) throws HomekitConfigurationException {
        if (newIndex <= 0) {
            throw new HomekitConfigurationException("Configuration index must be positive");
        }
        if (newIndex == configurationIndex) {
            throw new HomekitConfigurationException("New configuration index is the same as current index");
        }
    }

    protected synchronized void incrementConfigurationIndex() {
        synchronized (stateLock) {
            configurationIndex++;
            logger.debug("{}Configuration index incremented to {}", LOG_CONFIG, configurationIndex);
        }
    }

    @Override
    public void factoryReset() {
        logger.debug("{}Performing factory reset", LOG_CONFIG);
        synchronized (stateLock) {
            try {
                setState(HomekitAccessoryServerState.RESET);
                // After reset, transition to UNPAIRED state
                setState(HomekitAccessoryServerState.UNPAIRED);
            } catch (HomekitServerException e) {
                logger.error("{}Failed to perform factory reset: {}", LOG_ERROR, e.getMessage(), e);
            }
        }
    }

    // ========== Server Identity and Network Methods ==========
    @Override
    public InetAddress getAddress() {
        logger.debug("{}Getting server address: {}", LOG_CONFIG, address);
        return address;
    }

    @Override
    public int getPort() {
        logger.debug("{}Getting server port: {}", LOG_CONFIG, port);
        return port;
    }

    @Override
    public byte[] getPairingId() {
        logger.debug("{}Getting pairing ID", LOG_CONFIG);
        return pairingIdentifier;
    }

    @Override
    public HomekitAccessoryServerUID getUID() {
        logger.debug("{}Getting server UID", LOG_CONFIG);
        return new HomekitAccessoryServerUID(new String(getPairingId(), StandardCharsets.UTF_8).replace(":", ""));
    }

    @Override
    public byte[] getSecretKey() {
        logger.debug("{}Getting secret key", LOG_CONFIG);
        return secretKey;
    }

    @Override
    public byte[] getPublicKey(byte @NonNull [] destinationPairingId) {
        logger.debug("{}Getting public key for destination pairing ID: {}", LOG_CONFIG,
                HomekitByte.toHexString(destinationPairingId));
        HomekitPairing hp = pairingRegistry.get(new HomekitPairingUID(getPairingId(), destinationPairingId));
        return hp != null ? hp.getPublicKey() : new byte[0];
    }

    // ========== Setup Code Management Methods ==========
    @Override
    public String getSetupCode() {
        logger.debug("{}Getting setup code", LOG_CONFIG);
        return setupCode;
    }

    @Override
    public void setSetupCode(String setupCode) {
        if (setupCode.length() != 8 || !setupCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("Setup code must be 8 digits");
        }
        this.setupCode = setupCode;
    }

    // ========== HomekitPairing Management Methods ==========
    @Override
    public void addPairing(byte @NonNull [] pairingId, byte @NonNull [] publicKey) throws HomekitServerException {
        if (pairingId.length == 0) {
            throw new HomekitServerException("HomekitPairing ID cannot be empty");
        }
        if (publicKey.length == 0) {
            throw new HomekitServerException("Public key cannot be empty");
        }

        // Validate pairing ID uniqueness
        if (pairingRegistry.get(new HomekitPairingUID(getPairingId(), pairingId)) != null) {
            String error = String.format("HomekitPairing ID %s already exists", HomekitByte.toHexString(pairingId));
            logger.error("{}HomekitPairing validation error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{}Adding pairing - ID: {}", LOG_PAIRING, HomekitByte.toHexString(pairingId));

        try {
            HomekitPairing newPairing = new HomekitPairingImpl(getPairingId(), pairingId, publicKey);
            HomekitPairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("{}Removed existing pairing - Destination: {}, Public Key: {}", LOG_PAIRING,
                        HomekitByte.toHexString(oldPairing.getDestinationId()),
                        HomekitByte.toHexString(oldPairing.getPublicKey()));
                setState(HomekitAccessoryServerState.DISCONNECTED);
            }

            pairingRegistry.add(newPairing);
            logger.debug("{}HomekitPairing added successfully", LOG_PAIRING);
            setState(HomekitAccessoryServerState.PAIRED);
            logger.info("{}HomekitPairing added successfully - ID: {}", LOG_PAIRING,
                    HomekitByte.toHexString(pairingId));
        } catch (HomekitServerException e) {
            String error = String.format("Failed to add pairing %s: %s", HomekitByte.toHexString(pairingId),
                    e.getMessage());
            logger.error("{}HomekitPairing addition error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    @Override
    public @Nullable HomekitPairing getPairing(byte @NonNull [] pairingId) {
        if (pairingId.length == 0) {
            throw new IllegalArgumentException("HomekitPairing ID cannot be empty");
        }
        Collection<HomekitPairing> pairings = pairingRegistry.get(pairingId);
        return pairings.isEmpty() ? null : pairings.iterator().next();
    }

    @Override
    public Collection<HomekitPairing> getPairings() {
        logger.debug("{}Getting all pairings", LOG_PAIRING);
        return pairingRegistry.get(getPairingId());
    }

    @Override
    public void removePairing(byte @NonNull [] pairingId) throws HomekitServerException {
        if (pairingId == null || pairingId.length == 0) {
            String error = "HomekitPairing ID cannot be null or empty";
            logger.error("{}HomekitPairing validation error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{}Removing pairing - ID: {}", LOG_PAIRING, HomekitByte.toHexString(pairingId));

        try {
            HomekitPairingUID uid = new HomekitPairingUID(getPairingId(), pairingId);
            if (pairingRegistry.remove(uid) != null) {
                setState(HomekitAccessoryServerState.UNPAIRED);
                logger.info("{}HomekitPairing removed successfully - ID: {}", LOG_PAIRING,
                        HomekitByte.toHexString(pairingId));
            }
        } catch (HomekitServerException e) {
            String error = String.format("Failed to remove pairing %s: %s", HomekitByte.toHexString(pairingId),
                    e.getMessage());
            logger.error("{}HomekitPairing removal error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    protected void handlePairingVerification(boolean verified) throws HomekitServerException {
        logger.debug("{}Handling pairing verification - Verified: {}", LOG_STATE, verified);
        synchronized (stateLock) {
            if (verified) {
                setState(HomekitAccessoryServerState.PAIR_VERIFIED);
                logger.info("{}HomekitPairing verified successfully", LOG_STATE);
            } else {
                setState(HomekitAccessoryServerState.PAIRED);
                logger.info("{}HomekitPairing verification failed", LOG_STATE);
            }
        }
    }

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
                logger.error("{}Failed to update pairing state: {}", LOG_ERROR, e.getMessage(), e);
                // Return the current paired state without updating the server state
                return paired;
            }
        }
    }

    // ========== HomekitAccessory Management Methods ==========
    @Override
    public Collection<HomekitAccessory> getAccessories() {
        synchronized (accessoryLock) {
            return Collections.unmodifiableCollection(new ArrayList<>(accessories));
        }
    }

    @Override
    public @Nullable HomekitAccessory getAccessory(int accessoryId) {
        synchronized (accessoryLock) {
            Optional<HomekitAccessory> result = accessories.stream().filter(a -> a.getAccessoryId() == accessoryId)
                    .findFirst();
            return result.isPresent() ? result.get() : null;
        }
    }

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
            logger.debug("{}Adding accessory - UID: {}, Type: {}, Server: {}", LOG_ACCESSORY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), this.getUID());

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
                    logger.info("{}HomekitAccessory added successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());

                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{}Failed to add accessory due to runtime error - UID: {}, Error: {}", LOG_ERROR,
                            accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to add accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                } catch (Exception e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{}Failed to add accessory - UID: {}, Error: {}", LOG_ERROR, accessory.getUID(),
                            e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to add accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                }
            } else {
                logger.warn("{}HomekitAccessory already exists - UID: {}", LOG_WARN, accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("HomekitAccessory with UID %s already exists", accessory.getUID()));
            }
        }
    }

    @Override
    public void removeAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        validateAccessory(accessory);
        try {
            validateLifecycleOperation("remove accessory");
        } catch (HomekitServerException e) {
            throw new HomekitAccessoryOperationException(e.getMessage(), e);
        }
        validateAccessoryExists(accessory);

        synchronized (accessoryLock) {
            logger.debug("{}Removing accessory - UID: {}, Type: {}, Server: {}", LOG_ACCESSORY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), this.getUID());

            if (accessories.remove(accessory)) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    incrementConfigurationIndex();
                    advertise();
                    eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.ACCESSORY_REMOVED, this,
                            accessory, (HomekitService) null, (HomekitCharacteristic<?>) null));
                    logger.info("{}HomekitAccessory removed successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{}Failed to remove accessory due to runtime error - UID: {}, Error: {}", LOG_ERROR,
                            accessory.getUID(), e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to remove accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                } catch (Exception e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{}Failed to remove accessory - UID: {}, Error: {}", LOG_ERROR, accessory.getUID(),
                            e.getMessage());
                    throw new HomekitAccessoryOperationException(
                            String.format("Failed to remove accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                }

            } else {
                logger.warn("{}HomekitAccessory not found - UID: {}", LOG_WARN, accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("HomekitAccessory with UID %s not found", accessory.getUID()));
            }
        }
    }

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

    private void validateAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {

        if (accessory.getAccessoryId() < 0) {
            throw new HomekitAccessoryOperationException(String
                    .format("Invalid accessory ID %d - must be a non-negative number", accessory.getAccessoryId()));
        }

        validateAccessoryIdUniqueness(accessory);
        validateAccessoryUidUniqueness(accessory);
    }

    private void validateAccessoryExists(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        synchronized (stateLock) {
            if (!accessories.contains(accessory)) {
                throw new HomekitAccessoryOperationException(String.format(
                        "Cannot remove accessory with ID %d - accessory not found", accessory.getAccessoryId()));
            }
        }
    }

    // ========== Interfaces ==========
    public interface PrioritizedListener extends HomekitAccessoryServerChangeListener {
        int getPriority();
    }

    // ========== Utility Methods ==========
    protected static byte[] generateSecretKey() throws HomekitServerException {
        try {
            logger.debug("{}Generating new secret key", LOG_CONFIG);
            return HomekitKeyGenerator.generateSecretKey();
        } catch (Exception e) {
            logger.error("{}Failed to generate secret key: {}", LOG_ERROR, e.getMessage(), e);
            throw new HomekitServerException("Failed to generate secret key", e);
        }
    }

    protected static byte[] generatePairingId() throws HomekitServerException {
        try {
            logger.debug("{}Generating new pairing ID", LOG_CONFIG);
            return HomekitKeyGenerator.generateHexidecimalId();
        } catch (Exception e) {
            logger.error("{}Failed to generate pairing ID: {}", LOG_ERROR, e.getMessage(), e);
            throw new HomekitServerException("Failed to generate pairing ID", e);
        }
    }

    public boolean isBridge() {
        logger.debug("{}Checking if server is a bridge: {}", LOG_CONFIG, category == HomekitAccessoryCategory.BRIDGES);
        return category == HomekitAccessoryCategory.BRIDGES;
    }

    @Override
    public abstract void advertise();

    protected void handleConnection(boolean connected) throws HomekitServerException {
        logger.debug("{}Handling connection - Connected: {}", LOG_STATE, connected);
        synchronized (stateLock) {
            if (connected) {
                setState(HomekitAccessoryServerState.CONNECTED);
                logger.info("{}Connection established", LOG_STATE);
            } else {
                setState(HomekitAccessoryServerState.DISCONNECTED);
                logger.info("{}Connection terminated", LOG_STATE);
            }
        }
    }
}
