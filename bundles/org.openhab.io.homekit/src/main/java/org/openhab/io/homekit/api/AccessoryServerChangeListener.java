package org.openhab.io.homekit.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface AccessoryServerChangeListener {

    void onServerUpdated(AccessoryServer server);

    void onAccessoryAdded(Accessory accessory);

    void onAccessoryRemoved(Accessory accessory);

    void onServiceAdded(Service service);

    void onServiceRemoved(Service service);

    void onCharacteristicAdded(Characteristic characteristic);

    void onCharacteristicRemoved(Characteristic characteristic);

    void onCharacteristicStateChanged(Characteristic characteristic);

}
