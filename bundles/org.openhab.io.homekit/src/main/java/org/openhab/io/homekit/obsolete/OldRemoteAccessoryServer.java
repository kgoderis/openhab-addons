package org.openhab.io.homekit.obsolete;
// package org.openhab.io.homekit.internal.server;

// import java.net.InetAddress;
// import java.util.Collection;

// import org.eclipse.jdt.annotation.NonNull;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.io.homekit.api.hap.AccessoryCategory;
// import org.openhab.io.homekit.api.hap.Pairing;
// import org.openhab.io.homekit.api.registry.AccessoryRegistry;
// import org.openhab.io.homekit.api.registry.PairingRegistry;

// // A bridge is a special type of HAP accessory server that bridges HomeKit Accessory Protocol and different RF/transport protocols, such as ZigBee or Z-Wave. A bridge must expose all the user-addressable functionality supported by its connected devices as HAP accessory objects to the HAP controller(s). A bridge must ensure that the instance ID assigned to the HAP accessory objects exposed on behalf of its connected devices do not change for the lifetime of the server/client pairing.

// // For example, a bridge that bridges three lights would expose four HAP accessory objects: one HAP accessory object that represents the bridge itself that may include a "firmware update" service, and three additional HAP accessory objects that each contain a "lightbulb" service.

// // A bridge must not expose more than 150 HAP accessory objects. The HAP accessory object with an instance ID of 1 is considered the primary HAP accessory object. For bridges, this must be the bridge itself.

// public class RemoteAccessoryServer extends AbstractRemoteAccessoryServer {

//     public RemoteAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
//             AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry) {
//         super(category, address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry);
//     }

//     public RemoteAccessoryServer(AccessoryCategory category, InetAddress address,
//             int port, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry) throws Exception {
//         this(category, address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry);
//     }

//     public Pairing getPairing() {
//         @NonNull
//         Collection<@NonNull Pairing> pairings = pairingRegistry.get(getPairingId());

//         if (pairings.size() == 1) {
//             return (Pairing) pairings.toArray()[0];
//         }

//         if (pairings.size() > 1) {
//             // oh oh
//         }

//         return null;
//     }

// }
