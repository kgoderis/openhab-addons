package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Transmit Power Characteristic.
 * This characteristic represents the transmit power in dBm.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TransmitPower">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000242-0000-1000-8000-0026BB765291", name = "Transmit Power", tag = "transmitPower", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitTransmitPowerCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitTransmitPowerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, -128, 127, "dBm");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Transmit Power");
    }

    public HomekitTransmitPowerCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -128 && value <= 127;
    }
}
