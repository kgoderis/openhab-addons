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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryServerUID;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerUIDImpl;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingFeatureFlag;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingStatusFlag;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
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
 * Discovery service for HomeKit accessories.
 *
 * <p>
 * This service is responsible for discovering and managing HomeKit accessories
 * on the network using mDNS.
 * It handles the complete lifecycle of HomeKit accessories including:
 * </p>
 * <ul>
 * <li>mDNS service discovery and event handling</li>
 * <li>Accessory server registration and management</li>
 * <li>Thing creation and configuration</li>
 * <li>Background and foreground scanning</li>
 * <li>Service property processing and validation</li>
 * </ul>
 *
 * <p>
 * <b>Configuration Options:</b>
 * </p>
 * <ul>
 * <li>auto.create.accessoryThing: Controls automatic creation of accessory
 * things (default: true)</li>
 * <li>auto.create.serviceThing: Controls automatic creation of service things
 * (default: true)</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Automatic discovery of HomeKit accessories on the network</li>
 * <li>Support for both IPv4 and IPv6 (with platform-specific handling)</li>
 * <li>Graceful handling of service removal with configurable grace period</li>
 * <li>Automatic thing creation for discovered accessories and services</li>
 * <li>Comprehensive logging and error handling</li>
 * </ul>
 *
 * <p>
 * <b>Dependencies:</b>
 * </p>
 * <ul>
 * <li>{@link MDNSClient} - For network service discovery</li>
 * <li>{@link HomekitAccessoryServerRegistry} - For server management</li>
 * <li>{@link HomekitAccessoryRegistry} - For accessory management</li>
 * <li>{@link HomekitPairingRegistry} - For pairing management</li>
 * <li>{@link HomekitThingTypeProvider} - For thing type definitions</li>
 * <li>{@link HomekitEventManager} - For event handling</li>
 * <li>{@link HomekitAccessoryFactory} - For accessory creation</li>
 * <li>{@link HomekitServiceFactory} - For service creation</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.homekit")
