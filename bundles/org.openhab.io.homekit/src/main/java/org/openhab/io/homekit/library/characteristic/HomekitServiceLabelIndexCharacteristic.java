package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Service Label Index Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/ServiceLabelIndex">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000CB-0000-1000-8000-0026BB765291", name = "Service Label Index", tag = "serviceLabelIndex")
@NonNullByDefault
public class HomekitServiceLabelIndexCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitServiceLabelIndexCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 1, 255, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Service Label Index");
    }

    public HomekitServiceLabelIndexCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 1 && value <= 255;
    }
} 