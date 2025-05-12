package org.openhab.io.homekit.network.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.factory.HomekitFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingFeatureFlag;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingStatusFlag;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
import org.openhab.io.homekit.server.HomekitAccessoryServerUID;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Homekit accessories.
 * This service listens for Homekit accessories on the network using mDNS and creates corresponding things in the
 * system.
 * 
 * Configuration options:
 * - auto.create.accessoryThing: Enable/disable automatic creation of accessory things (default: true)
 * - auto.create.serviceThing: Enable/disable automatic creation of service things (default: true)
 * 
 * @author OpenHAB
 */
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.homekit")
public class HomekitAccessoryServerDiscoveryService extends AbstractDiscoveryService implements ServiceListener {
    /** Timeout for foreground scans in milliseconds */
    private static final Duration FOREGROUND_SCAN_TIMEOUT = Duration.ofMillis(200);
    /** Homekit service type for mDNS discovery */
    private static final String SERVICE_TYPE = "_hap._tcp.local.";

    /** Configuration keys */
    private static final String CONFIG_AUTO_CREATE_ACCESSORY = "auto.create.accessoryThing";
    private static final String CONFIG_AUTO_CREATE_SERVICE = "auto.create.serviceThing";
    /** Default configuration values */
    private static final boolean DEFAULT_AUTO_CREATE_ACCESSORY = true;
    private static final boolean DEFAULT_AUTO_CREATE_SERVICE = true;

    /** Logging prefixes */
    private static final String LOG_PREFIX = "Homekit Discovery: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_STATE = LOG_PREFIX + "State - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    private static final String LOG_PAIRING = LOG_PREFIX + "HomekitPairing - ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerDiscoveryService.class);

    private final MDNSClient mdnsClient;
    private final HomekitAccessoryServerRegistry accessoryServerRegistry;
    private final Map<@Nullable String, @Nullable ScheduledFuture<?>> deviceRemovalTasks = new ConcurrentHashMap<>();
    private final NetworkAddressService networkAddressService;
    private final HomekitAccessoryRegistry accessoryRegistry;
    private final HomekitPairingRegistry pairingRegistry;
    private final HomekitThingTypeProvider homekitThingTypeProvider;
    private boolean autoCreateAccessoryThing;
    private boolean autoCreateServiceThing;
    private ConfigurationAdmin configAdmin;
    private HomekitEventManager eventManager;
    private Set<HomekitFactory> homekitFactories;

    /**
     * Constructs a new Homekit discovery service.
     * 
     * @param configProperties Configuration properties for the service
     * @param mdnsClient MDNS client for service discovery
     * @param accessoryServerRegistry Registry for managing accessory servers
     * @param networkAddressService HomekitService for network address management
     * @param accessoryRegistry Registry for managing accessories
     * @param pairingRegistry Registry for managing pairings
     * @param homekitThingTypeProvider Provider for Homekit thing types
     * @param configAdmin Configuration admin service
     * @throws IllegalArgumentException if any required dependency is null
     */
    @Activate
    public HomekitAccessoryServerDiscoveryService(final @Nullable Map<String, Object> configProperties,
            final @Reference MDNSClient mdnsClient,
            final @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            final @Reference NetworkAddressService networkAddressService,
            @Reference HomekitAccessoryRegistry accessoryRegistry, @Reference HomekitPairingRegistry pairingRegistry,
            @Reference HomekitThingTypeProvider homekitThingTypeProvider, @Reference ConfigurationAdmin configAdmin,
            @Reference HomekitEventManager eventManager, @Reference Set<HomekitFactory> homekitFactories) {
        super(5);
        logger.debug("{}Initializing Homekit discovery service", LOG_INIT);

        // Initialize dependencies
        this.mdnsClient = mdnsClient;
        this.accessoryServerRegistry = accessoryServerRegistry;
        this.networkAddressService = networkAddressService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.configAdmin = configAdmin;
        this.eventManager = eventManager;
        this.homekitFactories = homekitFactories;
        // Load configuration
        loadConfiguration();

        logger.info("{}Homekit discovery service initialized successfully", LOG_INIT);
    }

