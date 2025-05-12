package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Slat Type Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/SlatType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000C0-0000-1000-8000-0026BB765291", name = "Slat Type", tag = "slatType")
@NonNullByDefault
public class HomekitSlatTypeCharacteristic extends HomekitIntegerCharacteristic {
    public enum SlatType {
        HORIZONTAL(0),
        VERTICAL(1);
        
        private final int code;
        
        SlatType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static SlatType fromCode(int code) {
            for (SlatType s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return HORIZONTAL;
        }
    }

    public HomekitSlatTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Slat Type");
    }

    public HomekitSlatTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == SlatType.HORIZONTAL.getCode() || value == SlatType.VERTICAL.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(SlatType.HORIZONTAL.getCode(), SlatType.VERTICAL.getCode());
    }
} 