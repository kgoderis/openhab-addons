package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
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
public abstract class AbstractAccessoryServer implements AccessoryServer {

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

    // ========== Constants and Fields ==========
    protected static final Logger logger = LoggerFactory.getLogger(AbstractAccessoryServer.class);
    protected static final String SERVICE_TYPE = "_hap._tcp.local.";

    protected final AccessoryRegistry accessoryRegistry;
    protected final PairingRegistry pairingRegistry;
    protected final InetAddress address;
    protected final int port;
    protected final byte[] pairingIdentifier;
    protected final byte[] secretKey;
    protected String setupCode;
    protected int configurationIndex = 1;
    private final AccessoryCategory category;
    private final Collection<AccessoryServerChangeListener> changeListeners = new CopyOnWriteArraySet<>();
    private final Collection<Accessory> accessories = new CopyOnWriteArraySet<>();
    protected AccessoryServerState currentState = AccessoryServerState.UNKNOWN;

    /** Server state management */
    private final Object stateLock = new Object();

    // ========== State Management ==========
    private static final Map<AccessoryServerState, Set<AccessoryServerState>> VALID_STATE_TRANSITIONS = Map.of(
        AccessoryServerState.UNPAIRED, Set.of(AccessoryServerState.PAIRED, AccessoryServerState.RESET),
        AccessoryServerState.PAIRED, Set.of(AccessoryServerState.UNPAIRED, AccessoryServerState.PAIR_VERIFIED, AccessoryServerState.DISCONNECTED),
        AccessoryServerState.PAIR_VERIFIED, Set.of(AccessoryServerState.PAIRED, AccessoryServerState.DISCONNECTED),
        AccessoryServerState.DISCONNECTED, Set.of(AccessoryServerState.CONNECTED, AccessoryServerState.PAIRED),
        AccessoryServerState.CONNECTED, Set.of(AccessoryServerState.DISCONNECTED),
        AccessoryServerState.RESET, Set.of(AccessoryServerState.UNPAIRED)
    );

