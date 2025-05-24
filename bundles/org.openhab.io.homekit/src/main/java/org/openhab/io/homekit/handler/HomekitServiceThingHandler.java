package org.openhab.io.homekit.handler;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.accessory.HomekitAccessoryEvent;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.network.discovery.HomekitBindingConstants;
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;

@NonNullByDefault
public class HomekitServiceThingHandler extends AbstractHomekitHandler {

    // ========== Constants ==========
    private static final String CONFIG_SERVICE_ID = "serviceId";

    // ========== Configuration Fields ==========
    private String serviceId = "";

    // ========== Component References and Locks ==========
    private final Object serviceLock = new Object();
    private @Nullable HomekitService service;

    // ========== State Management ==========
    private volatile boolean serviceAvailable = false;

    private final HomekitCharacteristicFactory characteristicFactory;
    private final HomekitServiceFactory serviceFactory;

    /**
     * Constructs a new HomekitServiceThingHandler.
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
     * @param serverRegistry Registry for Homekit accessory servers
     * @param accessoryRegistry Registry for Homekit accessories
     * @param homekitChannelTypeProvider Provider for Homekit channel types
     * @param homekitThingTypeProvider Provider for Homekit thing types
     * @param homekitChannelGroupTypeProvider Provider for Homekit channel group types
     */
    public HomekitServiceThingHandler(Thing thing, HomekitAccessoryServerRegistry serverRegistry,
            HomekitAccessoryRegistry accessoryRegistry, HomekitChannelTypeProvider homekitChannelTypeProvider,
            HomekitThingTypeProvider homekitThingTypeProvider, HomekitEventManager eventManager,
            HomekitServiceFactory serviceFactory, HomekitCharacteristicFactory characteristicFactory) {
        super(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider, homekitThingTypeProvider,
                eventManager);
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;

        // // Parse configuration
        // Configuration config = thing.getConfiguration();
        // validateConfiguration(config);

        // // Get the server from registry
        // HomekitAccessoryServer foundServer = this.serverRegistry.get(new HomekitAccessoryServerUID(deviceId));
        // if (foundServer == null) {
        // throw new IllegalArgumentException("No HomekitAccessoryServer found for deviceId: " + deviceId);
        // }
        // setServer(foundServer);

        // // Get the accessory from registry
        // HomekitAccessory foundAccessory = this.accessoryRegistry.get(new HomekitAccessoryUID(accessoryId));
        // if (foundAccessory == null) {
        // throw new IllegalArgumentException("No HomekitAccessory found for accessoryId: " + accessoryId);
        // }
        // setAccessory(foundAccessory);

        // // Get the service from accessory
        // HomekitService foundService = foundAccessory.getService(serviceId);
        // if (foundService == null) {
        // throw new IllegalArgumentException("No HomekitService found for serviceId: " + serviceId);
        // }
        // setService(foundService);

        // // Verify ThingType matches HomekitService type
        // String thingType = thing.getThingTypeUID().getId();
        // String serviceTag;
        // try {
        // serviceTag = homekitThingTypeProvider.getServiceTag(foundService.getType());
        // if (!thingType.equals(serviceTag)) {
        // throw new IllegalArgumentException(
        // "ThingType " + thingType + " does not match HomekitService type " + serviceTag);
        // }
        // } catch (HomekitException e) {
        // throw new IllegalArgumentException("HomekitService type could not be determined", e);
        // }

        // // Initialize channels
        // initializeChannels();

        // // Register as listener
        // foundServer.addChangeListener(this);
        // foundService.addChangeListener(this);
    }

    // /**
    // * Initializes the service handler.
    // *
    // * <p>
    // * This method performs initialization by:
    // * 1. Checking if already initialized or disposed
    // * 2. Validating and loading configuration
    // * 3. Initializing server, accessory and service references
    // * 4. Setting up state tracking
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for state updates
    // * - Thread-safe initialization of components
    // * - Atomic state transitions
    // * </p>
    // *
    // * <p>
    // * State Transitions:
    // * - Sets status to INITIALIZING
    // * - Validates configuration
    // * - Initializes server, accessory and service
    // * - Updates thing status
    // * </p>
    // *
    // * <p>
    // * Error Handling:
    // * - Validates configuration
    // * - Checks for required components
    // * - Updates thing status on errors
    // * </p>
    // */

    // @Override
    // public void initialize() {
    // try {
    // if (disposed) {
    // return;
    // }

    // synchronized (stateLock) {
    // if (initialized) {
    // return;
    // }
    // initialized = true;
    // currentStatus = ThingStatus.INITIALIZING;
    // updateState(currentStatus);
    // }

    // logger.debug("{}Starting initialization", LOG_INIT);
    // validateConfiguration(thing.getConfiguration());

    // synchronized (serverLock) {
    // if (server == null) {
    // HomekitAccessoryServer foundServer = serverRegistry.get(new HomekitAccessoryServerUID(deviceId));
    // if (foundServer == null) {
    // throw new IllegalStateException(String.format(ERROR_SERVER_NOT_FOUND, deviceId));
    // }
    // setServer(foundServer);
    // logger.debug("{}Server initialized - Device ID: {}", LOG_INIT, deviceId);
    // }
    // }

    // synchronized (accessoryLock) {
    // if (accessory == null) {
    // HomekitAccessory foundAccessory = accessoryRegistry.get(new HomekitAccessoryUID(accessoryId));
    // if (foundAccessory == null) {
    // throw new IllegalStateException(String.format(ERROR_ACCESSORY_NOT_FOUND, accessoryId));
    // }
    // setAccessory(foundAccessory);
    // logger.debug("{}HomekitAccessory initialized - ID: {}", LOG_INIT, accessoryId);
    // }
    // }

    // synchronized (serviceLock) {
    // if (service == null) {
    // HomekitService foundService = getAccessory().getService(serviceId);
    // if (foundService == null) {
    // throw new IllegalStateException(String.format(ERROR_SERVICE_NOT_FOUND, serviceId));
    // }
    // setService(foundService);
    // logger.debug("{}HomekitService initialized - ID: {}", LOG_INIT, serviceId);
    // }
    // }

    // initializeChannels();

    // HomekitAccessoryServer currentServer = getServer();
    // HomekitService currentService = getService();
    // if (currentServer != null) {
    // currentServer.addChangeListener(this);
    // logger.debug("{}Server change listener registered", LOG_INIT);
    // }
    // if (currentService != null) {
    // currentService.addChangeListener(this);
    // logger.debug("{}HomekitService change listener registered", LOG_INIT);
    // }

