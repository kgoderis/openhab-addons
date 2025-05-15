package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Current Heater Cooler State characteristic.
 * This characteristic represents the current state of a heater/cooler device.
 *
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 * @author Karel Goderis - Initial contribution
 */
@HomekitCharacteristicType(type = "000000B1-0000-1000-8000-0026BB765291", name = "Current Heater Cooler State", tag = "currentHeaterCoolerState", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitCurrentHeaterCoolerStateCharacteristic extends HomekitEnumCharacteristic {
    public enum HeaterCoolerState {
        INACTIVE(0),
        IDLE(1),
        HEATING(2),
        COOLING(3);

        private final int code;

        HeaterCoolerState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static HeaterCoolerState fromCode(int code) {
            for (HeaterCoolerState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }

    public HomekitCurrentHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, HeaterCoolerState.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
                .withDescription("Current Heater Cooler State");
    }

    public HomekitCurrentHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < HeaterCoolerState.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(HeaterCoolerState.INACTIVE.getCode(), HeaterCoolerState.IDLE.getCode(),
                HeaterCoolerState.HEATING.getCode(), HeaterCoolerState.COOLING.getCode());
    }
}
