package org.openhab.io.homekit.internal.server;

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
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitInvalidStateTransitionException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.HomekitEventManager;
import org.openhab.io.homekit.internal.events.HomekitEventType;
import org.openhab.io.homekit.internal.pairing.HomekitPairing;
import org.openhab.io.homekit.internal.pairing.PairingUID;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.HomekitKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractAccessoryServer implements AccessoryServer, AutoCloseable {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(AbstractAccessoryServer.class);
    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit Server: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // ========== State Management ==========
    protected final Object stateLock = new Object();
    private final Object accessoryLock = new Object();
    protected volatile AccessoryServerState currentState = AccessoryServerState.UNKNOWN;
    private volatile int configurationIndex = 1;
    private volatile boolean isShutdown = false;

    /** Server state management */
    private static final Map<AccessoryServerState, Set<AccessoryServerState>> VALID_STATE_TRANSITIONS = Map.of(
            AccessoryServerState.UNKNOWN, Set.of(AccessoryServerState.READY), AccessoryServerState.READY,
            Set.of(AccessoryServerState.STOPPED, AccessoryServerState.CONNECTED), AccessoryServerState.CONNECTED,
            Set.of(AccessoryServerState.DISCONNECTED, AccessoryServerState.PAIR_SETUP_INITIAL),
            AccessoryServerState.DISCONNECTED, Set.of(AccessoryServerState.CONNECTED, AccessoryServerState.STOPPED),
            AccessoryServerState.PAIR_SETUP_INITIAL,
            Set.of(AccessoryServerState.PAIRED, AccessoryServerState.DISCONNECTED), AccessoryServerState.PAIRED,
            Set.of(AccessoryServerState.PAIR_UNVERIFIED, AccessoryServerState.DISCONNECTED),
            AccessoryServerState.PAIR_UNVERIFIED,
            Set.of(AccessoryServerState.PAIR_VERIFIED, AccessoryServerState.DISCONNECTED),
            AccessoryServerState.PAIR_VERIFIED,
            Set.of(AccessoryServerState.PAIR_UNVERIFIED, AccessoryServerState.DISCONNECTED),
            AccessoryServerState.STOPPED, Set.of(AccessoryServerState.READY));

    // ========== Server Configuration ==========
    private final AccessoryCategory category;
    protected final InetAddress address;
    protected final int port;
    private final byte[] pairingIdentifier;
    protected final byte[] secretKey;
    protected volatile String setupCode;

    // ========== Collections and Executors ==========
    private final Collection<Accessory> accessories = new CopyOnWriteArraySet<>();

    // ========== Dependencies ==========
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;
    protected final HomekitEventManager eventManager;

    // ========== Constructor ==========
    public AbstractAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingId,
            byte[] privateKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            HomekitEventManager eventManager) throws HomekitConfigurationException {
        super();
        validateConstructorParameters(category, address, port, pairingId, privateKey, accessoryRegistry,
                pairingRegistry);

        logger.debug("{}Initializing HomeKit server - Category: {}, Address: {}, Port: {}", LOG_INIT, category, address,
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
        logger.debug("{}HomeKit server initialization completed", LOG_INIT);
    }

    private void validateConstructorParameters(AccessoryCategory category, InetAddress address, int port,
            byte[] pairingId, byte[] privateKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws HomekitConfigurationException {
        if (port <= 0 || port > 65535) {
            throw new HomekitConfigurationException(
                    String.format("HomeKit server port %d is invalid - must be between 1 and 65535", port));
        }
        if (pairingId.length == 0) {
            throw new HomekitConfigurationException(
                    "HomeKit server pairing ID cannot be empty - required for secure pairing");
        }
        if (privateKey.length == 0) {
            throw new HomekitConfigurationException(
                    "HomeKit server private key cannot be empty - required for secure pairing");
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
        List<Accessory> accessoriesToRemove;
        synchronized (accessoryLock) {
            accessoriesToRemove = new ArrayList<>(accessories);
        }

        for (Accessory accessory : accessoriesToRemove) {
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

            if (currentState == AccessoryServerState.READY) {
                logger.warn("{}Server is already running", LOG_SERVER);
                return;
            }
            if (currentState == AccessoryServerState.STOPPED) {
                logger.warn("{}Server is in stopped state, attempting to restart", LOG_SERVER);
            }

            try {
                initializeResources();
                setState(AccessoryServerState.READY);
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
            if (currentState == AccessoryServerState.STOPPED) {
                logger.warn("{}Server is already stopped", LOG_SERVER);
                return;
            }
            try {
                try {
                    setState(AccessoryServerState.STOPPED);
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
                setState(AccessoryServerState.STOPPED);
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
            setState(AccessoryServerState.STOPPED);
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
    public synchronized AccessoryServerState getCurrentState() {
        return currentState;
    }

    private void validateStateTransition(AccessoryServerState newState) throws HomekitInvalidStateTransitionException {
        if (!isValidStateTransition(currentState, newState)) {
            throw new HomekitInvalidStateTransitionException(currentState, newState);
        }
    }

    private boolean isValidStateTransition(AccessoryServerState current, AccessoryServerState next) {
        @Nullable Set<AccessoryServerState> validNextStates = VALID_STATE_TRANSITIONS.get(current);
        return validNextStates != null && validNextStates.contains(next);
    }

    protected synchronized void setState(AccessoryServerState newState) throws HomekitServerException {
        AccessoryServerState previousState = currentState;
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

            eventManager.publishEvent(new AccessoryServerEvent(newState.getEventType(), this, (Accessory) null,
                    (Service) null, (Characteristic<?>) null));
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
                        new AccessoryServerEvent(HomekitEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED, this,
                                (Accessory) null, (Service) null, (Characteristic<?>) null));
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
                setState(AccessoryServerState.RESET);
                // After reset, transition to UNPAIRED state
                setState(AccessoryServerState.UNPAIRED);
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
    public AccessoryServerUID getUID() {
        logger.debug("{}Getting server UID", LOG_CONFIG);
        return new AccessoryServerUID(new String(getPairingId(), StandardCharsets.UTF_8).replace(":", ""));
    }

    @Override
    public byte[] getSecretKey() {
        logger.debug("{}Getting secret key", LOG_CONFIG);
        return secretKey;
    }

    @Override
    public byte[] getPublicKey(byte @NonNull [] destinationPairingId) {
        logger.debug("{}Getting public key for destination pairing ID: {}", LOG_CONFIG,
                Byte.toHexString(destinationPairingId));
        Pairing hp = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingId));
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

    // ========== Pairing Management Methods ==========
    @Override
    public void addPairing(byte @NonNull [] pairingId, byte @NonNull [] publicKey) throws HomekitServerException {
        if (pairingId.length == 0) {
            throw new HomekitServerException("Pairing ID cannot be empty");
        }
        if (publicKey.length == 0) {
            throw new HomekitServerException("Public key cannot be empty");
        }

        // Validate pairing ID uniqueness
        if (pairingRegistry.get(new PairingUID(getPairingId(), pairingId)) != null) {
            String error = String.format("Pairing ID %s already exists", Byte.toHexString(pairingId));
            logger.error("{}Pairing validation error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{}Adding pairing - ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));

        try {
            Pairing newPairing = new HomekitPairing(getPairingId(), pairingId, publicKey);
            Pairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("{}Removed existing pairing - Destination: {}, Public Key: {}", LOG_PAIRING,
                        Byte.toHexString(oldPairing.getDestinationId()), Byte.toHexString(oldPairing.getPublicKey()));
                setState(AccessoryServerState.DISCONNECTED);
            }

            pairingRegistry.add(newPairing);
            logger.debug("{}Pairing added successfully", LOG_PAIRING);
            setState(AccessoryServerState.PAIRED);
            logger.info("{}Pairing added successfully - ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));
        } catch (HomekitServerException e) {
            String error = String.format("Failed to add pairing %s: %s", Byte.toHexString(pairingId), e.getMessage());
            logger.error("{}Pairing addition error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    @Override
    public @Nullable Pairing getPairing(byte @NonNull [] pairingId) {
        if (pairingId.length == 0) {
            throw new IllegalArgumentException("Pairing ID cannot be empty");
        }
        Collection<Pairing> pairings = pairingRegistry.get(pairingId);
        return pairings.isEmpty() ? null : pairings.iterator().next();
    }

    @Override
    public Collection<Pairing> getPairings() {
        logger.debug("{}Getting all pairings", LOG_PAIRING);
        return pairingRegistry.get(getPairingId());
    }

    @Override
    public void removePairing(byte @NonNull [] pairingId) throws HomekitServerException {
        if (pairingId == null || pairingId.length == 0) {
            String error = "Pairing ID cannot be null or empty";
            logger.error("{}Pairing validation error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        logger.debug("{}Removing pairing - ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));

        try {
            PairingUID uid = new PairingUID(getPairingId(), pairingId);
            if (pairingRegistry.remove(uid) != null) {
                setState(AccessoryServerState.UNPAIRED);
                logger.info("{}Pairing removed successfully - ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));
            }
        } catch (HomekitServerException e) {
            String error = String.format("Failed to remove pairing %s: %s", Byte.toHexString(pairingId),
                    e.getMessage());
            logger.error("{}Pairing removal error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    protected void handlePairingVerification(boolean verified) throws HomekitServerException {
        logger.debug("{}Handling pairing verification - Verified: {}", LOG_STATE, verified);
        synchronized (stateLock) {
            if (verified) {
                setState(AccessoryServerState.PAIR_VERIFIED);
                logger.info("{}Pairing verified successfully", LOG_STATE);
            } else {
                setState(AccessoryServerState.PAIRED);
                logger.info("{}Pairing verification failed", LOG_STATE);
            }
        }
    }

    @Override
    public boolean isPaired() {
        synchronized (stateLock) {
            boolean paired = !pairingRegistry.get(getPairingId()).isEmpty();
            try {
                if (paired) {
                    setState(AccessoryServerState.PAIRED);
                } else {
                    setState(AccessoryServerState.UNPAIRED);
                }
                return paired;
            } catch (HomekitServerException e) {
                logger.error("{}Failed to update pairing state: {}", LOG_ERROR, e.getMessage(), e);
                // Return the current paired state without updating the server state
                return paired;
            }
        }
    }

    // ========== Accessory Management Methods ==========
    @Override
    public Collection<Accessory> getAccessories() {
        synchronized (accessoryLock) {
            return Collections.unmodifiableCollection(new ArrayList<>(accessories));
        }
    }

    @Override
    public @Nullable Accessory getAccessory(int accessoryId) {
        synchronized (accessoryLock) {
            Optional<Accessory> result = accessories.stream().filter(a -> a.getAccessoryId() == accessoryId)
                    .findFirst();
            return result.isPresent() ? result.get() : null;
        }
    }

    @Override
    public void addAccessory(Accessory accessory) throws HomekitAccessoryOperationException {
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
                    eventManager.publishEvent(new AccessoryServerEvent(HomekitEventType.ACCESSORY_ADDED, this,
                            accessory, (Service) null, (Characteristic<?>) null));
                    logger.info("{}Accessory added successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());

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
                logger.warn("{}Accessory already exists - UID: {}", LOG_WARN, accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("Accessory with UID %s already exists", accessory.getUID()));
            }
        }
    }

    @Override
    public void removeAccessory(Accessory accessory) throws HomekitAccessoryOperationException {
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
                    eventManager.publishEvent(new AccessoryServerEvent(HomekitEventType.ACCESSORY_REMOVED, this,
                            accessory, (Service) null, (Characteristic<?>) null));
                    logger.info("{}Accessory removed successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
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
                logger.warn("{}Accessory not found - UID: {}", LOG_WARN, accessory.getUID());
                throw new HomekitAccessoryOperationException(
                        String.format("Accessory with UID %s not found", accessory.getUID()));
            }
        }
    }

    private void validateAccessoryIdUniqueness(Accessory accessory) throws HomekitAccessoryOperationException {
        synchronized (accessoryLock) {
            for (Accessory existing : accessories) {
                if (existing.getAccessoryId() == accessory.getAccessoryId()) {
                    throw new HomekitAccessoryOperationException(String.format(
                            "Cannot add accessory with ID %d - an accessory with this ID already exists (UID: %s, Type: %s)",
                            accessory.getAccessoryId(), existing.getUID(), existing.getClass().getSimpleName()));
                }
            }
        }
    }

    private void validateAccessoryUidUniqueness(Accessory accessory) throws HomekitAccessoryOperationException {
        synchronized (accessoryLock) {
            for (Accessory existing : accessories) {
                if (existing.getUID().equals(accessory.getUID())) {
                    throw new HomekitAccessoryOperationException(String.format(
                            "Cannot add accessory with UID %s - an accessory with this UID already exists (ID: %d, Type: %s)",
                            accessory.getUID(), existing.getAccessoryId(), existing.getClass().getSimpleName()));
                }
            }
        }
    }

    private void validateAccessory(Accessory accessory) throws HomekitAccessoryOperationException {

        if (accessory.getAccessoryId() < 0) {
            throw new HomekitAccessoryOperationException(String
                    .format("Invalid accessory ID %d - must be a non-negative number", accessory.getAccessoryId()));
        }

        validateAccessoryIdUniqueness(accessory);
        validateAccessoryUidUniqueness(accessory);
    }

    private void validateAccessoryExists(Accessory accessory) throws HomekitAccessoryOperationException {
        synchronized (stateLock) {
            if (!accessories.contains(accessory)) {
                throw new HomekitAccessoryOperationException(String.format(
                        "Cannot remove accessory with ID %d - accessory not found", accessory.getAccessoryId()));
            }
        }
    }

    // ========== Interfaces ==========
    public interface PrioritizedListener extends AccessoryServerChangeListener {
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
        logger.debug("{}Checking if server is a bridge: {}", LOG_CONFIG, category == AccessoryCategory.BRIDGES);
        return category == AccessoryCategory.BRIDGES;
    }

    @Override
    public abstract void advertise();

    protected void handleConnection(boolean connected) throws HomekitServerException {
        logger.debug("{}Handling connection - Connected: {}", LOG_STATE, connected);
        synchronized (stateLock) {
            if (connected) {
                setState(AccessoryServerState.CONNECTED);
                logger.info("{}Connection established", LOG_STATE);
            } else {
                setState(AccessoryServerState.DISCONNECTED);
                logger.info("{}Connection terminated", LOG_STATE);
            }
        }
    }
}
