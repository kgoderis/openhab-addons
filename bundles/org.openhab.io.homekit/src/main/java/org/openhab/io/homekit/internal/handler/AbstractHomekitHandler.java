package org.openhab.io.homekit.internal.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
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
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.ListenerNotificationException;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.events.AccessoryEvent;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.ServiceEvent;
import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractHomekitHandler extends BaseThingHandler implements AccessoryServerChangeListener,
        ServiceChangeListener, CharacteristicChangeListener, AccessoryChangeListener {

    // ========== Constants ==========
    protected static final int MAX_QUEUE_SIZE = 1000;
    protected static final long EVENT_PROCESSING_TIMEOUT = 5000; // 5 seconds
    protected static final String CONFIG_DEVICE_ID = "deviceId";
    protected static final String CONFIG_ACCESSORY_ID = "accessoryId";
    protected static final String DEFAULT_ACCESSORY_ID = "1";

    // ========== Error Messages ==========
    protected static final String ERROR_PREFIX = "HomeKit Handler Error: ";
    protected static final String ERROR_SERVER_NOT_FOUND = ERROR_PREFIX + "Server not found for deviceId: %s";
    protected static final String ERROR_ACCESSORY_NOT_FOUND = ERROR_PREFIX + "Accessory not found for accessoryId: %s";
    protected static final String ERROR_CONFIG_INVALID = ERROR_PREFIX + "Invalid configuration: %s";
    protected static final String ERROR_STATE_UPDATE = ERROR_PREFIX + "Failed to update state: %s";
    protected static final String ERROR_CHANNEL_OPERATION = ERROR_PREFIX + "Channel operation failed: %s";
    protected static final String ERROR_EVENT_PROCESSING = ERROR_PREFIX + "Event processing failed: %s";
    protected static final String ERROR_COMMAND_PROCESSING = ERROR_PREFIX + "Command processing failed: %s";
    protected static final String ERROR_PROCESSING_ACCESSORY_EVENT = "Error processing accessory event";
    protected static final String ERROR_PROCESSING_CHARACTERISTIC_EVENT = "Error processing characteristic event";
    protected static final String ERROR_PROCESSING_SERVICE_EVENT = "Error processing service event";

    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "HomeKit Handler: ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CHANNEL = LOG_PREFIX + "Channel - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_CLEANUP = LOG_PREFIX + "Cleanup - ";

    // ========== Core Dependencies ==========
    protected final Logger logger;
    protected final AccessoryServerRegistry serverRegistry;
    protected final AccessoryRegistry accessoryRegistry;
    protected final HomekitChannelTypeProvider homekitChannelTypeProvider;
    protected final HomekitThingTypeProvider homekitThingTypeProvider;

    // ========== Configuration Fields ==========
    protected String deviceId = "";
    protected String accessoryId = "";

    // ========== Component References and Locks ==========
    protected final Object serverLock = new Object();
    protected @Nullable AccessoryServer server;
    protected final Object accessoryLock = new Object();
    protected @Nullable Accessory accessory;
    protected final Object characteristicMapLock = new Object();
    protected final Map<Channel, @Nullable Characteristic<?>> characteristicMap = new ConcurrentHashMap<>();

    // ========== State Management ==========
    protected final Object stateLock = new Object();
    protected volatile boolean disposed = false;
    protected volatile boolean initialized = false;
    protected volatile ThingStatus currentStatus = ThingStatus.UNINITIALIZED;
    protected volatile ThingStatusDetail currentStatusDetail = ThingStatusDetail.NONE;
    protected volatile @Nullable String currentStatusDescription = null;
    protected volatile boolean serverConnected = false;
    protected volatile boolean serverPaired = false;
    protected volatile boolean accessoryAvailable = false;

    // ========== Event Processing ==========
    protected final Object eventQueueLock = new Object();
    protected final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    protected final ExecutorService eventExecutor;

    protected AbstractHomekitHandler(Thing thing, AccessoryServerRegistry serverRegistry,
            AccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider) {
        super(thing);
        this.logger = LoggerFactory.getLogger(getClass());
        this.serverRegistry = serverRegistry;
        this.accessoryRegistry = accessoryRegistry;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.homekitThingTypeProvider = homekitThingTypeProvider;

        this.eventExecutor = ThreadPoolManager.getScheduledPool("io.homekit.events");
    }

    // ========== Core Lifecycle Methods ==========
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
            initializeComponents();
            validateAndUpdateState();

            handleSpecificInitialization();

            logger.debug("{}Initialization completed successfully", LOG_INIT);
        } catch (Exception e) {
            logger.error("{}Initialization failed - Error: {}", LOG_INIT, e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    protected void initializeComponents() {
        synchronized (serverLock) {
            AccessoryServer foundServer = serverRegistry.get(new AccessoryServerUID(deviceId));
            if (foundServer == null) {
                throw new IllegalStateException(String.format(ERROR_SERVER_NOT_FOUND, deviceId));
            }
            setServer(foundServer);
            try {
                foundServer.addChangeListener(this);
            } catch (ListenerNotificationException e) {
                logger.error("{}Failed to add change listener to server: {}", LOG_PREFIX, e.getMessage(), e);
            }
        }

        synchronized (accessoryLock) {
            Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
            if (foundAccessory == null) {
                throw new IllegalStateException(String.format(ERROR_ACCESSORY_NOT_FOUND, accessoryId));
            }
            setAccessory(foundAccessory);
            foundAccessory.addChangeListener(this);
            accessoryAvailable = true;
        }
    }

    protected abstract void handleSpecificInitialization();

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
        }

        try {
            logger.debug("{}Starting cleanup", LOG_CLEANUP);

            // No need to shutdown the ThreadPoolManager pool as it's managed centrally
            // Just process remaining events if any
            synchronized (eventQueueLock) {
                Runnable task;
                while ((task = eventQueue.poll()) != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        logger.warn("{}Error processing final event: {}", LOG_CLEANUP, e.getMessage());
                    }
                }
            }

            cleanupComponents();
            handleSpecificDispose();

            logger.debug("{}Cleanup completed successfully", LOG_CLEANUP);
        } catch (Exception e) {
            logger.error("{}Unexpected error during cleanup: {}", LOG_CLEANUP, e.getMessage(), e);
        } finally {
            super.dispose();
        }
    }

    protected abstract void handleSpecificDispose();

    protected void cleanupComponents() {
        synchronized (serverLock) {
            if (server != null) {
                try {
                    server.removeChangeListener(this);
                } catch (ListenerNotificationException e) {
                    logger.error("{}Failed to remove change listener from server: {}", LOG_PREFIX, e.getMessage(), e);
                }
                server = null;
            }
        }

        synchronized (accessoryLock) {
            if (accessory != null) {
                accessory.removeChangeListener(this);
                accessory = null;
                accessoryAvailable = false;
            }
        }

        synchronized (characteristicMapLock) {
            characteristicMap.values().stream().filter(Objects::nonNull)
                    .forEach(characteristic -> characteristic.removeChangeListener(this));
            characteristicMap.clear();
        }
    }

    // ========== Configuration Methods ==========
    protected void validateConfiguration(Configuration config) {
        this.deviceId = (String) config.get(CONFIG_DEVICE_ID);
        this.accessoryId = (String) config.get(CONFIG_ACCESSORY_ID);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration must contain a valid deviceId");
        }
        if (accessoryId == null || accessoryId.trim().isEmpty()) {
            accessoryId = DEFAULT_ACCESSORY_ID;
        }

        validateSpecificConfiguration(config);
    }

    protected abstract void validateSpecificConfiguration(Configuration config);

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

    // ========== State Management Methods ==========
    protected void validateAndUpdateState() {
        try {
            synchronized (stateLock) {
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

    protected abstract ThingStatus determineThingStatus();

    protected abstract ThingStatusDetail determineThingStatusDetail();

    protected abstract String determineThingStatusDescription();

    protected void updateState(ThingStatus status, ThingStatusDetail detail, @Nullable String description) {
        synchronized (stateLock) {
            if (disposed) {
                return;
            }

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

    protected void updateState(ThingStatus status) {
        updateState(status, ThingStatusDetail.NONE, null);
    }

    // ========== Event Handling Methods ==========
    protected void processEvent(Runnable eventTask) {
        synchronized (eventQueueLock) {
            if (eventQueue.size() >= MAX_QUEUE_SIZE) {
                logger.warn("{}Event queue overflow, dropping oldest event", LOG_EVENT);
                eventQueue.poll();
            }
            eventQueue.offer(eventTask);
        }

        // Using ThreadPoolManager's pool
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

    @Override
    public void onAccessoryServerEvent(AccessoryServerEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            processEvent(() -> handleAccessoryServerEvent(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid server event data: {}", e.getMessage());
        }
    }

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

    @Override
    public void onServiceEvent(ServiceEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            processEvent(() -> handleServiceEvent(event));
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid service event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing service event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_SERVICE_EVENT, e);
        }
    }

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

    protected void handleAccessoryServerEvent(AccessoryServerEvent event) {
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

    protected void handleServiceEvent(ServiceEvent event) {
        try {
            synchronized (stateLock) {
  
                        switch (event.getType()) {
                            case CHARACTERISTIC_ADDED -> handleCharacteristicAdded(event.getCharacteristic());
                            case CHARACTERISTIC_REMOVED -> handleCharacteristicRemoved(event.getCharacteristic());
                            case CHARACTERISTIC_STATE_CHANGED -> handleCharacteristicStateChanged(event);
                            default -> logger.debug("{}Debug - Type: Service, Message: Unhandled service event type: {}",
                                    LOG_PREFIX, event.getType());
                        }
                        synchronizeChannels();
                    
                
            }
        } catch (Exception e) {
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error processing service state change: " + e.getMessage(), e);
        }
    }

    protected void handleAccessoryEvent(AccessoryEvent event) {

        synchronized (accessoryLock) {
        // Validate event data
        Accessory eventAccessory = event.getAccessory();
        if (eventAccessory == null) {
            logger.warn("{}Received event with null accessory", LOG_EVENT);
            return;
        }

        // Check if event is for our accessory
        Accessory currentAccessory = getAccessory();
        if (currentAccessory == null || !eventAccessory.equals(currentAccessory)) {
            logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
            return;
        }

        try {
            switch (event.getType()) {
                case SERVICE_ADDED:
                    handleAccessoryServiceAdded(event);
                    break;
                case SERVICE_REMOVED:
                    handleAccessoryServiceRemoved(event);
                    break;
                case SERVICE_STATE_CHANGED:
                    handleAccessoryServiceStateChanged(event);
                    break;
                default:
                    logger.warn("{}Unhandled accessory event type: {}", LOG_EVENT, event.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
            }
        }
    }

    protected void handleAccessoryServiceAdded(AccessoryEvent event) {


        synchronized (accessoryLock) {
                            // Validate event data
                            Accessory eventAccessory = event.getAccessory();
                            if (eventAccessory == null) {
                                logger.warn("{}Received event with null accessory", LOG_EVENT);
                                return;
                            }
            
                                    // Check if event is for our accessory
                    Accessory currentAccessory = getAccessory();
                    if (currentAccessory == null || !eventAccessory.equals(currentAccessory)) {
                        logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
                        return;
                    }
            if (accessory == null) {
                Accessory foundAccessory = accessoryRegistry.get(eventAccessory.getUID());
                if (foundAccessory != null) {
                    setAccessory(foundAccessory);
                    foundAccessory.addChangeListener(this);
                    accessoryAvailable = true;
                    validateAndUpdateState();
                } else {
                    logger.warn("{}Accessory not found in registry after add event", LOG_EVENT);
                    accessoryAvailable = false;
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Accessory not found");
                }
            }
        }
    }

    protected void handleAccessoryServiceRemoved(AccessoryEvent event) {



        synchronized (accessoryLock) {
                            // Validate event data
                            Accessory eventAccessory = event.getAccessory();
                            if (eventAccessory == null) {
                                logger.warn("{}Received event with null accessory", LOG_EVENT);
                                return;
                            }
            
                                    // Check if event is for our accessory
                    Accessory currentAccessory = getAccessory();
                    if (currentAccessory == null || !eventAccessory.equals(currentAccessory)) {
                        logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
                        return;
                    }

            if (accessory != null) {
                try {
                    accessory.removeChangeListener(this);
                } catch (Exception e) {
                    logger.warn("{}Failed to remove accessory change listener: {}", LOG_EVENT, e.getMessage());
                }
                accessory = null;
                accessoryAvailable = false;
                updateState(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Accessory removed");
            }
        }
        handleServiceRemoved();
    }

    protected void handleAccessoryServiceStateChanged(AccessoryEvent event) {



        synchronized (accessoryLock) {
                                    // Validate event data
                                    Accessory eventAccessory = event.getAccessory();
                                    if (eventAccessory == null) {
                                        logger.warn("{}Received event with null accessory", LOG_EVENT);
                                        return;
                                    }
                    
                                            // Check if event is for our accessory
                            Accessory currentAccessory = getAccessory();
                            if (currentAccessory == null || !eventAccessory.equals(currentAccessory)) {
                                logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
                                return;

                            }

                            // No Op - should be handled by the implementing class


            // if (accessory != null) {
            //     Service foundService = accessory.getService(serviceId);
            //     if (foundService != null) {
            //         accessoryAvailable = true;
            //         validateAndUpdateState();
            //         synchronizeChannels();
            //     } else {
            //         logger.warn("{}Service not found in accessory after change event", LOG_EVENT);
            //         accessoryAvailable = false;
            //         updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Service not found");
            //     }
            // }
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
    protected void handleCharacteristicEvent(CharacteristicEvent event) {

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
                    ThingStatus currentThingStatus = thing.getStatus();
                    handleChannelStateTransition(channel, characteristic, currentThingStatus, ThingStatus.ONLINE);
                    updateState(channelUID, (State) newValue);
                    logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL, channelUID, newValue);
                }
            }
        }
    }

    protected void handleServiceRemoved() {
        cleanupChannels();
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Service removed");
    }

    // ========== Channel Management Methods ==========
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

    protected void synchronizeChannels() {
        try {
            // Get current characteristics based on handler type
            Set<Characteristic<?>> currentCharacteristics = getCurrentCharacteristics();
            if (currentCharacteristics == null || currentCharacteristics.isEmpty()) {
                logger.warn("{}Cannot synchronize channels: no characteristics available", LOG_CHANNEL);
                return;
            }

            Set<String> currentCharacteristicTypes = currentCharacteristics.stream()
                    .map(Characteristic::getInstanceType).collect(Collectors.toSet());

            // Step 1: Remove channels for characteristics that no longer exist
            List<Channel> channelsToRemove = new ArrayList<>();
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                    if (!currentCharacteristicTypes.contains(entry.getValue().getInstanceType())) {
                        channelsToRemove.add(entry.getKey());
                        logger.debug("{}Removing channel for characteristic: {}", LOG_CHANNEL,
                                entry.getValue().getInstanceType());
                    }
                }

                // Remove obsolete channels
                channelsToRemove.forEach(channel -> {
                    Characteristic<?> characteristic = characteristicMap.get(channel);
                    if (characteristic != null) {
                        characteristic.removeChangeListener(this);
                    }
                    characteristicMap.remove(channel);
                    updateThing(editThing().withoutChannel(channel.getUID()).build());
                    logger.debug("{}Removed channel: {}", LOG_CHANNEL, channel.getUID());
                });
            }

            // Step 2: Add channels for new characteristics
            for (Characteristic<?> characteristic : currentCharacteristics) {
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
                    try {
                        addChannelForCharacteristic(characteristic);
                        logger.debug("{}Added channel for characteristic: {}", LOG_CHANNEL,
                                characteristic.getInstanceType());
                    } catch (Exception e) {
                        logger.warn("{}Failed to add channel for characteristic {}: {}", LOG_CHANNEL,
                                characteristic.getInstanceType(), e.getMessage());
                    }
                }
            }

            // Step 3: Update channel states for existing characteristics
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                    Channel channel = entry.getKey();
                    Characteristic<?> characteristic = entry.getValue();

                    if (characteristic.getValue() instanceof State) {
                        updateState(channel.getUID(), (State) characteristic.getValue());
                        logger.debug("{}Updated state for channel {}: {}", LOG_CHANNEL, channel.getUID(),
                                characteristic.getValue());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("{}Failed to synchronize channels: {}", LOG_CHANNEL, e.getMessage(), e);
        }
    }

    protected abstract Set<Characteristic<?>> getCurrentCharacteristics();

    protected abstract boolean validateCharacteristicBelongsToHandler(Characteristic<?> characteristic);

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

    protected @Nullable Channel addChannelForCharacteristic(Characteristic<?> characteristic) {


        try {
            // Let subclasses determine the channel ID
            ChannelUID channelUID = getChannelUID(characteristic);

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getInstanceType());
            ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
            if (channelType == null) {
                logger.warn("{}No ChannelType found for characteristic {}", LOG_CHANNEL, characteristic.getUID());
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
            logger.warn("{}Unexpected error adding channel for characteristic {}: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            return null;
        }
    }

    protected abstract ChannelUID getChannelUID(Characteristic<?> characteristic);

    protected void removeChannelForCharacteristic(Characteristic<?> characteristic) {

        try {
            // Validate that the characteristic belongs to this handler
            if (!validateCharacteristicBelongsToHandler(characteristic)) {
                logger.warn("{}Cannot remove channel: characteristic {} does not belong to this handler", LOG_CHANNEL,
                        characteristic.getUID());
                return;
            }

            // Find the channel associated with this characteristic
            Channel channelToRemove = null;
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                    if (characteristic.equals(entry.getValue())) {
                        channelToRemove = entry.getKey();
                        break;
                    }
                }

                if (channelToRemove != null) {
                    // Remove from characteristic map
                    characteristicMap.remove(channelToRemove);

                    // Unregister from characteristic updates
                    characteristic.removeChangeListener(this);

                    // Remove the channel from the thing
                    updateThing(editThing().withoutChannel(channelToRemove.getUID()).build());

                    logger.debug("{}Channel removed for characteristic {}", LOG_CHANNEL, characteristic.getUID());
                }
            }
        } catch (Exception e) {
            logger.warn("{}Failed to remove channel for characteristic {}: {}", LOG_CHANNEL, characteristic.getUID(),
                    e.getMessage());
        }
    }

    protected void handleChannelStateTransition(Channel channel, Characteristic<?> characteristic,
            ThingStatus oldStatus, ThingStatus newStatus) {
        try {
            if (channel == null || characteristic == null) {
                return;
            }

            if (currentStatus != newStatus) {
                logger.debug("{}Channel state transition - Channel: {}, Current Status: {}, Target Status: {}",
                        LOG_EVENT, channel.getUID(), currentStatus, newStatus);
                validateAndUpdateState();
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
            logger.error("{}Channel state transition failed - Error: {}", LOG_CHANNEL, e.getMessage());
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error handling channel state transition: " + e.getMessage(), e);
        }
    }

    protected void handleChannelTypeChange(Channel channel, Characteristic<?> characteristic) {
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

    private @Nullable Channel findChannelForCharacteristic(Characteristic<?> characteristic) {


        synchronized (characteristicMapLock) {
            for (Map.Entry<Channel, Characteristic<?>> entry : characteristicMap.entrySet()) {
                if (entry.getValue() == characteristic) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private @Nullable Characteristic<?> findCharacteristicForChannel(Channel channel) {
        synchronized (characteristicMapLock) {
            return characteristicMap.get(channel);
        }
    }

    private void validateEventData(AccessoryServerEvent event) {

        if (event.getServer() == null) {
            throw new IllegalArgumentException("Event server cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    private void validateEventData(ServiceEvent event) {

        if (event.getService() == null) {
            throw new IllegalArgumentException("Event service cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    private void validateEventData(AccessoryEvent event) {

        if (event.getAccessory() == null) {
            throw new IllegalArgumentException("Event accessory cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    private void validateEventData(CharacteristicEvent event) {

        if (event.getCharacteristic() == null) {
            throw new IllegalArgumentException("Event characteristic cannot be null");
        }
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
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

    // ========== Recovery Methods ==========
    protected void handleError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
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

    protected void handleRecoverableError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
        handleError(detail, message, cause);
        logger.debug("{}Scheduling recovery attempt", LOG_PREFIX);
        scheduler.schedule(this::attemptRecovery, 30, TimeUnit.SECONDS);
    }

    protected void attemptRecovery() {
        if (disposed) {
            return;
        }

        try {
            synchronized (stateLock) {
                if (disposed) {
                    return;
                }

                logger.debug("{}Starting recovery attempt", LOG_INIT);

                // Step 1: Recover server connection
                synchronized (serverLock) {
                    if (server == null) {
                        AccessoryServer foundServer = serverRegistry.get(new AccessoryServerUID(deviceId));
                        if (foundServer != null) {
                            setServer(foundServer);
                            try {
                                foundServer.addChangeListener(this);
                            } catch (ListenerNotificationException e) {
                                logger.error("{}Failed to add change listener to server: {}", LOG_PREFIX, e.getMessage(), e);
                            }
                            logger.debug("{}Recovered server connection", LOG_INIT);
                        } else {
                            logger.warn("{}Server not found during recovery", LOG_INIT);
                            throw new IllegalStateException(String.format(ERROR_SERVER_NOT_FOUND, deviceId));
                        }
                    }
                }

                // Step 2: Recover accessory connection
                synchronized (accessoryLock) {
                    if (accessory == null) {
                        Accessory foundAccessory = accessoryRegistry.get(new AccessoryUID(accessoryId));
                        if (foundAccessory != null) {
                            setAccessory(foundAccessory);
                            foundAccessory.addChangeListener(this);
                            logger.debug("{}Recovered accessory connection", LOG_INIT);
                        } else {
                            logger.warn("{}Accessory not found during recovery", LOG_INIT);
                            throw new IllegalStateException(String.format(ERROR_ACCESSORY_NOT_FOUND, accessoryId));
                        }
                    }
                }

                // Step 3: Handler-specific recovery
                performSpecificRecovery();

                // Step 4: Reinitialize components
                initializeComponents();

                // Step 5: Validate and update state
                validateAndUpdateState();

                logger.debug("{}Recovery attempt completed successfully", LOG_INIT);
            }
        } catch (Exception e) {
            logger.error("{}Recovery attempt failed: {}", LOG_INIT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_STATE_UPDATE, e);

            // Schedule next recovery attempt
            scheduler.schedule(this::attemptRecovery, 30, TimeUnit.SECONDS);
        }
    }

    protected abstract void performSpecificRecovery() throws Exception;

    // ========== Getters and Setters ==========
    protected @Nullable AccessoryServer getServer() {
        synchronized (serverLock) {
            return server;
        }
    }

    protected void setServer(@Nullable AccessoryServer server) {
        synchronized (serverLock) {
            this.server = server;
        }
    }

    protected @Nullable Accessory getAccessory() {
        synchronized (accessoryLock) {
            return accessory;
        }
    }

    protected void setAccessory(@Nullable Accessory accessory) {
        synchronized (accessoryLock) {
            this.accessory = accessory;
        }
    }

    protected abstract void initializeChannels();
}
