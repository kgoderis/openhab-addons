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
 * The type can be one of: SINGLE, DOUBLE, or LONG.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "0000022F-0000-1000-8000-0026BB765291", name = "Tap Type", tag = "tapType")
@NonNullByDefault
public class HomekitTapTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum TapType {
        SINGLE(0),
        DOUBLE(1),
        LONG(2);

        private final int value;

        TapType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TapType fromValue(int value) {
            for (TapType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid Tap Type value: " + value);
        }
    }

    public HomekitTapTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TapType.values().length);
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
        if (value == null) {
            return false;
        }
        try {
            TapType.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TapType.SINGLE.getValue(),
            TapType.DOUBLE.getValue(),
            TapType.LONG.getValue()
        );
    }
} 