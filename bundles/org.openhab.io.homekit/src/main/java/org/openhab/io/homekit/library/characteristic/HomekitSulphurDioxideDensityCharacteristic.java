package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Sulphur Dioxide Density Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/SulphurDioxideDensity">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000C5-0000-1000-8000-0026BB765291", name = "Sulphur Dioxide Density", tag = "sulphurDioxideDensity")
@NonNullByDefault
public class HomekitSulphurDioxideDensityCharacteristic extends HomekitFloatCharacteristic {

    public HomekitSulphurDioxideDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 1000.0, 1.0, "micrograms/m3");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Sulphur Dioxide Density");
    }

    public HomekitSulphurDioxideDensityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 1000.0;
    }
} 