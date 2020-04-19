package org.openhab.io.homekit.api;

import java.net.InetAddress;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
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

    void setSetupCode(String setupCode);

    /**
     * The private key used during pairing and message encryption.
     *
     * @return the private key.
     */
    byte[] getSecretKey();

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
    Collection<Accessory> getAccessories();

    @Nullable
    Accessory getAccessory(int instanceId);

    @Nullable
    Accessory getAccessory(Class<? extends Accessory> accessoryClass);

    void addAccessory(Accessory accessory);

    void removeAccessory(Accessory accessory);

    InetAddress getAddress();

    int getPort();

    void addChangeListener(AccessoryServerChangeListener listener);

    void removeChangeListener(AccessoryServerChangeListener listener);

    byte[] getPairingId();

    /**
     * During the pairing process one should store the pairing id and public key in a persistent manner so that
     * the public key can later be retrieved using {@link #getDestinationPublicKey(String)}.
     *
     * @param destinationPairingId the client's pairing id. The value will not be meaningful to anything but
     *            iOS.
     * @param destinationPublicKey the client's public key.
     */
    void addPairing(byte[] destinationPairingId, byte[] destinationPublicKey);

    /**
     * Remove an existing pairing. Subsequent calls to {@link #getDestinationPublicKey(String)} for this pairing id
     * return
     * null.
     *
     * @param destinationPairingId the clientPairingId to delete
     */
    void removePairing(byte[] destinationPairingId);

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

    @Nullable
    Pairing getPairing(byte[] destinationPairingId);

    /**
     * When an already paired client is re-connecting, the public key returned by this
     * method will be compared with the signature of the pair verification request to validate the
     * client.
     *
     * @param destinationPairingId the client pairing id of the client to retrieve the public key for.
     * @return the previously stored public key for this client.
     */
    byte @Nullable [] getDestinationPublicKey(byte[] destinationPairingId);

    void setConfigurationIndex(int configurationIndex);

    int getConfigurationIndex();

    Collection<Pairing> getPairings();
}