     // ========== Constructor ==========
    public AbstractAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingId,
            byte[] privateKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry) throws HomekitServerException {
        super();
        if (category == null) {
            throw new HomekitServerException("Category cannot be null");
        }
        if (address == null) {
            throw new HomekitServerException("Address cannot be null");
        }
        if (port <= 0 || port > 65535) {
            throw new HomekitServerException("Port must be between 1 and 65535");
        }
        if (pairingId == null || pairingId.length == 0) {
            throw new HomekitServerException("Pairing ID cannot be null or empty");
        }
        if (privateKey == null || privateKey.length == 0) {
            throw new HomekitServerException("Private key cannot be null or empty");
        }
        if (accessoryRegistry == null) {
            throw new HomekitServerException("Accessory registry cannot be null");
        }
        if (pairingRegistry == null) {
            throw new HomekitServerException("Pairing registry cannot be null");
        }

        logger.debug("{}Initializing HomeKit server - Category: {}, Address: {}, Port: {}", LOG_INIT, category, address, port);
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

    @Override
    public void activate(Map<String, Object> config) {
        try {
            logger.debug("Activating HomeKit server with config: {}", config);
            this.config = config;
            validateConfiguration();
            initializeServer();
            logger.info("HomeKit server activated successfully");
        } catch (ConfigurationException e) {
            logger.error("Failed to activate HomeKit server due to configuration error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to activate HomeKit server: {}", e.getMessage(), e);
            throw new HomekitServerException("Failed to activate HomeKit server", e);
        }
    }

    private void validateConfiguration() throws ConfigurationException {
        try {
            if (config == null) {
                throw new ConfigurationException("Configuration is null");
            }
            
            String pin = (String) config.get("pin");
            if (pin == null || pin.length() != 8) {
                throw new ConfigurationException("Invalid PIN configuration. PIN must be 8 digits");
            }
            
            String networkInterface = (String) config.get("networkInterface");
            if (networkInterface == null || networkInterface.isEmpty()) {
                throw new ConfigurationException("Network interface not specified");
            }
        } catch (ClassCastException e) {
            throw new ConfigurationException("Invalid configuration type", e);
        }
    }

    private void initializeServer() throws HomekitServerException {
        try {
            logger.debug("Initializing HomeKit server");
            server = new HomekitServer(networkInterface, port);
            server.start();
            logger.info("HomeKit server initialized successfully on port {}", port);
        } catch (IOException e) {
            throw new HomekitServerException("Failed to initialize HomeKit server", e);
        }
    }

    @Override
    public void deactivate() {
        try {
            logger.debug("Deactivating HomeKit server");
            if (server != null) {
                server.stop();
                server = null;
            }
            logger.info("HomeKit server deactivated successfully");
        } catch (Exception e) {
            logger.error("Error during HomeKit server deactivation: {}", e.getMessage(), e);
            throw new HomekitServerException("Failed to deactivate HomeKit server", e);
        }
    }

    protected synchronized void setState(AccessoryServerState newState) throws HomekitServerException {
        if (newState == null) {
            throw new HomekitServerException("New state cannot be null");
        }
        
        if (!isValidStateTransition(currentState, newState)) {
            String error = String.format("Invalid state transition from %s to %s", currentState, newState);
            logger.error("{}State transition error: {}", LOG_ERROR, error);
            throw new HomekitServerException(error);
        }

        if (!currentState.equals(newState)) {
            logger.debug("{}State changing from {} to {}", LOG_STATE, currentState, newState);
            AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null, newState.getEventType());
            currentState = newState;

            for (AccessoryServerChangeListener listener : changeListeners) {
                try {
                    listener.onAccessoryServerEvent(event);
                } catch (Exception e) {
                    logger.error("{}Failed to notify listener {} of state change: {}", LOG_ERROR, listener, e.getMessage(), e);
                    throw new HomekitServerException("Failed to notify listener of state change", e);
                }
            }
            logger.debug("{}State change completed", LOG_STATE);
        }
    }

    private boolean isValidStateTransition(AccessoryServerState current, AccessoryServerState next) {
        Set<AccessoryServerState> validNextStates = VALID_STATE_TRANSITIONS.get(current);
        return validNextStates != null && validNextStates.contains(next);
    }

    protected void handleConnection(boolean connected) {
        logger.debug("{}Handling connection - Connected: {}", LOG_STATE, connected);
        if (connected) {
            setState(AccessoryServerState.CONNECTED);
        } else {
            setState(AccessoryServerState.DISCONNECTED);
        }
    }

    protected void handlePairingVerification(boolean verified) {
        logger.debug("{}Handling pairing verification - Verified: {}", LOG_STATE, verified);
        if (verified) {
            setState(AccessoryServerState.PAIR_VERIFIED);
        } else {
            setState(AccessoryServerState.PAIRED);
        }
    }

    // ========== Server Configuration ==========
    @Override
    public int getConfigurationIndex() {
        logger.debug("{}Getting configuration index: {}", LOG_CONFIG, configurationIndex);
        return configurationIndex;
    }

    @Override
    public void setConfigurationIndex(int configurationIndex) {
        logger.debug("{}Setting configuration index from {} to {}", LOG_CONFIG, this.configurationIndex, configurationIndex);
        this.configurationIndex = configurationIndex;
        notifyChangeListeners(AccessoryServerEventType.SERVER_STATE_CONFIGURATION_NUMBER_CHANGED);
    }

    @Override
    public void factoryReset() {
        logger.debug("{}Performing factory reset", LOG_CONFIG);
        setState(AccessoryServerState.RESET);
    }

    // ========== Server Identity and Network ==========
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
        logger.debug("{}Getting public key for destination pairing ID: {}", LOG_CONFIG, Byte.toHexString(destinationPairingId));
        Pairing hp = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingId));
        return hp != null ? hp.getPublicKey() : null;
    }

    // ========== Setup Code Management ==========
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

    // ========== Accessory Management ==========
    @Override
    public Collection<Accessory> getAccessories() {
        logger.debug("{}Getting all accessories", LOG_ACCESSORY);
        return Collections.unmodifiableList(accessories.stream()
                .sorted((o1, o2) -> Long.valueOf(o1.getAccessoryId()).compareTo(Long.valueOf(o2.getAccessoryId())))
                .collect(Collectors.toList()));
    }

    @Override
    public @Nullable Accessory getAccessory(int acessoryId) {
        logger.debug("{}Getting accessory with ID: {}", LOG_ACCESSORY, acessoryId);
        return accessories.stream().filter(accessory -> accessory.getAccessoryId() == acessoryId).findFirst()
                .orElse(null);
    }

    @Override
    public void addAccessory(Accessory accessory) throws HomekitServerException {
        if (accessory == null) {
            throw new HomekitServerException("Accessory cannot be null");
        }

        // Validate accessory ID uniqueness
        if (accessories.stream().anyMatch(a -> a.getAccessoryId() == accessory.getAccessoryId())) {
            throw new HomekitServerException("Accessory ID " + accessory.getAccessoryId() + " already exists");
        }

        logger.debug("{}Adding accessory - UID: {}, Type: {}, Server: {}", LOG_ACCESSORY, accessory.getUID(),
                accessory.getClass().getSimpleName(), this.getUID());

        if (accessories.add(accessory)) {
            try {
                if (accessoryRegistry.update(accessory) == null) {
                    accessoryRegistry.add(accessory);
                }
                configurationIndex++;
                advertise();
                notifyChangeListeners(AccessoryServerEventType.ACCESSORY_ADDED);
                logger.info("{}Accessory added successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
            } catch (Exception e) {
                accessories.remove(accessory);
                throw new HomekitServerException("Failed to add accessory " + accessory.getUID() + ": " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void removeAccessory(Accessory accessory) throws HomekitServerException {
        if (accessory == null) {
            throw new HomekitServerException("Accessory cannot be null");
        }

        logger.debug("{}Removing accessory - UID: {}, Type: {}, Server: {}", LOG_ACCESSORY, accessory.getUID(),
                accessory.getClass().getSimpleName(), this.getUID());

        if (accessories.remove(accessory)) {
            try {
                accessoryRegistry.remove(accessory.getUID());
                configurationIndex++;
                advertise();
                notifyChangeListeners(AccessoryServerEventType.ACCESSORY_REMOVED);
                logger.info("{}Accessory removed successfully - UID: {}", LOG_ACCESSORY, accessory.getUID());
            } catch (Exception e) {
                accessories.add(accessory);
                throw new HomekitServerException("Failed to remove accessory " + accessory.getUID() + ": " + e.getMessage(), e);
            }
        }
    }

    // ========== Pairing Management ==========
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
            String error = String.format("Failed to remove pairing %s: %s", Byte.toHexString(pairingId), e.getMessage());
            logger.error("{}Pairing removal error: {}", LOG_ERROR, error, e);
            throw new HomekitServerException(error, e);
        }
    }

    @Override
    public boolean isPaired() {
        logger.debug("{}Checking pairing status", LOG_PAIRING);
        boolean paired = !pairingRegistry.get(getPairingId()).isEmpty();
        if (paired) {
            setState(AccessoryServerState.PAIRED);
        } else {
            setState(AccessoryServerState.UNPAIRED);
        }
        return paired;
    }

    // ========== Event Listener Management ==========
    @Override
    public void addChangeListener(AccessoryServerChangeListener listener) throws HomekitServerException {
        if (listener == null) {
            throw new HomekitServerException("Listener cannot be null");
        }
        logger.debug("{}Adding change listener: {}", LOG_EVENT, listener);
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(AccessoryServerChangeListener listener) throws HomekitServerException {
        if (listener == null) {
            throw new HomekitServerException("Listener cannot be null");
        }
        logger.debug("{}Removing change listener: {}", LOG_EVENT, listener);
        changeListeners.remove(listener);
    }

    protected synchronized void notifyChangeListeners(AccessoryServerEventType eventType) throws HomekitServerException {
        if (eventType == null) {
            throw new HomekitServerException("Event type cannot be null");
        }
        logger.debug("{}Notifying listeners of event type: {}", LOG_EVENT, eventType);
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null, eventType);
        for (AccessoryServerChangeListener listener : changeListeners) {
            try {
                listener.onAccessoryServerEvent(event);
            } catch (Exception e) {
                logger.error("{}Failed to notify listener {} of event {}: {}", LOG_ERROR, listener, eventType, e.getMessage(), e);
                throw new HomekitServerException("Failed to notify listener of event", e);
            }
        }
    }

    protected void notifyChangeListeners() throws HomekitServerException {
        logger.debug("{}Notifying listeners of server update", LOG_EVENT);
        AccessoryServerEvent event = new AccessoryServerEvent(this, null, null, null,
                AccessoryServerEvent.AccessoryServerEventType.SERVER_UPDATED);
        for (AccessoryServerChangeListener listener : this.changeListeners) {
            try {
                listener.onAccessoryServerEvent(event);
            } catch (Throwable throwable) {
                logger.error("{}Failed to notify listener {}: {}", LOG_ERROR, listener, throwable.getMessage(), throwable);
                throw new HomekitServerException("Failed to notify listener", throwable);
            }
        }
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
    public void start() throws HomekitServerException {
        synchronized (stateLock) {
            if (currentState == AccessoryServerState.READY) {
                logger.warn("{}Server is already running", LOG_SERVER);
                return;
            }
            if (currentState == AccessoryServerState.STOPPED) {
                logger.warn("{}Server is in stopped state, attempting to restart", LOG_SERVER);
            }
            setState(AccessoryServerState.READY);
            logger.info("{}Server started successfully", LOG_SERVER);
        }
    }

    @Override
    public void stop() {
        synchronized (stateLock) {
            if (currentState == AccessoryServerState.STOPPED) {
                logger.warn("{}Server is already stopped", LOG_SERVER);
                return;
            }
            setState(AccessoryServerState.STOPPED);
            logger.info("{}Server stopped successfully", LOG_SERVER);
        }
    }

    // ========== Abstract Methods ==========
    public abstract void advertise();
}
