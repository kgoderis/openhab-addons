package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Heater Cooler State Characteristic.
 * This characteristic represents the target state for a heater/cooler.
 * The state can be one of: AUTO, HEAT, or COOL.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "000000B2-0000-1000-8000-0026BB765291", name = "Target Heater Cooler State", tag = "targetHeaterCoolerState", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitTargetHeaterCoolerStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetHeaterCoolerState {
        AUTO(0),
        HEAT(1),
        COOL(2);

        private final int value;

        TargetHeaterCoolerState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetHeaterCoolerState fromValue(int value) {
            for (TargetHeaterCoolerState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Heater Cooler State value: " + value);
        }
    }

    public HomekitTargetHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, TargetHeaterCoolerState.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Heater Cooler State");
    }

    public HomekitTargetHeaterCoolerStateCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= TargetHeaterCoolerState.AUTO.getValue()
                && value <= TargetHeaterCoolerState.COOL.getValue();
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(TargetHeaterCoolerState.AUTO.getValue(), TargetHeaterCoolerState.HEAT.getValue(),
                TargetHeaterCoolerState.COOL.getValue());
    }

    public void setValue(TargetHeaterCoolerState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Heater Cooler State value", e);
        }
    }
}
