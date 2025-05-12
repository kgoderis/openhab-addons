package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Tap Type Characteristic.
 * This characteristic represents the type of tap interaction.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TapType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000022F-0000-1000-8000-0026BB765291", name = "Tap Type", tag = "tapType")
@NonNullByDefault
public class HomekitTapTypeCharacteristic extends HomekitEnumCharacteristic {
    public static final int SINGLE = 0;
    public static final int DOUBLE = 1;
    public static final int LONG = 2;

    public HomekitTapTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 2);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Tap Type");
    }

    public HomekitTapTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= SINGLE && value <= LONG;
    }
} 