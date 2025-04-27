package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.PairingFeatureFlag;
import org.openhab.io.homekit.api.hap.PairingStatusFlag;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.exception.AccessoryOperationException;
import org.openhab.io.homekit.exception.ConfigurationException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent.CharacteristicEventType;
import org.openhab.io.homekit.internal.http.HomekitHttpConnectionFactory;
import org.openhab.io.homekit.internal.http.HomekitRequestLogHandler;
import org.openhab.io.homekit.internal.http.HomekitSessionHandler;
import org.openhab.io.homekit.internal.server.servlet.AccessoryServlet;
import org.openhab.io.homekit.internal.server.servlet.CatchAnyServlet;
import org.openhab.io.homekit.internal.server.servlet.CharacteristicServlet;
import org.openhab.io.homekit.internal.server.servlet.PairSetupServlet;
import org.openhab.io.homekit.internal.server.servlet.PairVerificationServlet;
import org.openhab.io.homekit.internal.server.servlet.PairingServlet;
import org.openhab.io.homekit.util.Byte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalAccessoryServer extends AbstractAccessoryServer implements CharacteristicChangeListener {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(LocalAccessoryServer.class);

    // ========== Server Components ==========
    private Server server;
    private CharacteristicServlet characteristicServlet;
    protected final MDNSService mdnsService;
    protected ServiceDescription announcedServiceDescription;

    // ========== Synchronization ==========
    protected final Object notificationLock = new Object();
    private final Object instanceIdLock = new Object();

    // ========== Instance Management ==========
    protected String label;
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;

    // ========== Constructors ==========
    public LocalAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingId,
            byte[] secretKey, MDNSService mdnsService, AccessoryRegistry accessoryRegistry,
            PairingRegistry pairingRegistry) throws ConfigurationException  {
        super(category, address, port, pairingId, secretKey, accessoryRegistry, pairingRegistry);
        logger.debug("{}Initializing local server - Category: {}, Address: {}, Port: {}", LOG_INIT, category, address,
                port);
        this.mdnsService = mdnsService;
        logger.debug("{}Local server initialization completed", LOG_INIT);
    }

    public LocalAccessoryServer(AccessoryCategory category, InetAddress address, int port, MDNSService mdnsService,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws ConfigurationException, HomekitServerException {
        this(category, address, port, generatePairingId(), generateSecretKey(), mdnsService, accessoryRegistry,
                pairingRegistry);
    }

    // ========== Lifecycle Methods ==========
    @Override
    protected void initializeResources() throws HomekitServerException {
        logger.debug("{}Initializing server resources", LOG_INIT);

        super.initializeResources();

        try {
            // Jetty
            server = new Server();
            logger.debug("{}Created Jetty server instance", LOG_INIT);

            HomekitSessionHandler homekitSessionHandler = new HomekitSessionHandler();
            logger.debug("{}Created HomeKit session handler", LOG_INIT);

            HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setIdleTimeout(0);
            logger.debug("{}Configured HTTP settings - Idle timeout: {}", LOG_INIT, httpConfiguration.getIdleTimeout());

            ServerConnector http = new ServerConnector(server, new HomekitHttpConnectionFactory(homekitSessionHandler));
            http.setPort(port);
            http.setIdleTimeout(0);
            logger.debug("{}Configured server connector - Port: {}, Idle timeout: {}", LOG_INIT, http.getPort(),
                    http.getIdleTimeout());

            server.addConnector(http);
            characteristicServlet = new CharacteristicServlet(this);
            logger.debug("{}Created characteristic servlet", LOG_INIT);

            // ServletContextHandler servletContextHandler = new ServletContextHandler(
            //     ServletContextHandler.SESSIONS | ServletContextHandler.NO_SECURITY);
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

            server.setHandler(requestLogHandler);
            logger.debug("{}Server handler configuration completed", LOG_INIT);

        } catch (Exception e) {
            logger.error("{}Failed to initialize server resources: {}", LOG_ERROR, e.getMessage(), e);
            throw e;
        }


        // Netty - Do not Delete

        // logger.debug("Attempting {}:{}", localAddress, port);
        //
        // final ServerBootstrap bootstrap = new ServerBootstrap();
        // bootstrap.channel(NioServerSocketChannel.class);
        // bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup());
        //
        // WebappConfiguration webapp = new WebappConfiguration()
        // .addServletConfigurations(new ServletConfiguration(this, PairSetupServlet.class, "/pair-setup/"))
        // .addServletConfigurations(
        // new ServletConfiguration(this, PairVerificationServlet.class, "/pair-verify/"))
        // .addServletConfigurations(new ServletConfiguration(this, AccessoryServlet.class, "/accessories/"))
        // .addServletConfigurations(
        // new ServletConfiguration(this, CharacteristicServlet.class, "/characteristics/"))
        // .addServletConfigurations(new ServletConfiguration(this, PairingServlet.class, "/pairings/"));
        //
        // bootstrap.childHandler(new HomekitChannelInitializer(webapp));
        //
        // // Set up the event pipeline factory.
        // // bootstrap.setPipelineFactory(new ServletBridgeChannelPipelineFactory(webapp));
        //
        // final ChannelFuture serverChannel = bootstrap.bind(localAddress, port);
        // final CompletableFuture<Integer> portFuture = new CompletableFuture<Integer>();
        // serverChannel.addListener(new GenericFutureListener<Future<? super Void>>() {
        //
        // @Override
        // public void operationComplete(Future<? super Void> future) throws Exception {
        // try {
        // future.get();
        // SocketAddress socketAddress = serverChannel.channel().localAddress();
        // if (socketAddress instanceof InetSocketAddress) {
        // logger.debug("bound homekit listener to " + socketAddress.toString());
        // portFuture.complete(((InetSocketAddress) socketAddress).getPort());
        // } else {
        // throw new RuntimeException(
        // "Unknown socket address type: " + socketAddress.getClass().getName());
        // }
        // } catch (Exception e) {
        // portFuture.completeExceptionally(e);
        // }
        // }
        // });
    }

    private void addServlets(ServletContextHandler servletContextHandler) {
        logger.debug("{}Adding servlets to context handler", LOG_INIT);

        ServletHolder pairSetupHolder = new ServletHolder(new PairSetupServlet(this));
        servletContextHandler.addServlet(pairSetupHolder, "/pair-setup");
        logger.debug("{}Added pair setup servlet - Path: /pair-setup", LOG_INIT);

        ServletHolder pairVerificationHolder = new ServletHolder(new PairVerificationServlet(this));
        servletContextHandler.addServlet(pairVerificationHolder, "/pair-verify");
        logger.debug("{}Added pair verification servlet - Path: /pair-verify", LOG_INIT);

        ServletHolder accessoryHolder = new ServletHolder(new AccessoryServlet(this));
        servletContextHandler.addServlet(accessoryHolder, "/accessories");
        logger.debug("{}Added accessory servlet - Path: /accessories", LOG_INIT);

        ServletHolder characteristicsHolder = new ServletHolder(characteristicServlet);
        servletContextHandler.addServlet(characteristicsHolder, "/characteristics");
        logger.debug("{}Added characteristics servlet - Path: /characteristics", LOG_INIT);

        ServletHolder pairingsHolder = new ServletHolder(new PairingServlet(this));
        servletContextHandler.addServlet(pairingsHolder, "/pairings");
        logger.debug("{}Added pairings servlet - Path: /pairings", LOG_INIT);

        ServletHolder catchAnyHolder = new ServletHolder(new CatchAnyServlet(this));
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
    public void start()  throws HomekitServerException{
        logger.debug("{}Starting HomeKit server", LOG_SERVER);
        try {
            super.start(); // This will call initializeResources() and set state to READY
            logger.debug("{}Base server initialization completed", LOG_SERVER);

            // Initialize Jetty server if not already initialized
            if (server != null && !server.isStarted()) {
                server.start();
                logger.info("{}Jetty server started successfully", LOG_SERVER);
            } else {
                logger.debug("{}Jetty server already running", LOG_SERVER);
            }

        } catch (Exception e) {
            logger.error("{}Failed to start server: {}", LOG_ERROR, e.getMessage(), e);
            try {
                setState(AccessoryServerState.STOPPED);
                logger.debug("{}Server state set to STOPPED after start failure", LOG_STATE);
            } catch (HomekitServerException ex) {
                logger.error("{}Failed to set stopped state after start failure: {}", LOG_ERROR, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void stop()  throws HomekitServerException{
        logger.debug("{}Stopping HomeKit server", LOG_SERVER);
        try {
            // Stop Jetty server
            if (server != null && server.isStarted()) {
                server.stop();
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
    public void close()  throws HomekitServerException {
        logger.info("{}Closing local server - Server: {}", LOG_SERVER, getUID());

        try  {
            // First stop the server to clean up active connections
            stop();

            // Clean up Jetty server resources
            if (server != null) {
                logger.debug("{}Destroying Jetty server - Server: {}", LOG_SERVER, getUID());
                server.destroy();
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

    // ========== Pairing Management ==========
    @Override
    public void addPairing(byte @NonNull [] destinationPairingId, byte @NonNull [] destinationPublicKey) throws HomekitServerException {
        logger.debug("{}Adding pairing - Destination ID: {}", LOG_PAIRING, Byte.toHexString(destinationPairingId));
        super.addPairing(destinationPairingId, destinationPublicKey);
        advertise();
        logger.info("{}Pairing added and server advertised", LOG_PAIRING);
    }

    @Override
    public void removePairing(byte @NonNull [] destinationPairingId) throws HomekitServerException {
        logger.debug("{}Removing pairing - Destination ID: {}", LOG_PAIRING, Byte.toHexString(destinationPairingId));
        super.removePairing(destinationPairingId);
        advertise();
        logger.info("{}Pairing removed and server advertised", LOG_PAIRING);
    }

    @Override
    public boolean pairVerify() throws HomekitServerException {
        logger.debug("{}Verifying pairing", LOG_PAIRING);
        return isPaired();
    }

    @Override
    public void pairRemove() throws HomekitServerException {
        logger.debug("{}Remote pairing removal requested - No action needed for local server", LOG_PAIRING);
    }

    @Override
    public void pairSetup() throws HomekitServerException {
        logger.debug("{}Remote pairing setup requested - No action needed for local server", LOG_PAIRING);
    }

    // ========== Accessory Management ==========
    @Override
    public void updateAccessories() throws AccessoryOperationException {
        logger.debug("{}Accessory update requested - No action needed for local server", LOG_ACCESSORY);
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
                setState(AccessoryServerState.READY);
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
    @Override
    public synchronized void advertise() {
        logger.debug("{}Starting server advertisement for {}", LOG_SERVER, getUID());

        if (!server.isStarted()) {
            try {
                start();
                logger.debug("{}Server started for advertisement", LOG_SERVER);
            } catch ( HomekitServerException e) {
                logger.error("{}Failed to start server for advertisement: {}", LOG_ERROR, e.getMessage(), e);
                try {
                    setState(AccessoryServerState.STOPPED);
                } catch (HomekitServerException ex) {
                    logger.error("{}Failed to set stopped state: {}", LOG_ERROR, ex.getMessage(), ex);
                }
                return;
            }
        }

        // Announce the accessory via MDNS
        Hashtable<String, String> props = createAdvertisementProperties();
        logger.debug("{}Created advertisement properties: {}", LOG_SERVER, props);

        if (announcedServiceDescription != null) {
            updateExistingAdvertisement(props);
        } else {
            createNewAdvertisement(props);
        }
    }

    private Hashtable<String, String> createAdvertisementProperties() {
        Hashtable<String, String> props = new Hashtable<>();

        // Status flags (e.g. "0x04" for bit 3). Value should be an unsigned integer. See Table 6-8 (page 58). Required.
        props.put("sf", Integer
                .toString(!isPaired() ? PairingStatusFlag.NOT_PAIRED.getMask() : PairingStatusFlag.UNKNOWN.getMask()));

        // Device ID ("5.4 Device ID" (page 31)) of the accessory. The Device ID must be formatted as
        // "XX:XX:XX:XX:XX:XX", where "XX" is a hexadecimal string representing a byte. Required.
        // This value is also used as the accessory's Pairing Identifier. This identifier of the accessory must be a
        // unique random number generated at every factory reset and must persist across reboots.
        props.put("id", new String(getPairingId(), StandardCharsets.UTF_8));

        // Model name of the accessory (e.g. "Device1,1"). Required.
        props.put("md", getClass().getSimpleName());

        // Current configuration number. Required.
        // Must update when an accessory, service, or characteristic is added or removed on the accessory server.
        // Accessories must increment the config number after a firmware update.
        // This must have a range of 1-65535 and wrap to 1 when it overflows.
        // This value must persist across reboots, power cycles, etc.
        if (getConfigurationIndex() == 65535) {
            try {
                setConfigurationIndex(1);
            } catch (ConfigurationException ex) {
                logger.error("{}Failed to set configuration index: {}", LOG_ERROR, ex.getMessage(), ex);
            }
        }
        props.put("c#", Integer.toString(getConfigurationIndex()));
        try {
            setConfigurationIndex(getConfigurationIndex() + 1);
        } catch (ConfigurationException ex) {
            logger.error("{}Failed to increment configuration index: {}", LOG_ERROR, ex.getMessage(), ex);
        }

        // Current state number. Required.
        // This must have a value of "1".
        props.put("s#", "1");

        // Pairing Feature flags (e.g. "0x3" for bits 0 and 1). Required if non-zero. See Table 5-4 (page 49).
        props.put("ff", Integer.toString(PairingFeatureFlag.NOT_SUPPORTED.getMask()));

        // Protocol version string "X.Y" (e.g. "1.0"). Required if value is not "1.0".
        // props.put("pv", "1.1");

        // Accessory Category Identifier. Required. Indicates the category that best describes the primary function of
        // the accessory. This must have a range of 1-65535. This must take values defined in "13-1 Accessory
        // Categories" (page 252). This must persist across reboots, power cycles, etc.
        props.put("ci", Integer.toString(AccessoryCategory.BRIDGES.getValue()));

        return props;
    }

    private void updateExistingAdvertisement(Hashtable<String, String> props) {
        logger.debug("{}Updating existing advertisement", LOG_SERVER);
        announcedServiceDescription.serviceProperties = props;
        mdnsService.unregisterService(announcedServiceDescription);
        mdnsService.registerService(announcedServiceDescription);
        try {
            setState(AccessoryServerState.READY);
        } catch (HomekitServerException ex) {
            logger.error("{}Failed to set state to READY: {}", LOG_ERROR, ex.getMessage(), ex);
        }
        logger.info("{}Advertisement updated successfully", LOG_SERVER);
    }

    private void createNewAdvertisement(Hashtable<String, String> props) {
        logger.debug("{}Creating new advertisement", LOG_SERVER);
        announcedServiceDescription = new ServiceDescription(SERVICE_TYPE,
                "openHAB " + getClass().getSimpleName() + " " + getPort(), getPort(), props);
        mdnsService.registerService(announcedServiceDescription);
        try {
            setState(AccessoryServerState.READY);
        } catch (HomekitServerException ex) {
            logger.error("{}Failed to set state to READY: {}", LOG_ERROR, ex.getMessage(), ex);
        }
        logger.info("{}New advertisement created successfully", LOG_SERVER);
    }

    // ========== Event Handling ==========
    @Override
    public void onCharacteristicEvent(CharacteristicEvent event) {
        logger.debug("{}Received characteristic event - Type: {}, Characteristic: {}", LOG_EVENT, event.getEventType(),
                event.getCharacteristic().getClass().getSimpleName());
        if (event.getEventType() == CharacteristicEventType.CHARACTERISTIC_START_EVENTS) {
            characteristicServlet.publishCharacteristicUpdate(event.getCharacteristic());
            logger.debug("{}Published characteristic update", LOG_EVENT);
        }
    }

  



    @Override
    public void addAccessory(@NonNull Accessory accessory) throws AccessoryOperationException {
        logger.debug("{}Adding accessory - ID: {}, Type: {}", LOG_ACCESSORY, accessory.getAccessoryId(),
                accessory.getClass().getSimpleName());
        try {
            validateLifecycleOperation("add accessory");
            super.addAccessory(accessory);
            // Add characteristic listeners
            for (Service service : accessory.getServices()) {
                for (Characteristic<?> characteristic : service.getCharacteristics()) {
                    characteristic.addChangeListener(this);
                    logger.debug("{}Added change listener for characteristic: {}", LOG_ACCESSORY,
                            characteristic.getClass().getSimpleName());
                }
            }
            logger.info("{}Accessory added successfully - ID: {}", LOG_ACCESSORY, accessory.getAccessoryId());
        } catch (HomekitServerException e) {
            logger.error("{}Failed to add accessory: {}", LOG_ERROR, e.getMessage(), e);
            throw new AccessoryOperationException("Failed to add accessory: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeAccessory(@NonNull Accessory accessory) throws AccessoryOperationException {
        logger.debug("{}Removing accessory - ID: {}, Type: {}", LOG_ACCESSORY, accessory.getAccessoryId(),
                accessory.getClass().getSimpleName());
        try {
            validateLifecycleOperation("remove accessory");
            super.removeAccessory(accessory);
            // Remove characteristic listeners
            for (Service service : accessory.getServices()) {
                for (Characteristic<?> characteristic : service.getCharacteristics()) {
                    characteristic.removeChangeListener(this);
                    logger.debug("{}Removed change listener for characteristic: {}", LOG_ACCESSORY,
                            characteristic.getClass().getSimpleName());
                }
            }
            logger.info("{}Accessory removed successfully - ID: {}", LOG_ACCESSORY, accessory.getAccessoryId());
        } catch (HomekitServerException e) {
            logger.error("{}Failed to remove accessory: {}", LOG_ERROR, e.getMessage(), e);
            throw new AccessoryOperationException("Failed to remove accessory: " + e.getMessage(), e);
        }
    }
}
