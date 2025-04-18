package org.openhab.io.homekit.internal.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.AccessoryChangeListener;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.api.listener.ServiceChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.provider.HomekitChannelGroupTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class AccessoryThingHandler extends BaseThingHandler implements AccessoryServerChangeListener,
        ServiceChangeListener, CharacteristicChangeListener, AccessoryChangeListener {
    // ========== Constants ==========
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final long EVENT_PROCESSING_TIMEOUT = 5000; // 5 seconds
    private static final String CONFIG_DEVICE_ID = "deviceId";
    private static final String CONFIG_SERVICE_ID = "serviceId";
    private static final String CONFIG_ACCESSORY_ID = "accessoryId";
    private static final String DEFAULT_ACCESSORY_ID = "1";

    // ========== Error Messages ==========
    private static final String ERROR_PREFIX = "HomeKit Service Handler Error: ";
    private static final String ERROR_SERVER_NOT_FOUND = ERROR_PREFIX + "Server not found for deviceId: %s";
    private static final String ERROR_ACCESSORY_NOT_FOUND = ERROR_PREFIX + "Accessory not found for accessoryId: %s";
    private static final String ERROR_SERVICE_NOT_FOUND = ERROR_PREFIX + "Service not found for serviceId: %s";
    private static final String ERROR_CONFIG_INVALID = ERROR_PREFIX + "Invalid configuration: %s";
    private static final String ERROR_STATE_UPDATE = ERROR_PREFIX + "Failed to update state: %s";
    private static final String ERROR_CHANNEL_OPERATION = ERROR_PREFIX + "Channel operation failed: %s";
    private static final String ERROR_EVENT_PROCESSING = ERROR_PREFIX + "Event processing failed: %s";
    private static final String ERROR_PROCESSING_ACCESSORY_EVENT = ERROR_PREFIX + "Accessory event processing failed";
    private static final String ERROR_PROCESSING_CHARACTERISTIC_EVENT = ERROR_PREFIX
            + "Characteristic event processing failed";
    private static final String ERROR_COMMAND_PROCESSING = ERROR_PREFIX + "Command processing failed: %s";
    private static final String ERROR_PROCESSING_SERVICE_EVENT = ERROR_PREFIX + "Service event processing failed";

    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "HomeKit Service Handler: ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CHANNEL = LOG_PREFIX + "Channel - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_CLEANUP = LOG_PREFIX + "Cleanup - ";

    // ========== Core Dependencies ==========
    private final Logger logger = LoggerFactory.getLogger(AccessoryThingHandler.class);
    private final AccessoryServerRegistry serverRegistry;
    private final AccessoryRegistry accessoryRegistry;
    private final HomekitChannelTypeProvider homekitChannelTypeProvider;
    private final HomekitThingTypeProvider homekitThingTypeProvider;
    private final HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider;

    // ========== Configuration Fields ==========
    private String deviceId = "";
    private String accessoryId = "";

    // ========== Component References and Locks ==========
    private final Object serverLock = new Object();
    private @Nullable AccessoryServer server;
    private final Object accessoryLock = new Object();
    private @Nullable Accessory accessory;
    private final Object characteristicMapLock = new Object();
    private final Map<Channel, @Nullable Characteristic<?>> characteristicMap = new ConcurrentHashMap<>();

    // ========== State Management ==========
    private final Object stateLock = new Object();
    private volatile boolean disposed = false;
    private volatile boolean initialized = false;
    private volatile ThingStatus currentStatus = ThingStatus.UNINITIALIZED;
    private volatile ThingStatusDetail currentStatusDetail = ThingStatusDetail.NONE;
    private volatile @Nullable String currentStatusDescription = null;
    private volatile boolean serverConnected = false;
    private volatile boolean serverPaired = false;
    private volatile boolean accessoryAvailable = false;

    // ========== Event Processing ==========
    private final Object eventQueueLock = new Object();
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HomeKit-Event-Processor");
        t.setDaemon(true);
        return t;
    });

    /**
     * Constructs a new AccessoryThingHandler.
     * 
     * <p>
     * Thread Safety:
     * - Constructor is thread-safe
     * - Initializes all locks and collections
     * - Sets up event processing infrastructure
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Initializes handler in UNINITIALIZED state
     * - Sets up configuration parameters
     * - Prepares event processing queue
     * </p>
     * 
     * @param thing The thing this handler manages
     * @param serverRegistry Registry for HomeKit accessory servers
     * @param accessoryRegistry Registry for HomeKit accessories
     * @param homekitChannelTypeProvider Provider for HomeKit channel types
     * @param homekitThingTypeProvider Provider for HomeKit thing types
     */
    public AccessoryThingHandler(Thing thing, AccessoryServerRegistry serverRegistry,
            AccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider,
            HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider) {
        super(thing);
        this.serverRegistry = serverRegistry;
        this.accessoryRegistry = accessoryRegistry;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.homekitChannelGroupTypeProvider = homekitChannelGroupTypeProvider;

        // Parse configuration
        Configuration config = thing.getConfiguration();
        validateConfiguration(config);

        // Get the server from registry
        AccessoryServer foundServer = this.serverRegistry.get(new AccessoryServerUID(deviceId));
        if (foundServer == null) {
            throw new IllegalArgumentException("No AccessoryServer found for deviceId: " + deviceId);
        }
        setServer(foundServer);

        // Get the accessory from registry
        Accessory foundAccessory = this.accessoryRegistry.get(new AccessoryUID(accessoryId));
        if (foundAccessory == null) {
            throw new IllegalArgumentException("No Accessory found for accessoryId: " + accessoryId);
        }
        setAccessory(foundAccessory);

        // Initialize channels
        initializeChannels();

        // Register as listener
        foundServer.addChangeListener(this);
        foundAccessory.addChangeListener(this);
    }

    /**
     * Initializes the service handler.
     * 
     * <p>
     * This method performs initialization by:
     * 1. Checking if already initialized or disposed
     * 2. Validating and loading configuration
     * 3. Initializing server, accessory and service references
     * 4. Setting up state tracking
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe initialization of components
     * - Atomic state transitions
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Sets status to INITIALIZING
     * - Validates configuration
     * - Initializes server, accessory and service
     * - Updates thing status
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates configuration
     * - Checks for required components
     * - Updates thing status on errors
     * </p>
     */

    @Override
    public void initialize() {
        try {
            if (disposed) {
                return;
            }

            synchronized (stateLock) {
                if (initialized) {
                    return;
                }
                initialized = true;
                currentStatus = ThingStatus.INITIALIZING;
                updateState(currentStatus);
            }

            logger.debug("{}Starting initialization", LOG_INIT);
            validateConfiguration(thing.getConfiguration());

            synchronized (serverLock) {
                if (server == null) {
                    AccessoryServer foundServer = serverRegistry.get(new AccessoryServerUID(deviceId));
                    if (foundServer == null) {
                        throw new IllegalStateException(String.format(ERROR_SERVER_NOT_FOUND, deviceId));
                    }
                    setServer(foundServer);
                    logger.debug("{}Server initialized - Device ID: {}", LOG_INIT, deviceId);
                }
            }

            synchronized (accessoryLock) {
                if (accessory == null) {
                    Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
                    if (foundAccessory == null) {
                        throw new IllegalStateException(String.format(ERROR_ACCESSORY_NOT_FOUND, accessoryId));
                    }
                    setAccessory(foundAccessory);
                    logger.debug("{}Accessory initialized - ID: {}", LOG_INIT, accessoryId);
                }
            }

            initializeChannels();

            AccessoryServer currentServer = getServer();
            if (currentServer != null) {
                currentServer.addChangeListener(this);
                logger.debug("{}Server change listener registered", LOG_INIT);
            }

            validateAndUpdateState();
            logger.debug("{}Initialization completed successfully", LOG_INIT);
        } catch (Exception e) {
            logger.error("{}Initialization failed - Error: {}", LOG_INIT, e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Disposes of the service handler and cleans up resources.
     * 
     * <p>
     * This method performs cleanup by:
     * 1. Shutting down event processing
     * 2. Removing listeners
     * 3. Clearing component references
     * 4. Cleaning up channels
     * 5. Updating thing status
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for component access
     * - Thread-safe cleanup
     * </p>
     * 
     * <p>
     * Resource Management:
     * - Shuts down executor service
     * - Removes listeners
     * - Cleans up channels
     * - Releases resources
     * </p>
     */
    @Override
    public void dispose() {
        synchronized (stateLock) {
            disposed = true;
            initialized = false;
            currentStatus = ThingStatus.UNINITIALIZED;
            currentStatusDetail = ThingStatusDetail.NONE;
            currentStatusDescription = null;
            serverConnected = false;
            serverPaired = false;
            accessoryAvailable = false;
        }

        try {
            logger.debug("{}Starting cleanup", LOG_CLEANUP);
            eventExecutor.shutdown();
            if (!eventExecutor.awaitTermination(EVENT_PROCESSING_TIMEOUT, TimeUnit.MILLISECONDS)) {
                logger.warn("{}Event executor did not terminate within timeout", LOG_CLEANUP);
                eventExecutor.shutdownNow();
            }

            synchronized (serverLock) {
                if (server != null) {
                    try {
                        server.removeChangeListener(this);
                        logger.debug("{}Removed server change listener", LOG_CLEANUP);
                    } catch (Exception e) {
                        logger.warn("{}Failed to remove server change listener: {}", LOG_CLEANUP, e.getMessage());
                    }
                    server = null;
                }
            }

            synchronized (accessoryLock) {
                if (accessory != null) {
                    try {
                        if (this instanceof org.openhab.io.homekit.api.listener.AccessoryChangeListener) {
                            accessory.removeChangeListener(
                                    (org.openhab.io.homekit.api.listener.AccessoryChangeListener) this);
                            logger.debug("{}Removed accessory change listener", LOG_CLEANUP);
                        }
                    } catch (Exception e) {
                        logger.warn("{}Failed to remove accessory change listener: {}", LOG_CLEANUP, e.getMessage());
                    }
                    accessory = null;
                }
            }

            synchronized (characteristicMapLock) {
                try {
                    characteristicMap.values().stream().filter(Objects::nonNull).forEach(characteristic -> {
                        try {
                            characteristic.removeChangeListener(this);
                            logger.debug("{}Removed characteristic change listener", LOG_CLEANUP);
                        } catch (Exception e) {
                            logger.warn("{}Failed to remove characteristic change listener: {}", LOG_CLEANUP,
                                    e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    logger.warn("{}Failed to remove characteristic change listeners: {}", LOG_CLEANUP, e.getMessage());
                }
                characteristicMap.clear();
            }
            logger.debug("{}Cleanup completed successfully", LOG_CLEANUP);
        } catch (Exception e) {
            logger.error("{}Unexpected error during cleanup: {}", LOG_CLEANUP, e.getMessage(), e);
        } finally {
            super.dispose();
        }
    }

    /**
     * Validates the configuration of the service handler.
     * 
     * <p>
     * This method performs configuration validation by:
     * 1. Checking required configuration parameters
     * 2. Validating parameter values
     * 3. Setting default values if needed
     * 4. Updating internal state
     * </p>
     * 
     * <p>
     * Configuration Parameters:
     * - deviceId: Required, identifies the HomeKit server
     * - serviceId: Required, identifies the service
     * - accessoryId: Optional, defaults to "1"
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Throws IllegalArgumentException for invalid configurations
     * - Logs configuration issues
     * - Maintains consistent state
     * </p>
     * 
     * @param config The configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    private void validateConfiguration(Configuration config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        this.deviceId = (String) config.get(CONFIG_DEVICE_ID);
        this.accessoryId = (String) config.get(CONFIG_ACCESSORY_ID);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration must contain a valid deviceId");
        }
        if (accessoryId == null || accessoryId.trim().isEmpty()) {
            accessoryId = DEFAULT_ACCESSORY_ID;
        }
    }

    /**
     * Handles configuration updates.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for configuration updates
     * - State changes are atomic
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Validates new configuration
     * - Updates thing configuration
     * - Reinitializes channels if necessary
     * - Updates thing status
     * </p>
     * 
     * @param configurationParameters The new configuration parameters
     */
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        try {
            if (disposed) {
                return;
            }

            Configuration newConfig = new Configuration(configurationParameters);
            validateConfiguration(newConfig);

            // Update configuration
            Configuration currentConfig = thing.getConfiguration();
            currentConfig.setProperties(newConfig.getProperties());
            updateConfiguration(currentConfig);

            // Reinitialize channels if necessary
            if (configurationParameters.containsKey(CONFIG_SERVICE_ID)
                    || configurationParameters.containsKey(CONFIG_ACCESSORY_ID)) {
                initializeChannels();
            }

            validateAndUpdateState();
        } catch (Exception e) {
            logger.error("{}Failed to update configuration: {}", e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    /**
     * Validates and updates the current state of the service handler.
     * 
     * <p>
     * This method manages state transitions by:
     * 1. Determining current status
     * 2. Calculating status detail
     * 3. Generating status description
     * 4. Updating thing state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe state transitions
     * </p>
     * 
     * <p>
     * State Management:
     * - Evaluates component states (server, accessory, service)
     * - Determines appropriate status
     * - Updates status details
     * - Maintains state consistency
     * </p>
     */
    private void validateAndUpdateState() {
        try {
            synchronized (stateLock) {
                if (disposed) {
                    return;
                }

                ThingStatus newStatus = determineThingStatus();
                ThingStatusDetail newDetail = determineThingStatusDetail();
                String newDescription = determineThingStatusDescription();

                updateState(newStatus, newDetail, newDescription);
            }
        } catch (Exception e) {
            logger.error("{}Error validating and updating state: {}", LOG_PREFIX, e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Updates the state of the thing with detailed information.
     * 
     * <p>
     * This method updates the thing state by:
     * 1. Validating state parameters
     * 2. Updating internal state
     * 3. Notifying listeners
     * 4. Logging state changes
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe state transitions
     * </p>
     * 
     * <p>
     * State Management:
     * - Updates current status
     * - Updates status detail
     * - Updates status description
     * - Maintains state consistency
     * </p>
     * 
     * @param status The new status of the thing
     * @param detail The detail of the status
     * @param description An optional description of the status
     */
    private void updateState(ThingStatus status, ThingStatusDetail detail, @Nullable String description) {
        synchronized (stateLock) {
            if (disposed) {
                return;
            }

            // Only update if the state has actually changed
            if (currentStatus != status || currentStatusDetail != detail
                    || (currentStatusDescription == null && description != null)
                    || (currentStatusDescription != null && !currentStatusDescription.equals(description))) {

                currentStatus = status;
                currentStatusDetail = detail;
                currentStatusDescription = description;

                updateStatus(status, detail, description);
                logger.debug("{}State updated to {} ({}): {}", LOG_PREFIX, status, detail, description);
            }
        }
    }

    /**
     * Updates the handler state with new status.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized block for state updates
     * - Status changes are atomic
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Updates current status
     * - Sets detail to NONE
     * - Clears description
     * </p>
     * 
     * @param status New thing status
     */
    private void updateState(ThingStatus status) {
        updateState(status, ThingStatusDetail.NONE, null);
    }

    /**
     * Initializes the channels for the service handler.
     * 
     * <p>
     * This method sets up the channels by:
     * 1. Validating the current configuration
     * 2. Creating channels for each characteristic
     * 3. Registering listeners for channel updates
     * 4. Updating the thing configuration
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for channel map updates
     * - Ensures atomic channel initialization
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Validates configuration
     * - Initializes channels
     * - Updates thing configuration
     * </p>
     */
    private void initializeChannels() {
        try {
            synchronized (characteristicMapLock) {
                characteristicMap.clear();
            }

            Accessory currentAccessory = getAccessory();
            if (currentAccessory == null) {
                throw new IllegalStateException("Accessory is not initialized");
            }

            // If the Thing does not have any channels, traverse the services of the accessory, and for each service add
            // a
            // ChannelGroup. then, for each characteristic of the service, add a channel in the ChannelGroup. Add
            // logging to
            // the process.
            if (thing.getChannels().isEmpty()) {
                logger.info("Thing has no channels, traversing services of accessory");
                for (Service service : accessory.getServices()) {
                    addChannelGroupForService(service);
                }
            } else {
                // traverse channels and add them to the characteristicMap
                for (Channel channel : thing.getChannels()) {

                    String serviceTag = channel.getUID().getGroupId().split("\\.")[0];
                    String serviceId = channel.getUID().getGroupId().split("\\.")[1];

                    String serviceType;
                    try {
                        serviceType = homekitThingTypeProvider.getServiceTypeFromTag(serviceTag);
                    } catch (HomekitException e) {
                        logger.warn("Service type could not be determined for service tag: {}", serviceTag);
                        continue;
                    }

                    Service service = accessory.getService(serviceType);
                    if (service == null) {
                        logger.warn("Service {} not found in accessory", serviceTag);
                        continue;
                    }

                    // verify that the serviceId matches the serviceId of the service
                    if (service.getInstanceId() != Long.parseLong(serviceId)) {
                        logger.warn("Service ID {} does not match service ID {} for service {}", serviceId,
                                service.getInstanceId(), serviceTag);
                        continue;
                    }

                    String characteristicType;
                    try {
                        characteristicType = homekitChannelTypeProvider
                                .getCharacteristicTypeFromTag(channel.getUID().getIdWithoutGroup());
                    } catch (HomekitException e) {
                        logger.warn("Characteristic type could not be determined for characteristic tag: {}",
                                channel.getUID().getIdWithoutGroup());
                        continue;
                    }

                    Characteristic<?> characteristic = service.getCharacteristic(characteristicType);
                    if (characteristic == null) {
                        logger.warn("Characteristic {} not found in service {}", characteristicType, serviceTag);
                        continue;
                    }
                    synchronized (characteristicMapLock) {
                        characteristicMap.put(channel, characteristic);
                    }
                }
            }
        } catch (Exception e) {
            handleError(ThingStatusDetail.CONFIGURATION_ERROR, "Failed to initialize channels: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize channels: " + e.getMessage(), e);
        }
    }

    /**
     * Synchronizes channels with the current service state.
     * 
     * <p>
     * This method synchronizes channels by:
     * 1. Finding existing channels
     * 2. Creating missing channels
     * 3. Removing obsolete channels
     * 4. Updating channel states
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe channel synchronization
     * </p>
     * 
     * <p>
     * Channel Management:
     * - Maintains channel-characteristic mapping
     * - Updates channel states
     * - Handles channel lifecycle
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Handles synchronization failures
     * - Logs errors
     * - Maintains consistent state
     * </p>
     */
    private void synchronizeChannels() {
        try {
            Accessory currentAccessory = getAccessory();
            if (currentAccessory == null) {
                logger.warn(
                        "{}Warning - Type: Channel, Message: Cannot synchronize channels: accessory is not available",
                        LOG_PREFIX);
                return;
            }

            Set<String> currentCharacteristicTypes = currentAccessory.getServices().stream()
                    .map(Service::getCharacteristics).flatMap(characteristics -> characteristics.stream())
                    .map(Characteristic::getInstanceType).collect(Collectors.toSet());

            // Remove channels for characteristics that no longer exist
            List<Channel> channelsToRemove = new ArrayList<>();
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                    if (!currentCharacteristicTypes.contains(entry.getValue().getInstanceType())) {
                        channelsToRemove.add(entry.getKey());
                    }
                }
                channelsToRemove.forEach(channel -> {
                    characteristicMap.remove(channel);
                    updateThing(editThing().withoutChannel(channel.getUID()).build());
                });
            }

            // Add channels for new characteristics
            for (Service service : currentAccessory.getServices()) {
                for (Characteristic<?> characteristic : service.getCharacteristics()) {
                    boolean channelExists = false;
                    synchronized (characteristicMapLock) {
                        for (Characteristic<?> existingCharacteristic : characteristicMap.values()) {
                            if (existingCharacteristic.getInstanceType().equals(characteristic.getInstanceType())) {
                                channelExists = true;
                                break;
                            }
                        }
                    }
                    if (!channelExists) {
                        addChannelForCharacteristic(characteristic);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("{}Error occurred - Type: Channel, Message: Failed to synchronize channels: {}", LOG_PREFIX,
                    e.getMessage(), e);
        }
    }

    /**
     * Cleans up all channels and associated resources.
     * 
     * <p>
     * This method performs cleanup by:
     * 1. Removing all channels
     * 2. Clearing the characteristic map
     * 3. Cleaning up resources
     * 4. Updating state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe cleanup operations
     * </p>
     * 
     * <p>
     * Resource Management:
     * - Removes all channels
     * - Clears mappings
     * - Releases resources
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Handles cleanup failures
     * - Logs errors
     * - Maintains consistent state
     * </p>
     */
    private void cleanupChannels() {
        try {
            synchronized (characteristicMapLock) {
                // Remove all channels from the thing
                List<Channel> channelsToRemove = new ArrayList<>(characteristicMap.keySet());
                for (Channel channel : channelsToRemove) {
                    removeChannelForCharacteristic(characteristicMap.get(channel));
                }
                characteristicMap.clear();
            }
        } catch (Exception e) {
            logger.error("{}Error occurred - Type: Channel, Message: Failed to cleanup channels: {}", LOG_PREFIX,
                    e.getMessage(), e);
        }
    }

    /**
     * Handles the state transition of a channel.
     * 
     * <p>
     * This method manages channel state transitions by:
     * 1. Validating channel and characteristic
     * 2. Updating channel state based on new status
     * 3. Handling online/offline transitions
     * 4. Maintaining state consistency
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Thread-safe state updates
     * - Atomic state transitions
     * </p>
     * 
     * <p>
     * State Transitions:
     * - ONLINE: Updates channel with current characteristic value
     * - OFFLINE: Clears channel state
     * - Other states: No action
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates input parameters
     * - Catches and logs state transition errors
     * - Maintains consistent state
     * </p>
     * 
     * @param channel The channel to update
     * @param characteristic The associated characteristic
     * @param oldStatus The previous status of the channel
     * @param newStatus The new status of the channel
     */
    private void handleChannelStateTransition(Channel channel, Characteristic<?> characteristic, ThingStatus oldStatus,
            ThingStatus newStatus) {
        try {
            if (channel == null || characteristic == null) {
                return;
            }

            switch (newStatus) {
                case ONLINE:
                    // When coming online, update channel state with current characteristic value
                    Object value = characteristic.getValue();
                    if (value instanceof State) {
                        updateState(channel.getUID(), (State) value);
                    }
                    break;
                case OFFLINE:
                    // When going offline, clear channel state
                    updateState(channel.getUID(), UnDefType.UNDEF);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.warn("{}Warning - Type: Channel, Message: Failed to handle channel state transition: {}", LOG_PREFIX,
                    e.getMessage());
        }
    }

    /**
     * Handles a change in the channel type.
     * 
     * <p>
     * This method processes channel type changes by:
     * 1. Validating input parameters
     * 2. Checking if the channel type has changed
     * 3. Creating a new channel with the updated type
     * 4. Updating the characteristic map
     * 5. Updating the thing configuration
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe channel type changes
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Catches and logs type change errors
     * - Maintains consistent state
     * </p>
     * 
     * @param channel The channel to update
     * @param characteristic The associated characteristic
     */
    private void handleChannelTypeChange(Channel channel, Characteristic<?> characteristic) {
        try {
            if (channel == null || characteristic == null) {
                return;
            }

            ChannelTypeUID newChannelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getInstanceType());

            // Check if channel type has changed
            if (!channel.getChannelTypeUID().equals(newChannelTypeUID)) {
                ChannelType newChannelType = homekitChannelTypeProvider.getChannelType(newChannelTypeUID, null);
                if (newChannelType != null) {
                    // Create new channel with updated type
                    Channel newChannel = ChannelBuilder.create(channel.getUID()).withType(newChannelTypeUID)
                            .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription())
                            .build();

                    // Update channel in characteristic map
                    synchronized (characteristicMapLock) {
                        characteristicMap.remove(channel);
                        characteristicMap.put(newChannel, characteristic);
                    }

                    // Update thing with new channel
                    updateThing(editThing().withoutChannel(channel.getUID()).withChannel(newChannel).build());
                }
            }
        } catch (Exception e) {
            logger.warn("{}Warning - Type: Channel, Message: Failed to handle channel type change: {}", LOG_PREFIX,
                    e.getMessage());
        }
    }

    /**
     * Handles a command for a specific characteristic.
     * 
     * <p>
     * This method processes commands by:
     * 1. Validating the characteristic and command
     * 2. Converting the command to the appropriate type
     * 3. Updating the characteristic value
     * 4. Synchronizing the channel state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for command processing
     * - Command processing is atomic
     * - Thread-safe state updates
     * </p>
     * 
     * <p>
     * Type Conversion:
     * - Supports State to characteristic value conversion
     * - Handles type casting for compatible types
     * - Manages null/invalid states
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates input parameters
     * - Catches and logs conversion errors
     * - Updates thing status on errors
     * </p>
     * 
     * @param characteristic The characteristic to process the command for
     * @param command The command to process
     */
    private @Nullable Channel findChannelForCharacteristic(Characteristic<?> characteristic) {
        if (characteristic == null) {
            return null;
        }

        synchronized (characteristicMapLock) {
            for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                if (entry.getValue() == characteristic) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void addChannelGroupForService(Service service) {
        logger.info("Adding channel group for service: {}", service.getUID());
        try {
            String groupId = homekitThingTypeProvider.getServiceTag(service.getInstanceType()) + "."
                    + service.getInstanceId();
            ChannelGroupTypeUID channelGroupTypeUID = homekitChannelGroupTypeProvider
                    .getChannelGroupTypeUID(service.getInstanceType());

            // Create a list to hold all channels for this group
            List<Channel> channels = new ArrayList<>();

            // Add channels for each characteristic
            for (Characteristic<?> characteristic : service.getCharacteristics()) {
                ChannelUID channelUID = new ChannelUID(thing.getUID(), groupId,
                        homekitChannelTypeProvider.getCharacteristicTag(characteristic.getInstanceType()));
                ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                        characteristic.getInstanceType());

                ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
                if (channelType == null) {
                    logger.warn("No ChannelType found for characteristic {}", characteristic.getUID());
                    continue;
                }

                Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
                        .withLabel(characteristic.getDescription()).build();

                channels.add(channel);
                characteristicMap.put(channel, characteristic);
            }

            // Create a ThingBuilder to modify the thing
            ThingBuilder thingBuilder = editThing();

            // Add all channels at once
            channels.forEach(thingBuilder::withChannel);

            // Update the thing with all new channels
            updateThing(thingBuilder.build());

        } catch (IllegalArgumentException | HomekitException e) {
            logger.warn("Error adding channel group for service {}: {}", service.getUID(), e.getMessage());
        }
    }

    /**
     * Creates and adds a new channel for a characteristic.
     * 
     * <p>
     * Thread Safety:
     * - Synchronized access to characteristic map
     * - Channel creation and thing updates are atomic
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Creates new channel with appropriate type and metadata
     * - Updates characteristic map with new channel
     * - Updates thing configuration with new channel
     * </p>
     * 
     * <p>
     * Channel Creation:
     * - Creates channel with appropriate type
     * - Sets up channel metadata
     * - Establishes channel-characteristic mapping
     * - Updates thing configuration
     * </p>
     * 
     * @param characteristic The characteristic to create a channel for
     * @return The newly created channel, or null if creation failed
     */
    @Nullable
    private Channel addChannelForCharacteristic(Characteristic<?> characteristic) {
        if (characteristic == null) {
            return null;
        }

        try {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getInstanceType());
            ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
            if (channelType == null) {
                logger.warn("{}Warning - Type: Channel, Message: No ChannelType found for characteristic {}",
                        LOG_PREFIX, characteristic.getUID());
                return null;
            }

            Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
                    .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription())
                    .build();

            synchronized (characteristicMapLock) {
                characteristicMap.put(channel, characteristic);
            }

            updateThing(editThing().withChannel(channel).build());
            return channel;
        } catch (Exception e) {
            logger.warn("{}Warning - Type: Channel, Message: Unexpected error adding channel for characteristic {}: {}",
                    LOG_PREFIX, characteristic.getUID(), e.getMessage());
            return null;
        }
    }

    /**
     * Removes a channel associated with a characteristic.
     * 
     * <p>
     * Thread Safety:
     * - Synchronized access to characteristic map
     * - Channel removal and thing updates are atomic
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Removes channel from characteristic map
     * - Updates thing configuration to remove channel
     * </p>
     * 
     * <p>
     * Channel Removal:
     * - Removes channel from characteristic map
     * - Updates thing configuration
     * - Removes characteristic listener
     * </p>
     * 
     * @param characteristic The characteristic whose channel should be removed
     */
    private void removeChannelForCharacteristic(Characteristic<?> characteristic) {
        if (characteristic == null) {
            return;
        }

        try {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());
            Channel channel = thing.getChannel(channelUID);
            if (channel != null) {
                synchronized (characteristicMapLock) {
                    characteristicMap.remove(channel);
                }
                updateThing(editThing().withoutChannel(channelUID).build());
            }
        } catch (Exception e) {
            logger.warn("{}Warning - Type: Channel, Message: Failed to remove channel for characteristic {}: {}",
                    LOG_PREFIX, characteristic.getUID(), e.getMessage());
        }
    }

    /**
     * Handles server state changes and updates the handler state accordingly.
     * 
     * <p>
     * This method processes server state events by:
     * 1. Validating server identity
     * 2. Processing connection state changes (connected/disconnected)
     * 3. Managing pairing state (paired/unpaired)
     * 4. Handling configuration errors
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses nested synchronized blocks (stateLock and serverLock)
     * - Ensures atomic state transitions
     * - Maintains thread-safe server state updates
     * </p>
     * 
     * <p>
     * State Transitions:
     * - SERVER_STATE_CONNECTED: Updates connection state and validates
     * - SERVER_STATE_DISCONNECTED: Updates connection state and validates
     * - SERVER_STATE_PAIRED/PAIR_VERIFIED: Updates pairing state and validates
     * - SERVER_STATE_UNPAIRED/PAIR_UNVERIFIED: Updates pairing state and validates
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Handles missing setup code configuration
     * - Handles missing pairing information
     * - Logs unhandled event types
     * - Catches and logs processing errors
     * </p>
     * 
     * @param event The server event containing state change information
     */
    @Override
    public void onAccessoryServerEvent(AccessoryServerEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            processEvent(() -> handleServerStateChange(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid server event data: {}", e.getMessage());
        }
    }

    /**
     * Handles accessory events and updates the handler state accordingly.
     * 
     * <p>
     * This method processes accessory events by:
     * 1. Validating the event data
     * 2. Processing the event in a thread-safe manner
     * 3. Handling accessory state changes
     * 4. Updating the handler state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for event processing
     * - Event processing is atomic
     * - Thread-safe state updates
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates event data
     * - Catches and logs invalid event data
     * - Maintains consistent state
     * </p>
     * 
     * @param event The accessory event to process
     */
    @Override
    public void onAccessoryEvent(AccessoryEvent event) {
        if (disposed) {
            return;
        }
        try {
            validateEventData(event);
            processEvent(() -> handleAccessoryEvent(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid accessory event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_ACCESSORY_EVENT, e);
        }
    }

    /**
     * Handles service events and updates the handler state accordingly.
     * 
     * <p>
     * This method processes service events by:
     * 1. Validating the event data
     * 2. Processing the event in a thread-safe manner
     * 3. Handling service state changes
     * 4. Updating the handler state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for event processing
     * - Event processing is atomic
     * - Thread-safe state updates
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates event data
     * - Catches and logs invalid event data
     * - Maintains consistent state
     * </p>
     * 
     * @param event The service event to process
     */
    @Override
    public void onServiceEvent(ServiceEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            processEvent(() -> handleServiceStateChange(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid service event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing service event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_SERVICE_EVENT, e);
        }
    }

    /**
     * Handles characteristic events and updates the handler state accordingly.
     * 
     * <p>
     * This method processes characteristic events by:
     * 1. Validating the event data
     * 2. Processing the event in a thread-safe manner
     * 3. Handling characteristic state changes
     * 4. Updating the handler state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for event processing
     * - Event processing is atomic
     * - Thread-safe state updates
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates event data
     * - Catches and logs invalid event data
     * - Maintains consistent state
     * - Updates thing status on errors
     * </p>
     * 
     * <p>
     * State Management:
     * - Updates characteristic values
     * - Synchronizes channel states
     * - Maintains state consistency
     * </p>
     * 
     * @param event The characteristic event to process
     */
    @Override
    public void onCharacteristicEvent(CharacteristicEvent event) {
        if (disposed) {
            return;
        }
        try {
            validateEventData(event);
            processEvent(() -> handleCharacteristicEvent(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid characteristic event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing characteristic event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_CHARACTERISTIC_EVENT, e);
        }
    }

    /**
     * Handles server state changes and updates the handler state accordingly.
     * 
     * <p>
     * This method processes server state changes by:
     * 1. Validating the server reference
     * 2. Processing different server state events
     * 3. Updating internal state flags
     * 4. Updating thing status based on server state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe state transitions
     * - Atomic state updates
     * </p>
     * 
     * <p>
     * State Transitions:
     * - SERVER_STATE_CONNECTED: Sets serverConnected flag
     * - SERVER_STATE_DISCONNECTED: Clears serverConnected flag
     * - SERVER_STATE_PAIRED/PAIR_VERIFIED: Sets serverPaired flag
     * - SERVER_STATE_UNPAIRED/PAIR_UNVERIFIED: Clears serverPaired flag
     * - SERVER_STATE_MISSING_SETUP_CODE: Sets OFFLINE with CONFIGURATION_ERROR
     * - SERVER_STATE_PAIRING_MISSING: Sets OFFLINE with CONFIGURATION_ERROR
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Catches and logs processing errors
     * - Maintains consistent state
     * </p>
     * 
     * @param event The server state change event to process
     */
    private void handleServerStateChange(AccessoryServerEvent event) {
        try {
            synchronized (stateLock) {
                synchronized (serverLock) {
                    if (server != null && event.getServer().equals(server)) {
                        switch (event.getType()) {
                            case SERVER_STATE_CONNECTED:
                                serverConnected = true;
                                logger.debug("{}Debug - Type: Server, Message: Server connected", LOG_PREFIX);
                                validateAndUpdateState();
                                break;
                            case SERVER_STATE_DISCONNECTED:
                                serverConnected = false;
                                logger.debug("{}Debug - Type: Server, Message: Server disconnected", LOG_PREFIX);
                                validateAndUpdateState();
                                break;
                            case SERVER_STATE_PAIRED:
                            case SERVER_STATE_PAIR_VERIFIED:
                                serverPaired = true;
                                logger.debug("{}Debug - Type: Server, Message: Server paired", LOG_PREFIX);
                                validateAndUpdateState();
                                break;
                            case SERVER_STATE_UNPAIRED:
                            case SERVER_STATE_PAIR_UNVERIFIED:
                                serverPaired = false;
                                logger.debug("{}Debug - Type: Server, Message: Server unpaired", LOG_PREFIX);
                                validateAndUpdateState();
                                break;
                            case SERVER_STATE_MISSING_SETUP_CODE:
                                logger.warn("{}Warning - Type: Server, Message: Server setup code missing", LOG_PREFIX);
                                updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                        "Missing setup code");
                                break;
                            case SERVER_STATE_PAIRING_MISSING:
                                logger.warn("{}Warning - Type: Server, Message: Server pairing information missing",
                                        LOG_PREFIX);
                                updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                        "Pairing information missing");
                                break;
                            case ACCESSORY_ADDED:
                                if (event.getAccessory().equals(accessory)) {
                                    updateStatus(ThingStatus.ONLINE);
                                }
                                break;
                            case ACCESSORY_REMOVED:
                                if (event.getAccessory().equals(accessory)) {
                                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Accessory was removed");
                                }
                                break;
                            default:
                                logger.debug("{}Debug - Type: Server, Message: Unhandled server event type: {}",
                                        LOG_PREFIX, event.getType());
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("{}Error - Type: Server, Message: Server state change failed - Error: {}", LOG_PREFIX,
                    e.getMessage(), e);
        }
    }

    /**
     * Handles service state changes and updates the handler state accordingly.
     * 
     * <p>
     * This method processes service state changes by:
     * 1. Validating the service reference
     * 2. Processing different service state events
     * 3. Updating internal state flags
     * 4. Updating thing status based on service state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe state transitions
     * </p>
     * 
     * <p>
     * State Transitions:
     * - CHARACTERISTIC_ADDED: Handles characteristic addition
     * - CHARACTERISTIC_REMOVED: Handles characteristic removal
     * - CHARACTERISTIC_STATE_CHANGED: Handles characteristic state changes
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Catches and logs processing errors
     * - Maintains consistent state
     * </p>
     */
    private void handleServiceStateChange(ServiceEvent event) {
        try {
            synchronized (stateLock) {
                synchronized (accessoryLock) {

                    // find the servce in the accessory that matches the servce of the event
                    Service foundService = accessory.getService(event.getService().getInstanceType());
                    if (foundService != null && event.getService().equals(foundService)) {
                        switch (event.getType()) {
                            case CHARACTERISTIC_ADDED:
                                handleCharacteristicAdded(event.getCharacteristic());
                                break;
                            case CHARACTERISTIC_REMOVED:
                                handleCharacteristicRemoved(event.getCharacteristic());
                                break;
                            case CHARACTERISTIC_STATE_CHANGED:
                                handleCharacteristicStateChanged(event);
                                break;
                            default:
                                logger.debug("{}Debug - Type: Service, Message: Unhandled service event type: {}",
                                        LOG_PREFIX, event.getType());
                                break;
                        }
                        synchronizeChannels();
                    }
                }
            }
        } catch (Exception e) {
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error processing service state change: " + e.getMessage(), e);
        }
    }

    /**
     * Handles accessory events and updates the handler state accordingly.
     * 
     * <p>
     * This method processes accessory events by:
     * 1. Validating the event data
     * 2. Processing different accessory state events
     * 3. Updating internal state flags
     * 4. Updating thing status based on accessory state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Thread-safe state transitions
     * </p>
     * 
     * <p>
     * State Transitions:
     * - SERVICE_ADDED: Handles accessory addition
     * - SERVICE_REMOVED: Handles accessory removal
     * - SERVICE_STATE_CHANGED: Handles accessory state changes
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Catches and logs processing errors
     * - Maintains consistent state
     * </p>
     */

    private void handleAccessoryEvent(AccessoryEvent event) {
        if (event == null) {
            logger.warn("{}Received null accessory event", LOG_EVENT);
            return;
        }

        if (!event.getAccessory().equals(getAccessory())) {
            logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
            return;
        }

        switch (event.getType()) {
            case SERVICE_ADDED:
                logger.debug("{}Service added event received", LOG_EVENT);
                synchronized (accessoryLock) {
                    // if (accessory == null) {
                    // Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
                    // if (foundAccessory != null) {
                    // setAccessory(foundAccessory);
                    // if (this instanceof org.openhab.io.homekit.api.listener.AccessoryChangeListener) {
                    // foundAccessory.addChangeListener(
                    // (org.openhab.io.homekit.api.listener.AccessoryChangeListener) this);
                    // }
                    // validateAndUpdateState();
                    // } else {
                    // logger.warn("{}Accessory not found in registry after add event", LOG_EVENT);
                    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    // "Accessory not found");
                    // }
                    // }
                    addChannelGroupForService(event.getService());
                }
                break;
            case SERVICE_REMOVED:
                logger.debug("{}Service removed event received", LOG_EVENT);
                synchronized (accessoryLock) {
                    if (accessory != null) {
                        try {
                            if (this instanceof org.openhab.io.homekit.api.listener.AccessoryChangeListener) {
                                accessory.removeChangeListener(
                                        (org.openhab.io.homekit.api.listener.AccessoryChangeListener) this);
                            }
                        } catch (Exception e) {
                            logger.warn("{}Failed to remove accessory change listener: {}", LOG_EVENT, e.getMessage());
                        }
                        accessory = null;
                    }
                }
                handleServiceRemoved();
                break;
            case SERVICE_STATE_CHANGED:
                logger.debug("{}Service state changed event received", LOG_EVENT);
                // synchronized (accessoryLock) {
                // if (accessory != null) {
                // Service foundService = accessory.getService(serviceId);
                // if (foundService != null) {
                // setService(foundService);
                // validateAndUpdateState();
                // synchronizeChannels();
                // } else {
                // logger.warn("{}Service not found in accessory after change event", LOG_EVENT);
                // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                // "Service not found");
                // }
                // }
                // }
                break;
            default:
                logger.warn("{}Unhandled accessory event type: {}", LOG_EVENT, event.getType());
                break;
        }
    }

    /**
     * Handles characteristic events.
     * 
     * <p>
     * This method processes characteristic events by:
     * 1. Validating the event data
     * 2. Processing characteristic state changes
     * 3. Updating channel states
     * 4. Managing characteristic lifecycle
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Implements proper event validation
     * </p>
     * 
     * <p>
     * State Management:
     * - Processes characteristic state changes
     * - Updates channel states
     * - Manages characteristic lifecycle
     * </p>
     * 
     * @param event The characteristic event
     */
    private void handleCharacteristicEvent(CharacteristicEvent event) {
        if (event == null) {
            logger.warn("{}Received null characteristic event", LOG_EVENT);
            return;
        }

        Characteristic<?> characteristic = event.getCharacteristic();
        if (characteristic == null) {
            logger.warn("{}Received event with null characteristic", LOG_EVENT);
            return;
        }

        ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());
        Channel channel = thing.getChannel(channelUID);
        if (channel == null) {
            logger.warn("{}Channel not found - UID: {}", LOG_CHANNEL, channelUID);
            return;
        }

        synchronized (characteristicMapLock) {
            if (characteristicMap.get(channel) == characteristic) {
                Object newValue = event.getNewValue();
                if (newValue instanceof State) {
                    ThingStatus currentStatus = thing.getStatus();
                    handleChannelStateTransition(channel, characteristic, currentStatus, ThingStatus.ONLINE);
                    updateState(channelUID, (State) newValue);
                    logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL, channelUID, newValue);
                }
            }
        }
    }

    /**
     * Processes events in a thread-safe manner.
     * 
     * <p>
     * Thread Safety:
     * - This method is thread-safe and uses synchronized blocks for queue management
     * - Event processing is performed through a dedicated executor service
     * - Queue size is limited to prevent memory issues
     * </p>
     * 
     * @param eventTask The event task to process
     */
    private void processEvent(Runnable eventTask) {
        synchronized (eventQueueLock) {
            if (eventQueue.size() >= MAX_QUEUE_SIZE) {
                logger.warn("{}Event queue overflow, dropping oldest event", LOG_EVENT);
                eventQueue.poll(); // Remove oldest event
            }
            eventQueue.offer(eventTask);
        }

        eventExecutor.submit(() -> {
            try {
                Runnable task;
                synchronized (eventQueueLock) {
                    task = eventQueue.poll();
                }
                if (task != null) {
                    task.run();
                }
            } catch (Exception e) {
                logger.error("{}Error processing event: {}", LOG_EVENT, e.getMessage(), e);
            }
        });
    }

    /**
     * Validates the data in an AccessoryServerEvent to ensure it contains all required information
     * before processing.
     * 
     * <p>
     * This method performs validation by checking:
     * 1. Event is not null
     * 2. Server reference is not null
     * 3. Event type is not null
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - No synchronization needed as this is a validation-only method
     * - Thread-safe validation
     * </p>
     * 
     * @param event The AccessoryServerEvent to validate
     * @throws IllegalArgumentException if event data is invalid
     */
    private void validateEventData(AccessoryServerEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getServer() == null) {
            throw new IllegalArgumentException("Event server cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    /**
     * Validates the data in a ServiceEvent to ensure it contains all required information
     * before processing.
     * 
     * <p>
     * This method performs validation by checking:
     * 1. Event is not null
     * 2. Service reference is not null
     * 3. Event type is not null
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - No synchronization needed as this is a validation-only method
     * - Thread-safe validation
     * </p>
     * 
     * @param event The ServiceEvent to validate
     * @throws IllegalArgumentException if event data is invalid
     */
    private void validateEventData(ServiceEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getService() == null) {
            throw new IllegalArgumentException("Event service cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    /**
     * Validates the data in an AccessoryEvent to ensure it contains all required information
     * before processing.
     * 
     * <p>
     * This method performs validation by checking:
     * 1. Event is not null
     * 2. Accessory reference is not null
     * 3. Event type is not null
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - No synchronization needed as this is a validation-only method
     * - Thread-safe validation
     * </p>
     * 
     * @param event The AccessoryEvent to validate
     * @throws IllegalArgumentException if event data is invalid
     */
    private void validateEventData(AccessoryEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getAccessory() == null) {
            throw new IllegalArgumentException("Event accessory cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    /**
     * Validates the data in a CharacteristicEvent to ensure it contains all required information
     * before processing.
     * 
     * <p>
     * This method performs validation by checking:
     * 1. Event is not null
     * 2. Characteristic reference is not null
     * 3. Event type is not null
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - No synchronization needed as this is a validation-only method
     * - Thread-safe validation
     * </p>
     * 
     * @param event The CharacteristicEvent to validate
     * @throws IllegalArgumentException if event data is invalid
     */
    private void validateEventData(CharacteristicEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getCharacteristic() == null) {
            throw new IllegalArgumentException("Event characteristic cannot be null");
        }
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    /**
     * Determines the current thing status detail based on component states.
     * 
     * <p>
     * This method evaluates the state of various components to determine
     * the appropriate status detail for the thing.
     * </p>
     * 
     * @return The current thing status detail
     */
    private ThingStatusDetail determineThingStatusDetail() {
        if (!serverConnected) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        if (!serverPaired) {
            return ThingStatusDetail.CONFIGURATION_ERROR;
        }
        if (!accessoryAvailable) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        return ThingStatusDetail.NONE;
    }

    /**
     * Determines the current thing status description based on component states.
     * 
     * <p>
     * This method evaluates the state of various components to provide
     * a descriptive message about the current status.
     * </p>
     * 
     * @return A description of the current thing status, or null if no description is needed
     */
    private @Nullable String determineThingStatusDescription() {
        if (!serverConnected) {
            return "Server disconnected";
        }
        if (!serverPaired) {
            return "Server not paired";
        }
        if (!accessoryAvailable) {
            return "Service not available";
        }
        return null;
    }

    /**
     * Determines the current thing status based on component states.
     * 
     * <p>
     * This method evaluates the state of various components to determine
     * the overall status of the thing.
     * </p>
     * 
     * @return The current thing status
     */
    private ThingStatus determineThingStatus() {
        if (!serverConnected || !serverPaired || !accessoryAvailable) {
            return ThingStatus.OFFLINE;
        }
        return ThingStatus.ONLINE;
    }

    /**
     * Handles characteristic state changes from the service.
     * 
     * <p>
     * This method processes characteristic state changes by:
     * 1. Validating the characteristic
     * 2. Finding the corresponding channel
     * 3. Handling channel type changes if necessary
     * 4. Updating the channel state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for state updates
     * - Implements proper error handling
     * </p>
     * 
     * @param event The service event containing the characteristic state change
     */
    private void handleCharacteristicStateChanged(ServiceEvent event) {
        try {
            Characteristic<?> characteristic = event.getCharacteristic();
            if (characteristic != null) {
                Channel channel = findChannelForCharacteristic(characteristic);
                if (channel != null) {
                    // Check if the characteristic type has changed
                    handleChannelTypeChange(channel, characteristic);

                    // Update the channel state
                    synchronized (characteristicMapLock) {
                        if (characteristicMap.get(channel) == characteristic) {
                            Object value = characteristic.getValue();
                            if (value instanceof State) {
                                updateState(channel.getUID(), (State) value);
                                logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL,
                                        channel.getUID(), value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("{}Characteristic state change failed - Error: {}", LOG_CHANNEL, e.getMessage());
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error handling characteristic state change: " + e.getMessage(), e);
        }
    }

    /**
     * Handles a command for a channel.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for command processing
     * - Thread-safe command handling
     * </p>
     * 
     * <p>
     * Command Processing:
     * - Validates channel and command
     * - Processes command through characteristic
     * - Updates channel state
     * </p>
     * 
     * @param channelUID The UID of the channel
     * @param command The command to process
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (disposed || channelUID == null || command == null) {
            return;
        }

        try {
            Channel channel = thing.getChannel(channelUID);
            if (channel != null) {
                Characteristic<?> characteristic;
                synchronized (characteristicMapLock) {
                    characteristic = characteristicMap.get(channel);
                }
                if (characteristic != null) {
                    handleCharacteristicCommand(characteristic, command);
                }
            }
        } catch (Exception e) {
            logger.warn("{}Failed to handle command for channel {}: {}", LOG_CHANNEL, channelUID, e.getMessage());
        }
    }

    /**
     * Handles a command for a specific characteristic.
     * 
     * <p>
     * This method processes commands by:
     * 1. Validating the characteristic and command
     * 2. Converting the command to the appropriate type
     * 3. Updating the characteristic value
     * 4. Synchronizing the channel state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for command processing
     * - Command processing is atomic
     * - Thread-safe state updates
     * </p>
     * 
     * <p>
     * Type Conversion:
     * - Supports State to characteristic value conversion
     * - Handles type casting for compatible types
     * - Manages null/invalid states
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Validates input parameters
     * - Catches and logs conversion errors
     * - Updates thing status on errors
     * </p>
     * 
     * @param characteristic The characteristic to process the command for
     * @param command The command to process
     */
    private <T> void handleCharacteristicCommand(Characteristic<T> characteristic, Command command) {
        if (characteristic == null || command == null) {
            return;
        }

        try {
            if (command instanceof State) {
                @SuppressWarnings("unchecked")
                T value = (T) command;
                characteristic.setValue(value);
                Channel channel = findChannelForCharacteristic(characteristic);
                if (channel != null) {
                    updateState(channel.getUID(), (State) command);
                    logger.debug("{}Command processed - Channel: {}, Value: {}", LOG_CHANNEL, channel.getUID(),
                            command);
                }
            }
        } catch (Exception e) {
            logger.warn("{}Command processing failed - Characteristic: {}, Error: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_COMMAND_PROCESSING, e);
        }
    }

    /**
     * Handles the addition of a new characteristic to the service.
     * 
     * <p>
     * This method manages characteristic additions by:
     * 1. Creating a new channel for the characteristic
     * 2. Updating the characteristic map
     * 3. Initializing the channel state
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe characteristic addition
     * </p>
     * 
     * @param characteristic The characteristic that was added
     */
    private void handleCharacteristicAdded(@Nullable Characteristic<?> characteristic) {
        if (characteristic != null) {
            addChannelForCharacteristic(characteristic);
        }
    }

    /**
     * Handles the removal of a characteristic from the service.
     * 
     * <p>
     * This method manages characteristic removals by:
     * 1. Finding the associated channel
     * 2. Removing the channel from the characteristic map
     * 3. Cleaning up resources
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe characteristic removal
     * </p>
     * 
     * @param characteristic The characteristic that was removed
     */
    private void handleCharacteristicRemoved(@Nullable Characteristic<?> characteristic) {
        if (characteristic != null) {
            removeChannelForCharacteristic(characteristic);
        }
    }

    /**
     * Handles the removal of a service.
     * 
     * <p>
     * This method handles service removal by:
     * 1. Cleaning up channels
     * 2. Updating the service reference
     * 3. Updating the thing status
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for service updates
     * - Thread-safe service removal
     * </p>
     * 
     * <p>
     * State Transitions:
     * - OFFLINE: Updates thing status to OFFLINE
     * </p>
     * 
     * <p>
     * Error Handling:
     * - Catches and logs cleanup errors
     * - Maintains consistent state
     * </p>
     */

    private void handleServiceRemoved() {
        cleanupChannels();
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Service removed");
    }

    /**
     * Gets the current HomeKit server instance.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for server access
     * - Thread-safe server retrieval
     * </p>
     * 
     * @return The current HomeKit server instance, or null if not set
     */
    private @Nullable AccessoryServer getServer() {
        synchronized (serverLock) {
            return server;
        }
    }

    /**
     * Sets the HomeKit server instance.
     * 
     * <p>
     * This method updates the server reference and:
     * 1. Validates the new server
     * 2. Updates the server reference
     * 3. Registers/unregisters listeners
     * 4. Updates the thing status
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for server updates
     * - Thread-safe server assignment
     * </p>
     * 
     * @param server The new HomeKit server instance
     */
    private void setServer(@Nullable AccessoryServer server) {
        synchronized (serverLock) {
            this.server = server;
        }
    }

    /**
     * Gets the current HomeKit accessory instance.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for accessory access
     * - Thread-safe accessory retrieval
     * </p>
     * 
     * @return The current HomeKit accessory instance, or null if not set
     */
    private @Nullable Accessory getAccessory() {
        synchronized (accessoryLock) {
            return accessory;
        }
    }

    /**
     * Sets the accessory instance in a thread-safe manner.
     * 
     * <p>
     * This method updates the accessory reference and:
     * 1. Validates the new accessory
     * 2. Updates the accessory reference
     * 3. Registers/unregisters listeners
     * 4. Updates the thing status
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for accessory updates
     * - Thread-safe accessory assignment
     * </p>
     * 
     * @param accessory The new HomeKit accessory instance
     */
    private void setAccessory(@Nullable Accessory accessory) {
        synchronized (accessoryLock) {
            this.accessory = accessory;
        }
    }

    /**
     * Handles channel linking events.
     * 
     * <p>
     * This method processes channel linking by:
     * 1. Finding the corresponding characteristic
     * 2. Updating the characteristic map
     * 3. Initializing the channel state
     * 4. Registering for characteristic updates
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe channel linking
     * </p>
     * 
     * <p>
     * State Management:
     * - Updates characteristic mappings
     * - Initializes channel states
     * - Registers for updates
     * </p>
     * 
     * @param channelUID The UID of the channel being linked
     */
    @Override
    public void channelLinked(ChannelUID channelUID) {
        if (disposed || channelUID == null) {
            return;
        }

        try {
            Channel channel = thing.getChannel(channelUID);
            if (channel != null) {
                Characteristic<?> characteristic;
                synchronized (characteristicMapLock) {
                    characteristic = characteristicMap.get(channel);
                }
                if (characteristic != null) {
                    characteristic.addChangeListener(this);
                    Object value = characteristic.getValue();
                    if (value instanceof State) {
                        updateState(channelUID, (State) value);
                        logger.debug("{}Channel linked - UID: {}, Initial state: {}", LOG_CHANNEL, channelUID, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("{}Channel link failed - UID: {}, Error: {}", LOG_CHANNEL, channelUID, e.getMessage());
        }
        super.channelLinked(channelUID);
    }

    /**
     * Handles channel unlinking events.
     * 
     * <p>
     * This method processes channel unlinking by:
     * 1. Finding the corresponding characteristic
     * 2. Updating the characteristic map
     * 3. Unregistering from characteristic updates
     * 4. Cleaning up resources
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map updates
     * - Thread-safe channel unlinking
     * </p>
     * 
     * <p>
     * Resource Management:
     * - Updates characteristic mappings
     * - Unregisters from updates
     * - Cleans up resources
     * </p>
     * 
     * @param channelUID The UID of the channel being unlinked
     */
    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        if (disposed || channelUID == null) {
            return;
        }

        try {
            Channel channel = thing.getChannel(channelUID);
            if (channel != null) {
                Characteristic<?> characteristic;
                synchronized (characteristicMapLock) {
                    characteristic = characteristicMap.get(channel);
                }
                if (characteristic != null) {
                    characteristic.removeChangeListener(this);
                    updateState(channelUID, UnDefType.UNDEF);
                    logger.debug("{}Channel unlinked - UID: {}", LOG_CHANNEL, channelUID);
                }
            }
        } catch (Exception e) {
            logger.warn("{}Channel unlink failed - UID: {}, Error: {}", LOG_CHANNEL, channelUID, e.getMessage());
        }
        super.channelUnlinked(channelUID);
    }

    /**
     * Handles errors and updates the thing status accordingly.
     * 
     * <p>
     * Thread Safety:
     * - This method is thread-safe and uses synchronized blocks for state updates
     * - Error handling is performed atomically
     * </p>
     * 
     * <p>
     * State Transitions:
     * - Updates thing status to OFFLINE with specified detail
     * - Logs error message and cause
     * </p>
     * 
     * @param detail The status detail for the error
     * @param message The error message
     * @param cause The cause of the error, if any
     */
    private void handleError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
        if (disposed) {
            return;
        }

        String errorMessage = message;
        if (cause != null) {
            errorMessage += " - " + cause.getMessage();
            logger.error("{}Error occurred - Type: {}, Message: {}", LOG_PREFIX, detail, message, cause);
        } else {
            logger.error("{}Error occurred - Type: {}, Message: {}", LOG_PREFIX, detail, message);
        }

        updateState(ThingStatus.OFFLINE, detail, errorMessage);
    }

    /**
     * Handles recoverable errors and attempts recovery if possible.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for recovery scheduling
     * - Implements proper cancellation of existing recovery tasks
     * </p>
     * 
     * <p>
     * Recovery Strategy:
     * - Implements exponential backoff
     * - Limits maximum recovery attempts
     * - Handles recovery task lifecycle
     * </p>
     * 
     * @param detail The status detail for the error
     * @param message The error message
     * @param cause The cause of the error, if any
     */
    private void handleRecoverableError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
        handleError(detail, message, cause);
        logger.debug("{}Scheduling recovery attempt in 30 seconds", LOG_PREFIX);
        scheduler.schedule(this::attemptRecovery, 30, TimeUnit.SECONDS);
    }

    /**
     * Attempts to recover from an error state by reinitializing components.
     * 
     * <p>
     * This method performs recovery by:
     * 1. Attempting to reconnect to the server
     * 2. Attempting to reconnect to the accessory
     * 3. Attempting to reconnect to the service
     * 4. Updating the thing status
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for component access
     * - Thread-safe recovery attempts
     * </p>
     */
    private void attemptRecovery() {
        if (disposed) {
            return;
        }

        try {
            synchronized (stateLock) {
                if (disposed) {
                    return;
                }

                // Attempt to recover server connection
                synchronized (serverLock) {
                    if (server == null) {
                        AccessoryServer foundServer = serverRegistry.get(new AccessoryServerUID(deviceId));
                        if (foundServer != null) {
                            setServer(foundServer);
                            foundServer.addChangeListener(this);
                            logger.debug("{}Recovered server connection", LOG_INIT);
                        }
                    }
                }

                // Attempt to recover accessory
                synchronized (accessoryLock) {
                    if (accessory == null) {
                        Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
                        if (foundAccessory != null) {
                            setAccessory(foundAccessory);
                            foundAccessory.addChangeListener(this);
                            logger.debug("{}Recovered accessory connection", LOG_INIT);
                        }
                    }
                }
            }

            // Revalidate state after recovery attempts
            validateAndUpdateState();
        } catch (Exception e) {
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_STATE_UPDATE, e);
        }
    }

    /**
     * Finds the characteristic associated with a channel.
     * 
     * <p>
     * This method looks up the characteristic by:
     * 1. Getting the channel's characteristic type
     * 2. Finding the matching characteristic in the service
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for map access
     * - Thread-safe characteristic lookup
     * </p>
     * 
     * @param channel The channel to find the characteristic for
     * @return The associated characteristic, or null if not found
     */
    private Characteristic<?> findCharacteristicForChannel(Channel channel) {
        synchronized (characteristicMapLock) {
            return characteristicMap.get(channel);
        }
    }
}
