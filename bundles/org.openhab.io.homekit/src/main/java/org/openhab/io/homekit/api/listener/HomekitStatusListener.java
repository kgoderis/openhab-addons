package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Bridge;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

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
