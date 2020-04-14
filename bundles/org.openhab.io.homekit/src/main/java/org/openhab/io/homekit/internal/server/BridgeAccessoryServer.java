package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;

/**
 * A HomekitBridge is a special type of HomekitAccessoryServer that bridges the HomeKit Accessory Protocol. A
 * HomekitBridge must expose all the user-addressable functionality supported by its connected bridged endpoints as
 * HomekitAccessory objects to the HAP controllers. A HomekitBridge must ensure that the instance ID assigned to the
 * HomekitAccessory objects exposed on behalf of its connected bridged endpoints do not change for the lifetime of the
 * server/client pairing.
 *
 * For example, a HomekitBridge that bridges three lights would expose four HomekitAccessory objects: one
 * HomekitAccessory object that represents the HomekitBridge itself that may include a firmware update HomekitService,
 * and three additional HomekitAccessory objects that each contain a Light Bulb HomekitService.
 *
 * A HomekitBridge must not expose more than 150 HomekitAccessory objects.
 *
 * @author Karel Goderis
 */
@NonNullByDefault
public class BridgeAccessoryServer extends AbstractLocalAccessoryServer {

    public BridgeAccessoryServer(InetAddress localAddress, int port, byte[] pairingId, byte[] privateKey,
            @Nullable MDNSService mdnsService, @Nullable AccessoryRegistry accessoryRegistry,
            @Nullable PairingRegistry pairingRegistry, @Nullable NotificationRegistry notificationRegistry,
            @Nullable HomekitCommunicationManager manager, @Nullable SafeCaller safeCaller) throws Exception {
        super(localAddress, port, pairingId, privateKey, mdnsService, accessoryRegistry, pairingRegistry,
                notificationRegistry, manager, safeCaller);
    }

    public BridgeAccessoryServer(InetAddress localAddress, int port, @Nullable MDNSService mdnsService,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry,
            @Nullable NotificationRegistry notificationRegistry, @Nullable HomekitCommunicationManager manager,
            @Nullable SafeCaller safeCaller) throws Exception {
        this(localAddress, port, generatePairingId(), generateSecretKey(), mdnsService, accessoryRegistry,
                pairingRegistry, notificationRegistry, manager, safeCaller);
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("Bridge", getId());
    }
}
