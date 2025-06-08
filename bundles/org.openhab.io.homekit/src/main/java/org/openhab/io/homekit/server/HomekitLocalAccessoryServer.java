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

package org.openhab.io.homekit.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitCharacteristicUID;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerState;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.network.http.HomekitHttpConnectionFactory;
import org.openhab.io.homekit.network.http.HomekitRequestLogHandler;
import org.openhab.io.homekit.network.http.HomekitSessionHandler;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingFeatureFlag;
import org.openhab.io.homekit.protocol.pairing.HomekitPairingStatusFlag;
import org.openhab.io.homekit.server.servlet.HomekitAccessoryServlet;
import org.openhab.io.homekit.server.servlet.HomekitCatchAnyServlet;
import org.openhab.io.homekit.server.servlet.HomekitCharacteristicServlet;
import org.openhab.io.homekit.server.servlet.HomekitPairSetupServlet;
import org.openhab.io.homekit.server.servlet.HomekitPairVerificationServlet;
import org.openhab.io.homekit.server.servlet.HomekitPairingServlet;
import org.openhab.io.homekit.util.HomekitByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a local HomeKit accessory server that runs on the same machine as
 * OpenHAB.
 *
 * <p>
 * This class implements a local HomeKit accessory server that manages the
 * lifecycle of
 * HomeKit accessories, including server initialization, accessory registration,
 * mDNS
 * advertisement, HTTP request handling, and event processing.
 *
 * <p>
 * The class integrates with:
 * <ul>
 * <li>{@link HomekitAbstractAccessoryServer} for base server functionality</li>
 * <li>{@link MDNSService} for service discovery and advertisement</li>
 * <li>{@link Server} for HTTP request handling</li>
 * <li>{@link HomekitAccessoryRegistry} for accessory management</li>
 * <li>{@link HomekitPairingRegistry} for secure pairing management</li>
 * <li>{@link HomekitEventManager} for event handling</li>
 * </ul>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Local network service discovery via mDNS</li>
 * <li>Secure HTTP communication with HomeKit clients</li>
 * <li>Accessory lifecycle management</li>
 * <li>Event subscription and notification</li>
 * <li>Secure pairing and authentication</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitLocalAccessoryServer extends HomekitAbstractAccessoryServer {

    // ========== Log Message Prefixes ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitLocalAccessoryServer.class);
    protected static final String LOG_PREFIX = "Homekit LocalAccessoryServer: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    protected static final String LOG_PAIRING = LOG_PREFIX + "Pairing - ";
    protected static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    protected static final String LOG_SERVER = LOG_PREFIX + "Server - ";

    // ========== Server Components ==========
    private @Nullable Server server;
    private @Nullable HomekitCharacteristicServlet characteristicServlet;
    protected final MDNSService mdnsService;
    protected @Nullable ServiceDescription announcedServiceDescription;

    // ========== Synchronization ==========
    protected final Object notificationLock = new Object();
    private final Object instanceIdLock = new Object();

    // ========== Instance Management ==========
    protected @Nullable String label;
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;

    // ========== Event Handling ==========
    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    /**
     * Creates a new local HomeKit accessory server with the specified
     * configuration.
     *
     * <p>
     * This constructor initializes a local HomeKit server with explicit
     * configuration
     * parameters for network settings, security, and service integration.
     *
     * <p>
     * Key implementation details:
     * <ul>
     * <li>Validates and stores network configuration</li>
     * <li>Initializes security parameters</li>
     * <li>Sets up service integrations</li>
     * <li>Configures logging and monitoring</li>
     * </ul>
     *
     * @param category The category of accessories this server will host
     * @param address The network address to bind to
     * @param port The port to listen on
     * @param pairingId The unique pairing identifier for this server
     * @param secretKey The secret key used for encryption
     * @param mdnsService The mDNS service for advertising
     * @param accessoryRegistry The registry for managing accessories
     * @param pairingRegistry The registry for managing pairings
     * @param eventManager The manager for handling events
     * @throws HomekitConfigurationException if the configuration is invalid
     */
    public HomekitLocalAccessoryServer(HomekitAccessoryCategory category, InetAddress address, int port,
            byte[] pairingId, byte[] secretKey, MDNSService mdnsService, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry, HomekitEventManager eventManager)
            throws HomekitConfigurationException {
        super(category, address, port, pairingId, secretKey, accessoryRegistry, pairingRegistry, eventManager);
        logger.debug("{}Initializing local server - Category: {}, Address: {}, Port: {}", LOG_INIT, category, address,
                port);
        this.mdnsService = mdnsService;
        logger.debug("{}Local server initialization completed", LOG_INIT);
    }

    /**
     * Creates a new local HomeKit accessory server with auto-generated pairing ID
     * and secret key.
     *
     * <p>
     * This constructor initializes a local HomeKit server with auto-generated
     * security
     * credentials while maintaining the same network and service configuration
     * capabilities.
     *
     * <p>
     * Key implementation details:
     * <ul>
     * <li>Generates secure pairing ID and secret key</li>
     * <li>Initializes network configuration</li>
     * <li>Sets up service integrations</li>
     * <li>Configures logging and monitoring</li>
     * </ul>
     *
     * @param category The category of accessories this server will host
     * @param address The network address to bind to
     * @param port The port to listen on
     * @param mdnsService The mDNS service for advertising
     * @param accessoryRegistry The registry for managing accessories
     * @param pairingRegistry The registry for managing pairings
     * @param eventManager The manager for handling events
     * @throws HomekitConfigurationException if the configuration is invalid
     * @throws HomekitServerException if server creation fails
     */
    public HomekitLocalAccessoryServer(HomekitAccessoryCategory category, InetAddress address, int port,
            MDNSService mdnsService, HomekitAccessoryRegistry accessoryRegistry, HomekitPairingRegistry pairingRegistry,
            HomekitEventManager eventManager) throws HomekitConfigurationException, HomekitServerException {
        super(category, address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry,
                eventManager);
        this.mdnsService = mdnsService;
        logger.debug("{}Created new local server with auto-generated credentials", LOG_INIT);
    }

    // ========== Lifecycle Methods ==========
    /**
     * Initializes the server resources required for operation.
     *
     * <p>
     * This method sets up the HTTP server, servlets, and other resources needed
     * for the HomeKit server to function.
     *
     * <p>
     * Key implementation details:
     * <ul>
     * <li>Creates and configures Jetty server instance</li>
     * <li>Sets up HTTP configuration and connectors</li>
     * <li>Initializes servlets for various endpoints</li>
     * <li>Configures request logging and handlers</li>
     * </ul>
     *
     * @throws HomekitServerException if resource initialization fails
     */
    @Override
    protected void initializeResources() throws HomekitServerException {
        logger.debug("{}Initializing server resources", LOG_INIT);

        super.initializeResources();

        try {
            // Jetty
            server = new Server();
            logger.debug("{}Created Jetty server instance", LOG_INIT);

            HomekitSessionHandler homekitSessionHandler = new HomekitSessionHandler();
            logger.debug("{}Created Homekit session handler", LOG_INIT);

            HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setIdleTimeout(0);
            logger.debug("{}Configured HTTP settings - Idle timeout: {}", LOG_INIT, httpConfiguration.getIdleTimeout());

            ServerConnector http = new ServerConnector(server, new HomekitHttpConnectionFactory(homekitSessionHandler));
            http.setPort(port);
            http.setIdleTimeout(0);
            logger.debug("{}Configured server connector - Port: {}, Idle timeout: {}", LOG_INIT, http.getPort(),
                    http.getIdleTimeout());

            Objects.requireNonNull(server).addConnector(http);
            try {
                characteristicServlet = new HomekitCharacteristicServlet(this, eventManager);
                logger.debug("{}Created characteristic servlet", LOG_INIT);
            } catch (Exception e) {
                logger.error("{}Failed to create characteristic servlet: {}", LOG_ERROR, e.getMessage(), e);
                throw e;
            }

            ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContextHandler.setContextPath("/");
            servletContextHandler.setSessionHandler(homekitSessionHandler);
            logger.debug("{}Configured servlet context handler - Context path: {}", LOG_INIT,
                    servletContextHandler.getContextPath());

            // Add servlets
            addServlets(servletContextHandler);

            HomekitRequestLogHandler requestLogHandler = new HomekitRequestLogHandler();
            requestLogHandler.setHandler(servletContextHandler);
            logger.debug("{}Added request log handler", LOG_INIT);

            Objects.requireNonNull(server).setHandler(requestLogHandler);
            logger.debug("{}Server handler configuration completed", LOG_INIT);

        } catch (Exception e) {
            logger.error("{}Failed to initialize server resources: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Adds servlets to the servlet context handler.
     *
     * <p>
     * This method adds the following servlets:
     * <ul>
     * <li>Pair setup servlet for initial pairing</li>
     * <li>Pair verification servlet for secure communication</li>
     * <li>Accessory servlet for accessory information</li>
     * <li>Characteristic servlet for state management</li>
     * <li>Pairing servlet for pairing management</li>
     * <li>Catch-all servlet for other requests</li>
     * </ul>
     *
     * @param servletContextHandler The servlet context handler to add servlets to
     */
    private void addServlets(ServletContextHandler servletContextHandler) {
        logger.debug("{}Adding servlets to context handler", LOG_INIT);

        ServletHolder pairSetupHolder = new ServletHolder(new HomekitPairSetupServlet(this));
        servletContextHandler.addServlet(pairSetupHolder, "/pair-setup");
        logger.debug("{}Added pair setup servlet - Path: /pair-setup", LOG_INIT);

        ServletHolder pairVerificationHolder = new ServletHolder(new HomekitPairVerificationServlet(this));
        servletContextHandler.addServlet(pairVerificationHolder, "/pair-verify");
        logger.debug("{}Added pair verification servlet - Path: /pair-verify", LOG_INIT);

        ServletHolder accessoryHolder = new ServletHolder(new HomekitAccessoryServlet(this));
        servletContextHandler.addServlet(accessoryHolder, "/accessories");
        logger.debug("{}Added accessory servlet - Path: /accessories", LOG_INIT);

        ServletHolder characteristicsHolder = new ServletHolder(characteristicServlet);
        servletContextHandler.addServlet(characteristicsHolder, "/characteristics");
        logger.debug("{}Added characteristics servlet - Path: /characteristics", LOG_INIT);

        ServletHolder pairingsHolder = new ServletHolder(new HomekitPairingServlet(this));
        servletContextHandler.addServlet(pairingsHolder, "/pairings");
        logger.debug("{}Added pairings servlet - Path: /pairings", LOG_INIT);

        ServletHolder catchAnyHolder = new ServletHolder(new HomekitCatchAnyServlet(this));
        servletContextHandler.addServlet(catchAnyHolder, "/");
        logger.debug("{}Added catch-all servlet - Path: /", LOG_INIT);
    }

    @Override
    protected void cleanupResources() throws HomekitServerException {
        logger.debug("{}Cleaning up server resources", LOG_SERVER);

        super.cleanupResources();

        // Unregister from mDNS
        if (announcedServiceDescription != null) {
            mdnsService.unregisterService(announcedServiceDescription);
            announcedServiceDescription = null;
            logger.debug("{}mDNS service unregistered", LOG_SERVER);
        }

        // Clean up instance IDs
        synchronized (instanceIdLock) {
            usedInstanceIds.clear();
            nextInstanceId = 1;
            logger.debug("{}Instance IDs cleared and reset", LOG_SERVER);
        }
    }

    @Override
    public void start() throws HomekitServerException {
        logger.debug("{}Starting Homekit server", LOG_SERVER);
        try {
            super.start(); // This will call initializeResources() and set state to READY
            logger.debug("{}Base server initialization completed", LOG_SERVER);

            // Initialize Jetty server if not already initialized
            if (server != null && !server.isStarted()) {
                Objects.requireNonNull(server).start();
                logger.info("{}Jetty server started successfully", LOG_SERVER);
            } else {
                logger.debug("{}Jetty server already running", LOG_SERVER);
            }

        } catch (Exception e) {
            logger.error("{}Failed to start server: {}", LOG_ERROR, e.getMessage(), e);
            try {
                setState(HomekitAccessoryServerState.STOPPED);
                logger.debug("{}Server state set to STOPPED after start failure", LOG_STATE);
            } catch (HomekitServerException ex) {
                logger.error("{}Failed to set stopped state after start failure: {}", LOG_ERROR, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void stop() throws HomekitServerException {
        logger.debug("{}Stopping Homekit server", LOG_SERVER);
        try {
            // Stop Jetty server
            if (server != null && server.isStarted()) {
                Objects.requireNonNull(server).stop();
                logger.info("{}Jetty server stopped successfully", LOG_SERVER);
            } else {
                logger.debug("{}Jetty server already stopped", LOG_SERVER);
            }

            super.stop();
            logger.debug("{}Base server stopped", LOG_SERVER);

        } catch (Exception e) {
            logger.error("{}Failed to stop server: {}", LOG_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public void close() throws HomekitServerException {
        logger.info("{}Closing local server - Server: {}", LOG_SERVER, getUID());

        try {
            // First stop the server to clean up active connections
            stop();

            // Clean up Jetty server resources
            if (server != null) {
                logger.debug("{}Destroying Jetty server - Server: {}", LOG_SERVER, getUID());
                Objects.requireNonNull(server).destroy();
            }

            // Clean up mDNS service registration
            if (announcedServiceDescription != null) {
                logger.debug("{}Unregistering mDNS service - Server: {}", LOG_SERVER, getUID());
                mdnsService.unregisterService(announcedServiceDescription);
                announcedServiceDescription = null;
            }

            // Clean up instance IDs
            synchronized (instanceIdLock) {
                logger.debug("{}Clearing instance IDs - Server: {}", LOG_SERVER, getUID());
                usedInstanceIds.clear();
                nextInstanceId = 1;
            }

            // Call super.close() last to ensure proper cleanup of base class resources
            super.close();

            logger.debug("{}Local server closed successfully - Server: {}", LOG_SERVER, getUID());
        } catch (Exception e) {
            logger.error("{}Error during local server close - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Error during local server close", e);
        }
    }

    // ========== Instance ID Management ==========
    @Override
    public long getNextAvailableAccessoryId() {
        synchronized (instanceIdLock) {
            // First try to find a recycled ID
            for (long id = 1; id < nextInstanceId; id++) {
                if (!usedInstanceIds.contains(id)) {
                    usedInstanceIds.add(id);
                    logger.debug("{}Recycled instance ID: {} for server: {}", LOG_ACCESSORY, id, getUID());
                    return id;
                }
            }

            // If no recycled IDs available, use the next new ID
            long newId = nextInstanceId++;
            usedInstanceIds.add(newId);
            logger.debug("{}Assigned new instance ID: {} for server: {}", LOG_ACCESSORY, newId, getUID());
            return newId;
        }
    }

    // ========== HomekitPairing Management ==========
    /**
     * Adds a new pairing to the server.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Adds the pairing to the base server</li>
     * <li>Updates the server advertisement</li>
     * <li>Logs the pairing addition</li>
     * </ul>
     *
     * @param destinationPairingId The pairing ID of the destination device
     * @param destinationPublicKey The public key of the destination device
     * @throws HomekitServerException if pairing addition fails
     * @throws NullPointerException if either parameter is null
     */
    @Override
    public void addPairing(byte @NonNull [] destinationPairingId, byte @NonNull [] destinationPublicKey)
            throws HomekitServerException {
        logger.debug("{}Adding pairing - Destination ID: {}", LOG_PAIRING,
                HomekitByte.toHexString(destinationPairingId));
        super.addPairing(destinationPairingId, destinationPublicKey);
        advertise();
        logger.info("{}HomekitPairing added and server advertised", LOG_PAIRING);
    }

    /**
     * Removes a pairing from the server.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Removes the pairing from the base server</li>
     * <li>Updates the server advertisement</li>
     * <li>Logs the pairing removal</li>
     * </ul>
     *
     * @param destinationPairingId The pairing ID of the destination device to
     *            remove
     * @throws HomekitServerException if pairing removal fails
     * @throws NullPointerException if the pairing ID is null
     */
    @Override
    public void removePairing(byte @NonNull [] destinationPairingId) throws HomekitServerException {
        logger.debug("{}Removing pairing - Destination ID: {}", LOG_PAIRING,
                HomekitByte.toHexString(destinationPairingId));
        super.removePairing(destinationPairingId);
        advertise();
        logger.info("{}HomekitPairing removed and server advertised", LOG_PAIRING);
    }

    /**
     * Verifies the pairing status of the server.
     *
     * <p>
     * This method checks if the server is currently paired with any devices.
     *
     * @return true if the server is paired, false otherwise
     * @throws HomekitServerException if verification fails
     */
    @Override
    public boolean pairVerify() throws HomekitServerException {
        logger.debug("{}Verifying pairing", LOG_PAIRING);
        return isPaired();
    }

    /**
     * Handles pairing removal requests.
     *
     * <p>
     * This method is a no-op for local servers as pairing removal is handled
     * through the removePairing method.
     *
     * @throws HomekitServerException if an error occurs
     */
    @Override
    public void pairRemove() throws HomekitServerException {
        logger.debug("{}Remote pairing removal requested - No action needed for local server", LOG_PAIRING);
    }

    /**
     * Handles pairing setup requests.
     *
     * <p>
     * This method is a no-op for local servers as pairing setup is handled
     * through the addPairing method.
     *
     * @throws HomekitServerException if an error occurs
     */
    @Override
    public void pairSetup() throws HomekitServerException {
        logger.debug("{}Remote pairing setup requested - No action needed for local server", LOG_PAIRING);
    }

    // ========== HomekitAccessory Management ==========
    @Override
    public void updateAccessories() throws HomekitAccessoryOperationException {
        logger.debug("{}HomekitAccessory update requested - No action needed for local server", LOG_ACCESSORY);
    }

    // ========== Setup Code Management ==========
    @Override
    public @NonNull String getSetupCode() {
        logger.debug("{}Getting setup code", LOG_CONFIG);
        String currentCode = super.getSetupCode();
        if (currentCode == null || currentCode.isEmpty()) {
            String newCode;
            if (logger.isDebugEnabled()) {
                newCode = "123-12-123";
                logger.debug("{}Using debug setup code: {}", LOG_CONFIG, newCode);
            } else {
                newCode = generateSetupCode();
                logger.debug("{}Generated new setup code", LOG_CONFIG);
            }
            setSetupCode(newCode);
            try {
                setState(HomekitAccessoryServerState.READY);
            } catch (HomekitServerException ex) {
                logger.error("{}Failed to set state to READY: {}", LOG_ERROR, ex.getMessage(), ex);
            }
            logger.info("{}Setup code set and state updated to READY", LOG_CONFIG);
            return newCode;
        }
        return currentCode;
    }

    protected String generateSetupCode() {
        logger.debug("{}Generating setup code", LOG_CONFIG);
        String setupCode = String.format("%03d-%02d-%03d", HomekitEncryptionEngine.getSecureRandom().nextInt(1000),
                HomekitEncryptionEngine.getSecureRandom().nextInt(100),
                HomekitEncryptionEngine.getSecureRandom().nextInt(1000));

        if (isReservedSetupCode(setupCode)) {
            logger.debug("{}Generated reserved setup code {} - regenerating", LOG_CONFIG, setupCode);
            return generateSetupCode();
        }

        logger.debug("{}Setup code generated successfully: {}", LOG_CONFIG, setupCode);
        return setupCode;
    }

    private boolean isReservedSetupCode(String code) {
        return code.equals("000-00-000") || code.equals("111-11-111") || code.equals("222-22-222")
                || code.equals("333-33-333") || code.equals("444-44-444") || code.equals("555-55-555")
                || code.equals("666-66-666") || code.equals("777-77-777") || code.equals("888-88-888")
                || code.equals("999-99-999") || code.equals("123-45-678") || code.equals("876-54-321");
    }

    // ========== Security Management ==========
    @Override
    public boolean isSecure() {
        logger.debug("{}Security check requested - Remote controller handles security", LOG_CONFIG);
        return true;
    }

    // ========== Advertisement Management ==========
    /**
     * Advertises the server on the local network using mDNS.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Ensures the server is running</li>
     * <li>Creates advertisement properties</li>
     * <li>Updates or creates the mDNS advertisement</li>
     * <li>Updates server state to READY</li>
     * </ul>
     */
    @Override
    public synchronized void advertise() {
        logger.debug("{}Starting server advertisement for {}", LOG_SERVER, getUID());

        if (server != null && !server.isStarted()) {
            try {
                start();
                logger.debug("{}Server started for advertisement", LOG_SERVER);
            } catch (HomekitServerException e) {
                logger.error("{}Failed to start server for advertisement: {}", LOG_ERROR, e.getMessage(), e);
                try {
                    setState(HomekitAccessoryServerState.STOPPED);
                } catch (HomekitServerException ex) {
                    logger.error("{}Failed to set stopped state: {}", LOG_ERROR, ex.getMessage(), ex);
                }
                return;
            }
        }

        // Announce the accessory via MDNS
        Hashtable<@Nullable String, @Nullable String> props = createAdvertisementProperties();
        logger.debug("{}Created advertisement properties: {}", LOG_SERVER, props);

        if (announcedServiceDescription != null) {
            updateExistingAdvertisement(props);
        } else {
            createNewAdvertisement(props);
        }
    }

    /**
     * Creates the properties for mDNS advertisement.
     *
     * <p>
     * This method creates a set of properties required for mDNS advertisement,
     * including:
     * <ul>
     * <li>Status flags indicating pairing state</li>
     * <li>Device ID for unique identification</li>
     * <li>Model name and configuration number</li>
     * <li>Feature flags and protocol version</li>
     * <li>Accessory category identifier</li>
     * </ul>
     *
     * @return Hashtable containing the advertisement properties
     */
    private Hashtable<@Nullable String, @Nullable String> createAdvertisementProperties() {
        Hashtable<@Nullable String, @Nullable String> props = new Hashtable<>();

        // Status flags (e.g. "0x04" for bit 3). Value should be an unsigned integer.
        // See Table 6-8 (page 58). Required.
        props.put("sf", Integer.toString(!isPaired() ? HomekitPairingStatusFlag.NOT_PAIRED.getMask()
                : HomekitPairingStatusFlag.UNKNOWN.getMask()));

        // Device ID ("5.4 Device ID" (page 31)) of the accessory. The Device ID must be
        // formatted as
        // "XX:XX:XX:XX:XX:XX", where "XX" is a hexadecimal string representing a byte.
        // Required.
        // This value is also used as the accessory's HomekitPairing Identifier. This
        // identifier of the accessory must
        // be a unique random number generated at every factory reset and must persist
        // across reboots.
        props.put("id", new String(getPairingId(), StandardCharsets.UTF_8));

        // Model name of the accessory (e.g. "Device1,1"). Required.
        props.put("md", getClass().getSimpleName());

        // Current configuration number. Required.
        // Must update when an accessory, service, or characteristic is added or removed
        // on the accessory server.
        // Accessories must increment the config number after a firmware update.
        // This must have a range of 1-65535 and wrap to 1 when it overflows.
        // This value must persist across reboots, power cycles, etc.
        if (getConfigurationIndex() == 65535) {
            try {
                setConfigurationIndex(1);
            } catch (HomekitConfigurationException ex) {
                logger.error("{}Failed to set configuration index: {}", LOG_ERROR, ex.getMessage(), ex);
            }
        }
        props.put("c#", Integer.toString(getConfigurationIndex()));
        try {
            setConfigurationIndex(getConfigurationIndex() + 1);
        } catch (HomekitConfigurationException ex) {
            logger.error("{}Failed to increment configuration index: {}", LOG_ERROR, ex.getMessage(), ex);
        }

        // Current state number. Required.
        // This must have a value of "1".
        props.put("s#", "1");

        // HomekitPairing Feature flags (e.g. "0x3" for bits 0 and 1). Required if
        // non-zero. See Table 5-4 (page 49).
        props.put("ff", Integer.toString(HomekitPairingFeatureFlag.NOT_SUPPORTED.getMask()));

        // Protocol version string "X.Y" (e.g. "1.0"). Required if value is not "1.0".
        // props.put("pv", "1.1");

        // HomekitAccessory Category Identifier. Required. Indicates the category that
        // best describes the primary
        // function of the accessory. This must have a range of 1-65535. This must take
        // values defined in
        // "13-1 HomekitAccessory Categories" (page 252). This must persist across
        // reboots, power cycles, etc.
        props.put("ci", Integer.toString(HomekitAccessoryCategory.BRIDGES.getValue()));

        return props;
    }

    /**
     * Updates an existing mDNS advertisement with new properties.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Updates the service properties</li>
     * <li>Unregisters and re-registers the service</li>
     * <li>Updates the server state</li>
     * </ul>
     *
     * @param props The new advertisement properties
     */
    private void updateExistingAdvertisement(Hashtable<@Nullable String, @Nullable String> props) {
        logger.debug("{}Updating existing advertisement", LOG_SERVER);
        if (announcedServiceDescription != null) {
            Objects.requireNonNull(announcedServiceDescription).serviceProperties = props;
            Objects.requireNonNull(mdnsService).unregisterService(announcedServiceDescription);
            Objects.requireNonNull(mdnsService).registerService(announcedServiceDescription);
        }
        try {
            setState(HomekitAccessoryServerState.READY);
        } catch (HomekitServerException ex) {
            logger.error("{}Failed to set state to READY: {}", LOG_ERROR, ex.getMessage(), ex);
        }
        logger.info("{}Advertisement updated successfully", LOG_SERVER);
    }

    /**
     * Creates a new mDNS advertisement with the specified properties.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Creates a new service description</li>
     * <li>Registers the service with mDNS</li>
     * <li>Updates the server state</li>
     * </ul>
     *
     * @param props The advertisement properties
     */
    private void createNewAdvertisement(Hashtable<@Nullable String, @Nullable String> props) {
        logger.debug("{}Creating new advertisement", LOG_SERVER);
        announcedServiceDescription = new ServiceDescription(SERVICE_TYPE,
                "openHAB " + getClass().getSimpleName() + " " + getPort(), getPort(), props);
        mdnsService.registerService(announcedServiceDescription);
        try {
            setState(HomekitAccessoryServerState.READY);
        } catch (HomekitServerException ex) {
            logger.error("{}Failed to set state to READY: {}", LOG_ERROR, ex.getMessage(), ex);
        }
        logger.info("{}New advertisement created successfully", LOG_SERVER);
    }

    // ========== Event Handling ==========

    protected void handleCharacteristicEvent(HomekitCharacteristicEvent event) {
        logger.debug("{}Received characteristic event - Type: {}, HomekitCharacteristic: {}", LOG_EVENT,
                event.getType(), event.getCharacteristic().getClass().getSimpleName());
        if (event.getType() == HomekitEventType.CHARACTERISTIC_STATE_CHANGED && event.getCharacteristic() != null
                && characteristicServlet != null) {
            characteristicServlet.publishCharacteristicUpdate(event.getCharacteristic().get());
            logger.debug("{}Published characteristic update", LOG_EVENT);
        }
    }

    /**
     * Adds a new accessory to the server.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Validates the server's lifecycle state</li>
     * <li>Registers the accessory with the base server</li>
     * <li>Sets up event subscriptions for the accessory's characteristics</li>
     * <li>Logs the addition of the accessory</li>
     * </ul>
     *
     * @param accessory The accessory to add
     * @throws HomekitAccessoryOperationException if accessory addition fails
     * @throws NullPointerException if the accessory is null
     */
    @Override
    public void addAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        logger.debug("{}Adding accessory - ID: {}, Type: {}", LOG_ACCESSORY, accessory.getAccessoryId(),
                accessory.getClass().getSimpleName());
        try {
            validateLifecycleOperation("add accessory");
            super.addAccessory(accessory);

            // Subscribe to characteristic events using HomekitEventManager
            for (HomekitService service : accessory.getServices()) {
                for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                    eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                            (UID) characteristic.getUID(), (UID) getUID(),
                            event -> handleCharacteristicEvent((HomekitCharacteristicEvent) event)));
                    logger.debug("{}Subscribed to events for characteristic: {}", LOG_ACCESSORY,
                            characteristic.getClass().getSimpleName());
                }
            }
            logger.info("{}HomekitAccessory added successfully - ID: {}", LOG_ACCESSORY, accessory.getAccessoryId());
        } catch (HomekitServerException e) {
            logger.error("{}Failed to add accessory: {}", LOG_ERROR, e.getMessage(), e);
            throw new HomekitAccessoryOperationException("Failed to add accessory: " + e.getMessage(), e);
        }
    }

    /**
     * Removes an accessory from the server.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Validates the server's lifecycle state</li>
     * <li>Removes the accessory from the base server</li>
     * <li>Unsubscribes from all characteristic events</li>
     * <li>Cleans up event subscriptions</li>
     * <li>Logs the removal of the accessory</li>
     * </ul>
     *
     * @param accessory The accessory to remove
     * @throws HomekitAccessoryOperationException if accessory removal fails
     * @throws NullPointerException if the accessory is null
     */
    @Override
    public void removeAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        logger.debug("{}Removing accessory - ID: {}, Type: {}", LOG_ACCESSORY, accessory.getAccessoryId(),
                accessory.getClass().getSimpleName());
        try {
            validateLifecycleOperation("remove accessory");
            super.removeAccessory(accessory);

            // Collect all characteristic UIDs for this accessory
            Set<HomekitCharacteristicUID> characteristicUids = new HashSet<>();
            for (HomekitService service : accessory.getServices()) {
                for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                    characteristicUids.add(characteristic.getUID());
                }
            }

            // Remove and unsubscribe only those subscriptions that match
            eventSubscriptions.removeIf(subscription -> {
                if (characteristicUids.stream()
                        .anyMatch(uid -> uid.toString().equals(subscription.getPublisherUID().toString()))) {
                    eventManager.unsubscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                            subscription.getPublisherUID(), subscription.getSubscriber());
                    logger.debug("{}Unsubscribed from events for sourceUid: {}", LOG_ACCESSORY,
                            subscription.getPublisherUID());
                    return true;
                }
                return false;
            });

            logger.info("{}HomekitAccessory removed successfully - ID: {}", LOG_ACCESSORY, accessory.getAccessoryId());
        } catch (HomekitServerException e) {
            logger.error("{}Failed to remove accessory: {}", LOG_ERROR, e.getMessage(), e);
            throw new HomekitAccessoryOperationException("Failed to remove accessory: " + e.getMessage(), e);
        }
    }
}