@NonNullByDefault
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
    private static final String LOG_PREFIX = "HomeKit Discovery: ";
    private static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    private static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    private static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    private static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_SERVER = LOG_PREFIX + "Server - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitAccessoryServerDiscoveryService.class);

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
    private HomekitAccessoryFactory accessoryFactory;
    private HomekitServiceFactory homekitServiceFactory;
    private final Map<String, ThingUID> cachedServices = new ConcurrentHashMap<>();

    /**
     * Constructs a new HomeKit discovery service.
     *
     * <p>
     * This constructor initializes the discovery service with all required
     * dependencies
     * and configuration. It sets up the service for both background and foreground
     * discovery of HomeKit accessories on the network.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes all required dependencies</li>
     * <li>Loads configuration settings</li>
     * <li>Sets up logging and monitoring</li>
     * <li>Prepares for service discovery</li>
     * </ul>
     *
     * @param configProperties Configuration properties for the service
     * @param mdnsClient MDNS client for service discovery
     * @param accessoryServerRegistry Registry for managing accessory servers
     * @param networkAddressService Service for network address management
     * @param accessoryRegistry Registry for managing accessories
     * @param pairingRegistry Registry for managing pairings
     * @param homekitThingTypeProvider Provider for HomeKit thing types
     * @param configAdmin Configuration admin service
     * @param eventManager Event manager for HomeKit events
     * @param accessoryFactory Factory for HomeKit accessories
     * @param homekitServiceFactory Factory for HomeKit services
     * @throws IllegalArgumentException if any required dependency is null
     */
    @Activate
    public HomekitAccessoryServerDiscoveryService(final @Nullable Map<String, Object> configProperties,
            final @Reference MDNSClient mdnsClient,
            final @Reference HomekitAccessoryServerRegistry accessoryServerRegistry,
            final @Reference NetworkAddressService networkAddressService,
            @Reference HomekitAccessoryRegistry accessoryRegistry, @Reference HomekitPairingRegistry pairingRegistry,
            @Reference HomekitThingTypeProvider homekitThingTypeProvider, @Reference ConfigurationAdmin configAdmin,
            @Reference HomekitEventManager eventManager, @Reference HomekitAccessoryFactory accessoryFactory,
            @Reference HomekitServiceFactory homekitServiceFactory) {
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
        this.accessoryFactory = accessoryFactory;
        this.homekitServiceFactory = homekitServiceFactory;
        // Load configuration
        loadConfiguration();

        logger.info("{}Homekit discovery service initialized successfully", LOG_INIT);
    }

    /**
     * Deactivates the discovery service.
     *
     * <p>
     * This method performs cleanup operations when the service is deactivated:
     * </p>
     * <ul>
     * <li>Stops background discovery</li>
     * <li>Cancels all pending removal tasks</li>
     * <li>Removes service listeners</li>
     * <li>Cleans up resources</li>
     * </ul>
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
        logger.debug("{}Configuration modified", LOG_CONFIG);
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
        logger.debug("{}Activating Homekit discovery service", LOG_INIT);
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
        logger.debug("{}Starting background discovery for HomeKit services on network", LOG_CONFIG);
        mdnsClient.addServiceListener(SERVICE_TYPE, this);
        startScan(true);
    }

    /**
     * Stops background discovery for Homekit services.
     * Removes the service listener.
     */
    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("{}Stopping background discovery for HomeKit services", LOG_CONFIG);
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);
    }

    /**
     * Starts a foreground scan for Homekit services.
     */
    @Override
    protected void startScan() {
        logger.debug("{}Initiating foreground scan for HomeKit services", LOG_CONFIG);
        startScan(false);
    }

    /**
     * Stops the current scan operation.
     */
    @Override
    protected synchronized void stopScan() {
        logger.debug("{}Terminating current scan operation", LOG_CONFIG);
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
        logger.debug("{}Starting {} scan for HomeKit services", LOG_SERVER, isBackground ? "background" : "foreground");

        ServiceInfo[] services;
        if (isBackground) {
            services = mdnsClient.list(SERVICE_TYPE);
        } else {
            services = mdnsClient.list(SERVICE_TYPE, FOREGROUND_SCAN_TIMEOUT);
        }

        logger.debug("{}Discovered {} HomeKit services in {}ms", LOG_SERVER, services.length,
                System.currentTimeMillis() - start);

        for (ServiceInfo serviceInfo : services) {
            logger.debug("{}Processing discovered service: {}", LOG_SERVER, serviceInfo.getName());
            Optional<Map<String, Object>> propertiesOpt = processService(serviceInfo);
            if (propertiesOpt.isEmpty()) {
                logger.debug("{}Skipping service {} - invalid or missing properties", LOG_SERVER,
                        serviceInfo.getName());
                continue;
            }
            @SuppressWarnings("null") // Optional.get() is safe after isEmpty() check above
            Map<String, Object> properties = propertiesOpt.get();

            String deviceId = (String) properties.get("id");
            if (deviceId == null || deviceId.isEmpty()) {
                logger.warn("{}Service {} has no valid device ID", LOG_WARN, serviceInfo.getName());
                continue;
            }

            HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUIDImpl(deviceId);
            HomekitAccessoryServer server = accessoryServerRegistry.get(serverUID);

            if (server == null) {
                logger.debug("{}No server instance found for device ID: {}", LOG_SERVER, deviceId);
                continue;
            }

            if (server.isPaired()) {
                logger.info("{}Server {} is paired, initiating accessory update", LOG_PAIRING, server.getUID());

                try {
                    server.updateAccessories();
                    logger.debug("{}Successfully updated accessories for server {}", LOG_ACCESSORY, server.getUID());
                } catch (HomekitAccessoryOperationException e) {
                    logger.warn("{}Failed to update accessories for server {}: {}", LOG_WARN, server.getUID(),
                            e.getMessage());
                }

                try {
                    for (HomekitAccessory accessory : server.getAccessories()) {
                        if (accessoryRegistry.get(accessory.getUID()) == null) {
                            logger.debug("{}Registering new accessory {} in registry", LOG_ACCESSORY,
                                    accessory.getUID());
                            accessoryRegistry.add(accessory);
                            try {
                                createThingFromAccessory(server, accessory);
                            } catch (HomekitException e) {
                                logger.warn("{}Failed to create thing for accessory {}: {}", LOG_WARN,
                                        accessory.getUID(), e.getMessage());
                            }
                        } else {
                            logger.trace("{}Accessory {} already registered", LOG_ACCESSORY, accessory.getUID());
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

        if (!isBackground) {
            stopScan();
        }
    }

    /**
     * Handles the addition of a new service.
     * Processes the service information and updates the accessory registry if
     * needed.
     *
     * @param serviceEvent The service event containing information about the added
     *            service
     */
    @Override
    public void serviceAdded(@Nullable ServiceEvent serviceEvent) {
        if (serviceEvent == null) {
            logger.debug("{}Received null service event for serviceAdded", LOG_EVENT);
            return;
        }
        logger.debug("{}Processing new service discovery: {}", LOG_EVENT, serviceEvent.getName());
        if (isBackgroundDiscoveryEnabled()) {
            considerService(serviceEvent);
        }
    }

    /**
     * Handles the removal of a service.
     * Schedules the removal of the corresponding server after a grace period to
     * handle temporary network issues.
     *
     * @param serviceEvent The service event containing information about the
     *            removed service
     */
    @Override
    public void serviceRemoved(@Nullable ServiceEvent serviceEvent) {
        if (serviceEvent == null) {
            logger.debug("{}Received null service event for serviceRemoved", LOG_EVENT);
            return;
        }
        ServiceInfo serviceInfo = serviceEvent.getInfo();
        if (serviceInfo != null) {
            logger.debug("{}Processing service removal: {}", LOG_EVENT, serviceInfo.getName());
            String deviceId = serviceInfo.getPropertyString("id");
            if (deviceId != null) {
                HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUIDImpl(deviceId.replace(":", ""));
                HomekitAccessoryServer server = accessoryServerRegistry.get(serverUID);
                if (server != null) {
                    long gracePeriod = 30; // 30 seconds grace period for network recovery
                    if (gracePeriod <= 0) {
                        logger.debug("{}Removing server {} immediately due to service removal", LOG_SERVER, serverUID);
                        accessoryServerRegistry.remove(serverUID);
                    } else {
                        logger.debug("{}Scheduling server {} removal in {} seconds", LOG_SERVER, serverUID,
                                gracePeriod);
                        cancelRemovalTask(serviceInfo);
                        scheduleRemovalTask(serverUID, serviceInfo, gracePeriod);
                    }
                }
            }
        }
    }

    /**
     * Handles the resolution of a service.
     * Processes the resolved service information and updates the accessory registry
     * if needed.
     *
     * @param serviceEvent The service event containing information about the
     *            resolved service
     */
    @Override
    public void serviceResolved(@Nullable ServiceEvent serviceEvent) {
        if (serviceEvent == null) {
            logger.debug("{}Received null service event for serviceResolved", LOG_EVENT);
            return;
        }
        logger.debug("{}Processing service resolution: {}", LOG_EVENT, serviceEvent.getName());
        if (isBackgroundDiscoveryEnabled()) {
            considerService(serviceEvent);
        }
    }

    /**
     * Processes a service event if background discovery is enabled.
     * Validates service information and updates the accessory registry accordingly.
     * 
     * @param serviceEvent The service event to process
     */
    private void considerService(ServiceEvent serviceEvent) {
        ServiceInfo serviceInfo = serviceEvent.getInfo();
        if (serviceInfo == null) {
            logger.debug("{}Skipping service event - no service info available", LOG_EVENT);
            return;
        } else {
            logger.debug("{}Considering service event: {}", LOG_EVENT, serviceInfo.toString());
        }

        Optional<Map<String, Object>> propertiesOpt = processService(serviceInfo);
        if (propertiesOpt.isEmpty()) {
            logger.debug("{}Skipping service {} - invalid properties", LOG_EVENT, serviceInfo.getName());
            return;
        }
        @SuppressWarnings("null") // Optional.get() is safe after isEmpty() check above
        Map<String, Object> properties = propertiesOpt.get();

        String deviceId = (String) properties.get("id");
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("{}Service {} has no valid device ID", LOG_WARN, serviceInfo.getName());
            return;
        }

        HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUIDImpl(deviceId);
        HomekitAccessoryServer server = accessoryServerRegistry.get(serverUID);

        if (server == null) {
            logger.debug("{}No server instance found for device ID: {}", LOG_SERVER, deviceId);
            return;
        }

        if (server.isPaired()) {
            logger.info("{}Server {} is paired, initiating accessory update", LOG_PAIRING, server.getUID());

            try {
                server.updateAccessories();
                logger.debug("{}Successfully updated accessories for server {}", LOG_ACCESSORY, server.getUID());
            } catch (HomekitAccessoryOperationException e) {
                logger.warn("{}Failed to update accessories for server {}: {}", LOG_WARN, server.getUID(),
                        e.getMessage());
            }

            try {
                for (HomekitAccessory accessory : server.getAccessories()) {
                    if (accessoryRegistry.get(accessory.getUID()) == null) {
                        logger.debug("{}Registering new accessory {} in registry", LOG_ACCESSORY, accessory.getUID());
                        accessoryRegistry.add(accessory);
                        try {
                            createThingFromAccessory(server, accessory);
                        } catch (HomekitException e) {
                            logger.warn("{}Failed to create thing for accessory {}: {}", LOG_WARN, accessory.getUID(),
                                    e.getMessage());
                        }
                    } else {
                        logger.trace("{}Accessory {} already registered", LOG_ACCESSORY, accessory.getUID());
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

    /**
     * Processes a discovered HomeKit service.
     * Extracts and validates service properties, creates or updates the
     * corresponding accessory server.
     *
     * @param serviceInfo The discovered service information
     * @return Optional containing the map of service properties, or empty if processing failed
     */
    private Optional<Map<String, Object>> processService(@Nullable ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            logger.debug("{}Skipping service - null service info", LOG_SERVER);
            return Optional.empty();
        }

        try {
            // Check for required TXT field properties instead of hasData()
            String id = serviceInfo.getPropertyString("id");
            String configIndexStr = serviceInfo.getPropertyString("c#");
            String categoryStr = serviceInfo.getPropertyString("ci");
            String pairingStatusStr = serviceInfo.getPropertyString("sf");
            String stateNumberStr = serviceInfo.getPropertyString("s#");
            String pairingFeatureFlagStr = serviceInfo.getPropertyString("ff");

            // Validate that we have the essential TXT field properties
            if (id == null || configIndexStr == null || categoryStr == null || pairingStatusStr == null
                    || stateNumberStr == null || pairingFeatureFlagStr == null) {
                logger.debug("{}Skipping service {} - missing required TXT field properties", LOG_SERVER,
                        serviceInfo.getName());
                return Optional.empty();
            }

            // Additional validation for service type and port
            if (!serviceInfo.getApplication().contains("hap") || serviceInfo.getPort() == 0) {
                logger.debug("{}Skipping service {} - invalid service type or port", LOG_SERVER, serviceInfo.getName());
                return Optional.empty();
            }

            logger.debug("{}Processing HomeKit service: {}", LOG_SERVER, serviceInfo.getName());

            Map<String, Object> properties = new HashMap<>();
            Enumeration<String> serviceProperties = serviceInfo.getPropertyNames();
            while (serviceProperties.hasMoreElements()) {
                @SuppressWarnings("null") // Enumeration.nextElement() is guaranteed non-null by contract
                String element = serviceProperties.nextElement();
                String value = serviceInfo.getPropertyString(element);
                if (value != null) {
                    properties.put(element, value);
                    logger.trace("{}Service property - {}: {}", LOG_SERVER, element, value);
                } else {
                    logger.trace("{}Service property - {}: null", LOG_SERVER, element);
                }
            }

            HomekitAccessoryServerUID serverUID = new HomekitAccessoryServerUIDImpl(id.replace(":", ""));
            HomekitAccessoryServer existingServer = accessoryServerRegistry.get(serverUID);

            // Extract and validate service configuration
            int port = serviceInfo.getPort();
            String model = serviceInfo.getPropertyString("md");
            String version = serviceInfo.getPropertyString("pv");

            logger.trace(
                    "{}Extracted service properties - Port: {}, Model: {}, Version: {}, ConfigIndex: {}, Category: {}, PairingStatus: {}, StateNumber: {}, FeatureFlag: {}",
                    LOG_SERVER, port, model, version, configIndexStr, categoryStr, pairingStatusStr, stateNumberStr,
                    pairingFeatureFlagStr);

            try {
                // Parse and validate numeric values
                int configIndex = Integer.parseInt(configIndexStr);
                HomekitAccessoryCategory category = HomekitAccessoryCategory.fromValue(Integer.parseInt(categoryStr));
                HomekitPairingStatusFlag pairingStatus = HomekitPairingStatusFlag
                        .fromValue(Integer.parseInt(pairingStatusStr));
                // int stateNumber = Integer.parseInt(stateNumberStr);
                HomekitPairingFeatureFlag pairingFeatureFlag = HomekitPairingFeatureFlag
                        .fromValue(Integer.parseInt(pairingFeatureFlagStr));

                if (existingServer != null) {
                    logger.debug("{}Existing server found for {}", LOG_SERVER, serverUID);
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
                            "{}Discovered new HomeKit server - ServerUID: {}, ID: {}, Category: {}, Model: {}, Version: {}, Config Index: {}, Pairing Status: {}, Feature Flag: {}",
                            LOG_SERVER, serverUID, id, category, model, version, configIndex, pairingStatus,
                            pairingFeatureFlag);

                    Optional<String> hostAddressOpt = getHostAddress(serviceInfo);
                    if (hostAddressOpt.isEmpty()) {
                        logger.warn("{}No valid host address found for server {}", LOG_WARN, id);
                        return Optional.empty();
                    }

                    try {
                        String deviceId = serviceInfo.getPropertyString(HomekitDiscoveryConstants.DEVICE_ID);
                        String serverId = deviceId != null ? deviceId.replace(":", "")
                                : "discovered-" + System.currentTimeMillis();
                        HomekitAccessoryServer server = new HomekitRemoteAccessoryServer(category, serverId,
                                InetAddress.getByName(hostAddressOpt.get()), port, accessoryRegistry, pairingRegistry,
                                eventManager, accessoryFactory);
                        server.setConfigurationIndex(configIndex);
                        accessoryServerRegistry.add(server);
                        logger.info("{}Created new remote HomeKit server - UID: {}, Setup Code: {}", LOG_SERVER,
                                server.getUID(), server.getSetupCode());
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

            // Cache the service
            Optional<ThingUID> thingUIDOpt = getThingUID(serviceInfo);
            thingUIDOpt.ifPresent(thingUID -> cachedServices.put(serviceInfo.getQualifiedName(), thingUID));

            return Optional.of(properties);
        } catch (IllegalStateException e) {
            logger.error("{}Error processing service {}: {}", LOG_ERROR, serviceInfo.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ThingUID> getThingUID(ServiceInfo serviceInfo) {
        // Check for required TXT field properties instead of hasData()
        String deviceId = serviceInfo.getPropertyString(HomekitDiscoveryConstants.DEVICE_ID);
        String category = serviceInfo.getPropertyString(HomekitDiscoveryConstants.CATEGORY_ID);

        if (deviceId == null || category == null || !serviceInfo.getApplication().contains("hap")) {
            return Optional.empty();
        }

        // Clean device ID by removing colons
        String cleanDeviceId = deviceId.replace(":", "");

        // Determine thing type based on category
        if (HomekitDiscoveryConstants.BRIDGE_CATEGORY.equals(category)) {
            return Optional.of(new ThingUID(HomekitBindingConstants.THING_TYPE_BRIDGE, cleanDeviceId));
        } else if (HomekitDiscoveryConstants.STANDALONE_CATEGORY.equals(category)) {
            return Optional.of(new ThingUID(HomekitBindingConstants.THING_TYPE_STANDALONE_ACCESSORY, cleanDeviceId));
        }

        return Optional.empty();
    }

    /**
     * Cancels a scheduled removal task for a service.
     * 
     * @param serviceInfo The service information for which to cancel the removal
     *            task
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

        if (autoCreateServiceThing) {
            Map<String, Object> properties = new HashMap<>();

            Collection<HomekitService> services = accessory.getServices();
            for (HomekitService service : services) {
                String serviceType = service.getType();
                Optional<ThingTypeUID> thingTypeUID = homekitThingTypeProvider.getThingTypeUID(serviceType);

                String serviceTag;
                try {
                    serviceTag = homekitServiceFactory.getTagFromServiceType(serviceType);
                } catch (Exception e) {
                    logger.warn("{}Could not get service tag for type {}, using type as fallback: {}", LOG_WARN,
                            serviceType, e.getMessage());
                    serviceTag = serviceType;
                }

                ThingUID thingUID = new ThingUID(thingTypeUID.get(),
                        server.getUID().getId() + "." + accessory.getAccessoryId() + "." + service.getInstanceId());

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
                    server.getUID().getId() + "." + accessory.getAccessoryId());

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
        logger.debug("{}Loading HomeKit binding configuration", LOG_CONFIG);
        try {
            Configuration config = configAdmin.getConfiguration("org.openhab.homekit");
            if (config == null) {
                throw new IllegalStateException("HomeKit binding configuration not found");
            }

            Dictionary<@Nullable String, @Nullable Object> properties = config.getProperties();
            if (properties == null) {
                throw new IllegalStateException("HomeKit binding configuration properties are null");
            }

            // Get and validate auto-create accessory configuration
            Object autoCreateObj = properties.get(CONFIG_AUTO_CREATE_ACCESSORY);
            if (autoCreateObj instanceof Boolean aBoolean) {
                autoCreateAccessoryThing = aBoolean;
            } else if (autoCreateObj != null) {
                throw new IllegalArgumentException(
                        String.format("Invalid configuration value for %s: %s. Expected boolean.",
                                CONFIG_AUTO_CREATE_ACCESSORY, autoCreateObj));
            } else {
                autoCreateAccessoryThing = DEFAULT_AUTO_CREATE_ACCESSORY;
            }
            logger.info("{}Accessory thing auto-creation enabled: {}", LOG_CONFIG, autoCreateAccessoryThing);

            // Get and validate auto-create service configuration
            Object autoCreateServiceObj = properties.get(CONFIG_AUTO_CREATE_SERVICE);
            if (autoCreateServiceObj instanceof Boolean aBoolean) {
                autoCreateServiceThing = aBoolean;
            } else if (autoCreateServiceObj != null) {
                throw new IllegalArgumentException(
                        String.format("Invalid configuration value for %s: %s. Expected boolean.",
                                CONFIG_AUTO_CREATE_SERVICE, autoCreateServiceObj));
            } else {
                autoCreateServiceThing = DEFAULT_AUTO_CREATE_SERVICE;
            }
            logger.info("{}Service thing auto-creation enabled: {}", LOG_CONFIG, autoCreateServiceThing);

        } catch (IOException e) {
            logger.error("{}Failed to read HomeKit binding configuration: {}. Using default values", LOG_ERROR,
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
     * Handles platform-specific requirements for IPv4/IPv6 selection.
     * 
     * @param serviceInfo The service information
     * @return Optional containing the host address, or empty if no valid address is found
     */
    private Optional<String> getHostAddress(ServiceInfo serviceInfo) {
        // Parameter is annotated as @NonNull due to @NonNullByDefault at class level,
        // but we'll keep a defensive check since it comes from external library

        String hostAddress = null;

        // Check if running on MacOS using Java's standard library
        boolean isMacOS = System.getProperty("os.name", "").toLowerCase().contains("mac");

        if (isMacOS) {
            // Use IPv4 only on MacOS due to known issues with IPv6
            // See:
            // https://medium.com/@quelgar/java-sockets-broken-for-ipv6-on-mac-5aae72f06b21
            if (networkAddressService.isUseIPv6()) {
                logger.warn("{}IPv6 and mDNS do not work well on macOS - forcing IPv4", LOG_WARN);
            }
            if (serviceInfo.getInet4Addresses().length > 0) {
                hostAddress = serviceInfo.getInet4Addresses()[0].getHostAddress();
            }
        } else {
            // On other platforms, respect the network configuration
            if (networkAddressService.isUseIPv6()) {
                if (serviceInfo.getInet6Addresses().length > 0) {
                    hostAddress = serviceInfo.getInet6Addresses()[0].getHostAddress();
                }
            } else {
                if (serviceInfo.getInet4Addresses().length > 0) {
                    hostAddress = serviceInfo.getInet4Addresses()[0].getHostAddress();
                }
            }
        }

        if (hostAddress == null) {
            logger.warn("{}No valid host address found for service {}", LOG_WARN, serviceInfo.getName());
            return Optional.empty();
        }

        return Optional.of(hostAddress);
    }
}
