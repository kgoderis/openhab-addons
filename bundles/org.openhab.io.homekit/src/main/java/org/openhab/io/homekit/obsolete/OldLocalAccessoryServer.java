package org.openhab.io.homekit.obsolete;
// package org.openhab.io.homekit.internal.server;

// import java.net.InetAddress;
// import java.security.InvalidAlgorithmParameterException;

// import org.openhab.core.common.SafeCaller;
// import org.openhab.core.io.transport.mdns.MDNSService;
// import org.openhab.io.homekit.api.registry.AccessoryRegistry;
// import org.openhab.io.homekit.api.registry.PairingRegistry;

// /**
//  * An HomekitAccessoryServer is a class that supports HomeKit Accessory Protocol and exposes a collection of
//  * HomekitAccessory to the HAP controller(s). An HomekitAccessoryServer represents one endpoint of the pairing
//  * relationship established with HAP Pairing, and exposes at least one HomekitAccessory object.
//  *
//  * @author Karel Goderis
//  */
// public class LocalAccessoryServer extends AbstractAccessoryServer {

//     public LocalAccessoryServer(InetAddress address, int port, byte[] pairingId, byte[] secretKey,
//             MDNSService mdnsService, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry,
//             SafeCaller safeCaller) throws InvalidAlgorithmParameterException {
//         super(address, port, pairingId, secretKey, mdnsService, accessoryRegistry, pairingRegistry, safeCaller);
//     }
// }
