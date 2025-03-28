package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.NonNull;
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
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.LocalAccessoryServer;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.http.HomekitRequestLogHandler;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpConnectionFactory;
import org.openhab.io.homekit.internal.http.jetty.HomekitSessionHandler;
import org.openhab.io.homekit.internal.notification.NotificationImpl;
import org.openhab.io.homekit.internal.notification.NotificationUID;
import org.openhab.io.homekit.internal.servlet.AccessoryServlet;
import org.openhab.io.homekit.internal.servlet.CatchAnyServlet;
import org.openhab.io.homekit.internal.servlet.CharacteristicServlet;
import org.openhab.io.homekit.internal.servlet.PairSetupServlet;
import org.openhab.io.homekit.internal.servlet.PairVerificationServlet;
import org.openhab.io.homekit.internal.servlet.PairingServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO : Interface AccessoryHolder -> AccessoryServer
//                 AccessoryHolder -> AccessoryClient (iso HomekitClient)
//       Abstract class AccesspryHolder
//       Component AccessoryClient (die httpclient heeft)

public abstract class AbstractLocalAccessoryServer extends AbstractAccessoryServer implements LocalAccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractLocalAccessoryServer.class);

    private final Server server;
    protected final MDNSService mdnsService;
    protected ServiceDescription announcedServiceDescription;
    protected final HomekitCommunicationManager homekitCommunicationManager;
    protected final SafeCaller safeCaller;

    protected String label;
    private long instanceIdPool = 1;

    public AbstractLocalAccessoryServer(InetAddress address, int port, byte[] pairingId, byte[] secretKey,
            MDNSService mdnsService, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry, HomekitCommunicationManager manager, SafeCaller safeCaller)
            throws InvalidAlgorithmParameterException {
        super(address, port, pairingId, secretKey, accessoryRegistry, pairingRegistry, notificationRegistry);

        this.mdnsService = mdnsService;
        this.homekitCommunicationManager = manager;
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
    }

    public void stop() throws Exception {
        server.stop();
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

    @Override
    public void addAccessory(Accessory accessory) {
        super.addAccessory(accessory);
        advertise();
    }

    @Override
    public void removeAccessory(Accessory accessory) {
        super.removeAccessory(accessory);
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
        return instanceIdPool;
    }

    @Override
    public synchronized void advertise() {

        logger.debug("Advertising {}", this.getUID());

        if (!server.isStarted()) {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Announce the accessory via MDNS
        Hashtable<String, String> props = new Hashtable<>();

        // Status flags (e.g. ”0x04” for bit 3). Value should be an unsigned integer. See Table 6-8 (page 58). Required.
        props.put("sf", !isPaired() ? "1" : "0");

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
        if (getConfigurationIndex() == 65535) {
            setConfigurationIndex(1);
        }
        props.put("c#", Integer.toString(getConfigurationIndex()));
        setConfigurationIndex(getConfigurationIndex() + 1);

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
    public HomekitCommunicationManager getCommunicationManager() {
        return homekitCommunicationManager;
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

}
