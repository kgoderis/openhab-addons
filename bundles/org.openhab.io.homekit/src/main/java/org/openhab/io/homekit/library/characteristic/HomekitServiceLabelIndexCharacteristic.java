package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Service Label Index Characteristic.
 * This characteristic represents the index of a service label.
 * The value ranges from 1 to 255, where each number corresponds to a specific label.
 * This is used to identify and organize services in the HomeKit ecosystem.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000CB-0000-1000-8000-0026BB765291", name = "Service Label Index", tag = "serviceLabelIndex", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitServiceLabelIndexCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitServiceLabelIndexCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 1, 255, "");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(false)
                .withDescription("Service Label Index");
    }

    public HomekitServiceLabelIndexCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 1 && value <= 255;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
