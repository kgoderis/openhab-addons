package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Bridge;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.hap.HomekitCharacteristic;
import org.openhab.io.homekit.api.hap.HomekitService;

@NonNullByDefault
public interface HomekitStatusListener {

    void onAccessoryAdded(Bridge bridge, HomekitAccessory accessory);

    void onAccessoryRemoved(Bridge bridge, HomekitAccessory accessory);

    void onServiceAdded(Bridge bridge, HomekitService service);

    void onServiceRemoved(Bridge bridge, HomekitService service);

    void onCharacteristicAdded(Bridge bridge, HomekitCharacteristic characteristic);

    void onCharacteristicRemoved(Bridge bridge, HomekitCharacteristic characteristic);

    void onCharacteristicStateChanged(Bridge bridge, HomekitCharacteristic characteristic);
}
