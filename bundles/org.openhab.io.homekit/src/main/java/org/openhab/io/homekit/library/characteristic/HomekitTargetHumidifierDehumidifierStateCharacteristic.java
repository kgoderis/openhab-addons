package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Humidifier Dehumidifier State Characteristic.
 * This characteristic represents the target state for a humidifier/dehumidifier.
 * The state can be one of: HUMIDIFIER (0), DEHUMIDIFIER (1), or AUTO (2).
 * This is used to control whether the device should humidify, dehumidify, or automatically switch between modes.
 *
 * @author Karel Goderis
 * @see <a href=
 *      "https://developer.apple.com/documentation/homekit/hap-characteristic-types/target-humidifier-dehumidifier-state">HAP
 *      Specification</a>
 */
@HomekitCharacteristicType(type = "000000B4-0000-1000-8000-0026BB765291", name = "Target Humidifier Dehumidifier State", tag = "targetHumidifierDehumidifierState", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitTargetHumidifierDehumidifierStateCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetHumidifierDehumidifierState {
        HUMIDIFIER(0),
        DEHUMIDIFIER(1),
        AUTO(2);

        private final int value;

        TargetHumidifierDehumidifierState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TargetHumidifierDehumidifierState fromValue(int value) {
            for (TargetHumidifierDehumidifierState state : values()) {
                if (state.value == value) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Target Humidifier Dehumidifier State value: " + value);
        }
    }

    public HomekitTargetHumidifierDehumidifierStateCharacteristic(HomekitService service,
            HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 3);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(true).withEvents(true)
                .withDescription("Target Humidifier Dehumidifier State");
    }

    public HomekitTargetHumidifierDehumidifierStateCharacteristic(HomekitService service,
            HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        try {
            return value != null && TargetHumidifierDehumidifierState.fromValue(value) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> values = new java.util.HashSet<>();
        for (TargetHumidifierDehumidifierState state : TargetHumidifierDehumidifierState.values()) {
            values.add(state.getValue());
        }
        return values;
    }

    public void setValue(TargetHumidifierDehumidifierState value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Target Humidifier Dehumidifier State value", e);
        }
    }
}
