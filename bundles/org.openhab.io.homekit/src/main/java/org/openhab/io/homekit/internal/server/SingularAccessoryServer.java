package org.openhab.io.homekit.internal.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryRegistry;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.openhab.io.homekit.api.PairingRegistry;

/**
 * An HomekitAccessoryServer is a class that supports HomeKit Accessory Protocol and exposes a collection of
 * HomekitAccessory to the HAP controller(s). An HomekitAccessoryServer represents one endpoint of the pairing
 * relationship established with HAP Pairing, and exposes at least one HomekitAccessory object.
 *
 * @author Karel Goderis
 */
public class SingularAccessoryServer extends AbstractAccessoryServer {

    public SingularAccessoryServer(InetAddress localAddress, int port, String pairingId, BigInteger salt,
            byte[] privateKey, MDNSService mdnsService, AccessoryRegistry accessoryRegistry,
            PairingRegistry pairingRegistry, NotificationRegistry notificationRegistry,
            HomekitCommunicationManager manager) throws InvalidAlgorithmParameterException {
        super(localAddress, port, pairingId, salt, privateKey, mdnsService, accessoryRegistry, pairingRegistry,
                notificationRegistry, manager);
    }

    @Override
    public @NonNull AccessoryServerUID getUID() {
        return new AccessoryServerUID("Singular", getId());
    }
}
