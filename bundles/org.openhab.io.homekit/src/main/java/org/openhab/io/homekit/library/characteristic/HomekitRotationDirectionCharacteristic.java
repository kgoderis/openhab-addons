package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "00000028-0000-1000-8000-0026BB765291", name = "Rotation Direction", tag = "rotationDirection")
@NonNullByDefault
public class HomekitRotationDirectionCharacteristic extends HomekitEnumCharacteristic {

    public enum RotationDirection {
        CLOCKWISE(0),
        COUNTER_CLOCKWISE(1);

        private final int code;

        RotationDirection(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static RotationDirection fromCode(int code) {
            for (RotationDirection direction : values()) {
                if (direction.code == code) {
                    return direction;
                }
            }
            return CLOCKWISE;
        }
    }

    public HomekitRotationDirectionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, RotationDirection.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Rotation Direction");
    }

    public HomekitRotationDirectionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == RotationDirection.CLOCKWISE.getCode() || 
                                value == RotationDirection.COUNTER_CLOCKWISE.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(RotationDirection.CLOCKWISE.getCode(), 
                               RotationDirection.COUNTER_CLOCKWISE.getCode());
    }
} 