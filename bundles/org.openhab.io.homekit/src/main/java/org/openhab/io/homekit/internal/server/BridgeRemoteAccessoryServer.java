package org.openhab.io.homekit.internal.server;

import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;

public class BridgeRemoteAccessoryServer extends AbstractRemoteAccessoryServer {

    private final MDNSService mdnsService;
    private final SafeCaller safeCaller;

    public BridgeRemoteAccessoryServer(InetAddress address, int port, MDNSService mdnsService,
            @Nullable AccessoryRegistry accessoryRegistry, @Nullable PairingRegistry pairingRegistry,
            SafeCaller safeCaller) throws IOException {
        super(address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry);
        this.mdnsService = mdnsService;
        this.safeCaller = safeCaller;
    }

    @Override
    protected void handleCharacteristicCommand(@NonNull String command) throws IOException {
        // Bridge-specific command handling
        super.handleCharacteristicCommand(command);
    }
}
