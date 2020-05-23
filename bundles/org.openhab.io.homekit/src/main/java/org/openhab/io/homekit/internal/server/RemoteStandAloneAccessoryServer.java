package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.Pairing;
import org.openhab.io.homekit.api.PairingRegistry;

public class RemoteStandAloneAccessoryServer extends AbstractRemoteAccessoryServer {

    public RemoteStandAloneAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
            NotificationRegistry notificationRegistry) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry, notificationRegistry);
    }

    public RemoteStandAloneAccessoryServer(InetAddress localAddress, int port,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry,
            @Nullable NotificationRegistry notificationRegistry) throws Exception {
        this(localAddress, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry,
                notificationRegistry);
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
