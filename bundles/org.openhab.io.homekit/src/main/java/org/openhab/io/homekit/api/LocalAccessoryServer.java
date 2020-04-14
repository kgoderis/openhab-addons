package org.openhab.io.homekit.api;

import org.eclipse.jetty.server.HttpConnection;
import org.openhab.io.homekit.HomekitCommunicationManager;

public interface LocalAccessoryServer extends AccessoryServer {

    /**
     * Every Accessory must support a manufacturer-defined mechanism to restore itself to a “factory reset” state where
     * all pairing information is erased and restored to factory default settings. This mechanism should be easily
     * accessible to a user, e.g. a physical button or a reset code.
     */
    void factoryReset();

    int getConfigurationIndex();

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

    HomekitCommunicationManager getCommunicationManager();

    void addNotification(ManagedCharacteristic<?> characteristic, HttpConnection connection);

    void removeNotification(ManagedCharacteristic<?> characteristic);

    // /**
    // * The salt that will be used when hashing the setup code to send to the client.
    // *
    // * @return the salt.
    // */
    // BigInteger getSalt();

}
