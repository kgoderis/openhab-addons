package org.openhab.io.homekit.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUIDImpl;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerUIDImpl;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.accessory.HomekitAccessoryEvent;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public abstract class AbstractHomekitHandler extends BaseThingHandler {

    // ========== Constants ==========
    protected static final int MAX_QUEUE_SIZE = 1000;
    protected static final long EVENT_PROCESSING_TIMEOUT = 5000; // 5 seconds
    protected static final String CONFIG_DEVICE_ID = "deviceId";
    protected static final String CONFIG_ACCESSORY_ID = "accessoryId";
    protected static final String DEFAULT_ACCESSORY_ID = "1";

    // ========== Error Messages ==========
    protected static final String ERROR_PREFIX = "Homekit Handler Error: ";
    protected static final String ERROR_SERVER_NOT_FOUND = ERROR_PREFIX + "Server not found for deviceId: %s";
    protected static final String ERROR_ACCESSORY_NOT_FOUND = ERROR_PREFIX
            + "HomekitAccessory not found for accessoryId: %s";
    protected static final String ERROR_CONFIG_INVALID = ERROR_PREFIX + "Invalid configuration: %s";
    protected static final String ERROR_STATE_UPDATE = ERROR_PREFIX + "Failed to update state: %s";
    protected static final String ERROR_CHANNEL_OPERATION = ERROR_PREFIX + "Channel operation failed: %s";
    protected static final String ERROR_EVENT_PROCESSING = ERROR_PREFIX + "Event processing failed: %s";
    protected static final String ERROR_COMMAND_PROCESSING = ERROR_PREFIX + "Command processing failed: %s";
    protected static final String ERROR_PROCESSING_ACCESSORY_EVENT = "Error processing accessory event";
    protected static final String ERROR_PROCESSING_CHARACTERISTIC_EVENT = "Error processing characteristic event";
    protected static final String ERROR_PROCESSING_SERVICE_EVENT = "Error processing service event";

    // ========== Log HomekitMessage Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit Handler: ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CHANNEL = LOG_PREFIX + "Channel - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_CLEANUP = LOG_PREFIX + "Cleanup - ";

    // ========== Core Dependencies ==========
    protected final Logger logger;
    protected final HomekitAccessoryServerRegistry serverRegistry;
    protected final HomekitAccessoryRegistry accessoryRegistry;
    protected final HomekitChannelTypeProvider homekitChannelTypeProvider;
    protected final HomekitThingTypeProvider homekitThingTypeProvider;
    protected final HomekitEventManager eventManager;

    // ========== Configuration Fields ==========
    protected String accessoryServerPairingId = "";
    protected String accessoryId = "";

    // ========== Component References and Locks ==========
    protected final Object serverLock = new Object();
    protected @Nullable HomekitAccessoryServer server;
    protected final Object accessoryLock = new Object();
    protected @Nullable HomekitAccessory accessory;
    protected final Object characteristicMapLock = new Object();
    protected final Map<Channel, org.openhab.io.homekit.api.characteristic.HomekitCharacteristic<?>> characteristicMap = new ConcurrentHashMap<>();

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
    protected volatile boolean accessoryServerAvailable = false;

    // ========== Event Processing ==========
    protected final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    protected AbstractHomekitHandler(Thing thing, HomekitAccessoryServerRegistry serverRegistry,
            HomekitAccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider, HomekitEventManager eventManager) {
        super(thing);
        this.logger = LoggerFactory.getLogger(getClass());
        this.serverRegistry = serverRegistry;
        this.accessoryRegistry = accessoryRegistry;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.eventManager = eventManager;
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
            intializeSpecificComponents();
            setupEventSubscriptions();
            validateAndUpdateState();

            logger.debug("{}Initialization completed successfully", LOG_INIT);
        } catch (Exception e) {
            logger.error("{}Initialization failed - Error: {}", LOG_INIT, e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    protected void initializeComponents() {
        synchronized (serverLock) {
            HomekitAccessoryServer foundServer = serverRegistry
                    .get(new HomekitAccessoryServerUIDImpl(accessoryServerPairingId));
            if (foundServer == null) {
                throw new IllegalStateException(String.format(ERROR_SERVER_NOT_FOUND, accessoryServerPairingId));
            }
            setServer(foundServer);
        }

        synchronized (accessoryLock) {
            HomekitAccessory foundAccessory = accessoryRegistry
                    .get(new HomekitAccessoryUIDImpl(accessoryServerPairingId, Long.parseLong(accessoryId)));
            if (foundAccessory == null) {
                throw new IllegalStateException(String.format(ERROR_ACCESSORY_NOT_FOUND, accessoryId));
            }
            setAccessory(foundAccessory);
        }
    }

    protected abstract void intializeSpecificComponents();

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
            teardownEventSubscriptions();
            handleSpecificDispose();
            cleanupComponents();
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
            setServer(null);
        }
        synchronized (accessoryLock) {
            setAccessory(null);
        }
        synchronized (characteristicMapLock) {
            characteristicMap.clear();
        }
    }

    // ========== Configuration Methods ==========
    protected void validateConfiguration(Configuration config) {
        this.accessoryServerPairingId = (String) config.get(CONFIG_DEVICE_ID);
        this.accessoryId = (String) config.get(CONFIG_ACCESSORY_ID);

        if (accessoryServerPairingId == null || accessoryServerPairingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration must contain a valid deviceId");
        }
        if (accessoryId == null || accessoryId.trim().isEmpty()) {
            accessoryId = DEFAULT_ACCESSORY_ID;
        }

        validateSpecificConfiguration(config);
    }

    protected abstract void validateSpecificConfiguration(Configuration config);

    @Override
    @SuppressWarnings("null")
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        try {
            if (disposed) {
                return;
            }

            Objects.requireNonNull(configurationParameters);
            Configuration newConfig = new Configuration(configurationParameters);
            validateConfiguration(newConfig);

            // Update configuration
            Configuration currentConfig = thing.getConfiguration();
            currentConfig.setProperties(newConfig.getProperties());
            updateConfiguration(currentConfig);

            // Reinitialize channels if necessary
            if (configurationParameters.containsKey(CONFIG_ACCESSORY_ID)) {
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

    // // ========== Event Handling Methods ==========

    // protected void processEvent(Runnable eventTask) {
    // synchronized (eventQueueLock) {
    // if (eventQueue.size() >= MAX_QUEUE_SIZE) {
    // logger.warn("{}Event queue overflow, dropping oldest event", LOG_EVENT);
    // eventQueue.poll();
    // }
    // eventQueue.offer(eventTask);
    // }

    // // Using ThreadPoolManager's pool
    // eventExecutor.submit(() -> {
    // try {
    // @Nullable
    // Runnable task;
    // synchronized (eventQueueLock) {
    // task = eventQueue.poll();
    // }
    // if (task != null) {
    // task.run();
    // }
    // } catch (Exception e) {
    // logger.error("{}Error processing event: {}", LOG_EVENT, e.getMessage(), e);
    // }
    // });
    // }

    public void onAccessoryServerEvent(HomekitAccessoryServerEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            handleAccessoryServerEvent(event);
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid server event data: {}", e.getMessage());
        }
    }

    public void onAccessoryEvent(HomekitAccessoryEvent event) {
        if (disposed) {
            return;
        }
        try {
            validateEventData(event);
            handleAccessoryEvent(event);
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid accessory event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_ACCESSORY_EVENT, e);
        }
    }

    public void onServiceEvent(HomekitServiceEvent event) {
        if (disposed) {
            return;
        }

        try {
            validateEventData(event);
            handleServiceEvent(event);
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid service event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing service event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_SERVICE_EVENT, e);
        }
    }

    public void onCharacteristicEvent(HomekitCharacteristicEvent event) {
        if (disposed) {
            return;
        }
        try {
            validateEventData(event);
            handleCharacteristicEvent(event);
        } catch (IllegalArgumentException e) {
            logger.error("{}Invalid characteristic event data: {}", LOG_EVENT, e.getMessage());
        } catch (Exception e) {
            logger.error("{}Error processing characteristic event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_CHARACTERISTIC_EVENT, e);
        }
    }

    protected void handleAccessoryServerEvent(HomekitAccessoryServerEvent event) {
        try {
            synchronized (serverLock) {

                // Check if event is for our server
                HomekitAccessoryServer currentServer = getServer();
                if (currentServer == null || !event.getServer().equals(currentServer)) {
                    logger.debug("{}Received event for different server, ignoring", LOG_EVENT);
                    return;
                }
                switch (event.getType()) {
                    case SERVER_STATE_CONNECTED -> {
                        serverConnected = true;
                        logger.debug("{}Debug - Type: Server, HomekitMessage: Server connected", LOG_PREFIX);
                        validateAndUpdateState();
                    }
                    case SERVER_STATE_DISCONNECTED -> {
                        serverConnected = false;
                        logger.debug("{}Debug - Type: Server, HomekitMessage: Server disconnected", LOG_PREFIX);
                        validateAndUpdateState();
                    }
                    case SERVER_STATE_PAIRED, SERVER_STATE_PAIR_VERIFIED -> {
                        serverPaired = true;
                        logger.debug("{}Debug - Type: Server, HomekitMessage: Server paired", LOG_PREFIX);
                        validateAndUpdateState();
                    }
                    case SERVER_STATE_UNPAIRED, SERVER_STATE_PAIR_UNVERIFIED -> {
                        serverPaired = false;
                        logger.debug("{}Debug - Type: Server, HomekitMessage: Server unpaired", LOG_PREFIX);
                        validateAndUpdateState();
                    }
                    case SERVER_STATE_MISSING_SETUP_CODE -> {
                        logger.warn("{}Warning - Type: Server, HomekitMessage: Server setup code missing", LOG_PREFIX);
                        updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing setup code");
                    }
                    case SERVER_STATE_PAIRING_MISSING -> {
                        logger.warn("{}Warning - Type: Server, HomekitMessage: Server pairing information missing",
                                LOG_PREFIX);
                        updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "HomekitPairing information missing");
                    }
                    default -> logger.debug("{}Debug - Type: Server, HomekitMessage: Unhandled server event type: {}",
                            LOG_PREFIX, event.getType());
                }
            }
        } catch (Exception e) {
            logger.error("{}Error - Type: Server, HomekitMessage: Server state change failed - Error: {}", LOG_PREFIX,
                    e.getMessage(), e);
        }
    }

    protected void handleAccessoryEvent(HomekitAccessoryEvent event) {

        try {
            synchronized (accessoryLock) {
                event.getAccessory().ifPresentOrElse(eventAccessory -> {
                    // Check if event is for our accessory
                    HomekitAccessory currentAccessory = getAccessory();
                    if (currentAccessory == null || !eventAccessory.equals(currentAccessory)) {
                        logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
                        return;
                    }

                    try {
                        switch (event.getType()) {
                            case SERVICE_ADDED -> handleAccessoryServiceAdded(event);
                            case SERVICE_REMOVED -> handleAccessoryServiceRemoved(event);
                            case ACCESSORY_STATE_CHANGED -> handleAccessoryStateChanged(event);
                            default -> logger.warn("{}Unhandled accessory event type: {}", LOG_EVENT, event.getType());
                        }
                    } catch (Exception e) {
                        logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
                        handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                                String.format("Error handling accessory event: %s", e.getMessage()), e);
                    }
                }, () -> {
                    logger.warn("{}Received event with null accessory", LOG_EVENT);
                });
            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
        }
    }

    protected void handleServiceEvent(HomekitServiceEvent event) {
        try {
            synchronized (stateLock) {
                switch (event.getType()) {
                    case CHARACTERISTIC_ADDED -> handleCharacteristicAdded(event);
                    case CHARACTERISTIC_REMOVED -> handleCharacteristicRemoved(event);
                    case SERVICE_STATE_CHANGED -> handleServiceStateChanged(event);
                    default ->
                        logger.debug("{}Debug - Type: HomekitService, HomekitMessage: Unhandled service event type: {}",
                                LOG_PREFIX, event.getType());
                }
                synchronizeChannels();
            }
        } catch (Exception e) {
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error processing service state change: " + e.getMessage(), e);
        }
    }

    protected void handleAccessoryServiceAdded(HomekitAccessoryEvent event) {
        try {
            synchronized (accessoryLock) {
                event.getAccessory().ifPresentOrElse(eventAccessory -> {
                    HomekitAccessory foundAccessory = accessoryRegistry.get(eventAccessory.getUID());
                    if (foundAccessory != null) {
                        setAccessory(foundAccessory);

                        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.ACCESSORY_STATE_CHANGED,
                                (UID) foundAccessory.getUID(), (UID) thing.getUID(),
                                someEvent -> onAccessoryEvent((HomekitAccessoryEvent) someEvent)));

                        // TODO : What should a thing do if it receives an accessory state changed event for an
                        // accessory that is not found in the registry?
                        // TODO : What wiht the added service ? Subscribe to its events ?

                        handleServiceAdded();
                        validateAndUpdateState();
                    } else {
                        logger.warn("{}HomekitAccessory not found in registry after add event", LOG_EVENT);
                        setAccessory(null);
                        validateAndUpdateState();
                    }

                }, () -> {
                    logger.warn("{}Received event with null accessory", LOG_EVENT);
                });
            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
        }
    }

    protected void handleAccessoryServiceRemoved(HomekitAccessoryEvent event) {
        try {
            synchronized (accessoryLock) {
                // Validate event data
                event.getAccessory().ifPresentOrElse(eventAccessory -> {

                    HomekitAccessory foundAccessory = accessoryRegistry.get(eventAccessory.getUID());
                    if (foundAccessory != null) {
                        // eventManager.unsubscribe(HomekitEventType.ACCESSORY_STATE_CHANGED,
                        // accessory.getUID().toString(), this);

                        // TODO : What should we do with a removed service ? unsubsribe only ?
                        handleServiceRemoved();
                        validateAndUpdateState();
                    } else {
                        logger.warn("{}HomekitAccessory not found in registry after remove event", LOG_EVENT);
                        setAccessory(null);
                        validateAndUpdateState();
                    }
                }, () -> {
                    logger.warn("{}Received event with null accessory", LOG_EVENT);
                });

            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
        }
    }

    protected void handleServiceRemoved() {
        cleanupChannels();
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "HomekitService removed");
    }

    protected void handleServiceAdded() {
        initializeChannels();
    }

    protected void handleAccessoryStateChanged(HomekitAccessoryEvent event) {
        try {
            synchronized (accessoryLock) {
                // TODO : do we take accessory from the field in the event, or from the service in the event? (each
                // service has an accessory)

                event.getAccessory().ifPresentOrElse(eventAccessory -> {
                    HomekitAccessory foundAccessory = accessoryRegistry.get(eventAccessory.getUID());
                    if (foundAccessory != null) {
                        // No Op - should be handled by the implementing class

                        // if (accessory != null) {
                        // HomekitService foundService = accessory.getService(serviceId);
                        // if (foundService != null) {
                        // accessoryAvailable = true;
                        // validateAndUpdateState();
                        // synchronizeChannels();
                        // } else {
                        // logger.warn("{}HomekitService not found in accessory after change event", LOG_EVENT);
                        // accessoryAvailable = false;
                        // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "HomekitService not
                        // found");
                        // }
                        // }
                    } else {
                        logger.warn("{}HomekitAccessory not found in registry after change event", LOG_EVENT);
                        setAccessory(null);
                        validateAndUpdateState();
                    }
                }, () -> {
                    logger.warn("{}Received event with null accessory", LOG_EVENT);
                });
            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
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
    private void handleCharacteristicAdded(HomekitServiceEvent event) {
        event.getCharacteristic().ifPresentOrElse(addedCharacteristic -> {
            try {
                ChannelUID channelUID = getChannelUID(addedCharacteristic);
                Channel channel = thing.getChannel(channelUID);
                if (channel != null) {
                    logger.warn("{}There exists already a channel for this characteristic - UID: {}", LOG_CHANNEL,
                            channelUID);
                    return;
                }

                addChannelForCharacteristic(addedCharacteristic);
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                        (UID) addedCharacteristic.getUID(), (UID) thing.getUID(),
                        someEvent -> onCharacteristicEvent((HomekitCharacteristicEvent) someEvent)));
            } catch (HomekitException e) {
                logger.warn("{}Failed to add channel for characteristic: {}", LOG_CHANNEL, e.getMessage());
            }
        }, () -> {
            logger.warn("{}Received event with null characteristic", LOG_EVENT);
        });
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
    private void handleCharacteristicRemoved(HomekitServiceEvent event) {
        event.getCharacteristic().ifPresentOrElse(removedCharacteristic -> {

            ChannelUID channelUID = getChannelUID(removedCharacteristic);
            Channel channel = thing.getChannel(channelUID);
            if (channel == null) {
                logger.warn("{}Channel not found for this characteristic - UID: {}", LOG_CHANNEL, channelUID);
                return;
            }

            removeChannelForCharacteristic(removedCharacteristic);
            teardownSubscriptionsForPublisher(removedCharacteristic.getUID().toString(),
                    HomekitEventType.CHARACTERISTIC_STATE_CHANGED);
        }, () -> {
            logger.warn("{}Received event with null characteristic", LOG_EVENT);
        });
    }

    private void handleServiceStateChanged(HomekitServiceEvent event) {
        event.getCharacteristic().ifPresentOrElse(removedCharacteristic -> {

            ChannelUID channelUID = getChannelUID(removedCharacteristic);
            Channel channel = thing.getChannel(channelUID);
            if (channel == null) {
                logger.warn("{}Channel not found for this characteristic - UID: {}", LOG_CHANNEL, channelUID);
            }

            // TODO : Handle the state change

        }, () -> {
            logger.warn("{}Received event with null characteristic", LOG_EVENT);
        });
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
    protected void handleCharacteristicEvent(HomekitCharacteristicEvent event) {

        event.getCharacteristic().ifPresentOrElse(characteristic -> {
            ChannelUID channelUID = getChannelUID(characteristic);
            Channel channel = thing.getChannel(channelUID);
            if (channel == null) {
                logger.warn("{}Channel not found for this characteristic - UID: {}", LOG_CHANNEL, channelUID);
                return;
            }

            try {
                synchronized (stateLock) {
                    switch (event.getType()) {
                        case CHARACTERISTIC_VALUE_CHANGED -> handleCharacteristicValueChanged(event);
                        case CHARACTERISTIC_STATE_CHANGED -> handleCharacteristicStateChanged(event);
                        default -> logger.debug(
                                "{}Debug - Type: HomekitService, HomekitMessage: Unhandled service event type: {}",
                                LOG_PREFIX, event.getType());
                    }
                    synchronizeChannels();
                }
            } catch (Exception e) {
                handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error processing service state change: " + e.getMessage(), e);
            }
            // synchronized (characteristicMapLock) {

            // if(event.getType() == HomekitEventType.CHARACTERISTIC_VALUE_CHANGED) {
            // if (characteristicMap.get(channel) == characteristic) {
            // JsonValue newValue = event.getNewValue();
            // ThingStatus currentThingStatus = thing.getStatus();
            // handleChannelStateTransition(channel, characteristic, newValue, currentThingStatus, ThingStatus.ONLINE);
            // logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL, channelUID, newValue);
            // }
            // } else if(event.getType() == HomekitEventType.CHARACTERISTIC_STATE_CHANGED) {
            // if (characteristicMap.get(channel) == characteristic) {
            // handleCharacteristicStateChanged(event);
            // }
            // }
            // }
        }, () -> {
            logger.warn("{}Received event with null characteristic", LOG_EVENT);
        });
    }

    @SuppressWarnings("null")
    private void handleCharacteristicValueChanged(HomekitCharacteristicEvent event) {
        try {
            event.getCharacteristic().ifPresentOrElse(characteristic -> {
                ChannelUID channelUID = getChannelUID(characteristic);
                Channel channel = thing.getChannel(channelUID);
                if (channel == null) {
                    logger.warn("{}Channel not found for this characteristic - UID: {}", LOG_CHANNEL, channelUID);
                    return;
                }

                if (event.getNewValue().isPresent()) {
                    JsonValue newValue = event.getNewValue().get();
                    ThingStatus currentThingStatus = thing.getStatus();
                    handleChannelStateTransition(channel, characteristic, newValue, currentThingStatus,
                            ThingStatus.ONLINE);
                    logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL, channelUID, newValue);
                }
            }, () -> {
                logger.warn("{}Received event with null characteristic", LOG_EVENT);
            });
        } catch (Exception e) {
            logger.error("{}HomekitCharacteristic value change failed - Error: {}", LOG_CHANNEL, e.getMessage());
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error handling characteristic value change: " + e.getMessage(), e);
        }
    }

    private void handleCharacteristicStateChanged(HomekitCharacteristicEvent event) {
        try {
            event.getCharacteristic().ifPresentOrElse(characteristic -> {

                ChannelUID channelUID = getChannelUID(characteristic);
                Channel channel = thing.getChannel(channelUID);
                if (channel == null) {
                    logger.warn("{}Channel not found for this characteristic - UID: {}", LOG_CHANNEL, channelUID);
                    return;
                }

                channel = findChannelForCharacteristic(characteristic);
                if (channel != null) {
                    // Check if the characteristic type has changed
                    handleChannelTypeChange(channel, characteristic);

                    // Update the channel state
                    synchronized (characteristicMapLock) {
                        if (characteristicMap.get(channel) == characteristic) {
                            Object value = characteristic.getValue();
                            if (value instanceof State state) {
                                updateState(channel.getUID(), state);
                                logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL,
                                        channel.getUID(), value);
                            }
                        }
                    }
                }
            }, () -> {
                logger.warn("{}Received event with null characteristic", LOG_EVENT);
            });
        } catch (Exception e) {
            logger.error("{}HomekitCharacteristic state change failed - Error: {}", LOG_CHANNEL, e.getMessage());
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error handling characteristic state change: " + e.getMessage(), e);
        }
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
                HomekitCharacteristic<?> characteristic;
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

    private <T> void handleCharacteristicCommand(HomekitCharacteristic<T> characteristic, Command command) {
        if (characteristic == null || command == null) {
            return;
        }

        try {
            if (command instanceof State state) {
                @SuppressWarnings("unchecked")
                T value = (T) command;
                // characteristic.setValue(value);
                // replace this with a publish event
                HomekitCharacteristicEvent newEvent = new HomekitCharacteristicEvent(
                        HomekitEventType.CHARACTERISTIC_CHANGE_VALUE, (UID) thing.getUID(),
                        (UID) characteristic.getUID(), characteristic, JsonValue.NULL,
                        characteristic.toValueJson(value),
                        new HomekitEventMetadata(thing.getUID(), null, thing.getUID(), Collections.emptySet()));
                // TODO Define and check peergroup
                eventManager.publishEvent(newEvent);

                Channel channel = findChannelForCharacteristic(characteristic);
                if (channel != null) {
                    updateState(channel.getUID(), state);
                    logger.debug("{}Command processed - Channel: {}, Value: {}", LOG_CHANNEL, channel.getUID(),
                            command);
                }
            }
        } catch (Exception e) {
            logger.warn("{}Command processing failed - HomekitCharacteristic: {}, Error: {}", LOG_CHANNEL,
                    characteristic.getUID(), e.getMessage());
            handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_COMMAND_PROCESSING, e);
        }
    }

    protected void synchronizeChannels() {
        try {
            // Get current characteristics based on handler type
            Set<HomekitCharacteristic<?>> currentCharacteristics = getCurrentCharacteristics();
            if (currentCharacteristics == null || currentCharacteristics.isEmpty()) {
                logger.warn("{}Cannot synchronize channels: no characteristics available", LOG_CHANNEL);
                return;
            }

            Set<String> currentCharacteristicTypes = currentCharacteristics.stream().map(HomekitCharacteristic::getType)
                    .collect(Collectors.toSet());

            // Step 1: Remove channels for characteristics that no longer exist
            List<Channel> channelsToRemove = new ArrayList<>();
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, org.openhab.io.homekit.api.characteristic.HomekitCharacteristic<?>> entry : characteristicMap
                        .entrySet()) {
                    @Nullable
                    HomekitCharacteristic<?> characteristic = entry.getValue();
                    if (characteristic != null && !currentCharacteristicTypes.contains(characteristic.getType())) {
                        channelsToRemove.add(entry.getKey());
                        logger.debug("{}Removing channel for characteristic: {}", LOG_CHANNEL,
                                characteristic.getType());
                    }
                }

                // Remove obsolete channels
                channelsToRemove.forEach(channel -> {
                    HomekitCharacteristic<?> characteristic = characteristicMap.get(channel);
                    if (characteristic != null) {
                        teardownSubscriptionsForPublisher(characteristic.getUID().toString(),
                                HomekitEventType.CHARACTERISTIC_STATE_CHANGED);
                    }
                    characteristicMap.remove(channel);
                    updateThing(editThing().withoutChannel(channel.getUID()).build());
                    logger.debug("{}Removed channel: {}", LOG_CHANNEL, channel.getUID());
                });
            }

            // Step 2: Add channels for new characteristics
            for (HomekitCharacteristic<?> characteristic : currentCharacteristics) {
                boolean channelExists = false;
                synchronized (characteristicMapLock) {
                    for (HomekitCharacteristic<?> existingCharacteristic : characteristicMap.values()) {
                        if (existingCharacteristic != null
                                && existingCharacteristic.getType().equals(characteristic.getType())) {
                            channelExists = true;
                            break;
                        }
                    }
                }

                if (!channelExists) {
                    try {
                        addChannelForCharacteristic(characteristic);
                        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                                (UID) characteristic.getUID(), (UID) thing.getUID(),
                                someEvent -> onCharacteristicEvent((HomekitCharacteristicEvent) someEvent)));
                        logger.debug("{}Added channel for characteristic: {}", LOG_CHANNEL, characteristic.getType());
                    } catch (HomekitException e) {
                        logger.warn("{}Failed to add channel for characteristic {}: {}", LOG_CHANNEL,
                                characteristic.getType(), e.getMessage());
                    }
                }
            }

            // Step 3: Update channel states for existing characteristics
            synchronized (characteristicMapLock) {
                for (Map.Entry<Channel, org.openhab.io.homekit.api.characteristic.HomekitCharacteristic<?>> entry : characteristicMap
                        .entrySet()) {
                    @Nullable
                    Channel channel = entry.getKey();
                    @Nullable
                    HomekitCharacteristic<?> characteristic = entry.getValue();

                    if (characteristic != null && characteristic.getValue() instanceof State state) {
                        updateState(channel.getUID(), state);
                        logger.debug("{}Updated state for channel {}: {}", LOG_CHANNEL, channel.getUID(),
                                characteristic.getValue());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("{}Failed to synchronize channels: {}", LOG_CHANNEL, e.getMessage(), e);
        }
    }

    protected abstract Set<HomekitCharacteristic<?>> getCurrentCharacteristics();

    protected abstract boolean validateCharacteristicBelongsToHandler(HomekitCharacteristic<?> characteristic);

    private void cleanupChannels() {
        try {
            synchronized (characteristicMapLock) {
                // Remove all channels from the thing
                List<Channel> channelsToRemove = new ArrayList<>(characteristicMap.keySet());
                for (Channel channel : channelsToRemove) {
                    @Nullable
                    HomekitCharacteristic<?> characteristic = characteristicMap.get(channel);
                    if (characteristic != null) {
                        removeChannelForCharacteristic(characteristic);
                        teardownSubscriptionsForPublisher(characteristic.getUID().toString(),
                                HomekitEventType.CHARACTERISTIC_STATE_CHANGED);
                    }
                }
                characteristicMap.clear();
            }
        } catch (Exception e) {
            logger.error("{}Error occurred - Type: Channel, HomekitMessage: Failed to cleanup channels: {}", LOG_PREFIX,
                    e.getMessage(), e);
        }
    }

    protected abstract @Nullable Channel addChannelForCharacteristic(HomekitCharacteristic<?> characteristic)
            throws HomekitException;

    protected abstract ChannelUID getChannelUID(HomekitCharacteristic<?> characteristic);

    protected void removeChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {

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
                for (Map.Entry<Channel, org.openhab.io.homekit.api.characteristic.HomekitCharacteristic<?>> entry : characteristicMap
                        .entrySet()) {
                    @Nullable
                    HomekitCharacteristic<?> entryValue = entry.getValue();
                    if (entryValue != null && entryValue == characteristic) {
                        @Nullable
                        Channel possibleChannel = entry.getKey();
                        if (possibleChannel != null) {
                            channelToRemove = possibleChannel;
                            break;
                        }
                    }
                }

                if (channelToRemove != null) {
                    // Remove from characteristic map
                    characteristicMap.remove(channelToRemove);

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

    protected void handleChannelStateTransition(Channel channel, HomekitCharacteristic<?> characteristic,
            JsonValue newValue, ThingStatus oldStatus, ThingStatus newStatus) {
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
                case ONLINE -> {
                    State state = characteristic.toState(newValue);
                    updateState(channel.getUID(), state);
                }
                case OFFLINE -> // When going offline, clear channel state
                    updateState(channel.getUID(), UnDefType.UNDEF);
                default -> {
                }
            }
        } catch (Exception e) {
            logger.error("{}Channel state transition failed - Error: {}", LOG_CHANNEL, e.getMessage());
            handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error handling channel state transition: " + e.getMessage(), e);
        }
    }

    protected void handleChannelTypeChange(Channel channel, HomekitCharacteristic<?> characteristic) {
        try {
            if (channel == null || characteristic == null) {
                return;
            }

            ChannelTypeUID newChannelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getType());

            // Check if channel type has changed
            ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
            if (channelTypeUID != null && !channelTypeUID.equals(newChannelTypeUID)) {
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
            logger.warn("{}Warning - Type: Channel, HomekitMessage: Failed to handle channel type change: {}",
                    LOG_PREFIX, e.getMessage());
        }
    }

    private @Nullable Channel findChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {
        synchronized (characteristicMapLock) {
            for (Map.Entry<Channel, org.openhab.io.homekit.api.characteristic.HomekitCharacteristic<?>> entry : characteristicMap
                    .entrySet()) {
                @Nullable
                HomekitCharacteristic<?> entryValue = entry.getValue();
                if (entryValue != null && entryValue == characteristic) {
                    @Nullable
                    Channel possibleChannel = entry.getKey();
                    if (possibleChannel != null) {
                        return possibleChannel;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void validateEventData(HomekitAccessoryServerEvent event) {
        if (event.getServer() == null) {
            throw new IllegalArgumentException("Event server cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    @SuppressWarnings("unused")
    private void validateEventData(HomekitServiceEvent event) {
        if (event.getService() == null) {
            throw new IllegalArgumentException("Event service cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    @SuppressWarnings("unused")
    private void validateEventData(HomekitAccessoryEvent event) {
        if (event.getAccessory() == null) {
            throw new IllegalArgumentException("Event accessory cannot be null");
        }
        if (event.getType() == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
    }

    @SuppressWarnings("unused")
    private void validateEventData(HomekitCharacteristicEvent event) {
        if (event.getCharacteristic() == null) {
            throw new IllegalArgumentException("Event characteristic cannot be null");
        }
        if (event.getType() == null) {
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
                HomekitCharacteristic<?> characteristic;
                synchronized (characteristicMapLock) {
                    characteristic = characteristicMap.get(channel);
                }
                if (characteristic != null) {
                    eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_VALUE_CHANGED,
                            (UID) characteristic.getUID(), (UID) thing.getUID(),
                            event -> onCharacteristicEvent((HomekitCharacteristicEvent) event)));
                    Object value = characteristic.getValue();
                    if (value instanceof State state) {
                        updateState(channelUID, state);
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
                HomekitCharacteristic<?> characteristic;
                synchronized (characteristicMapLock) {
                    characteristic = characteristicMap.get(channel);
                }
                if (characteristic != null) {
                    teardownSubscriptionsForPublisher(characteristic.getUID().toString(),
                            HomekitEventType.CHARACTERISTIC_VALUE_CHANGED);
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
            logger.error("{}Error occurred - Type: {}, HomekitMessage: {}", LOG_PREFIX, detail, message, cause);
        } else {
            logger.error("{}Error occurred - Type: {}, HomekitMessage: {}", LOG_PREFIX, detail, message);
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
                        HomekitAccessoryServer foundServer = serverRegistry
                                .get(new HomekitAccessoryServerUIDImpl(accessoryServerPairingId));
                        if (foundServer != null) {
                            setServer(foundServer);
                            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVER_STATE_CHANGED,
                                    (UID) foundServer.getUID(), (UID) thing.getUID(),
                                    event -> onAccessoryServerEvent((HomekitAccessoryServerEvent) event)));
                            logger.debug("{}Recovered server connection", LOG_INIT);
                        } else {
                            logger.warn("{}Server not found during recovery", LOG_INIT);
                            throw new IllegalStateException(
                                    String.format(ERROR_SERVER_NOT_FOUND, accessoryServerPairingId));
                        }
                    }
                }

                // Step 2: Recover accessory connection
                synchronized (accessoryLock) {
                    if (accessory == null) {
                        HomekitAccessory foundAccessory = accessoryRegistry
                                .get(new HomekitAccessoryUIDImpl(accessoryId));
                        if (foundAccessory != null) {
                            setAccessory(foundAccessory);
                            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.ACCESSORY_STATE_CHANGED,
                                    (UID) foundAccessory.getUID(), (UID) thing.getUID(),
                                    event -> onAccessoryEvent((HomekitAccessoryEvent) event)));
                            logger.debug("{}Recovered accessory connection", LOG_INIT);
                        } else {
                            logger.warn("{}HomekitAccessory not found during recovery", LOG_INIT);
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
    protected @Nullable HomekitAccessoryServer getServer() {
        synchronized (serverLock) {
            return server;
        }
    }

    protected void setServer(@Nullable HomekitAccessoryServer server) {
        synchronized (serverLock) {
            if (server != null) {
                this.server = server;
                accessoryServerAvailable = true;
            } else {
                this.server = null;
                accessoryServerAvailable = false;
            }
        }
    }

    protected @Nullable HomekitAccessory getAccessory() {
        synchronized (accessoryLock) {
            return accessory;
        }
    }

    protected void setAccessory(@Nullable HomekitAccessory accessory) {
        synchronized (accessoryLock) {
            if (accessory != null) {
                this.accessory = accessory;
                accessoryAvailable = true;
            } else {
                this.accessory = null;
                accessoryAvailable = false;
            }
        }
    }

    protected abstract void initializeChannels();

    protected void setupEventSubscriptions() {

        // TODO : implement ANY event types

        // Server subscriptiops

        HomekitAccessoryServer foundServer = getServer();
        if (foundServer != null && foundServer.getUID() != null) {
            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVER_STATE_CHANGED,
                    (UID) foundServer.getUID(), (UID) thing.getUID(),
                    event -> onAccessoryServerEvent((HomekitAccessoryServerEvent) event)));
        }

        // HomekitAccessory subscriptions

        HomekitAccessory foundAccessory = getAccessory();
        if (foundAccessory != null && foundAccessory.getUID() != null) {
            eventSubscriptions
                    .add(eventManager.subscribe(HomekitEventType.ACCESSORY_STATE_CHANGED, (UID) foundAccessory.getUID(),
                            (UID) thing.getUID(), event -> onAccessoryEvent((HomekitAccessoryEvent) event)));

            // HomekitService subscriptions
            for (HomekitService foundService : foundAccessory.getServices()) {
                if (foundService != null && foundService.getUID() != null) {
                    eventSubscriptions.add(eventManager.subscribe(
                        HomekitEventType.SERVICE_STATE_CHANGED,
                        (UID) foundService.getUID(),
                        (UID) thing.getUID(),
                        event -> onServiceEvent((HomekitServiceEvent) event)
                    ));

                    for (HomekitCharacteristic<?> foundCharacteristic : foundService.getCharacteristics()) {
                        if (foundCharacteristic != null && foundCharacteristic.getUID() != null) {
                            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                                    (UID) foundCharacteristic.getUID(), (UID) thing.getUID(),
                                    event -> onCharacteristicEvent((HomekitCharacteristicEvent) event)));
                        }
                    }
                }

            }
        }
    }

    protected void teardownEventSubscriptions() {
        for (HomekitEventSubscription sub : eventSubscriptions) {
            try {
                eventManager.unsubscribe(sub.getEventType(), sub.getPublisherUID(), sub.getSubscriber());
            } catch (Exception e) {
                logger.warn("{}Failed to unsubscribe from event: {}", LOG_EVENT, e.getMessage());
            }
        }
        eventSubscriptions.clear();
    }

    protected void tearDownSubscriptionsForPublisher(String publisherUID) {
        // get set of subscriptions for the publisher from the eventSubscriptions set
        Set<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                .filter(subscription -> subscription.getPublisherUID().equals(publisherUID))
                .collect(Collectors.toSet());
        // unsubscribe from the event
        subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription));
        // remove the subscription from the event subscriptions set
        eventSubscriptions.removeAll(subscriptions);
    }

    protected void teardownSubscriptionsForPublisher(String publisherUID, HomekitEventType eventType) {
        // get set of subscriptions for the publisher from the eventSubscriptions set
        Set<HomekitEventSubscription> subscriptions = eventSubscriptions.stream()
                .filter(subscription -> subscription.getPublisherUID().equals(publisherUID)
                        && subscription.getEventType().equals(eventType))
                .collect(Collectors.toSet());
        // unsubscribe from the event
        subscriptions.forEach(subscription -> eventManager.unsubscribe(subscription));
        // remove the subscription from the event subscriptions set
        eventSubscriptions.removeAll(subscriptions);
    }
}
