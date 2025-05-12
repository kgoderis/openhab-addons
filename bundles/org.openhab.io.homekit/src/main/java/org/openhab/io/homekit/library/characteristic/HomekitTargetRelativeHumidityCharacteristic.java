package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Relative Humidity Characteristic.
 * This characteristic represents the target relative humidity that should be maintained.
 * The value is expressed as a percentage between 0 and 100.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristictypetargetrelativehumidity">HomeKit Documentation</a>
 */
@NonNullByDefault
@HomekitCharacteristicType(type = "00000034-0000-1000-8000-0026BB765291", name = "Target Relative Humidity", tag = "targetRelativeHumidity")
public class HomekitTargetRelativeHumidityCharacteristic extends HomekitFloatCharacteristic {

    /**
     * Creates a new Target Relative Humidity characteristic.
     * The value range is 0-100% with 0.1% step.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 0.0, 100.0, 0.1, "percentage");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Target Relative Humidity");
    }

    /**
     * Creates a new Target Relative Humidity characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitTargetRelativeHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }
} 