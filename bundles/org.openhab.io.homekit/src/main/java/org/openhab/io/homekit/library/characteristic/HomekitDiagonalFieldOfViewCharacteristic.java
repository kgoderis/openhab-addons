package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Diagonal Field Of View Characteristic.
 * This characteristic represents the diagonal field of view in degrees.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/DiagonalFieldOfView">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000224-0000-1000-8000-0026BB765291", name = "Diagonal Field Of View", tag = "diagonalFieldOfView")
@NonNullByDefault
public class HomekitDiagonalFieldOfViewCharacteristic extends HomekitFloatCharacteristic {
    public HomekitDiagonalFieldOfViewCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 360.0, 1.0, "arcdegrees");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Diagonal Field Of View");
    }

    public HomekitDiagonalFieldOfViewCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 360.0;
    }
} 