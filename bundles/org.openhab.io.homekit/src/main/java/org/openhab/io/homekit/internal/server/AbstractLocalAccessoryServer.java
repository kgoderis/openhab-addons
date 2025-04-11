package org.openhab.io.homekit.internal.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.openhab.io.homekit.internal.http.HomekitRequestLogHandler;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpConnectionFactory;
import org.openhab.io.homekit.internal.http.jetty.HomekitSessionHandler;
import org.openhab.io.homekit.internal.server.servlet.AccessoryServlet;
import org.openhab.io.homekit.internal.server.servlet.CatchAnyServlet;
import org.openhab.io.homekit.internal.server.servlet.CharacteristicServlet;
import org.openhab.io.homekit.internal.server.servlet.PairSetupServlet;
import org.openhab.io.homekit.internal.server.servlet.PairVerificationServlet;
import org.openhab.io.homekit.internal.server.servlet.PairingServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO : Interface AccessoryHolder -> AccessoryServer
//                 AccessoryHolder -> AccessoryClient (iso HomekitClient)
//       Abstract class AccesspryHolder
//       Component AccessoryClient (die httpclient heeft)

public abstract class AbstractLocalAccessoryServer extends AbstractAccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractLocalAccessoryServer.class);

    private final Server server;
    protected final MDNSService mdnsService;
    protected ServiceDescription announcedServiceDescription;
    protected final SafeCaller safeCaller;
    protected final Object notificationLock = new Object();
    // private final Map<Characteristic<?>, Set<HttpConnection>> characteristicConnections = new ConcurrentHashMap<>();
    // private final Map<HttpConnection, List<JsonObject>> batchedNotifications = new ConcurrentHashMap<>();
    // private final Map<HttpConnection, Boolean> batchModeEnabled = new ConcurrentHashMap<>();

    protected String label;
    private final Object instanceIdLock = new Object();
    private final Set<Long> usedInstanceIds = new HashSet<>();
    private long nextInstanceId = 1;

    private final CharacteristicServlet characteristicServlet;

    public AbstractLocalAccessoryServer(InetAddress address, int port, byte[] pairingId, byte[] secretKey,
            MDNSService mdnsService, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            SafeCaller safeCaller) throws InvalidAlgorithmParameterException {
        super(address, port, pairingId, secretKey, accessoryRegistry, pairingRegistry);

        // TODO : Remove SafeCaller

        this.mdnsService = mdnsService;
        this.safeCaller = safeCaller;

        // Jetty
        server = new Server();

        HomekitSessionHandler homekitSessionHandler = new HomekitSessionHandler();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setIdleTimeout(0);

        ServerConnector http = new ServerConnector(server, new HomekitHttpConnectionFactory(homekitSessionHandler));
        http.setPort(port);
        http.setIdleTimeout(0);
        // http.addBean(new Connection.Listener() {

        // @Override
        // public void onOpened(org.eclipse.jetty.io.Connection connection) {
        // logger.debug("onOpened {}", connection.toString());
        // // No Op
        // }

        // @Override
        // public void onClosed(org.eclipse.jetty.io.Connection connection) {
        // logger.debug("onClosed {}", connection.toString());
        // for (Notification notification : notificationRegistry.getAll()) {
        // if (notification.getConnection().equals(connection)) {
        // removeNotification(notification.getCharacteristic());
        // }
        // }
        // }
        // });
        server.addConnector(http);
        characteristicServlet = new CharacteristicServlet(this);

        ServletContextHandler servletContextHandler = new ServletContextHandler(
                ServletContextHandler.SESSIONS | ServletContextHandler.NO_SECURITY);
        servletContextHandler.setContextPath("/");
        servletContextHandler.setSessionHandler(homekitSessionHandler);

        ServletHolder pairSetupHolder = new ServletHolder(new PairSetupServlet(this));
        servletContextHandler.addServlet(pairSetupHolder, "/pair-setup");

        ServletHolder pairVerificationHolder = new ServletHolder(new PairVerificationServlet(this));
        servletContextHandler.addServlet(pairVerificationHolder, "/pair-verify");

        ServletHolder accessoryHolder = new ServletHolder(new AccessoryServlet(this));
        servletContextHandler.addServlet(accessoryHolder, "/accessories");

        ServletHolder characteristicsHolder = new ServletHolder(characteristicServlet);
        servletContextHandler.addServlet(characteristicsHolder, "/characteristics");

        ServletHolder pairingsHolder = new ServletHolder(new PairingServlet(this));
        servletContextHandler.addServlet(pairingsHolder, "/pairings");

        ServletHolder catchAnyHolder = new ServletHolder(new CatchAnyServlet(this));
        servletContextHandler.addServlet(catchAnyHolder, "/");

        HomekitRequestLogHandler requestLogHandler = new HomekitRequestLogHandler();
        requestLogHandler.setHandler(servletContextHandler);

        server.setHandler(requestLogHandler);

        // Netty

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

    @Override
    public void start() throws Exception {
        logger.debug("Starting HomeKit server");
        setState(AccessoryServerState.CONNECTED);
    }

    @Override
    public void stop() throws Exception {
        logger.debug("Stopping HomeKit server");
        setState(AccessoryServerState.STOPPED);
    }

    @Override
    public void addPairing(byte @NonNull [] destinationPairingId, byte @NonNull [] destinationPublicKey) {
        super.addPairing(destinationPairingId, destinationPublicKey);
        advertise();
    }

    @Override
    public void removePairing(byte @NonNull [] destinationPairingId) {
        super.removePairing(destinationPairingId);
        advertise();
    }

    // TODO : Advertise() the server after it is used to create an accessory

    // @Override
    // public void addAccessory(Accessory accessory) {
    // super.addAccessory(accessory);
    // advertise();
    // }

    // @Override
    // public void removeAccessory(Accessory accessory) {
    // super.removeAccessory(accessory);
    // advertise();
    // }

    protected void publishNotification(Characteristic<?> characteristic) {
        // String characteristicId = characteristic.getService().getAccessory().getId() +
        // "." + characteristic.getId();
        characteristicServlet.publishCharacteristicUpdate((Characteristic<?>) characteristic);
    }

    @Override
    public long getNextAvailableAccessoryId() {
        synchronized (instanceIdLock) {
            // First try to find a recycled ID
            for (long id = 1; id < nextInstanceId; id++) {
                if (!usedInstanceIds.contains(id)) {
                    usedInstanceIds.add(id);
                    logger.debug("Recycled instance ID: {} for server: {}", id, getUID());
                    return id;
                }
            }

            // If no recycled IDs available, use the next new ID
            long newId = nextInstanceId++;
            usedInstanceIds.add(newId);
            logger.debug("Assigned new instance ID: {} for server: {}", newId, getUID());
            return newId;
        }
    }

    @Override
    public synchronized void advertise() {

        logger.debug("Advertising {}", this.getUID());

        if (!server.isStarted()) {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
                setState(AccessoryServerState.STOPPED);
            }
        }

        // Announce the accessory via MDNS
        Hashtable<String, String> props = new Hashtable<>();

        // Status flags (e.g. "0x04" for bit 3). Value should be an unsigned integer. See Table 6-8 (page 58). Required.
        props.put("sf", !isPaired() ? "1" : "0");

        // Device ID ("5.4 Device ID" (page 31)) of the accessory. The Device ID must be formatted as
        // "XX:XX:XX:XX:XX:XX", where "XX" is a hexadecimal string representing a byte. Required.
        // This value is also used as the accessory's Pairing Identifier.
        props.put("id", (new String(getPairingId(), StandardCharsets.UTF_8)));

        // Model name of the accessory (e.g. "Device1,1"). Required.
        props.put("md", getClass().getSimpleName());

        // Current configuration number. Required.
        // Must update when an accessory, service, or characteristic is added or removed on the accessory server.
        // Accessories must increment the config number after a firmware update.
        // This must have a range of 1-65535 and wrap to 1 when it overflows.
        // This value must persist across reboots, power cycles, etc.
        if (getConfigurationIndex() == 65535) {
            setConfigurationIndex(1);
        }
        props.put("c#", Integer.toString(getConfigurationIndex()));
        setConfigurationIndex(getConfigurationIndex() + 1);

        // Current state number. Required.
        // This must have a value of "1".
        props.put("s#", "1");

        // Pairing Feature flags (e.g. "0x3" for bits 0 and 1). Required if non-zero. See Table 5-4 (page 49).
        props.put("ff", "0");

        // Protocol version string "X.Y" (e.g. "1.0"). Required if value is not "1.0".
        // props.put("pv", "1.1");

        // Accessory Category Identifier. Required. Indicates the category that best describes the primary function of
        // the accessory. This must have a range of 1-65535. This must take values defined in "13-1 Accessory
        // Categories" (page 252). This must persist across reboots, power cycles, etc.
        props.put("ci", "2");

        if (announcedServiceDescription != null) {
            announcedServiceDescription.serviceProperties = props;
            // safeCaller.create(mdnsService, MDNSService.class).withAsync()
            // .withIdentifier(announcedServiceDescription.serviceName).build()
            // .updateService(announcedServiceDescription);
            // mdnsService.updateService(announcedServiceDescription);
            mdnsService.unregisterService(announcedServiceDescription);
            mdnsService.registerService(announcedServiceDescription);
            setState(AccessoryServerState.READY);
        } else {
            announcedServiceDescription = new ServiceDescription(SERVICE_TYPE,
                    "openHAB " + getClass().getSimpleName() + " " + getPort(), port, props);
            mdnsService.registerService(announcedServiceDescription);
            setState(AccessoryServerState.READY);
        }
    }

    @Override
    public String getSetupCode() {
        if (setupCode == null || setupCode.isEmpty()) {
            if (logger.isDebugEnabled()) {
                setupCode = "123-12-123";
            } else {
                setupCode = generateSetupCode();
            }
            setState(AccessoryServerState.READY);
        }
        return setupCode;
    }

    protected String generateSetupCode() {
        String setupCode = String.format("%03d-%02d-%03d", HomekitEncryptionEngine.getSecureRandom().nextInt(1000),
                HomekitEncryptionEngine.getSecureRandom().nextInt(100),
                HomekitEncryptionEngine.getSecureRandom().nextInt(1000));

        if (setupCode == "000-00-000" || setupCode == "111-11-111" || setupCode == "222-22-222"
                || setupCode == "333-33-333" || setupCode == "444-44-444" || setupCode == "555-55-555"
                || setupCode == "666-66-666" || setupCode == "777-77-777" || setupCode == "888-88-888"
                || setupCode == "999-99-999" || setupCode == "123-45-678" || setupCode == "876-54-321") {
            return generateSetupCode();
        }

        return setupCode;
    }

    // /**
    // * Publishes a notification for a characteristic change.
    // * This method is thread-safe and ensures notifications are sent to all registered clients.
    // *
    // * @param characteristic the characteristic that changed
    // * @param value the new value of the characteristic
    // */
    // protected void publishNotification(Characteristic<?> characteristic, Object value) {
    // synchronized (notificationLock) {
    // try {
    // NotificationUID notificationUID = new NotificationUID(getId(),
    // characteristic.getService().getAccessory().getId(),
    // characteristic.getService().getId(),
    // characteristic.getId());

    // Notification notification = notificationRegistry.get(notificationUID);
    // if (notification != null) {
    // logger.debug("Publishing notification for characteristic {} with value {}",
    // characteristic.getId(), value);

    // // Send notification to all registered connections
    // for (HttpConnection connection : notification.getConnections()) {
    // try {
    // homekitCommunicationManager.sendNotification(connection, characteristic, value);
    // } catch (Exception e) {
    // logger.warn("Failed to send notification to connection {}: {}",
    // connection, e.getMessage());
    // }
    // }
    // }
    // } catch (Exception e) {
    // logger.error("Error publishing notification: {}", e.getMessage(), e);
    // }
    // }
    // }

    // @Override
    // public void addNotification(Characteristic<?> characteristic, HttpConnection connection) {
    // synchronized (notificationLock) {
    // NotificationUID notificationUID = new NotificationUID(getId(),
    // characteristic.getService().getAccessory().getId(),
    // characteristic.getService().getId(),
    // characteristic.getId());

    // Notification notification = notificationRegistry.get(notificationUID);
    // if (notification == null) {
    // notification = new NotificationImpl(notificationUID);
    // notificationRegistry.add(notification);
    // logger.debug("Added new notification for characteristic {}", characteristic.getId());
    // }

    // notification.addConnection(connection);
    // characteristic.setHasEvents(true);
    // logger.debug("Added connection {} to notification for characteristic {}",
    // connection, characteristic.getId());
    // }
    // }

    // @Override
    // public void removeNotification(Characteristic<?> characteristic) {
    // synchronized (notificationLock) {
    // NotificationUID notificationUID = new NotificationUID(getId(),
    // characteristic.getService().getAccessory().getId(), characteristic.getService().getId(),
    // characteristic.getId());

    // Notification notification = notificationRegistry.get(notificationUID);
    // if (notification != null) {
    // notificationRegistry.remove(notificationUID);
    // characteristic.setHasEvents(false);
    // logger.debug("Removed notification for characteristic {}", characteristic.getId());
    // }
    // }
    // }

    // public void disableBatchMode() {
    // this.batchMode = false;
    // // loop through all accessories, then the characteristics, then the connections and disable batch mode
    // for (Accessory accessory : getAccessories()) {
    // for (Service service : accessory.getServices()) {
    // for (Characteristic<?> characteristic : service.getCharacteristics()) {
    // disableBatchMode(characteristic);

    // }
    // }
    // }

    // // flush all batched notifications
    // for (HttpConnection connection : characteristicConnections.values()) {
    // flushBatchedNotifications(connection);
    // }
    // }

    // public void enableBatchMode() {
    // this.batchMode = true;

    // // loop through all accessories, then the characteristics, then the connections and enable batch mode
    // for (Accessory accessory : getAccessories()) {
    // for (Service service : accessory.getServices()) {
    // for (Characteristic characteristic : service.getCharacteristics()) {
    // enableBatchMode(characteristic);
    // }
    // }
    // }

    // private boolean batchMode = false;

    // @Override
    // public synchronized void publish(JsonObject notification) {
    // if (characteristic.getAccessoryId() == this.characteristic.getAccessoryId()) {
    // if (batchMode) {
    // notifications.add(notification);
    // } else {
    // JsonArrayBuilder notifications = Json.createArrayBuilder().add(notification);
    // publish(notifications);
    // }
    // }
    // }

    // @Override
    // public synchronized void publish() {
    // if (notifications.size() > 0) {
    // JsonArrayBuilder notificationsBuilder = Json.createArrayBuilder();

    // for (JsonObject notification : notifications) {
    // notificationsBuilder.add(notification);
    // }

    // publish(notificationsBuilder);

    // notifications.clear();
    // }
    // }

    // List<JsonObject> notifications = Collections.synchronizedList(new LinkedList<JsonObject>());

    // @Override
    // public synchronized void publish(JsonObject notification) {
    // if (characteristic.getAccessoryId() == this.characteristic.getAccessoryId()) {
    // if (batchMode) {
    // notifications.add(notification);
    // } else {
    // JsonArrayBuilder notifications = Json.createArrayBuilder().add(notification);
    // publish(notifications);
    // }
    // }
    // }

    // public void handleUpdate(State state) {
    // for (AccessoryServer server : accessoryServerRegistry.getAll()) {
    // for (Accessory accessory : server.getAccessories()) {
    // for (Service service : accessory.getServices()) {
    // for (Characteristic characteristic : service.getCharacteristics()) {
    // if (link.getLinkedUID().equals(((ManagedCharacteristic<?>) characteristic).getChannelUID())) {
    // Notification notification = notificationRegistry.get(new NotificationUID(
    // ((ManagedCharacteristic<?>) characteristic).getUID().toString()));

    // if (notification != null) {
    // notification.publish(((ManagedCharacteristic<?>) characteristic).toEventJson(state));
    // }
    // }
    // }
    // }
    // }
    // }
    // }

    // /**
    // * Maps an HttpConnection to a Characteristic for notifications.
    // * Multiple connections can be mapped to the same characteristic.
    // *
    // * @param characteristic The characteristic to map to
    // * @param connection The HTTP connection to map
    // */
    // protected synchronized void addConnectionMapping(Characteristic<?> characteristic, HttpConnection connection) {
    // characteristicConnections.computeIfAbsent(characteristic, k -> ConcurrentHashMap.newKeySet()).add(connection);
    // logger.debug("Added connection mapping for characteristic {} to connection {}", characteristic, connection);
    // }

    // /**
    // * Removes a connection mapping for a characteristic.
    // * If this was the last connection mapped to the characteristic, removes the characteristic entry.
    // *
    // * @param characteristic The characteristic to remove the mapping from
    // * @param connection The HTTP connection to remove
    // */
    // protected synchronized void removeConnectionMapping(Characteristic<?> characteristic, HttpConnection connection)
    // {
    // Set<HttpConnection> connections = characteristicConnections.get(characteristic);
    // if (connections != null) {
    // connections.remove(connection);
    // if (connections.isEmpty()) {
    // characteristicConnections.remove(characteristic);
    // }
    // logger.debug("Removed connection mapping for characteristic {} from connection {}", characteristic,
    // connection);
    // }
    // }

    // /**
    // * Gets all connections mapped to a characteristic.
    // *
    // * @param characteristic The characteristic to get connections for
    // * @return Set of HttpConnections mapped to the characteristic, or empty set if none
    // */
    // protected Set<HttpConnection> getConnectionsForCharacteristic(Characteristic<?> characteristic) {
    // return characteristicConnections.getOrDefault(characteristic, Collections.emptySet());
    // }

    // /**
    // * Enables batch mode for a specific HTTP connection.
    // * When enabled, notifications will be queued instead of sent immediately.
    // *
    // * @param connection The HTTP connection to enable batch mode for
    // */
    // protected void enableBatchMode(HttpConnection connection) {
    // batchModeEnabled.put(connection, true);
    // batchedNotifications.putIfAbsent(connection, new ArrayList<>());
    // logger.debug("Enabled batch mode for connection {}", connection);
    // }

    // /**
    // * Disables batch mode for a specific HTTP connection.
    // * Any queued notifications will be sent immediately.
    // *
    // * @param connection The HTTP connection to disable batch mode for
    // */
    // protected void disableBatchMode(HttpConnection connection) {
    // batchModeEnabled.put(connection, false);
    // flushBatchedNotifications(connection);
    // logger.debug("Disabled batch mode for connection {}", connection);
    // }

    // /**
    // * Enables batch mode for all connections associated with a characteristic.
    // * When enabled, notifications will be queued instead of sent immediately.
    // *
    // * @param characteristic The characteristic to enable batch mode for
    // */
    // protected void enableBatchMode(Characteristic characteristic) {
    // Set<HttpConnection> connections = getConnectionsForCharacteristic(characteristic);
    // for (HttpConnection connection : connections) {
    // enableBatchMode(connection);
    // }
    // logger.debug("Enabled batch mode for all connections of characteristic {}", characteristic);
    // }

    // /**
    // * Disables batch mode for all connections associated with a characteristic.
    // * Any queued notifications will be sent immediately.
    // *
    // * @param characteristic The characteristic to disable batch mode for
    // */
    // protected void disableBatchMode(Characteristic characteristic) {
    // Set<HttpConnection> connections = getConnectionsForCharacteristic(characteristic);
    // for (HttpConnection connection : connections) {
    // disableBatchMode(connection);
    // }
    // logger.debug("Disabled batch mode for all connections of characteristic {}", characteristic);
    // }

    // /**
    // * Publishes a notification either immediately or queues it based on batch mode.
    // *
    // * @param connection The HTTP connection to publish to
    // * @param notification The notification to publish
    // */
    // protected synchronized void publishNotification(HttpConnection connection, JsonObject notification) {
    // if (Boolean.TRUE.equals(batchModeEnabled.get(connection))) {
    // batchedNotifications.get(connection).add(notification);
    // logger.debug("Queued notification for batched sending on connection {}", connection);
    // } else {
    // try {
    // JsonArrayBuilder notifications = Json.createArrayBuilder().add(notification);
    // publish();
    // logger.debug("Sent immediate notification on connection {}", connection);
    // } catch (Exception e) {
    // logger.warn("Failed to send notification to connection {}: {}", connection, e.getMessage());
    // }
    // }
    // }
    // /**
    // * Sends all queued notifications for a connection and clears the queue.
    // *
    // * @param connection The HTTP connection to flush notifications for
    // */
    // protected synchronized void flushBatchedNotifications(HttpConnection connection) {
    // List<JsonObject> notifications = batchedNotifications.get(connection);
    // if (notifications != null && !notifications.isEmpty()) {
    // try {
    // JsonArrayBuilder builder = Json.createArrayBuilder();
    // notifications.forEach(builder::add);
    // publish(builder);
    // notifications.clear();
    // logger.debug("Flushed batched notifications for connection {}", connection);
    // } catch (Exception e) {
    // logger.warn("Failed to flush batched notifications for connection {}: {}", connection, e.getMessage());
    // }
    // }
    // }

    // protected synchronized void publish(Characteristic<?> characteristic, JsonArrayBuilder arrayBuilder) {
    // JsonObjectBuilder builder = Json.createObjectBuilder().add("characteristics", arrayBuilder);

    // try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    // Json.createWriter(baos).write(builder.build());
    // byte[] dataBytes = baos.toByteArray();

    // HttpFields fields = new HttpFields();
    // fields.add("X-HAP-Event", "True");
    // fields.add("Content-Type", "application/hap+json");

    // MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.OK_200, "", fields,
    // dataBytes.length);
    // FutureCallback callback = new FutureCallback();
    // logger.debug("Publishing Notification to connection {}", connection.toString());
    // if (dataBytes.length > 0) {
    // try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
    // HexDump.dump(dataBytes, 0, stream, 0);
    // stream.flush();
    // logger.debug("\n{}", stream.toString(StandardCharsets.UTF_8.name()));
    // }
    // }
    // connection.send(info, false, ByteBuffer.wrap(dataBytes), true, callback);
    // connection.getGenerator().reset();
    // callback.get();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }}

    @Override
    public boolean isSecure() {
        // We are not in control, the remote controller will handle this
        return true;
    }

    @Override
    public boolean pairVerify() {
        // We are not in control, the remote controller will handle this
        return isPaired();
    }

    @Override
    public void pairRemove() throws HomekitException, IOException {
        // We are not in control, the remote controller will handle this
    }
}
