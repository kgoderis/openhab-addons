package org.openhab.io.homekit.internal.server;

import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.internal.server.registry.AccessoryServerUID;

/**
 * An HomekitAccessoryServer is a class that supports HomeKit Accessory Protocol and exposes a collection of
 * HomekitAccessory to the HAP controller(s). An HomekitAccessoryServer represents one endpoint of the pairing
 * relationship established with HAP Pairing, and exposes at least one HomekitAccessory object.
 *
 * @author Karel Goderis
 */
public class StandAloneLocalAccessoryServer extends AbstractLocalAccessoryServer {

    public StandAloneLocalAccessoryServer(InetAddress address, int port, byte[] pairingId, byte[] secretKey,
            MDNSService mdnsService, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
              SafeCaller safeCaller)
            throws InvalidAlgorithmParameterException {
        super(address, port, pairingId, secretKey, mdnsService, accessoryRegistry, pairingRegistry,
                 safeCaller);
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("StandAlone", getAccessoryId());
    }
}
