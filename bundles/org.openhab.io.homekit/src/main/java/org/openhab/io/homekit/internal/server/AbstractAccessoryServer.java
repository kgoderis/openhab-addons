package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.exception.AccessoryOperationException;
import org.openhab.io.homekit.exception.ConfigurationException;
import org.openhab.io.homekit.exception.InvalidStateTransitionException;
import org.openhab.io.homekit.exception.ListenerNotificationException;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent.AccessoryServerEventType;
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
    private static final long EVENT_TIMEOUT_MS = 5000; // 5 seconds timeout for event notifications
    private static final int MAX_RETRIES = 2; // Maximum number of retries for failed notifications

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
    private final Object stateLock = new Object();
    private final Object accessoryLock = new Object();
    private volatile AccessoryServerState currentState = AccessoryServerState.UNKNOWN;
    private volatile int configurationIndex = 1;
    private volatile boolean isShutdown = false;

    /** Server state management */
    private static final Map<AccessoryServerState, Set<AccessoryServerState>> VALID_STATE_TRANSITIONS = Map.of(
            AccessoryServerState.UNPAIRED, Set.of(AccessoryServerState.PAIRED, AccessoryServerState.RESET),
            AccessoryServerState.PAIRED,
            Set.of(AccessoryServerState.UNPAIRED, AccessoryServerState.PAIR_VERIFIED,
                    AccessoryServerState.DISCONNECTED),
            AccessoryServerState.PAIR_VERIFIED, Set.of(AccessoryServerState.PAIRED, AccessoryServerState.DISCONNECTED),
            AccessoryServerState.DISCONNECTED, Set.of(AccessoryServerState.CONNECTED, AccessoryServerState.PAIRED),
            AccessoryServerState.CONNECTED, Set.of(AccessoryServerState.DISCONNECTED), AccessoryServerState.RESET,
            Set.of(AccessoryServerState.UNPAIRED));

    // ========== Server Configuration ==========
    private final AccessoryCategory category;
    private final InetAddress address;
    private final int port;
    private final byte[] pairingIdentifier;
    private final byte[] secretKey;
    private volatile String setupCode;

    // ========== Collections and Executors ==========
    private final Collection<AccessoryServerChangeListener> changeListeners = new CopyOnWriteArraySet<>();
    private final Collection<Accessory> accessories = new CopyOnWriteArraySet<>();
    private final ExecutorService eventExecutor = ThreadPoolManager.getPool("homekit-event");

    // ========== Dependencies ==========
    private final AccessoryRegistry accessoryRegistry;
    private final PairingRegistry pairingRegistry;

    // ========== Constructor ==========
    public AbstractAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingId,
            byte[] privateKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws HomekitServerException {
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
        logger.debug("{}HomeKit server initialization completed", LOG_INIT);
    }

    private void validateConstructorParameters(AccessoryCategory category, InetAddress address, int port,
            byte[] pairingId, byte[] privateKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws ConfigurationException {
        if (category == null) {
            throw new ConfigurationException(
                    "HomeKit server category cannot be null - required for proper accessory type identification");
        }
        if (address == null) {
            throw new ConfigurationException(
                    "HomeKit server network address cannot be null - required for device discovery");
        }
        if (port <= 0 || port > 65535) {
            throw new ConfigurationException(
                    String.format("HomeKit server port %d is invalid - must be between 1 and 65535", port));
        }
        if (pairingId == null || pairingId.length == 0) {
            throw new ConfigurationException(
                    "HomeKit server pairing ID cannot be null or empty - required for secure pairing");
        }
        if (privateKey == null || privateKey.length == 0) {
            throw new ConfigurationException(
                    "HomeKit server private key cannot be null or empty - required for secure communication");
        }
        if (accessoryRegistry == null) {
            throw new ConfigurationException(
                    "HomeKit accessory registry cannot be null - required for accessory management");
        }
        if (pairingRegistry == null) {
            throw new ConfigurationException(
                    "HomeKit pairing registry cannot be null - required for secure pairing management");
        }
    }

    // ========== Lifecycle Methods ==========
    /**
     * Initialize any resources needed by the server.
     * Subclasses should override this method to initialize their specific resources.
     * 
     * @throws Exception if initialization fails
     */
    protected void initializeResources() throws Exception {
        // Base implementation does nothing
    }

    /**
     * Cleanup any resources held by the server.
     * Subclasses should override this method to cleanup their specific resources.
     * 
     * @throws Exception if cleanup fails
     */
    protected void cleanupResources() throws Exception {
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
            } catch (AccessoryOperationException e) {
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
    public void stop() {
        synchronized (stateLock) {
            if (currentState == AccessoryServerState.STOPPED) {
                logger.warn("{}Server is already stopped", LOG_SERVER);
                return;
            }
            try {
                try {
                    setState(AccessoryServerState.STOPPED);
                } catch (Exception e) {
                    if (firstException == null)
                        firstException = e;
                    logger.error("{}Failed to set stopped state during shutdown: {}", LOG_ERROR, e.getMessage(), e);
                }
            } catch (Exception e) {
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
                cleanupListeners();
            } catch (Exception e) {
                firstException = e;
                logger.error("{}Error during listener cleanup: {}", LOG_ERROR, e.getMessage(), e);
            }

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

    private void validateStateTransition(AccessoryServerState newState) throws ConfigurationException {
        if (newState == null) {
            throw new ConfigurationException(
                    "Cannot transition to null state - valid HomeKit server state is required");
        }
        if (isShutdown && newState != AccessoryServerState.STOPPED) {
            throw new ConfigurationException("Cannot transition to " + newState + " - server is shutting down");
        }
    }

    private boolean isValidStateTransition(AccessoryServerState current, AccessoryServerState next) {
        Set<AccessoryServerState> validNextStates = VALID_STATE_TRANSITIONS.get(current);
        return validNextStates != null && validNextStates.contains(next);
    }

    protected synchronized void setState(AccessoryServerState newState) throws HomekitServerException {
        validateStateTransition(newState);

        AccessoryServerState current;
        synchronized (stateLock) {
            current = currentState;
            if (!isValidStateTransition(current, newState)) {
                throw new InvalidStateTransitionException(
                        String.format("Invalid state transition from %s to %s - Valid transitions from %s are: %s",
                                current, newState, current, VALID_STATE_TRANSITIONS.get(current)));
            }

            if (!current.equals(newState)) {
                logger.debug("{}State changing from {} to {}", LOG_STATE, current, newState);
                currentState = newState;
            } else {
                return; // No state change needed
            }
        }

        // Notify listeners outside of stateLock to prevent deadlocks
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null, newState.getEventType());
        try {
            notifyStateChangeListeners(event);
            logger.debug("{}State change completed", LOG_STATE);
        } catch (ListenerNotificationException e) {
            // Rollback state change
            synchronized (stateLock) {
                currentState = current;
            }
            throw new ListenerNotificationException(
                    String.format("Failed to notify listeners of state change from %s to %s: %s", current, newState,
                            e.getMessage()),
                    e);
        }
    }

    private void notifyStateChangeListeners(AccessoryServerEvent event) throws ListenerNotificationException {
        List<AccessoryServerChangeListener> listeners;
        synchronized (stateLock) {
            listeners = new ArrayList<>(changeListeners);
        }

        List<AccessoryServerChangeListener> failedListeners = new ArrayList<>();
        List<AccessoryServerChangeListener> timedOutListeners = new ArrayList<>();
        List<Future<Void>> futures = new ArrayList<>();

        // Submit all notifications asynchronously
        for (AccessoryServerChangeListener listener : listeners) {
            Future<Void> future = eventExecutor.submit(() -> {
                try {
                    notifyListenerWithTimeout(listener, event);
                    return null;
                } catch (Exception e) {
                    logger.error("{}Failed to notify listener {}: {}", LOG_ERROR, listener, e.getMessage(), e);
                    failedListeners.add(listener);
                    throw e;
                }
            });
            futures.add(future);
        }

        // Wait for all notifications to complete or timeout
        for (int i = 0; i < futures.size(); i++) {
            Future<Void> future = futures.get(i);
            AccessoryServerChangeListener listener = listeners.get(i);
            try {
                future.get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.warn("{}Listener {} timed out after {}ms", LOG_WARN, listener, EVENT_TIMEOUT_MS);
                timedOutListeners.add(listener);
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                logger.warn("{}Interrupted while waiting for listener {} notification", LOG_WARN, listener);
                failedListeners.add(listener);
                future.cancel(true);
            } catch (Exception e) {
                logger.error("{}Error waiting for listener {} notification: {}", LOG_ERROR, listener, e.getMessage(),
                        e);
                failedListeners.add(listener);
            }
        }

        // Handle failures
        if (!failedListeners.isEmpty() || !timedOutListeners.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Failed to notify some listeners: ");
            if (!failedListeners.isEmpty()) {
                errorMessage.append("Failed listeners: ").append(failedListeners);
            }
            if (!timedOutListeners.isEmpty()) {
                if (!failedListeners.isEmpty()) {
                    errorMessage.append(", ");
                }
                errorMessage.append("Timed out listeners: ").append(timedOutListeners);
            }
            throw new ListenerNotificationException(errorMessage.toString());
        }
    }

    private void notifyListenerWithTimeout(AccessoryServerChangeListener listener, AccessoryServerEvent event)
            throws ListenerNotificationException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                listener.onAccessoryServerEvent(event);
            } catch (ListenerNotificationException e) {
                throw new CompletionException(e);
            } catch (RuntimeException e) {
                throw new CompletionException(new ListenerNotificationException(
                        String.format("Listener %s threw unexpected runtime exception: %s", listener, e.getMessage()),
                        e));
            }
        }, eventExecutor);

        try {
            future.get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ListenerNotificationException(String
                    .format("Listener %s failed to process event within %dms timeout", listener, EVENT_TIMEOUT_MS), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ListenerNotificationException) {
                throw (ListenerNotificationException) e.getCause();
            }
            throw new ListenerNotificationException(
                    String.format("Listener %s failed to process event: %s", listener, e.getCause().getMessage()),
                    e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            future.cancel(true);
            throw new ListenerNotificationException(
                    String.format("Interrupted while waiting for listener %s to process event", listener), e);
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
    public void setConfigurationIndex(int newIndex) throws ConfigurationException {
        synchronized (stateLock) {
            validateConfigurationIndex(newIndex);
            int oldIndex = configurationIndex;
            try {
                configurationIndex = newIndex;
                notifyChangeListeners(AccessoryServerEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED);
                logger.info("{}Configuration index updated from {} to {}", LOG_CONFIG, oldIndex, newIndex);
            } catch (Exception e) {
                // Rollback on failure
                configurationIndex = oldIndex;
                throw new ConfigurationException(
                        String.format("Failed to update configuration index from %d to %d", oldIndex, newIndex), e);
            }
        }
    }

    private void validateConfigurationIndex(int newIndex) throws ConfigurationException {
        if (newIndex <= 0) {
            throw new ConfigurationException("Configuration index must be positive");
        }
        if (newIndex == configurationIndex) {
            throw new ConfigurationException("New configuration index is the same as current index");
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
        return hp != null ? hp.getPublicKey() : null;
    }

    // ========== Setup Code Management Methods ==========
    @Override
    public String getSetupCode() {
        logger.debug("{}Getting setup code", LOG_CONFIG);
        return setupCode;
    }

    @Override
    public void setSetupCode(String setupCode) {
        if (setupCode == null || setupCode.length() != 8 || !setupCode.matches("\\d{8}")) {
            String error = "Setup code must be an 8-digit number";
            logger.error("{}Setup code validation error: {}", LOG_ERROR, error);
            throw new IllegalArgumentException(error);
        }
        logger.debug("{}Setting setup code", LOG_CONFIG);
        this.setupCode = setupCode;
    }

    // ========== Pairing Management Methods ==========
    @Override
    public void addPairing(byte @NonNull [] pairingId, byte @NonNull [] publicKey) throws HomekitServerException {
        if (pairingId == null || pairingId.length == 0) {
            String error = "Pairing ID cannot be null or empty";
            logger.error("{}Pairing validation error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }
        if (publicKey == null || publicKey.length == 0) {
            throw new HomekitServerException("Public key cannot be null or empty");
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
        } catch (Exception e) {
            String error = String.format("Failed to add pairing %s: %s", Byte.toHexString(pairingId), e.getMessage());
            logger.error("{}Pairing addition error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    @Override
    public Pairing getPairing(byte @NonNull [] pairingId) {
        logger.debug("{}Getting pairing for ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));
        return pairingRegistry.get(new PairingUID(getPairingId(), pairingId));
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
            if (pairingRegistry.remove(uid)) {
                setState(AccessoryServerState.UNPAIRED);
                logger.info("{}Pairing removed successfully - ID: {}", LOG_PAIRING, Byte.toHexString(pairingId));
            }
        } catch (Exception e) {
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
            } else {
                setState(AccessoryServerState.PAIRED);
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
        if (accessoryId < 0) {
            logger.warn("{}Invalid accessory ID: {}", LOG_WARN, accessoryId);
            return null;
        }
        synchronized (accessoryLock) {
            return accessories.stream().filter(accessory -> accessory.getAccessoryId() == accessoryId).findFirst()
                    .orElse(null);
        }
    }

    @Override
    public void addAccessory(Accessory accessory) throws AccessoryOperationException {
        validateAccessory(accessory);
        validateLifecycleOperation("add accessory");

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
                    notifyChangeListeners(AccessoryServerEventType.ACCESSORY_ADDED);
                    logger.info("{}Accessory added successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
                } catch (AccessoryOperationException e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{}Failed to add accessory - UID: {}, Error: {}", LOG_ERROR, accessory.getUID(),
                            e.getMessage());
                    throw new AccessoryOperationException(
                            String.format("Failed to add accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.remove(accessory);
                    logger.error("{}Failed to add accessory due to runtime error - UID: {}, Error: {}", LOG_ERROR,
                            accessory.getUID(), e.getMessage());
                    throw new AccessoryOperationException(
                            String.format("Failed to add accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                }
            } else {
                logger.warn("{}Accessory already exists - UID: {}", LOG_WARN, accessory.getUID());
                throw new AccessoryOperationException(
                        String.format("Accessory with UID %s already exists", accessory.getUID()));
            }
        }
    }

    @Override
    public void removeAccessory(Accessory accessory) throws AccessoryOperationException {
        validateAccessory(accessory);
        validateLifecycleOperation("remove accessory");
        validateAccessoryExists(accessory);

        synchronized (accessoryLock) {
            logger.debug("{}Removing accessory - UID: {}, Type: {}, Server: {}", LOG_ACCESSORY, accessory.getUID(),
                    accessory.getClass().getSimpleName(), this.getUID());

            if (accessories.remove(accessory)) {
                try {
                    accessoryRegistry.remove(accessory.getUID());
                    incrementConfigurationIndex();
                    advertise();
                    notifyChangeListeners(AccessoryServerEventType.ACCESSORY_REMOVED);
                    logger.info("{}Accessory removed successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
                } catch (AccessoryOperationException e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{}Failed to remove accessory - UID: {}, Error: {}", LOG_ERROR, accessory.getUID(),
                            e.getMessage());
                    throw new AccessoryOperationException(
                            String.format("Failed to remove accessory %s: %s", accessory.getUID(), e.getMessage()), e);
                } catch (RuntimeException e) {
                    // Rollback on failure
                    accessories.add(accessory);
                    logger.error("{}Failed to remove accessory due to runtime error - UID: {}, Error: {}", LOG_ERROR,
                            accessory.getUID(), e.getMessage());
                    throw new AccessoryOperationException(
                            String.format("Failed to remove accessory %s due to runtime error: %s", accessory.getUID(),
                                    e.getMessage()),
                            e);
                }
            } else {
                logger.warn("{}Accessory not found - UID: {}", LOG_WARN, accessory.getUID());
                throw new AccessoryOperationException(
                        String.format("Accessory with UID %s not found", accessory.getUID()));
            }
        }
    }

    private void validateAccessoryIdUniqueness(Accessory accessory) throws AccessoryOperationException {
        synchronized (accessoryLock) {
            if (accessories.stream().anyMatch(a -> a.getAccessoryId() == accessory.getAccessoryId())) {
                Accessory existing = accessories.stream().filter(a -> a.getAccessoryId() == accessory.getAccessoryId())
                        .findFirst().orElse(null);
                throw new AccessoryOperationException(String.format(
                        "Cannot add accessory with ID %d - an accessory with this ID already exists (UID: %s, Type: %s)",
                        accessory.getAccessoryId(), existing != null ? existing.getUID() : "unknown",
                        existing != null ? existing.getClass().getSimpleName() : "unknown"));
            }
        }
    }

    private void validateAccessoryUidUniqueness(Accessory accessory) throws AccessoryOperationException {
        synchronized (accessoryLock) {
            if (accessories.stream().anyMatch(a -> a.getUID().equals(accessory.getUID()))) {
                Accessory existing = accessories.stream().filter(a -> a.getUID().equals(accessory.getUID())).findFirst()
                        .orElse(null);
                throw new AccessoryOperationException(String.format(
                        "Cannot add accessory with UID %s - an accessory with this UID already exists (ID: %d, Type: %s)",
                        accessory.getUID(), existing != null ? existing.getAccessoryId() : -1,
                        existing != null ? existing.getClass().getSimpleName() : "unknown"));
            }
        }
    }

    private void validateAccessory(Accessory accessory) throws AccessoryOperationException {
        if (accessory == null) {
            throw new AccessoryOperationException("Cannot add/remove null accessory - accessory object is required");
        }
        if (accessory.getAccessoryId() < 0) {
            throw new AccessoryOperationException(String
                    .format("Invalid accessory ID %d - must be a non-negative number", accessory.getAccessoryId()));
        }
        if (accessory.getUID() == null || accessory.getUID().isEmpty()) {
            throw new AccessoryOperationException(
                    String.format("Invalid accessory UID for accessory ID %d - UID cannot be null or empty",
                            accessory.getAccessoryId()));
        }
        validateAccessoryIdUniqueness(accessory);
        validateAccessoryUidUniqueness(accessory);
    }

    private void validateAccessoryExists(Accessory accessory) throws AccessoryOperationException {
        synchronized (stateLock) {
            if (!accessories.contains(accessory)) {
                throw new AccessoryOperationException(String.format(
                        "Cannot remove accessory with ID %d - accessory not found", accessory.getAccessoryId()));
            }
        }
    }

    // ========== Event Listener Management Methods ==========
    @Override
    public void addChangeListener(AccessoryServerChangeListener listener) throws ListenerNotificationException {
        validateListener(listener);
        logger.debug("{}Adding change listener: {}", LOG_EVENT, listener);
        synchronized (stateLock) {
            if (!changeListeners.add(listener)) {
                logger.warn("{}Listener already registered: {}", LOG_WARN, listener);
            }
        }
    }

    @Override
    public void removeChangeListener(AccessoryServerChangeListener listener) throws ListenerNotificationException {
        validateListener(listener);
        logger.debug("{}Removing change listener: {}", LOG_EVENT, listener);
        synchronized (stateLock) {
            if (!changeListeners.remove(listener)) {
                logger.warn("{}Listener not found: {}", LOG_WARN, listener);
            }
        }
    }

    private void validateListener(AccessoryServerChangeListener listener) throws ListenerNotificationException {
        if (listener == null) {
            throw new ListenerNotificationException("Cannot add/remove null listener - listener object is required");
        }
    }

    private void cleanupListeners() {
        synchronized (stateLock) {
            if (!changeListeners.isEmpty()) {
                logger.debug("{}Cleaning up {} listeners", LOG_EVENT, changeListeners.size());
                List<AccessoryServerChangeListener> listenersToRemove = new ArrayList<>(changeListeners);
                for (AccessoryServerChangeListener listener : listenersToRemove) {
                    try {
                        removeChangeListener(listener);
                    } catch (ListenerNotificationException e) {
                        logger.error("{}Failed to remove listener during cleanup: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }
                if (!changeListeners.isEmpty()) {
                    logger.warn("{}{} listeners remain after cleanup", LOG_WARN, changeListeners.size());
                }
            }
        }
    }

    protected void notifyChangeListeners(AccessoryServerEventType eventType) throws ListenerNotificationException {
        validateEventType(eventType);
        logger.debug("{}Notifying listeners of event type: {}", LOG_EVENT, eventType);
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null, eventType);
        notifyListenersAsync(event);
    }

    protected void notifyChangeListeners() throws ListenerNotificationException {
        logger.debug("{}Notifying listeners of server update", LOG_EVENT);
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null,
                AccessoryServerEvent.AccessoryServerEventType.SERVER_UPDATED);
        notifyListenersAsync(event);
    }

    private void notifyListenersAsync(AccessoryServerEvent event) throws ListenerNotificationException {
        List<Future<Void>> futures = new ArrayList<>();
        List<AccessoryServerChangeListener> failedListeners = new ArrayList<>();
        List<AccessoryServerChangeListener> timedOutListeners = new ArrayList<>();

        // Sort listeners by priority (if they implement PrioritizedListener)
        List<AccessoryServerChangeListener> sortedListeners = changeListeners.stream().sorted((l1, l2) -> {
            int p1 = (l1 instanceof PrioritizedListener) ? ((PrioritizedListener) l1).getPriority() : 0;
            int p2 = (l2 instanceof PrioritizedListener) ? ((PrioritizedListener) l2).getPriority() : 0;
            return Integer.compare(p2, p1); // Higher priority first
        }).collect(Collectors.toList());

        for (AccessoryServerChangeListener listener : sortedListeners) {
            Future<Void> future = eventExecutor.submit(() -> {
                try {
                    notifyListenerWithTimeout(listener, event);
                    return null;
                } catch (Exception e) {
                    logger.error("{}Failed to notify listener {}: {}", LOG_ERROR, listener, e.getMessage(), e);
                    failedListeners.add(listener);
                    throw e;
                }
            });
            futures.add(future);
        }

        // Wait for all notifications to complete or timeout
        for (int i = 0; i < futures.size(); i++) {
            Future<Void> future = futures.get(i);
            AccessoryServerChangeListener listener = sortedListeners.get(i);
            try {
                future.get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.warn("{}Listener {} timed out after {}ms", LOG_WARN, listener, EVENT_TIMEOUT_MS);
                timedOutListeners.add(listener);
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                logger.warn("{}Interrupted while waiting for listener {} notification", LOG_WARN, listener);
                failedListeners.add(listener);
                future.cancel(true);
            } catch (Exception e) {
                logger.error("{}Error waiting for listener {} notification: {}", LOG_ERROR, listener, e.getMessage(),
                        e);
                failedListeners.add(listener);
            }
        }

        // Handle failures
        if (!failedListeners.isEmpty() || !timedOutListeners.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Failed to notify some listeners: ");
            if (!failedListeners.isEmpty()) {
                errorMessage.append("Failed listeners: ").append(failedListeners);
            }
            if (!timedOutListeners.isEmpty()) {
                if (!failedListeners.isEmpty()) {
                    errorMessage.append(", ");
                }
                errorMessage.append("Timed out listeners: ").append(timedOutListeners);
            }
            throw new ListenerNotificationException(errorMessage.toString());
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

    public abstract void advertise();

    private void validateEventType(AccessoryServerEventType eventType) throws ListenerNotificationException {
        if (eventType == null) {
            throw new ListenerNotificationException(
                    "Cannot notify listeners with null event type - valid event type is required");
        }
    }

    protected void handleConnection(boolean connected) throws HomekitServerException {
        logger.debug("{}Handling connection - Connected: {}", LOG_STATE, connected);
        synchronized (stateLock) {
            if (connected) {
                setState(AccessoryServerState.CONNECTED);
            } else {
                setState(AccessoryServerState.DISCONNECTED);
            }
        }
    }
}
