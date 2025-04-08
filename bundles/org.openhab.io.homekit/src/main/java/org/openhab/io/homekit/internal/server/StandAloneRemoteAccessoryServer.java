package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;

public class StandAloneRemoteAccessoryServer extends AbstractRemoteAccessoryServer {

    public StandAloneRemoteAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry
            ) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry);
    }

    public StandAloneRemoteAccessoryServer(InetAddress localAddress, int port,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry
            ) throws Exception {
        this(localAddress, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry
                );
    }

    public Pairing getPairing() {
        @NonNull Collection<@NonNull Pairing> pairings = pairingRegistry.get(getPairingId());

        if (pairings.size() == 1) {
            return (Pairing) pairings.toArray()[0];
        }

        if (pairings.size() > 1) {
            // oh oh
        }

        return null;
    }
}
