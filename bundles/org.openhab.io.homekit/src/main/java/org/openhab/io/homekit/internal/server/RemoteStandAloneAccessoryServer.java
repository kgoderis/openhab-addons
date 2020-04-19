package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;

public class RemoteStandAloneAccessoryServer extends AbstractRemoteAccessoryServer {

    public RemoteStandAloneAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            @Nullable MDNSService mdnsService, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry, @Nullable HomekitCommunicationManager communicationManager,
            @Nullable SafeCaller safeCaller) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry, notificationRegistry);
    }

    public RemoteStandAloneAccessoryServer(InetAddress localAddress, int port, @Nullable MDNSService mdnsService,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry,
            @Nullable NotificationRegistry notificationRegistry, @Nullable HomekitCommunicationManager manager,
            @Nullable SafeCaller safeCaller) throws Exception {
        this(localAddress, port, generatePairingId(), generateSecretKey(), mdnsService, accessoryRegistry,
                pairingRegistry, notificationRegistry, manager, safeCaller);
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("RemoteStandAlone", getId());
    }

    public Pairing getPairing() {
        Collection<@NonNull Pairing> pairings = pairingRegistry.get(getPairingId());

        if (pairings.size() == 1) {
            return (Pairing) pairings.toArray()[0];
        }

        if (pairings.size() > 1) {
            // oh oh
        }

        return null;
    }
}