    /**
     * Deactivates the discovery service.
     * Stops background discovery, cancels all pending removal tasks, and cleans up resources.
     */
    @Deactivate
    @Override
    protected void deactivate() {
        logger.debug("{}Deactivating Homekit discovery service", LOG_INIT);

        // Stop background discovery
        stopBackgroundDiscovery();

        // Cancel all pending removal tasks
        deviceRemovalTasks.values().forEach(task -> {
            if (task != null) {
                task.cancel(false);
            }
        });
        deviceRemovalTasks.clear();

        // Remove all service listeners
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);

        // Clean up any remaining servers
        // accessoryServerRegistry.getAll().forEach(server -> {
        // try {
        // server.stop();
        // } catch (Exception e) {
        // logger.warn("{}Failed to stop server {} during deactivation: {}", LOG_WARN,
        // server.getUID(), e.getMessage());
        // }
        // });

        super.deactivate();
        logger.info("{}Homekit discovery service deactivated", LOG_INIT);
    }

    /**
     * Handles configuration updates for the service.
     * 
     * @param configProperties Updated configuration properties
     */
    @Modified
    @Override
    protected void modified(@Nullable Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    /**
     * Activates the discovery service.
     * Initializes background discovery if enabled.
     * 
     * @param configProperties Configuration properties for activation
     */
    @Override
    @Activate
    protected void activate(@Nullable Map<String, Object> configProperties) {
        super.activate(configProperties);
        if (isBackgroundDiscoveryEnabled()) {
            logger.debug("{}Enabling background discovery for service type: {}", LOG_CONFIG, SERVICE_TYPE);
            mdnsClient.addServiceListener(SERVICE_TYPE, this);
        }
    }

    /**
     * Starts background discovery for Homekit services.
     * Registers the service listener and initiates a scan.
     */
    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("{}Starting background discovery for service type: {}", LOG_CONFIG, SERVICE_TYPE);
        mdnsClient.addServiceListener(SERVICE_TYPE, this);
        startScan(true);
    }

    /**
     * Stops background discovery for Homekit services.
     * Removes the service listener.
     */
    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("{}Stopping background discovery for service type: {}", LOG_CONFIG, SERVICE_TYPE);
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);
    }

    /**
     * Starts a foreground scan for Homekit services.
     */
    @Override
    protected void startScan() {
        startScan(false);
    }

    /**
     * Stops the current scan operation.
     */
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
    }

    /**
     * Initiates a scan for Homekit services.
     * 
     * @param isBackground Whether the scan is running in background mode
     */
    private void startScan(boolean isBackground) {
        scheduler.schedule(() -> {
            scan(isBackground);
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * Performs the actual scan for Homekit services.
     * Processes discovered services and updates the registry accordingly.
     * 
     * @param isBackground Whether the scan is running in background mode
     */
    private void scan(boolean isBackground) {
        long start = System.currentTimeMillis();
        logger.debug("{}Starting {} scan for Homekit services", LOG_SERVER, isBackground ? "background" : "foreground");

        ServiceInfo[] services;
        if (isBackground) {
            services = mdnsClient.list(SERVICE_TYPE);
        } else {
            services = mdnsClient.list(SERVICE_TYPE, FOREGROUND_SCAN_TIMEOUT);
        }

        logger.debug("{}Found {} Homekit services in {}ms", LOG_SERVER, services.length,
                System.currentTimeMillis() - start);

        for (ServiceInfo serviceInfo : services) {
            logger.debug("{}Processing service: {}", LOG_SERVER, serviceInfo.getName());
            Map<String, String> properties = processService(serviceInfo);

            if (properties == null) {
                logger.debug("{}Skipping service {} - no valid properties found", LOG_SERVER, serviceInfo.getName());
                continue;
            }

            @Nullable
            String deviceId = properties.get("id");
            if (deviceId == null || deviceId.isEmpty()) {
                logger.warn("{}HomekitService {} has no device ID", LOG_WARN, serviceInfo.getName());
                continue;
            }

            HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUID(deviceId);
            HomekitAccessoryServer server = accessoryServerRegistry.get(serverUID);

            if (server == null) {
                logger.debug("{}No server found for device ID: {}", LOG_SERVER, deviceId);
                continue;
            }

            if (server.isPaired()) {
                logger.info("{}Server {} is paired, updating accessories", LOG_PAIRING, server.getUID());

                try {
                    server.updateAccessories();
                    logger.debug("{}Successfully updated accessories for server {}", LOG_ACCESSORY, server.getUID());
                } catch (HomekitAccessoryOperationException e) {
                    logger.warn("{}Failed to update accessories for server {}: {}", LOG_WARN, server.getUID(),
                            e.getMessage());
                }

                try {
                    for (HomekitAccessory accessory : server.getAccessories()) {
                        if (accessoryRegistry != null && accessoryRegistry.get(accessory.getUID()) == null) {
                            logger.debug("{}Adding new accessory {} to registry", LOG_ACCESSORY, accessory.getUID());
                            accessoryRegistry.add(accessory);
                            try {
                                createThingFromAccessory(server, accessory);
                            } catch (HomekitException e) {
                                logger.warn("{}Failed to create thing for accessory {}: {}", LOG_WARN,
                                        accessory.getUID(), e.getMessage());
                            }
                        } else {
                            logger.trace("{}HomekitAccessory {} already exists in registry", LOG_ACCESSORY,
                                    accessory.getUID());
                        }
                    }
                } catch (HomekitAccessoryOperationException e) {
                    logger.warn("{}Failed to process accessories for server {}: {}", LOG_WARN, server.getUID(),
                            e.getMessage());
                }
            } else {
                logger.warn("{}Server {} is not paired, skipping accessory processing", LOG_PAIRING, server.getUID());
            }
        }
    }

    /**
     * Handles the addition of a new service.
     * 
     * @param serviceEvent The service event containing information about the added service
     */
    @Override
    public void serviceAdded(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        logger.debug("{}New service added: {}", LOG_EVENT, serviceEvent.getName());
        considerService(serviceEvent);
    }

    /**
     * Handles the removal of a service.
     * Schedules the removal of the corresponding server after a grace period.
     * 
     * @param serviceEvent The service event containing information about the removed service
     */
    @Override
    public void serviceRemoved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        ServiceInfo serviceInfo = serviceEvent.getInfo();
        if (serviceInfo != null) {
            logger.debug("{}HomekitService removed: {}", LOG_EVENT, serviceInfo.getName());
            HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUID(serviceInfo.getName());
            HomekitAccessoryServer server = accessoryServerRegistry.get(serverUID);
            if (server != null) {
                long gracePeriod = 30; // 30 seconds grace period
                if (gracePeriod <= 0) {
                    logger.debug("{}Removing server {} immediately", LOG_SERVER, serverUID);
                    accessoryServerRegistry.remove(serverUID);
                } else {
                    logger.debug("{}Scheduling removal of server {} in {} seconds", LOG_SERVER, serverUID, gracePeriod);
                    cancelRemovalTask(serviceInfo);
                    scheduleRemovalTask(serverUID, serviceInfo, gracePeriod);
                }
            }
        }
    }

    /**
     * Handles the resolution of a service.
     * 
     * @param serviceEvent The service event containing information about the resolved service
     */
    @Override
    public void serviceResolved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        logger.debug("{}HomekitService resolved: {}", LOG_EVENT, serviceEvent.getName());
        considerService(serviceEvent);
    }

    /**
     * Processes a service event if background discovery is enabled.
     * 
     * @param serviceEvent The service event to process
     */
    private void considerService(ServiceEvent serviceEvent) {
        if (isBackgroundDiscoveryEnabled()) {
            logger.debug("{}Processing service in background discovery: {}", LOG_EVENT, serviceEvent.getName());
            processService(serviceEvent.getInfo());
        }
    }

    /**
     * Processes a discovered Homekit service.
     * Extracts service properties and creates or updates the corresponding accessory server.
     * 
     * @param serviceInfo The discovered service information
     * @return Map of service properties, or null if processing failed
     */
    private @Nullable Map<String, String> processService(@Nullable ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            logger.warn("{}Received null service info, skipping processing", LOG_WARN);
            return null;
        }

        Map<String, String> properties = new HashMap<>();
        try {
            if (!serviceInfo.hasData() || !serviceInfo.getApplication().contains("hap") || serviceInfo.getPort() == 0) {
                return null;
            }

            logger.debug("{}Processing Homekit service: {}", LOG_SERVER, serviceInfo.getName());

            // Extract all service properties
            Enumeration<@Nullable String> serviceProperties = serviceInfo.getPropertyNames();
            while (serviceProperties.hasMoreElements()) {
                @Nullable
                String element = serviceProperties.nextElement();
                if (element != null) {
                    String value = serviceInfo.getPropertyString(element);
                    if (value != null) {
                        properties.put(element, value);
                    }
                }
            }

            // Validate required service properties
            String id = serviceInfo.getPropertyString("id");
            if (id == null) {
                throw new IllegalStateException("HomekitService " + serviceInfo.getName() + " has no ID property");
            }

            HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUID(id.replace(":", ""));
            HomekitAccessoryServer existingServer = accessoryServerRegistry.get(serverUID);

            // Extract and validate service configuration
            int port = serviceInfo.getPort();
            String deviceId = serviceInfo.getPropertyString("id");
            String model = serviceInfo.getPropertyString("md");
            String version = serviceInfo.getPropertyString("pv");
            String configIndexStr = serviceInfo.getPropertyString("c#");
            String categoryStr = serviceInfo.getPropertyString("ci");
            String pairingStatusStr = serviceInfo.getPropertyString("sf");
            String stateNumberStr = serviceInfo.getPropertyString("s#");
            String pairingFeatureFlagStr = serviceInfo.getPropertyString("ff");

            if (configIndexStr == null || categoryStr == null || pairingStatusStr == null || stateNumberStr == null
                    || pairingFeatureFlagStr == null) {
                throw new IllegalStateException("Missing required service properties for " + serviceInfo.getName());
            }

            try {
                // Parse and validate numeric values
                int configIndex = Integer.parseInt(configIndexStr);
                HomekitAccessoryCategory category = HomekitAccessoryCategory.fromValue(Integer.parseInt(categoryStr));
                HomekitPairingStatusFlag pairingStatus = HomekitPairingStatusFlag
                        .fromValue(Integer.parseInt(pairingStatusStr));
                int stateNumber = Integer.parseInt(stateNumberStr);
                HomekitPairingFeatureFlag pairingFeatureFlag = HomekitPairingFeatureFlag
                        .fromValue(Integer.parseInt(pairingFeatureFlagStr));

                if (existingServer != null) {
                    // Update existing server configuration if needed
                    if (existingServer.getConfigurationIndex() != configIndex) {
                        logger.info("{}Updating configuration index for server {} from {} to {}", LOG_CONFIG,
                                existingServer.getUID(), existingServer.getConfigurationIndex(), configIndex);
                        try {
                            existingServer.setConfigurationIndex(configIndex);
                        } catch (HomekitServerException e) {
                            logger.warn("{}Failed to update configuration index for server {}: {}", LOG_WARN,
                                    existingServer.getUID(), e.getMessage());
                        }
                    }
                } else {
                    // Create new server for discovered accessory
                    logger.info(
                            "{}Discovered new Homekit server - ID: {}, Category: {}, Model: {}, Version: {}, Config Index: {}, HomekitPairing Status: {}, Feature Flag: {}",
                            LOG_SERVER, id, category, model, version, configIndex, pairingStatus, pairingFeatureFlag);

                    String hostAddress = getHostAddress(serviceInfo);
                    if (hostAddress == null) {
                        logger.warn("{}No valid host address found for server {}", LOG_WARN, id);
                        return null;
                    }

                    try {
                        HomekitAccessoryServer server = new HomekitRemoteAccessoryServer(category,
                                InetAddress.getByName(hostAddress), port, accessoryRegistry, pairingRegistry,
                                eventManager, homekitFactories);
                        server.setConfigurationIndex(configIndex);
                        accessoryServerRegistry.add(server);
                        logger.info("{}Created new Remote HomekitAccessory Server - UID: {}, Setup Code: {}",
                                LOG_SERVER, server.getUID(), server.getSetupCode());
                    } catch (IOException e) {
                        logger.error("{}Failed to create accessory server: {}", LOG_ERROR, e.getMessage(), e);
                    } catch (HomekitServerException e) {
                        logger.error("{}Failed to initialize accessory server: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }

                cancelRemovalTask(serviceInfo);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid numeric value in service properties for " + serviceInfo.getName(), e);
            }
        } catch (IllegalStateException e) {
            logger.error("{}Error processing service {}: {}", LOG_ERROR, serviceInfo.getName(), e.getMessage());
            return null;
        }
        return properties;
    }

    /**
     * Cancels a scheduled removal task for a service.
     * 
     * @param serviceInfo The service information for which to cancel the removal task
     */
    private void cancelRemovalTask(ServiceInfo serviceInfo) {
        ScheduledFuture<?> deviceRemovalTask = deviceRemovalTasks.remove(serviceInfo.getQualifiedName());
        if (deviceRemovalTask != null) {
            logger.debug("{}Cancelled removal task for service: {}", LOG_EVENT, serviceInfo.getQualifiedName());
            deviceRemovalTask.cancel(false);
        }
    }

    /**
     * Schedules a removal task for a server after a grace period.
     * 
     * @param serverUID The UID of the server to remove
     * @param serviceInfo The service information
     * @param gracePeriod The grace period in seconds before removal
     */
    private void scheduleRemovalTask(HomekitAccessoryServerUID serverUID, ServiceInfo serviceInfo, long gracePeriod) {
        logger.debug("{}Scheduling removal task for server {} in {} seconds", LOG_SERVER, serverUID, gracePeriod);
        deviceRemovalTasks.put(serviceInfo.getQualifiedName(), scheduler.schedule(() -> {
            logger.info("{}Removing server {} after grace period", LOG_SERVER, serverUID);
            accessoryServerRegistry.remove(serverUID);
            cancelRemovalTask(serviceInfo);
        }, gracePeriod, TimeUnit.SECONDS));
    }

    /**
     * Creates a thing from an accessory.
     * 
     * @param server The accessory server
     * @param accessory The accessory to create a thing for
     * @throws HomekitException if there is an error creating the thing
     */
    private void createThingFromAccessory(HomekitAccessoryServer server, HomekitAccessory accessory)
            throws HomekitException {
        logger.debug("{}Creating thing from accessory {} on server {}", LOG_ACCESSORY, accessory.getUID(),
                server.getUID());

        if (autoCreateServiceThing && homekitThingTypeProvider != null) {
            Map<String, Object> properties = new HashMap<>();

            Collection<HomekitService> services = accessory.getServices();
            for (HomekitService service : services) {
                String serviceType = service.getType();
                ThingTypeUID thingTypeUID = homekitThingTypeProvider.getThingTypeUID(serviceType);

                String serviceTag;
                try {
                    serviceTag = homekitThingTypeProvider.getServiceTag(serviceType);
                } catch (HomekitException e) {
                    logger.warn("{}Could not get service tag for type {}, using type as fallback: {}", LOG_WARN,
                            serviceType, e.getMessage());
                    serviceTag = serviceType;
                }

                ThingUID thingUID = new ThingUID(thingTypeUID, server.getUID().getPairingId() + "."
                        + accessory.getAccessoryId() + "." + service.getInstanceId());

                DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withProperty("accessoryId", accessory.getAccessoryId())
                        .withProperty("instanceId", service.getInstanceId())
                        .withProperty("pairingId", new String(server.getPairingId(), StandardCharsets.UTF_8))
                        .withLabel("Homekit " + serviceTag);

                thingDiscovered(builder.build());

                logger.debug("{}Discovered service - Type: {}, ID: {}, Label: {}, HomekitAccessory: {}", LOG_ACCESSORY,
                        serviceType, thingUID, serviceTag, accessory.getUID());
            }
        }

        if (autoCreateAccessoryThing) {
            ThingUID thingUID = new ThingUID(HomekitBindingConstants.THING_TYPE_ACCESSORY,
                    server.getUID().getPairingId() + "." + accessory.getAccessoryId());

            DiscoveryResultBuilder builder = DiscoveryResultBuilder.create(thingUID)
                    .withProperty("accessoryId", accessory.getAccessoryId())
                    .withProperty("pairingId", new String(server.getPairingId(), StandardCharsets.UTF_8))
                    .withLabel("Homekit " + accessory.getClass().getSimpleName());

            thingDiscovered(builder.build());
            logger.debug("{}Created thing {} for Homekit accessory {} on server {}", LOG_ACCESSORY, thingUID,
                    accessory.getAccessoryId(), server.getUID());
        }
    }

    /**
     * Loads and validates the Homekit binding configuration.
     * Sets default values if configuration is missing or invalid.
     */
    private void loadConfiguration() {
        logger.debug("{}Loading Homekit binding configuration", LOG_CONFIG);
        try {
            Configuration config = configAdmin.getConfiguration("org.openhab.homekit");
            if (config == null) {
                throw new IllegalStateException("Configuration for 'org.openhab.homekit' not found");
            }

            Dictionary<@Nullable String, @Nullable Object> properties = config.getProperties();
            if (properties == null) {
                throw new IllegalStateException("Configuration properties for 'org.openhab.homekit' are null");
            }

            // Get and validate auto-create accessory configuration
            Object autoCreateObj = properties.get(CONFIG_AUTO_CREATE_ACCESSORY);
            if (autoCreateObj instanceof Boolean aBoolean) {
                autoCreateAccessoryThing = aBoolean;
            } else if (autoCreateObj != null) {
                throw new IllegalArgumentException(String.format("Invalid value for %s: %s. Expected boolean.",
                        CONFIG_AUTO_CREATE_ACCESSORY, autoCreateObj));
            } else {
                autoCreateAccessoryThing = DEFAULT_AUTO_CREATE_ACCESSORY;
            }
            logger.info("{}Thing auto-creation enabled: {}", LOG_CONFIG, autoCreateAccessoryThing);

            // Get and validate auto-create service configuration
            Object autoCreateServiceObj = properties.get(CONFIG_AUTO_CREATE_SERVICE);
            if (autoCreateServiceObj instanceof Boolean aBoolean) {
                autoCreateServiceThing = aBoolean;
            } else if (autoCreateServiceObj != null) {
                throw new IllegalArgumentException(String.format("Invalid value for %s: %s. Expected boolean.",
                        CONFIG_AUTO_CREATE_SERVICE, autoCreateServiceObj));
            } else {
                autoCreateServiceThing = DEFAULT_AUTO_CREATE_SERVICE;
            }
            logger.info("{}HomekitService thing auto-creation enabled: {}", LOG_CONFIG, autoCreateServiceThing);

        } catch (IOException e) {
            logger.error("{}Failed to read Homekit binding configuration: {}. Using default values", LOG_ERROR,
                    e.getMessage(), e);
            autoCreateAccessoryThing = DEFAULT_AUTO_CREATE_ACCESSORY;
            autoCreateServiceThing = DEFAULT_AUTO_CREATE_SERVICE;
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("{}Configuration error: {}. Using default values", LOG_ERROR, e.getMessage());
            autoCreateAccessoryThing = DEFAULT_AUTO_CREATE_ACCESSORY;
            autoCreateServiceThing = DEFAULT_AUTO_CREATE_SERVICE;
        }
    }

    /**
     * Gets the host address for a service based on system configuration.
     * Prefers IPv4 on macOS and respects IPv6 preference on other systems.
     * 
     * @param serviceInfo The service information
     * @return The host address, or null if no valid address is found
     */
    private @Nullable String getHostAddress(ServiceInfo serviceInfo) {
        if (SystemUtils.IS_OS_MAC) {
            if (networkAddressService.isUseIPv6()) {
                logger.warn("{}IPv6 and MDNS may not work well on MacOS", LOG_WARN);
            }
            if (serviceInfo.getInet4Addresses().length > 0) {
                return serviceInfo.getInet4Addresses()[0].getHostAddress();
            }
        } else {
            if (networkAddressService.isUseIPv6()) {
                if (serviceInfo.getInet6Addresses().length > 0) {
                    return serviceInfo.getInet6Addresses()[0].getHostAddress();
                }
            } else {
                if (serviceInfo.getInet4Addresses().length > 0) {
                    return serviceInfo.getInet4Addresses()[0].getHostAddress();
                }
            }
        }
        return null;
    }
}
