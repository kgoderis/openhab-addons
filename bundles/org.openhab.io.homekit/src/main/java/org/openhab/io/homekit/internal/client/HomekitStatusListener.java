package org.openhab.io.homekit.internal.client;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.api.Characteristic;

@NonNullByDefault
public interface HomekitStatusListener {

    void onAccessoryRemoved(@Nullable Bridge bridge, Accessory accessory);

    void onAccessoryAdded(@Nullable Bridge bridge, Accessory accessory);

    void onCharacteristicRemoved(@Nullable Bridge bridge, Characteristic characteristic);

    void onCharacteristicAdded(@Nullable Bridge bridge, Characteristic characteristic);

    void onCharacteristicStateChanged(@Nullable Bridge bridge, Characteristic characteristic);

}