package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Transport Characteristic.
 * This characteristic represents the current transport state.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentTransport">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000022B-0000-1000-8000-0026BB765291", name = "Current Transport", tag = "currentTransport", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitCurrentTransportCharacteristic extends HomekitEnumCharacteristic {
    public static final int STOPPED = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int TRANSITIONING = 3;

    public HomekitCurrentTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, 3);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Current Transport");
    }

    public HomekitCurrentTransportCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= STOPPED && value <= TRANSITIONING;
    }
}
