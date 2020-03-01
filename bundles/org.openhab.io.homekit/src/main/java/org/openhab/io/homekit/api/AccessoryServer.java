package org.openhab.io.homekit.api;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.server.HttpConnection;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.internal.server.AccessoryServerUID;

/**
 * Interface for an HAP Accessory Server that exposes a collection of Accessories to the HAP controller(s). An HAP
 * Accessory Server represents one endpoint of the pairing relationship established with HAP Pairing
 *
 * @author Karel Goderis
 */
@NonNullByDefault
public interface AccessoryServer extends Identifiable<AccessoryServerUID> {

    /**
     * A unique identifier that will be advertised by HAP, and that will be used to pair the AccessoryServer.
     *
     * @return the unique pairing identifier.
     */
    String getId();

    /**
     * A setup code used for pairing the AccessoryServer. This setup code will be required by the cliet (e.g iOS/iPadOS)
     * in order to
     * complete pairing. The setup codes cannot be sequential and should not have a repeating pattern.
     *
     * @return the setup code, in the form ###-##-###
     */
    String getSetupCode();

    /**
     * The salt that will be used when hashing the setup code to send to the client.
     *
     * @return the salt.
     */
    BigInteger getSalt();

    /**
     * The private key used during pairing and message encryption.
     *
     * @return the private key.
     */
    byte[] getPrivateKey();

    int getConfigurationIndex();

    /**
     * Every Accessory must support a manufacturer-defined mechanism to restore itself to a “factory reset” state where
     * all pairing information is erased and restored to factory default settings. This mechanism should be easily
     * accessible to a user, e.g. a physical button or a reset code.
     */
    void factoryReset();

    /**
     * An Accessory object represents a physical accessory on an AccessoryServer. For example, a
     * thermostat would expose a single Accessory object that represents the user-addressable functionality of
     * the thermostat
     *
     * The Accessory object with an instance ID of 1 is considered the primary Accessory object. For
     * BridgeAccessoryServers, this must be the BridgeAccessoryServer itself.
     *
     * @return the list of HomekitAccessories.
     */
    Collection<ManagedAccessory> getAccessories();

    @Nullable
    ManagedAccessory getAccessory(int instanceId);

    @Nullable
    ManagedAccessory getAccessory(Class<? extends ManagedAccessory> accessoryClass);

    void addAccessory(ManagedAccessory accessory);

    void removeAccessory(ManagedAccessory accessory);

    /**
     * Accessory Instance IDs are assigned from the same number pool that is global across entire
     * AccessoryServer. For example, if the first Accessory object has an Instance ID of “1”, then no
     * other Accessory object can have an Instance ID of “1” within the AccessoryServer.
     *
     * @return the next available unique identifier for to be used by an accessory
     */
    long getInstanceId();

    // long getCurrentInstanceId();

    void advertise();

    /**
     * During the pairing process one should store the pairing id and public key in a persistent manner so that
     * the public key can later be retrieved using {@link #getPairingPublicKey(String)}.
     *
     * @param clientPairingId the client's pairing id. The value will not be meaningful to anything but
     *            iOS.
     * @param clientPublicKey the client's public key.
     */
    void addPairing(byte[] clientPairingId, byte[] clientPublicKey);

    /**
     * Remove an existing pairing. Subsequent calls to {@link #getPairingPublicKey(String)} for this pairing id return
     * null.
     *
     * @param clientPairingId the clientPairingId to delete
     */
    void removePairing(byte[] clientPairingId);

    /**
     * When an already paired client is re-connecting, the public key returned by this
     * method will be compared with the signature of the pair verification request to validate the
     * client.
     *
     * @param clientPairingId the client pairing id of the client to retrieve the public key for.
     * @return the previously stored public key for this client.
     */
    byte @Nullable [] getPairingPublicKey(byte[] clientPairingId);

    /**
     * When the Accessory Server has been paired, the homekit server advertises whether the
     * server has already been paired. At this time, it's unclear whether multiple pairings can be
     * created, however it is known that advertising as unpaired will break in iOS 9. The default
     * value has been provided to maintain API compatibility for implementations targeting iOS 8.
     *
     * @return whether a pairing has been established and stored
     */
    default boolean isPaired() {
        return false;
    }

    void addNotification(ManagedCharacteristic<?> characteristic, HttpConnection connection);

    void removeNotification(ManagedCharacteristic<?> characteristic);

    InetAddress getLocalAddress();

    int getPort();

    void addChangeListener(AccessoryServerChangeListener listener);

    void removeChangeListener(AccessoryServerChangeListener listener);

    byte[] getPairingId();

    HomekitCommunicationManager getCommunicationManager();

}
