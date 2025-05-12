package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Air Quality Characteristic.
 * This characteristic represents the target air quality for a device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetAirQuality">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AE-0000-1000-8000-0026BB765291", name = "Target Air Quality", tag = "targetAirQuality")
@NonNullByDefault
public class HomekitTargetAirQualityCharacteristic extends HomekitEnumCharacteristic {
    public enum TargetAirQuality {
        UNKNOWN(0),
        EXCELLENT(1),
        GOOD(2),
        FAIR(3),
        INFERIOR(4),
        POOR(5);
        private final int code;
        TargetAirQuality(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TargetAirQuality fromCode(int code) {
            for (TargetAirQuality s : values()) {
                if (s.code == code) return s;
            }
            return UNKNOWN;
        }
    }
    public HomekitTargetAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TargetAirQuality.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Air Quality");
    }
    public HomekitTargetAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < TargetAirQuality.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            TargetAirQuality.UNKNOWN.getCode(),
            TargetAirQuality.EXCELLENT.getCode(),
            TargetAirQuality.GOOD.getCode(),
            TargetAirQuality.FAIR.getCode(),
            TargetAirQuality.INFERIOR.getCode(),
            TargetAirQuality.POOR.getCode()
        );
    }
} 