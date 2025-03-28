package org.openhab.io.homekit.api;

import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

//TODO : ServerFactory that can create AccessoryHolder for the client side
// Rename to AccessoryHolderFactory

@NonNullByDefault
public interface AccessoryServerFactory {

    /**
     * Creates a new AccessoryServer instance of type <code>factoryType</code>
     *
     * @param factoryType
     * @return a new AccessoryServer of type <code>factoryType</code> or <code>null</code> if no matching class
     *         is known.
     */
    @Nullable
    AccessoryServer createServer(String factoryType, InetAddress localAddress, int port);

    /**
     * Creates a new AccessoryServer instance of type <code>factoryType</code>
     *
     * @param factoryType
     * @param pairingId
     * @param salt
     * @param privateKey
     * @param configurationIndex
     * @param instanceIdPool
     * @return a new AccessoryServer of type <code>factoryType</code> or <code>null</code> if no matching class
     *         is known.
     */
    @Nullable
    AccessoryServer createServer(String factoryType, InetAddress localAddress, int port, byte[] pairingId,
            byte[] privateKey, int configurationIndex);

    /**
     * Returns the list of all supported AccessoryServer types of this factory.
     *
     * @return the supported AccessoryServer types
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
    // * This is used as the unique identifier of the AccessoryServer during mDNS advertising. It is a valid MAC
    // * address generated in the locally administered range so as not to conflict with any commercial
    // * devices.
    // *
    // * @return the generated pairing id
    // */
    // byte[] generatePairingId();
}