    // validateAndUpdateState();
    // logger.debug("{}Initialization completed successfully", LOG_INIT);
    // } catch (Exception e) {
    // logger.error("{}Initialization failed - Error: {}", LOG_INIT, e.getMessage(), e);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    // }
    // }

    @Override
    protected void intializeSpecificComponents() {
        try {
            initializeService();
            initializeChannels();
        } catch (HomekitException e) {
            logger.error("{}Failed to initialize service: {}", LOG_INIT, e.getMessage(), e);
            updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    protected void initializeService() throws HomekitException {

        // Verify ThingType matches HomekitService type
        String thingType = thing.getThingTypeUID().getId();
        @Nullable
        String serviceTag = null;
        HomekitService currentService = getService();
        if (currentService == null) {
            throw new IllegalArgumentException("No HomekitService found for serviceId: " + serviceId);
        }
        try {
            serviceTag = serviceFactory.getTagFromServiceType(currentService.getType());
        } catch (Exception e) {
            throw new IllegalArgumentException("HomekitService type could not be determined", e);
        }

        if (!thingType.equals(serviceTag)) {
            throw new IllegalArgumentException(
                    "ThingType " + thingType + " does not match HomekitService type " + serviceTag);
        }
        setService(currentService);
        serviceAvailable = true;
        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVICE_STATE_CHANGED, (UID) currentService.getUID(),
                (UID) thing.getUID(), event -> onServiceEvent((HomekitServiceEvent) event)));
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
    @Override
    protected void initializeChannels() {

        synchronized (characteristicMapLock) {
            characteristicMap.clear();
        }

        HomekitService currentService = getService();
        if (currentService == null) {
            throw new IllegalStateException("HomekitService is not initialized");
        }

        if (thing.getChannels().isEmpty()) {
            // If no channels configured, add all characteristics as channels
            for (HomekitCharacteristic<?> characteristic : currentService.getCharacteristics()) {
                addChannelForCharacteristic(characteristic);
            }
        } else {
            // Compare existing channels with characteristics
            for (Channel channel : thing.getChannels()) {
                String channelTag = channel.getUID().getIdWithoutGroup();
                String characteristicType;
                try {
                    characteristicType = characteristicFactory.getCharacteristicTypeFromTag(channelTag);
                } catch (Exception e) {
                    handleRecoverableError(ThingStatusDetail.CONFIGURATION_ERROR,
                            "HomekitCharacteristic type could not be determined for channel " + channel.getUID(), e);
                    continue;
                }

                Optional<HomekitCharacteristic<?>> characteristic = currentService
                        .getCharacteristic(characteristicType);
                if (characteristic.isEmpty()) {
                    handleRecoverableError(ThingStatusDetail.CONFIGURATION_ERROR, "HomekitCharacteristic "
                            + characteristicType + " not found in HomekitService " + currentService.getUID(), null);
                } else {
                    synchronized (characteristicMapLock) {
                        characteristicMap.put(channel, characteristic.get());
                    }
                    handleChannelTypeChange(channel, characteristic.get());
                    updateThing(editThing().withChannel(channel).build());
                }
            }
        }
    }

    @Override
    protected @Nullable Channel addChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {

        try {
            // Let subclasses determine the channel ID
            ChannelUID channelUID = getChannelUID(characteristic);

            ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
                    characteristic.getType());
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

    // /**
    // * Disposes of the service handler and cleans up resources.
    // *
    // * <p>
    // * This method performs cleanup by:
    // * 1. Shutting down event processing
    // * 2. Removing listeners
    // * 3. Clearing component references
    // * 4. Cleaning up channels
    // * 5. Updating thing status
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for component access
    // * - Thread-safe cleanup
    // * </p>
    // *
    // * <p>
    // * Resource Management:
    // * - Shuts down executor service
    // * - Removes listeners
    // * - Cleans up channels
    // * - Releases resources
    // * </p>
    // */
    // @Override
    // public void dispose() {
    // synchronized (stateLock) {
    // disposed = true;
    // initialized = false;
    // currentStatus = ThingStatus.UNINITIALIZED;
    // currentStatusDetail = ThingStatusDetail.NONE;
    // currentStatusDescription = null;
    // serverConnected = false;
    // serverPaired = false;
    // serviceAvailable = false;
    // }

    // try {
    // logger.debug("{}Starting cleanup", LOG_CLEANUP);
    // eventExecutor.shutdown();
    // if (!eventExecutor.awaitTermination(EVENT_PROCESSING_TIMEOUT, TimeUnit.MILLISECONDS)) {
    // logger.warn("{}Event executor did not terminate within timeout", LOG_CLEANUP);
    // eventExecutor.shutdownNow();
    // }

    // synchronized (serverLock) {
    // if (server != null) {
    // try {
    // server.removeChangeListener(this);
    // logger.debug("{}Removed server change listener", LOG_CLEANUP);
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove server change listener: {}", LOG_CLEANUP, e.getMessage());
    // }
    // server = null;
    // }
    // }

    // synchronized (accessoryLock) {
    // if (accessory != null) {
    // try {
    // if (this instanceof org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) {
    // accessory.removeChangeListener(
    // (org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) this);
    // logger.debug("{}Removed accessory change listener", LOG_CLEANUP);
    // }
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove accessory change listener: {}", LOG_CLEANUP, e.getMessage());
    // }
    // accessory = null;
    // }
    // }

    // synchronized (serviceLock) {
    // if (service != null) {
    // try {
    // service.removeChangeListener(this);
    // logger.debug("{}Removed service change listener", LOG_CLEANUP);
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove service change listener: {}", LOG_CLEANUP, e.getMessage());
    // }
    // service = null;
    // }
    // }

    // synchronized (characteristicMapLock) {
    // try {
    // characteristicMap.values().stream().filter(Objects::nonNull).forEach(characteristic -> {
    // try {
    // characteristic.removeChangeListener(this);
    // logger.debug("{}Removed characteristic change listener", LOG_CLEANUP);
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove characteristic change listener: {}", LOG_CLEANUP,
    // e.getMessage());
    // }
    // });
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove characteristic change listeners: {}", LOG_CLEANUP, e.getMessage());
    // }
    // characteristicMap.clear();
    // }
    // logger.debug("{}Cleanup completed successfully", LOG_CLEANUP);
    // } catch (Exception e) {
    // logger.error("{}Unexpected error during cleanup: {}", LOG_CLEANUP, e.getMessage(), e);
    // } finally {
    // super.dispose();
    // }
    // }
    @Override
    protected void handleSpecificDispose() {
        // No additional cleanup needed for service handler
        synchronized (serviceLock) {
            if (service != null) {
                try {
                    tearDownSubscriptionsForPublisher(service.getUID().toString());
                    logger.debug("{}Removed service change listener", LOG_CLEANUP);
                } catch (Exception e) {
                    logger.warn("{}Failed to remove service change listener: {}", LOG_CLEANUP, e.getMessage());
                }
                service = null;
                serviceAvailable = false;
            }
        }
    }

    // /**
    // * Validates the configuration of the service handler.
    // *
    // * <p>
    // * This method performs configuration validation by:
    // * 1. Checking required configuration parameters
    // * 2. Validating parameter values
    // * 3. Setting default values if needed
    // * 4. Updating internal state
    // * </p>
    // *
    // * <p>
    // * Configuration Parameters:
    // * - deviceId: Required, identifies the Homekit server
    // * - serviceId: Required, identifies the service
    // * - accessoryId: Optional, defaults to "1"
    // * </p>
    // *
    // * <p>
    // * Error Handling:
    // * - Throws IllegalArgumentException for invalid configurations
    // * - Logs configuration issues
    // * - Maintains consistent state
    // * </p>
    // *
    // * @param config The configuration to validate
    // * @throws IllegalArgumentException if the configuration is invalid
    // */
    // private void validateConfiguration(Configuration config) {
    // if (config == null) {
    // throw new IllegalArgumentException("Configuration cannot be null");
    // }

    // this.deviceId = (String) config.get(CONFIG_DEVICE_ID);
    // this.serviceId = (String) config.get(CONFIG_SERVICE_ID);
    // this.accessoryId = (String) config.get(CONFIG_ACCESSORY_ID);

    // if (deviceId == null || deviceId.trim().isEmpty()) {
    // throw new IllegalArgumentException("Configuration must contain a valid deviceId");
    // }
    // if (serviceId == null || serviceId.trim().isEmpty()) {
    // throw new IllegalArgumentException("Configuration must contain a valid serviceId");
    // }
    // if (accessoryId == null || accessoryId.trim().isEmpty()) {
    // accessoryId = DEFAULT_ACCESSORY_ID;
    // }
    // }

    @Override
    protected void validateSpecificConfiguration(Configuration config) {
        // No additional validation needed for service handler
        this.serviceId = (String) config.get(CONFIG_SERVICE_ID);
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration must contain a valid serviceId");
        }
    }

    // /**
    // * Handles configuration updates.
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for configuration updates
    // * - State changes are atomic
    // * </p>
    // *
    // * <p>
    // * State Transitions:
    // * - Validates new configuration
    // * - Updates thing configuration
    // * - Reinitializes channels if necessary
    // * - Updates thing status
    // * </p>
    // *
    // * @param configurationParameters The new configuration parameters
    // */
    // @Override
    // public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
    // try {
    // if (disposed) {
    // return;
    // }

    // Configuration newConfig = new Configuration(configurationParameters);
    // validateConfiguration(newConfig);

    // // Update configuration
    // Configuration currentConfig = thing.getConfiguration();
    // currentConfig.setProperties(newConfig.getProperties());
    // updateConfiguration(currentConfig);

    // // Reinitialize channels if necessary
    // if (configurationParameters.containsKey(CONFIG_SERVICE_ID)
    // || configurationParameters.containsKey(CONFIG_ACCESSORY_ID)) {
    // initializeChannels();
    // }

    // validateAndUpdateState();
    // } catch (Exception e) {
    // logger.error("{}Failed to update configuration: {}", e.getMessage(), e);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
    // }
    // }

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
     * // *
     * </p>
     * //
     */
    // protected void validateAndUpdateState() {
    // try {
    // synchronized (stateLock) {
    // if (disposed) {
    // return;
    // }

    // ThingStatus newStatus = determineThingStatus();
    // ThingStatusDetail newDetail = determineThingStatusDetail();
    // String newDescription = determineThingStatusDescription();

    // updateState(newStatus, newDetail, newDescription);
    // }
    // } catch (Exception e) {
    // logger.error("{}Error validating and updating state: {}", LOG_PREFIX, e.getMessage(), e);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    // }
    // }

    @Override
    protected ThingStatus determineThingStatus() {
        if (!serverConnected) {
            return ThingStatus.OFFLINE;
        }
        if (!serverPaired) {
            return ThingStatus.OFFLINE;
        }

        if (!serviceAvailable) {
            return ThingStatus.OFFLINE;
        }
        return ThingStatus.ONLINE;
    }

    @Override
    protected ThingStatusDetail determineThingStatusDetail() {
        if (!serverConnected) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        if (!serverPaired) {
            return ThingStatusDetail.CONFIGURATION_ERROR;
        }

        if (!serviceAvailable) {
            return ThingStatusDetail.COMMUNICATION_ERROR;
        }
        return ThingStatusDetail.NONE;
    }

    @Override
    protected String determineThingStatusDescription() {
        if (!serverConnected) {
            return "Server disconnected";
        }
        if (!serverPaired) {
            return "Server not paired";
        }
        if (!serviceAvailable) {
            return "HomekitService not available";
        }
        return "Server is connected and paired";
    }

    @Override
    protected void handleAccessoryServiceAdded(HomekitAccessoryEvent event) {

        try {
            synchronized (accessoryLock) {
                event.getAccessory().ifPresentOrElse(eventAccessory -> {
                    HomekitAccessory foundAccessory = accessoryRegistry.get(eventAccessory.getUID());
                    if (foundAccessory != null) {
                        setAccessory(foundAccessory);

                        // eventManager.subscribe(HomekitEventType.ACCESSORY_STATE_CHANGED,
                        // foundAccessory.getUID().toString(), this);

                        // TODO : What should a thing do if it receives an accessory state changed event for an
                        // accessory that is not found in the registry?
                        // TODO : What wiht the added service ? Subscribe to its events ?

                        eventSubscriptions.add(eventManager.subscribe(HomekitEventType.ACCESSORY_STATE_CHANGED,
                                (UID) foundAccessory.getUID(), (UID) thing.getUID(),
                                someEvent -> onAccessoryEvent((HomekitAccessoryEvent) someEvent)));

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

            HomekitAccessory currentAccessory = getAccessory();

            if (currentAccessory != null) {
                synchronized (serviceLock) {
                    event.getService().ifPresentOrElse(eventService -> {
                        Optional<HomekitService> foundService = currentAccessory.getService(serviceId);
                        foundService.ifPresent(someService -> {
                            setService(service);
                            eventSubscriptions.add(eventManager.subscribe(HomekitEventType.SERVICE_STATE_CHANGED,
                                    (UID) someService.getUID(), (UID) thing.getUID(),
                                    someEvent -> onServiceEvent((HomekitServiceEvent) someEvent)));
                            validateAndUpdateState();
                        });
                        // ... handle not found if needed ...
                    }, () -> {
                        logger.warn("{}Received event with null service", LOG_EVENT);
                    });
                }
            }
        } catch (Exception e) {
            logger.error("{}Error handling accessory event: {}", LOG_EVENT, e.getMessage(), e);
            handleError(ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error handling accessory event: %s", e.getMessage()), e);
        }
    }

    @Override
    protected void handleAccessoryServiceRemoved(HomekitAccessoryEvent event) {
        synchronized (accessoryLock) {
            synchronized (accessoryLock) {
                if (accessory != null) {
                    try {
                        tearDownSubscriptionsForPublisher(accessory.getUID().toString());
                    } catch (Exception e) {
                        logger.warn("{}Failed to remove accessory change listener: {}", LOG_EVENT, e.getMessage());
                    }
                    setAccessory(null);
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "HomekitAccessory removed");
                }
            }

            synchronized (serviceLock) {
                if (service != null) {
                    try {
                        tearDownSubscriptionsForPublisher(service.getUID().toString());
                    } catch (Exception e) {
                        logger.warn("{}Failed to remove service change listener: {}", LOG_EVENT, e.getMessage());
                    }
                    setService(null);
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "HomekitService removed");
                }
            }
        }
        handleServiceRemoved();
    }

    @Override
    protected void handleAccessoryStateChanged(HomekitAccessoryEvent event) {
        // TODO : Handle the accessory state changed event
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
    // protected void updateState(ThingStatus status, ThingStatusDetail detail, @Nullable String description) {
    // synchronized (stateLock) {
    // if (disposed) {
    // return;
    // }

    // // Only update if the state has actually changed
    // if (currentStatus != status || currentStatusDetail != detail
    // || (currentStatusDescription == null && description != null)
    // || (currentStatusDescription != null && !currentStatusDescription.equals(description))) {

    // currentStatus = status;
    // currentStatusDetail = detail;
    // currentStatusDescription = description;

    // updateStatus(status, detail, description);
    // logger.debug("{}State updated to {} ({}): {}", LOG_PREFIX, status, detail, description);
    // }
    // }
    // }

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
    // protected void updateState(ThingStatus status) {
    // updateState(status, ThingStatusDetail.NONE, null);
    // }

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
    // protected void synchronizeChannels() {
    // try {
    // HomekitService currentService = getService();
    // if (currentService == null) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Cannot synchronize channels: service is not available",
    // LOG_PREFIX);
    // return;
    // }

    // Set<String> currentCharacteristicTypes = currentService.getCharacteristics().stream()
    // .map(HomekitCharacteristic::getInstanceType).collect(Collectors.toSet());

    // // Remove channels for characteristics that no longer exist
    // List<Channel> channelsToRemove = new ArrayList<>();
    // synchronized (characteristicMapLock) {
    // for (Map.Entry<Channel, HomekitCharacteristic<?>> entry : characteristicMap.entrySet()) {
    // if (!currentCharacteristicTypes.contains(entry.getValue().getType())) {
    // channelsToRemove.add(entry.getKey());
    // }
    // }
    // channelsToRemove.forEach(channel -> {
    // characteristicMap.remove(channel);
    // updateThing(editThing().withoutChannel(channel.getUID()).build());
    // });
    // }

    // // Add channels for new characteristics
    // for (HomekitCharacteristic<?> characteristic : currentService.getCharacteristics()) {
    // boolean channelExists = false;
    // synchronized (characteristicMapLock) {
    // for (HomekitCharacteristic<?> existingCharacteristic : characteristicMap.values()) {
    // if (existingCharacteristic.getType().equals(characteristic.getType())) {
    // channelExists = true;
    // break;
    // }
    // }
    // }
    // if (!channelExists) {
    // addChannelForCharacteristic(characteristic);
    // }
    // }
    // } catch (Exception e) {
    // logger.error("{}Error occurred - Type: Channel, HomekitMessage: Failed to synchronize channels: {}", LOG_PREFIX,
    // e.getMessage(), e);
    // }
    // }

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
     * //
     */
    // private void cleanupChannels() {
    // try {
    // synchronized (characteristicMapLock) {
    // // Remove all channels from the thing
    // List<Channel> channelsToRemove = new ArrayList<>(characteristicMap.keySet());
    // for (Channel channel : channelsToRemove) {
    // removeChannelForCharacteristic(characteristicMap.get(channel));
    // }
    // characteristicMap.clear();
    // }
    // } catch (Exception e) {
    // logger.error("{}Error occurred - Type: Channel, HomekitMessage: Failed to cleanup channels: {}", LOG_PREFIX,
    // e.getMessage(), e);
    // }
    // }

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
     *            // * @param newStatus The new status of the channel
     *            //
     */
    // protected void handleChannelStateTransition(Channel channel, HomekitCharacteristic<?> characteristic,
    // ThingStatus oldStatus, ThingStatus newStatus) {
    // try {
    // if (channel == null || characteristic == null) {
    // return;
    // }

    // switch (newStatus) {
    // case ONLINE:
    // // When coming online, update channel state with current characteristic value
    // Object value = characteristic.getValue();
    // if (value instanceof State) {
    // updateState(channel.getUID(), (State) value);
    // }
    // break;
    // case OFFLINE:
    // // When going offline, clear channel state
    // updateState(channel.getUID(), UnDefType.UNDEF);
    // break;
    // default:
    // break;
    // }
    // } catch (Exception e) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Failed to handle channel state transition: {}",
    // LOG_PREFIX,
    // e.getMessage());
    // }
    // }

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
     *            //
     */
    // protected void handleChannelTypeChange(Channel channel, HomekitCharacteristic<?> characteristic) {
    // try {
    // if (channel == null || characteristic == null) {
    // return;
    // }

    // ChannelTypeUID newChannelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
    // characteristic.getType());

    // // Check if channel type has changed
    // if (!channel.getChannelTypeUID().equals(newChannelTypeUID)) {
    // ChannelType newChannelType = homekitChannelTypeProvider.getChannelType(newChannelTypeUID, null);
    // if (newChannelType != null) {
    // // Create new channel with updated type
    // Channel newChannel = ChannelBuilder.create(channel.getUID()).withType(newChannelTypeUID)
    // .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription())
    // .build();

    // // Update channel in characteristic map
    // synchronized (characteristicMapLock) {
    // characteristicMap.remove(channel);
    // characteristicMap.put(newChannel, characteristic);
    // }

    // // Update thing with new channel
    // updateThing(editThing().withoutChannel(channel.getUID()).withChannel(newChannel).build());
    // }
    // }
    // } catch (Exception e) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Failed to handle channel type change: {}", LOG_PREFIX,
    // e.getMessage());
    // }
    // }

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
    // private @Nullable Channel findChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {
    // if (characteristic == null) {
    // return null;
    // }

    // synchronized (characteristicMapLock) {
    // for (Map.Entry<Channel, HomekitCharacteristic<?>> entry : characteristicMap.entrySet()) {
    // if (entry.getValue() == characteristic) {
    // return entry.getKey();
    // }
    // }
    // }
    // return null;
    // }

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
    // @Nullable
    // protected Channel addChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {
    // if (characteristic == null) {
    // return null;
    // }

    // try {
    // ChannelUID channelUID = new ChannelUID(thing.getUID(),
    // homekitChannelTypeProvider.getCharacteristicTag(characteristic.getType()));

    // ChannelTypeUID channelTypeUID = new ChannelTypeUID(HomekitBindingConstants.BINDING_ID,
    // characteristic.getType());
    // ChannelType channelType = homekitChannelTypeProvider.getChannelType(channelTypeUID, null);
    // if (channelType == null) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: No ChannelType found for characteristic {}",
    // LOG_PREFIX, characteristic.getUID());
    // return null;
    // }

    // Channel channel = ChannelBuilder.create(channelUID).withType(channelTypeUID)
    // .withLabel(characteristic.getDescription()).withDescription(characteristic.getDescription())
    // .build();

    // synchronized (characteristicMapLock) {
    // characteristicMap.put(channel, characteristic);
    // }

    // updateThing(editThing().withChannel(channel).build());
    // return channel;
    // } catch (HomekitException e) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Failed to add channel for characteristic {}: {}",
    // LOG_PREFIX, characteristic.getUID(), e.getMessage());
    // return null;
    // } catch (Exception e) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Unexpected error adding channel for characteristic {}:
    // {}",
    // LOG_PREFIX, characteristic.getUID(), e.getMessage());
    // return null;
    // }
    // }

    @Override
    protected Set<HomekitCharacteristic<?>> getCurrentCharacteristics() {
        HomekitService currentService = getService();
        if (currentService != null) {
            return currentService.getCharacteristics();
        }
        return Collections.emptySet();
    }

    @Override
    protected ChannelUID getChannelUID(HomekitCharacteristic<?> characteristic) {
        try {
            return new ChannelUID(thing.getUID(),
                    characteristicFactory.getTagFromCharacteristicType(characteristic.getType()));
        } catch (Exception e) {
            throw new IllegalArgumentException("HomekitCharacteristic type could not be determined", e);
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
    // protected void removeChannelForCharacteristic(HomekitCharacteristic<?> characteristic) {
    // if (characteristic == null) {
    // return;
    // }

    // try {
    // ChannelUID channelUID = new ChannelUID(thing.getUID(), characteristic.getUID().getHomekitId());
    // Channel channel = thing.getChannel(channelUID);
    // if (channel != null) {
    // synchronized (characteristicMapLock) {
    // characteristicMap.remove(channel);
    // }
    // updateThing(editThing().withoutChannel(channelUID).build());
    // }
    // } catch (Exception e) {
    // logger.warn("{}Warning - Type: Channel, HomekitMessage: Failed to remove channel for characteristic {}: {}",
    // LOG_PREFIX, characteristic.getUID(), e.getMessage());
    // }
    // }

    @Override
    @SuppressWarnings("null")
    protected boolean validateCharacteristicBelongsToHandler(HomekitCharacteristic<?> characteristic) {

        HomekitService charService = characteristic.getService();
        if (charService == null || service == null) {
            return false;
        }

        if (charService.getUID() == null || service.getUID() == null) {
            return false;
        }

        return charService.getUID().equals(service.getUID());
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
    // @Override
    // public void onAccessoryServerEvent(HomekitAccessoryServerEvent event) {
    // if (disposed) {
    // return;
    // }

    // try {
    // validateEventData(event);
    // processEvent(() -> handleServerStateChange(event));
    // } catch (IllegalArgumentException e) {
    // logger.error("{}Invalid server event data: {}", e.getMessage());
    // }
    // }

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
    // @Override
    // public void onAccessoryEvent(HomekitAccessoryEvent event) {
    // if (disposed) {
    // return;
    // }
    // try {
    // validateEventData(event);
    // processEvent(() -> handleAccessoryEvent(event));
    // } catch (IllegalArgumentException e) {
    // logger.error("{}Invalid accessory event data: {}", LOG_EVENT, e.getMessage());
    // } catch (Exception e) {
    // logger.error("{}Error processing accessory event: {}", LOG_EVENT, e.getMessage(), e);
    // handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_ACCESSORY_EVENT, e);
    // }
    // }

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
     *            //
     */
    // @Override
    // public void onServiceEvent(HomekitServiceEvent event) {
    // if (disposed) {
    // return;
    // }

    // try {
    // validateEventData(event);
    // processEvent(() -> handleServiceStateChange(event));
    // } catch (IllegalArgumentException e) {
    // logger.error("{}Invalid service event data: {}", LOG_EVENT, e.getMessage());
    // } catch (Exception e) {
    // logger.error("{}Error processing service event: {}", LOG_EVENT, e.getMessage(), e);
    // handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_SERVICE_EVENT, e);
    // }
    // }

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
     *            //
     */
    // @Override
    // public void onCharacteristicEvent(HomekitCharacteristicEvent event) {
    // if (disposed) {
    // return;
    // }
    // try {
    // validateEventData(event);
    // processEvent(() -> handleCharacteristicEvent(event));
    // } catch (IllegalArgumentException e) {
    // logger.error("{}Invalid characteristic event data: {}", LOG_EVENT, e.getMessage());
    // } catch (Exception e) {
    // logger.error("{}Error processing characteristic event: {}", LOG_EVENT, e.getMessage(), e);
    // handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_PROCESSING_CHARACTERISTIC_EVENT, e);
    // }
    // }

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
     *            //
     */
    // private void handleServerStateChange(HomekitAccessoryServerEvent event) {
    // try {
    // synchronized (stateLock) {
    // synchronized (serverLock) {
    // if (server != null && event.getServer().equals(server)) {
    // switch (event.getType()) {
    // case SERVER_STATE_CONNECTED:
    // serverConnected = true;
    // logger.debug("{}Debug - Type: Server, HomekitMessage: Server connected", LOG_PREFIX);
    // validateAndUpdateState();
    // break;
    // case SERVER_STATE_DISCONNECTED:
    // serverConnected = false;
    // logger.debug("{}Debug - Type: Server, HomekitMessage: Server disconnected", LOG_PREFIX);
    // validateAndUpdateState();
    // break;
    // case SERVER_STATE_PAIRED:
    // case SERVER_STATE_PAIR_VERIFIED:
    // serverPaired = true;
    // logger.debug("{}Debug - Type: Server, HomekitMessage: Server paired", LOG_PREFIX);
    // validateAndUpdateState();
    // break;
    // case SERVER_STATE_UNPAIRED:
    // case SERVER_STATE_PAIR_UNVERIFIED:
    // serverPaired = false;
    // logger.debug("{}Debug - Type: Server, HomekitMessage: Server unpaired", LOG_PREFIX);
    // validateAndUpdateState();
    // break;
    // case SERVER_STATE_MISSING_SETUP_CODE:
    // logger.warn("{}Warning - Type: Server, HomekitMessage: Server setup code missing", LOG_PREFIX);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
    // "Missing setup code");
    // break;
    // case SERVER_STATE_PAIRING_MISSING:
    // logger.warn("{}Warning - Type: Server, HomekitMessage: Server pairing information missing",
    // LOG_PREFIX);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
    // "HomekitPairing information missing");
    // break;
    // default:
    // logger.debug("{}Debug - Type: Server, HomekitMessage: Unhandled server event type: {}",
    // LOG_PREFIX, event.getType());
    // break;
    // }
    // }
    // }
    // }
    // } catch (Exception e) {
    // logger.error("{}Error - Type: Server, HomekitMessage: Server state change failed - Error: {}", LOG_PREFIX,
    // e.getMessage(), e);
    // }
    // }

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
    // private void handleServiceStateChange(HomekitServiceEvent event) {
    // try {
    // synchronized (stateLock) {
    // synchronized (serviceLock) {
    // if (service != null && event.getService().equals(service)) {
    // switch (event.getType()) {
    // case CHARACTERISTIC_ADDED:
    // handleCharacteristicAdded(event.getCharacteristic());
    // break;
    // case CHARACTERISTIC_REMOVED:
    // handleCharacteristicRemoved(event.getCharacteristic());
    // break;
    // case CHARACTERISTIC_STATE_CHANGED:
    // handleCharacteristicStateChanged(event);
    // break;
    // default:
    // logger.debug("{}Debug - Type: HomekitService, HomekitMessage: Unhandled service event type: {}",
    // LOG_PREFIX, event.getType());
    // break;
    // }
    // synchronizeChannels();
    // }
    // }
    // } catch (Exception e) {
    // handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
    // "Error processing service state change: " + e.getMessage(), e);
    // }
    // }

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

    // private void handleAccessoryEvent(HomekitAccessoryEvent event) {
    // if (event == null) {
    // logger.warn("{}Received null accessory event", LOG_EVENT);
    // return;
    // }

    // if (!event.getAccessory().equals(getAccessory())) {
    // logger.debug("{}Received event for different accessory, ignoring", LOG_EVENT);
    // return;
    // }

    // switch (event.getType()) {
    // case SERVICE_ADDED:
    // logger.debug("{}HomekitService added event received", LOG_EVENT);
    // synchronized (accessoryLock) {
    // if (accessory == null) {
    // HomekitAccessory foundAccessory = accessoryRegistry.get(new HomekitAccessoryUID(accessoryId));
    // if (foundAccessory != null) {
    // setAccessory(foundAccessory);
    // if (this instanceof org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) {
    // foundAccessory.addChangeListener(
    // (org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) this);
    // }
    // validateAndUpdateState();
    // } else {
    // logger.warn("{}HomekitAccessory not found in registry after add event", LOG_EVENT);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
    // "HomekitAccessory not found");
    // }
    // }
    // }
    // }
    // break;
    // case SERVICE_REMOVED:
    // logger.debug("{}HomekitService removed event received", LOG_EVENT);
    // synchronized (accessoryLock) {
    // if (accessory != null) {
    // try {
    // if (this instanceof org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) {
    // accessory.removeChangeListener(
    // (org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener) this);
    // }
    // } catch (Exception e) {
    // logger.warn("{}Failed to remove accessory change listener: {}", LOG_EVENT, e.getMessage());
    // }
    // accessory = null;
    // }
    // }
    // handleServiceRemoved();
    // break;
    // case SERVICE_STATE_CHANGED:
    // logger.debug("{}HomekitService state changed event received", LOG_EVENT);
    // synchronized (accessoryLock) {
    // if (accessory != null) {
    // HomekitService foundService = accessory.getService(serviceId);
    // if (foundService != null) {
    // setService(foundService);
    // validateAndUpdateState();
    // synchronizeChannels();
    // } else {
    // logger.warn("{}HomekitService not found in accessory after change event", LOG_EVENT);
    // updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
    // "HomekitService not found");
    // }
    // }
    // }
    // break;
    // default:
    // logger.warn("{}Unhandled accessory event type: {}", LOG_EVENT, event.getType());
    // break;
    // }
    // }

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
    // protected void processEvent(Runnable eventTask) {
    // synchronized (eventQueueLock) {
    // if (eventQueue.size() >= MAX_QUEUE_SIZE) {
    // logger.warn("{}Event queue overflow, dropping oldest event", LOG_EVENT);
    // eventQueue.poll(); // Remove oldest event
    // }
    // eventQueue.offer(eventTask);
    // }

    // eventExecutor.submit(() -> {
    // try {
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

    /**
     * Validates the data in an HomekitAccessoryServerEvent to ensure it contains all required information
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
     * @param event The HomekitAccessoryServerEvent to validate
     * @throws IllegalArgumentException if event data is invalid
     *             //
     */
    // private void validateEventData(HomekitAccessoryServerEvent event) {
    // if (event == null) {
    // throw new IllegalArgumentException("Event cannot be null");
    // }
    // if (event.getServer() == null) {
    // throw new IllegalArgumentException("Event server cannot be null");
    // }
    // if (event.getType() == null) {
    // throw new IllegalArgumentException("Event type cannot be null");
    // }
    // }

    // /**
    // * Validates the data in a HomekitServiceEvent to ensure it contains all required information
    // * before processing.
    // *
    // * <p>
    // * This method performs validation by checking:
    // * 1. Event is not null
    // * 2. HomekitService reference is not null
    // * 3. Event type is not null
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - No synchronization needed as this is a validation-only method
    // * - Thread-safe validation
    // * </p>
    // *
    // * @param event The HomekitServiceEvent to validate
    // * @throws IllegalArgumentException if event data is invalid
    // */
    // private void validateEventData(HomekitServiceEvent event) {
    // if (event == null) {
    // throw new IllegalArgumentException("Event cannot be null");
    // }
    // if (event.getService() == null) {
    // throw new IllegalArgumentException("Event service cannot be null");
    // }
    // if (event.getType() == null) {
    // throw new IllegalArgumentException("Event type cannot be null");
    // }
    // }

    /**
     * Validates the data in an HomekitAccessoryEvent to ensure it contains all required information
     * before processing.
     * 
     * <p>
     * This method performs validation by checking:
     * 1. Event is not null
     * 2. HomekitAccessory reference is not null
     * 3. Event type is not null
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - No synchronization needed as this is a validation-only method
     * - Thread-safe validation
     * </p>
     * // *
     * // * @param event The HomekitAccessoryEvent to validate
     * // * @throws IllegalArgumentException if event data is invalid
     * // *
     */
    // private void validateEventData(HomekitAccessoryEvent event) {
    // if (event == null) {
    // throw new IllegalArgumentException("Event cannot be null");
    // }
    // if (event.getAccessory() == null) {
    // throw new IllegalArgumentException("Event accessory cannot be null");
    // }
    // if (event.getType() == null) {
    // throw new IllegalArgumentException("Event type cannot be null");
    // }
    // }

    // /**
    // * Validates the data in a HomekitCharacteristicEvent to ensure it contains all required information
    // * before processing.
    // *
    // * <p>
    // * This method performs validation by checking:
    // * 1. Event is not null
    // * 2. HomekitCharacteristic reference is not null
    // * 3. Event type is not null
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - No synchronization needed as this is a validation-only method
    // * - Thread-safe validation
    // * </p>
    // *
    // * @param event The HomekitCharacteristicEvent to validate
    // * @throws IllegalArgumentException if event data is invalid
    // */
    // private void validateEventData(HomekitCharacteristicEvent event) {
    // if (event == null) {
    // throw new IllegalArgumentException("Event cannot be null");
    // }
    // if (event.getCharacteristic() == null) {
    // throw new IllegalArgumentException("Event characteristic cannot be null");
    // }
    // if (event.getEventType() == null) {
    // throw new IllegalArgumentException("Event type cannot be null");
    // }
    // }

    // /**
    // * Determines the current thing status detail based on component states.
    // *
    // * <p>
    // * This method evaluates the state of various components to determine
    // * the appropriate status detail for the thing.
    // * </p>
    // *
    // * @return The current thing status detail
    // */
    // protected ThingStatusDetail determineThingStatusDetail() {
    // if (!serverConnected) {
    // return ThingStatusDetail.COMMUNICATION_ERROR;
    // }
    // if (!serverPaired) {
    // return ThingStatusDetail.CONFIGURATION_ERROR;
    // }
    // if (!serviceAvailable) {
    // return ThingStatusDetail.COMMUNICATION_ERROR;
    // }
    // return ThingStatusDetail.NONE;
    // }

    // /**
    // * Determines the current thing status description based on component states.
    // *
    // * <p>
    // * This method evaluates the state of various components to provide
    // * a descriptive message about the current status.
    // * </p>
    // *
    // * @return A description of the current thing status, or null if no description is needed
    // */
    // private @Nullable String determineThingStatusDescription() {
    // if (!serverConnected) {
    // return "Server disconnected";
    // }
    // if (!serverPaired) {
    // return "Server not paired";
    // }
    // if (!serviceAvailable) {
    // return "HomekitService not available";
    // }
    // return null;
    // }

    // /**
    // * Determines the current thing status based on component states.
    // *
    // * <p>
    // * This method evaluates the state of various components to determine
    // * the overall status of the thing.
    // * </p>
    // *
    // * @return The current thing status
    // */
    // protected ThingStatus determineThingStatus() {
    // if (!serverConnected || !serverPaired || !serviceAvailable) {
    // return ThingStatus.OFFLINE;
    // }
    // return ThingStatus.ONLINE;
    // }

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
    // private void handleCharacteristicStateChanged(HomekitServiceEvent event) {
    // try {
    // HomekitCharacteristic<?> characteristic = event.getCharacteristic();
    // if (characteristic != null) {
    // Channel channel = findChannelForCharacteristic(characteristic);
    // if (channel != null) {
    // // Check if the characteristic type has changed
    // handleChannelTypeChange(channel, characteristic);

    // // Update the channel state
    // synchronized (characteristicMapLock) {
    // if (characteristicMap.get(channel) == characteristic) {
    // Object value = characteristic.getValue();
    // if (value instanceof State) {
    // updateState(channel.getUID(), (State) value);
    // logger.debug("{}Channel state updated - UID: {}, Value: {}", LOG_CHANNEL,
    // channel.getUID(), value);
    // }
    // }
    // }
    // }
    // } catch (Exception e) {
    // logger.error("{}HomekitCharacteristic state change failed - Error: {}", LOG_CHANNEL, e.getMessage());
    // handleRecoverableError(ThingStatusDetail.COMMUNICATION_ERROR,
    // "Error handling characteristic state change: " + e.getMessage(), e);
    // }
    // }

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
     *            //
     */
    // @Override
    // public void handleCommand(ChannelUID channelUID, Command command) {
    // if (disposed || channelUID == null || command == null) {
    // return;
    // }

    // try {
    // Channel channel = thing.getChannel(channelUID);
    // if (channel != null) {
    // HomekitCharacteristic<?> characteristic;
    // synchronized (characteristicMapLock) {
    // characteristic = characteristicMap.get(channel);
    // }
    // if (characteristic != null) {
    // handleCharacteristicCommand(characteristic, command);
    // }
    // }
    // } catch (Exception e) {
    // logger.warn("{}Failed to handle command for channel {}: {}", LOG_CHANNEL, channelUID, e.getMessage());
    // }
    // }

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
     *            //
     */
    // private <T> void handleCharacteristicCommand(HomekitCharacteristic<T> characteristic, Command command) {
    // if (characteristic == null || command == null) {
    // return;
    // }

    // try {
    // if (command instanceof State) {
    // @SuppressWarnings("unchecked")
    // T value = (T) command;
    // characteristic.setValue(value);
    // Channel channel = findChannelForCharacteristic(characteristic);
    // if (channel != null) {
    // updateState(channel.getUID(), (State) command);
    // logger.debug("{}Command processed - Channel: {}, Value: {}", LOG_CHANNEL, channel.getUID(),
    // command);
    // }
    // }
    // } catch (Exception e) {
    // logger.warn("{}Command processing failed - HomekitCharacteristic: {}, Error: {}", LOG_CHANNEL,
    // characteristic.getUID(), e.getMessage());
    // handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_COMMAND_PROCESSING, e);
    // }
    // }

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

    // protected void handleServiceRemoved() {
    // cleanupChannels();
    // synchronized (serviceLock) {
    // service = null;
    // }
    // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "HomekitService removed");
    // }

    /**
     * Gets the current Homekit server instance.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for server access
     * - Thread-safe server retrieval
     * </p>
     * 
     * // * @return The current Homekit server instance, or null if not set
     * // *
     */
    // private @Nullable HomekitAccessoryServer getServer() {
    // synchronized (serverLock) {
    // return server;
    // }
    // }

    // /**
    // * Sets the Homekit server instance.
    // *
    // * <p>
    // * This method updates the server reference and:
    // * 1. Validates the new server
    // * 2. Updates the server reference
    // * 3. Registers/unregisters listeners
    // * 4. Updates the thing status
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for server updates
    // * - Thread-safe server assignment
    // * </p>
    // *
    // * @param server The new Homekit server instance
    // */
    // private void setServer(@Nullable HomekitAccessoryServer server) {
    // synchronized (serverLock) {
    // this.server = server;
    // }
    // }

    // /**
    // * Gets the current Homekit accessory instance.
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for accessory access
    // * - Thread-safe accessory retrieval
    // * </p>
    // *
    // * @return The current Homekit accessory instance, or null if not set
    // */
    // private @Nullable HomekitAccessory getAccessory() {
    // synchronized (accessoryLock) {
    // return accessory;
    // }
    // }

    // /**
    // * Sets the accessory instance in a thread-safe manner.
    // *
    // * <p>
    // * This method updates the accessory reference and:
    // * 1. Validates the new accessory
    // * 2. Updates the accessory reference
    // * 3. Registers/unregisters listeners
    // * 4. Updates the thing status
    // * </p>
    // *
    // * <p>
    // * Thread Safety:
    // * - Uses synchronized blocks for accessory updates
    // * - Thread-safe accessory assignment
    // * </p>
    // *
    // * @param accessory The new Homekit accessory instance
    // */
    // private void setAccessory(@Nullable HomekitAccessory accessory) {
    // synchronized (accessoryLock) {
    // this.accessory = accessory;
    // }
    // }

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
    // private void handleError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
    // if (disposed) {
    // return;
    // }

    // String errorMessage = message;
    // if (cause != null) {
    // errorMessage += " - " + cause.getMessage();
    // logger.error("{}Error occurred - Type: {}, HomekitMessage: {}", LOG_PREFIX, detail, message, cause);
    // } else {
    // logger.error("{}Error occurred - Type: {}, HomekitMessage: {}", LOG_PREFIX, detail, message);
    // }

    // updateState(ThingStatus.OFFLINE, detail, errorMessage);
    // }

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
     *            // * @param message The error message
     *            // * @param cause The cause of the error, if any
     *            // *
     */
    // private void handleRecoverableError(ThingStatusDetail detail, String message, @Nullable Throwable cause) {
    // handleError(detail, message, cause);
    // logger.debug("{}Scheduling recovery attempt in 30 seconds", LOG_PREFIX);
    // scheduler.schedule(this::attemptRecovery, 30, TimeUnit.SECONDS);
    // }

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
    // protected void attemptRecovery() {
    // if (disposed) {
    // return;
    // }

    // try {
    // synchronized (stateLock) {
    // if (disposed) {
    // return;
    // }

    // // Attempt to recover server connection
    // synchronized (serverLock) {
    // if (server == null) {
    // HomekitAccessoryServer foundServer = serverRegistry.get(new HomekitAccessoryServerUID(deviceId));
    // if (foundServer != null) {
    // setServer(foundServer);
    // foundServer.addChangeListener(this);
    // logger.debug("{}Recovered server connection", LOG_INIT);
    // }
    // }
    // }

    // // Attempt to recover accessory
    // synchronized (accessoryLock) {
    // if (accessory == null) {
    // HomekitAccessory foundAccessory = accessoryRegistry.get(new HomekitAccessoryUID(accessoryId));
    // if (foundAccessory != null) {
    // setAccessory(foundAccessory);
    // foundAccessory.addChangeListener(this);
    // logger.debug("{}Recovered accessory connection", LOG_INIT);
    // }
    // }
    // }

    // // Attempt to recover service
    // synchronized (serviceLock) {
    // if (service == null && accessory != null) {
    // HomekitService foundService = accessory.getService(serviceId);
    // if (foundService != null) {
    // setService(foundService);
    // foundService.addChangeListener(this);
    // logger.debug("{}Recovered service connection", LOG_INIT);
    // }
    // }
    // }
    // }

    // // Revalidate state after recovery attempts
    // validateAndUpdateState();
    // } catch (Exception e) {
    // handleError(ThingStatusDetail.COMMUNICATION_ERROR, ERROR_STATE_UPDATE, e);
    // }
    // }

    @Override
    protected void performSpecificRecovery() throws Exception {
        // No additional recovery steps needed for service handler
        synchronized (serviceLock) {
            if (service == null && accessory != null) {
                Optional<HomekitService> foundService = accessory.getService(serviceId);
                foundService.ifPresentOrElse(someService -> {
                    setService(someService);
                    eventSubscriptions
                            .add(eventManager.subscribe(HomekitEventType.SERVICE_STATE_CHANGED, (UID) someService.getUID(),
                                    thing.getUID(), someEvent -> onServiceEvent((HomekitServiceEvent) someEvent)));
                    logger.debug("{}Recovered service connection", LOG_INIT);
                }, () -> {
                    setService(null);
                    logger.warn("{}HomekitService not found in accessory after recovery", LOG_EVENT);
                    updateState(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "HomekitService not found");
                });
            }
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
    // private HomekitCharacteristic<?> findCharacteristicForChannel(Channel channel) {
    // synchronized (characteristicMapLock) {
    // return characteristicMap.get(channel);
    // }
    // }
    /**
     * Gets the current Homekit service instance.
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for service access
     * - Thread-safe service retrieval
     * </p>
     * 
     * @return The current Homekit service instance, or null if not set
     */
    private @Nullable HomekitService getService() {
        synchronized (serviceLock) {
            return service;
        }
    }

    /**
     * Sets the Homekit service instance.
     * 
     * <p>
     * This method updates the service reference and:
     * 1. Validates the new service
     * 2. Updates the service reference
     * 3. Registers/unregisters listeners
     * 4. Updates the thing status
     * 5. Synchronizes channels
     * </p>
     * 
     * <p>
     * Thread Safety:
     * - Uses synchronized blocks for service updates
     * - Thread-safe service assignment
     * </p>
     * 
     * @param service The new Homekit service instance
     */
    private void setService(@Nullable HomekitService service) {
        synchronized (serviceLock) {
            this.service = service;
            serviceAvailable = (service != null);
        }
        synchronizeChannels();
    }
}
