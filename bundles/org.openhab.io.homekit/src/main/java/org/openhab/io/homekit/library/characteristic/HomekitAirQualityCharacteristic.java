package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Air Quality Characteristic.
 * This characteristic represents the air quality level of a device.
 * The air quality is measured on a scale from UNKNOWN to POOR.
 *
 * @author Karel Goderis
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000095-0000-1000-8000-0026BB765291", name = "Air Quality", tag = "airQuality")
@NonNullByDefault
public class HomekitAirQualityCharacteristic extends HomekitEnumCharacteristic {

    public enum AirQuality {
        UNKNOWN(0),
        EXCELLENT(1),
        GOOD(2),
        FAIR(3),
        INFERIOR(4),
        POOR(5);

        private final int value;
        AirQuality(int value) { this.value = value; }
        public int getValue() { return value; }
        public static AirQuality fromValue(int value) {
            for (AirQuality a : values()) {
                if (a.value == value) return a;
            }
            throw new IllegalArgumentException("Invalid AirQuality value: " + value);
        }
    }

    public HomekitAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 6);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("Air Quality");
    }

    public HomekitAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        if (value == null) return false;
        try {
            AirQuality.fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        java.util.Set<Integer> allowed = new java.util.HashSet<>();
        for (AirQuality a : AirQuality.values()) {
            allowed.add(a.getValue());
        }
        return allowed;
    }
} 