package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * Air Particulate Size Characteristic.
 */
@HomekitCharacteristicType(type = "00000065-0000-1000-8000-0026BB765291", name = "Air Particulate Size", tag = "airParticulateSize")
@NonNullByDefault
public class HomekitAirParticulateSizeCharacteristic extends HomekitIntegerCharacteristic {

    public enum AirParticulateSize {
        SIZE_2_5_M(0),
        SIZE_10_M(1);
        private final int code;
        AirParticulateSize(int code) { this.code = code; }
        public int getCode() { return code; }
        public static AirParticulateSize fromCode(int code) {
            for (AirParticulateSize s : values()) {
                if (s.code == code) return s;
            }
            return SIZE_2_5_M;
        }
    }

    public HomekitAirParticulateSizeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Air Particulate Size");
    }

    public HomekitAirParticulateSizeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == 0 || value == 1);
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> set = new java.util.HashSet<>();
        for (AirParticulateSize s : AirParticulateSize.values()) {
            set.add(s.getCode());
        }
        return set;
    }
} 