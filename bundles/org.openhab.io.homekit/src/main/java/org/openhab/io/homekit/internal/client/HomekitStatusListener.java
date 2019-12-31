package org.openhab.io.homekit.internal.client;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Bridge;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;
import org.openhab.io.homekit.api.Service;

@NonNullByDefault
public interface HomekitStatusListener {

    void onAccessoryAdded(Bridge bridge, Accessory accessory);

    void onAccessoryRemoved(Bridge bridge, Accessory accessory);

    void onServiceAdded(Bridge bridge, Service service);

    void onServiceRemoved(Bridge bridge, Service service);

    void onCharacteristicAdded(Bridge bridge, Characteristic characteristic);

    void onCharacteristicRemoved(Bridge bridge, Characteristic characteristic);

    void onCharacteristicStateChanged(Bridge bridge, Characteristic characteristic);

}