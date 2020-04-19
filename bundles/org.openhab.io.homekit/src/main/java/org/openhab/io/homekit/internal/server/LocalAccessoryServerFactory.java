package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.AccessoryServerFactory;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { AccessoryServerFactory.class })
@NonNullByDefault
public class LocalAccessoryServerFactory implements AccessoryServerFactory {

    private final Logger logger = LoggerFactory.getLogger(LocalAccessoryServerFactory.class);

    @Nullable
    private MDNSService mdnsService;
    @Nullable
    private AccessoryRegistry accessoryRegistry;
    @Nullable
    private PairingRegistry pairingRegistry;
    @Nullable
    private NetworkAddressService networkAddressService;
    @Nullable
    private NotificationRegistry notificationRegistry;
    @Nullable
    private HomekitCommunicationManager communicationManager;
    @Nullable
    private SafeCaller safeCaller;

    @Activate
    public LocalAccessoryServerFactory(@Nullable MDNSService mdnsService, @Nullable AccessoryRegistry accessoryRegistry,
            @Nullable PairingRegistry pairingRegistry, @Nullable NetworkAddressService networkAddressService,
            @Nullable NotificationRegistry notificationRegistry,
            @Nullable HomekitCommunicationManager communicationManager, @Nullable SafeCaller safeCaller) {
        super();

        this.mdnsService = mdnsService;
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.networkAddressService = networkAddressService;
        this.notificationRegistry = notificationRegistry;
        this.communicationManager = communicationManager;
        this.safeCaller = safeCaller;
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port) {
        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            LocalBridgeAccessoryServer newBridge = null;

            try {
                newBridge = new LocalBridgeAccessoryServer(localAddress, port, mdnsService, accessoryRegistry,
                        pairingRegistry, notificationRegistry, communicationManager, safeCaller);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), localAddress.toString(), port);
                }
                return newBridge;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public @Nullable AccessoryServer createServer(@NonNull String factoryType, InetAddress localAddress, int port,
            byte[] id, byte[] privateKey, int configurationIndex) {

        if (Arrays.stream(getSupportedServerTypes()).anyMatch(factoryType::equals)) {
            LocalBridgeAccessoryServer newBridge = null;

            try {
                newBridge = new LocalBridgeAccessoryServer(localAddress, port, id, privateKey, mdnsService,
                        accessoryRegistry, pairingRegistry, notificationRegistry, communicationManager, safeCaller);
                if (newBridge != null) {
                    logger.debug("Created an Accessory Server {} of Type {} running at {}:{}", newBridge.getUID(),
                            newBridge.getClass().getSimpleName(), localAddress.toString(), port);
                }
                return newBridge;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public String @NonNull [] getSupportedServerTypes() {
        return new String[] { LocalBridgeAccessoryServer.class.getSimpleName() };
    }
}
