package org.openhab.io.homekit.internal.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;
import org.openhab.io.homekit.internal.http.HomekitRequestLogHandler;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpConnectionFactory;
import org.openhab.io.homekit.internal.http.jetty.HomekitSessionHandler;
import org.openhab.io.homekit.internal.notification.NotificationImpl;
import org.openhab.io.homekit.internal.notification.NotificationUID;
import org.openhab.io.homekit.internal.pairing.PairingImpl;
import org.openhab.io.homekit.internal.pairing.PairingUID;
import org.openhab.io.homekit.internal.servlet.AccessoryServlet;
import org.openhab.io.homekit.internal.servlet.CatchAnyServlet;
import org.openhab.io.homekit.internal.servlet.CharacteristicServlet;
import org.openhab.io.homekit.internal.servlet.PairSetupServlet;
import org.openhab.io.homekit.internal.servlet.PairVerificationServlet;
import org.openhab.io.homekit.internal.servlet.PairingServlet;
import org.openhab.io.homekit.library.accessory.BridgeAccessory;
import org.openhab.io.homekit.util.Byte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAccessoryServer implements AccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractAccessoryServer.class);

    private static final String SERVICE_TYPE = "_hap._tcp.local.";
    private static volatile SecureRandom secureRandom;

    private final Server server;
    protected final MDNSService mdnsService;
    protected final AccessoryRegistry accessoryRegistry;
    protected final PairingRegistry pairingRegistry;
    protected final NotificationRegistry notificationRegistry;
    protected final HomekitCommunicationManager homekitCommunicationManager;
    protected final SafeCaller safeCaller;

    private final InetAddress localAddress;
    private final int port;
    private final byte[] pairingIdentifier;
    private final BigInteger salt;
    private final byte[] privateKey;
    private String setupCode;
    protected String label;
    private long instanceIdPool = 1;

    protected ServiceDescription announcedServiceDescription;
    protected boolean hasBeenPaired = false;
    protected int configurationIndex = 1;

    private final Collection<AccessoryServerChangeListener> listeners = new CopyOnWriteArraySet<>();

    public AbstractAccessoryServer(InetAddress localAddress, int port, byte[] pairingId, BigInteger salt,
            byte[] privateKey, MDNSService mdnsService, AccessoryRegistry accessoryRegistry,
            PairingRegistry pairingRegistry, NotificationRegistry notificationRegistry,
            HomekitCommunicationManager manager, SafeCaller safeCaller) throws InvalidAlgorithmParameterException {
        super();
        this.localAddress = localAddress;
        this.port = port;
        this.mdnsService = mdnsService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.notificationRegistry = notificationRegistry;
        this.salt = salt;
        this.privateKey = privateKey;
        this.homekitCommunicationManager = manager;
        this.pairingIdentifier = pairingId;
        this.safeCaller = safeCaller;

        // Jetty
        server = new Server();

        HomekitSessionHandler homekitSessionHandler = new HomekitSessionHandler();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setIdleTimeout(0);

        ServerConnector http = new ServerConnector(server, new HomekitHttpConnectionFactory(homekitSessionHandler));
        http.setPort(port);
        http.setIdleTimeout(0);
        http.addBean(new Connection.Listener() {

            @Override
            public void onOpened(org.eclipse.jetty.io.Connection connection) {
                logger.debug("onOpened {}", connection.toString());
                // No Op
            }

            @Override
            public void onClosed(org.eclipse.jetty.io.Connection connection) {
                logger.debug("onClosed {}", connection.toString());
                for (Notification notification : notificationRegistry.getAll()) {
                    if (notification.getConnection().equals(connection)) {
                        removeNotification(notification.getCharacteristic());
                    }
                }
            }
        });
        server.addConnector(http);

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

        ServletHolder characteristicsHolder = new ServletHolder(new CharacteristicServlet(this));
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

    public void start() throws Exception {
        server.start();
        // server.dumpStdErr();
    }

    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void addPairing(byte @NonNull [] clientPairingId, byte @NonNull [] clientPublicKey) {
        try {
            Pairing newPairing = new PairingImpl(getPairingId(), clientPairingId, clientPublicKey);
            Pairing oldPairing = pairingRegistry.remove(newPairing.getUID());

            if (oldPairing != null) {
                logger.debug("Removed Pairing of Server {} with Client {} and Public Key {}", getId(),
                        oldPairing.getDestinationPairingId(),
                        Byte.byteToHexString(oldPairing.getDestinationLongtermPublicKey()));
            }

            pairingRegistry.add(newPairing);
            hasBeenPaired = true;
            logger.debug("Paired Server {} with Client {} and Public Key {}", getId(), clientPairingId,
                    Byte.byteToHexString(clientPublicKey));
            advertise();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removePairing(byte @NonNull [] clientPairingId) {
        pairingRegistry.remove(new PairingUID(getPairingId(), clientPairingId));
        if (pairingRegistry.get(getPairingId()).isEmpty()) {
            hasBeenPaired = false;
            advertise();
        }
    }

    @Override
    public Collection<ManagedAccessory> getAccessories() {
        return Collections.unmodifiableList(accessoryRegistry.get(getId()).stream()
                .sorted((o1, o2) -> new Long(o1.getId()).compareTo(new Long(o2.getId()))).collect(Collectors.toList()));
    }

    @Override
    public @Nullable ManagedAccessory getAccessory(int instanceId) {
        AccessoryUID id = new AccessoryUID(getId(), Integer.toString(instanceId));
        return accessoryRegistry.get(id);
    }

    @Override
    public @Nullable ManagedAccessory getAccessory(Class<? extends ManagedAccessory> accessoryClass) {
        Collection<ManagedAccessory> accessories = accessoryRegistry.get(getId());
        for (ManagedAccessory accessory : accessories) {
            if (accessory.getClass() == accessoryClass) {
                return accessory;
            }
        }
        return null;
    }

    @Override
    public void addAccessory(ManagedAccessory accessory) {
        logger.debug("Adding Accessory {} of Type {} to Accessory Server {}", accessory.getUID(),
                accessory.getClass().getSimpleName(), this.getUID());
        if (accessory.getId() <= 1 && !(accessory instanceof BridgeAccessory)) {
            throw new IndexOutOfBoundsException("The ID of an accessory used in a bridge must be greater than 1");
        }

        if (accessoryRegistry.update(accessory) == null) {
            logger.debug("Adding Accessory {} of Type {} to the Accessory Registry", accessory.getUID(),
                    accessory.getClass().getSimpleName(), this.getUID());
            accessoryRegistry.add(accessory);
        }

        advertise();
    }

    @Override
    public void removeAccessory(ManagedAccessory accessory) {
        accessoryRegistry.remove(accessory.getUID());
        advertise();
    }

    @Override
    public void addNotification(ManagedCharacteristic<?> characteristic, HttpConnection connection) {
        Notification notification = new NotificationImpl(characteristic, connection);

        logger.debug("addNotification {} {}", notification.getUID(), connection.toString());

        if (notificationRegistry.update(notification) == null) {
            logger.debug("Adding Notification {} for Connection {} to the Notification Registry", notification.getUID(),
                    connection.toConnectionString());
            notificationRegistry.add(notification);
            characteristic.setEventsEnabled(true);
        }

        // TODO : Notificfation to more than one controller? -> Adjust Notification UID, of , >1 Connection per
        // NotificatuionUID in de registry?
    }

    @Override
    public void removeNotification(ManagedCharacteristic<?> characteristic) {
        notificationRegistry.remove(new NotificationUID(getId(), characteristic.getService().getAccessory().getId(),
                characteristic.getService().getId(), characteristic.getId()));
        characteristic.setEventsEnabled(false);
    }

    @Override
    public long getInstanceId() {
        try {
            long maxInstanceIdInRegistry = accessoryRegistry.get(this.getId()).stream().map(a -> a.getId())
                    .max(Long::compare).get();
            if (maxInstanceIdInRegistry > instanceIdPool) {
                instanceIdPool = maxInstanceIdInRegistry;
            }
        } catch (NoSuchElementException e) {
        }

        instanceIdPool++;
        // notifyListeners();
        return instanceIdPool;
    }

    // TODO : Remove address depedency everywhere, bind to all local interfaces
    @Override
    public InetAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getId() {
        return (new String(pairingIdentifier, StandardCharsets.UTF_8)).replace(":", "");
    }

    @Override
    public byte[] getPairingId() {
        return pairingIdentifier;
    }

    @Override
    public BigInteger getSalt() {
        return salt;
    }

    @Override
    public byte[] getPrivateKey() {
        return privateKey;
    }

    @Override
    public int getConfigurationIndex() {
        return configurationIndex;
    }

    @Override
    public String getSetupCode() {
        if (setupCode == null) {
            if (logger.isDebugEnabled()) {
                setupCode = "123-12-123";

            } else {
                setupCode = generateSetupCode();
            }
        }
        return setupCode;
    }

    @Override
    public synchronized void advertise() {

        logger.debug("Advertising {}", this.getUID());

        if (!server.isStarted()) {
            try {
                start();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // if (announcedServiceDescription != null) {
        // mdnsService.unregisterService(announcedServiceDescription);
        // }

        // Announce the accessory via MDNS
        Hashtable<String, String> props = new Hashtable<>();

        // Status flags (e.g. ”0x04” for bit 3). Value should be an unsigned integer. See Table 6-8 (page 58). Required.
        props.put("sf", !hasBeenPaired ? "1" : "0");

        // Device ID (”5.4 Device ID” (page 31)) of the accessory. The Device ID must be formatted as
        // ”XX:XX:XX:XX:XX:XX”, where ”XX” is a hexadecimal string representing a byte. Required.
        // This value is also used as the accessoryʼs Pairing Identifier.
        props.put("id", (new String(getPairingId(), StandardCharsets.UTF_8)));

        // Model name of the accessory (e.g. ”Device1,1”). Required.
        props.put("md", getClass().getSimpleName());

        // Current configuration number. Required.
        // Must update when an accessory, service, or characteristic is added or removed on the accessory server.
        // Accessories must increment the config number after a firmware update.
        // This must have a range of 1-65535 and wrap to 1 when it overflows.
        // This value must persist across reboots, power cycles, etc.
        if (configurationIndex == 65535) {
            configurationIndex = 1;
        }
        props.put("c#", Integer.toString(configurationIndex));
        configurationIndex++;

        // Current state number. Required.
        // This must have a value of ”1”.
        props.put("s#", "1");

        // Pairing Feature flags (e.g. ”0x3” for bits 0 and 1). Required if non-zero. See Table 5-4 (page 49).
        props.put("ff", "0");

        // Protocol version string ”X.Y” (e.g. ”1.0”). Required if value is not ”1.0”.
        // props.put("pv", "1.1");

        // Accessory Category Identifier. Required. Indicates the category that best describes the primary function of
        // the accessory. This must have a range of 1-65535. This must take values defined in ”13-1 Accessory
        // Categories” (page 252). This must persist across reboots, power cycles, etc.
        props.put("ci", "2");

        if (announcedServiceDescription != null) {
            announcedServiceDescription.serviceProperties = props;
            safeCaller.create(mdnsService, MDNSService.class).withAsync()
                    .withIdentifier(announcedServiceDescription.serviceName).build()
                    .updateService(announcedServiceDescription);
            // mdnsService.updateService(announcedServiceDescription);
        } else {
            announcedServiceDescription = new ServiceDescription(SERVICE_TYPE,
                    "openHAB " + getClass().getSimpleName() + " " + getPort(), port, props);
            mdnsService.registerService(announcedServiceDescription);
        }
    }

    @Override
    public void factoryReset() {
        // TODO Auto-generated method stub
        // TODO Remove all crypto keys
        // TODO id is a unique random number, regenerate

    }

    @Override
    public byte[] getPairingPublicKey(byte @NonNull [] clientPairingId) {
        Pairing hp = pairingRegistry.get(new PairingUID(getPairingId(), clientPairingId));
        return hp != null ? hp.getDestinationLongtermPublicKey() : null;
    }

    private String generateSetupCode() {
        String setupCode = String.format("%03d-%02d-%03d", getSecureRandom().nextInt(1000),
                getSecureRandom().nextInt(100), getSecureRandom().nextInt(1000));

        if (setupCode == "000-00-000" || setupCode == "111-11-111" || setupCode == "222-22-222"
                || setupCode == "333-33-333" || setupCode == "444-44-444" || setupCode == "555-55-555"
                || setupCode == "666-66-666" || setupCode == "777-77-777" || setupCode == "888-88-888"
                || setupCode == "999-99-999" || setupCode == "123-45-678" || setupCode == "876-54-321") {
            return generateSetupCode();
        }

        return setupCode;
    }

    private static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized (HomekitCommunicationManager.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }

    @Override
    public void addChangeListener(AccessoryServerChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(AccessoryServerChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners() {
        for (AccessoryServerChangeListener listener : this.listeners) {
            try {
                listener.updated(this);
            } catch (Throwable throwable) {
                logger.error("Cannot inform listener {} ", listener, throwable.getMessage(), throwable);
            }
        }
    }

    @Override
    public HomekitCommunicationManager getCommunicationManager() {
        return homekitCommunicationManager;
    }

}
