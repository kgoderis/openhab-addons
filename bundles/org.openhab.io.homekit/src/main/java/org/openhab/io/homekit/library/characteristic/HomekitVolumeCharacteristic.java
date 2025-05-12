package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Volume Characteristic.
 * This characteristic represents the volume level for a device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/Volume">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000119-0000-1000-8000-0026BB765291", name = "Volume", tag = "volume")
@NonNullByDefault
public class HomekitVolumeCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitVolumeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 100, "%");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Volume");
    }

    public HomekitVolumeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value <= 100;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
} 