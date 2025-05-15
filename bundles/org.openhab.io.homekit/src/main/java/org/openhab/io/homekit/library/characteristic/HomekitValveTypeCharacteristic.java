package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Valve Type Characteristic.
 * This characteristic represents the type of valve for a device.
 * The type can be one of: GENERIC, IRRIGATION, SHOWER_HEAD, WATER_FAUCET.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(
    type = "000000D5-0000-1000-8000-0026BB765291",
    name = "Valve Type",
    tag = "valveType",
    acceptedItemTypes = {"Number", "String"}
)
@NonNullByDefault
public class HomekitValveTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum ValveType {
        GENERIC(0),
        IRRIGATION(1),
        SHOWER_HEAD(2),
        WATER_FAUCET(3);

        private final int value;

        ValveType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ValveType fromValue(int value) {
            for (ValveType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Valve Type value: " + value);
        }
    }

    public HomekitValveTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, ValveType.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("Valve Type");
    }

    public HomekitValveTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) {
            return false;
        }
        try {
            ValveType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ValveType.GENERIC.getValue(), ValveType.IRRIGATION.getValue(),
                ValveType.SHOWER_HEAD.getValue(), ValveType.WATER_FAUCET.getValue());
    }

    public void setValue(ValveType value) {
        try {
            setValue(value.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set Valve Type value", e);
        }
    }
}
