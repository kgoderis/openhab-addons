package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Digital Zoom Characteristic.
 * This characteristic represents the digital zoom level for a device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "0000011D-0000-1000-8000-0026BB765291", name = "Digital Zoom", tag = "digitalZoom")
@NonNullByDefault
public class HomekitDigitalZoomCharacteristic extends HomekitFloatCharacteristic {

    public HomekitDigitalZoomCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 0.1, "");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Digital Zoom");
    }

    public HomekitDigitalZoomCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        // No explicit min/max in spec, but typically 0-100 is a safe default
        return value != null && value >= 0.0 && value <= 100.0;
    }
} 