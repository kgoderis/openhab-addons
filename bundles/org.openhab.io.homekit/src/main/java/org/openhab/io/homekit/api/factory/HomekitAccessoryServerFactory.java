package org.openhab.io.homekit.api.factory;

import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;

//TODO : ServerFactory that can create AccessoryHolder for the client side
// Rename to AccessoryHolderFactory

@NonNullByDefault
public interface HomekitAccessoryServerFactory {

    /**
     * Creates a new HomekitAccessoryServer instance of type <code>factoryType</code>
     *
     * @param factoryType
     * @return a new HomekitAccessoryServer of type <code>factoryType</code> or <code>null</code> if no matching class
     *         is known.
     */
    @Nullable
    HomekitAccessoryServer createServer(String factoryType, InetAddress localAddress, int port);

    /**
     * Creates a new HomekitAccessoryServer instance of type <code>factoryType</code>
     *
     * @param factoryType
     * @param pairingId
     * @param salt
     * @param privateKey
     * @param configurationIndex
     * @param instanceIdPool
     * @return a new HomekitAccessoryServer of type <code>factoryType</code> or <code>null</code> if no matching class
     *         is known.
     */
    @Nullable
    HomekitAccessoryServer createServer(String factoryType, InetAddress localAddress, int port, byte[] pairingId,
            byte[] privateKey, int configurationIndex);

    /**
     * Returns the list of all supported HomekitAccessoryServer types of this factory.
     *
     * @return the supported HomekitAccessoryServer types
     */
    String[] getSupportedServerTypes();

    // /**
    // * This is used to salt the setup code during pairing.
    // *
    // * @return the generated salt
    // */
    // BigInteger generateSalt();

    // /**
    // * This is used as the private key during pairing and connection setup
    // * setup.
    // *
    // * @return the generated key
    // * @throws InvalidAlgorithmParameterException if the JVM does not contain the necessary encryption
    // * algorithms.
    // */
    // byte[] generatePrivateKey() throws InvalidAlgorithmParameterException;
    //
    // /**
    // * This is used as the unique identifier of the HomekitAccessoryServer during mDNS advertising. It is a valid MAC
    // * address generated in the locally administered range so as not to conflict with any commercial
    // * devices.
    // *
    // * @return the generated pairing id
    // */
    // byte[] generatePairingId();
}
