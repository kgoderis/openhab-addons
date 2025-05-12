package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Air Quality Characteristic.
 * This characteristic represents the current air quality level.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/AirQuality">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000095-0000-1000-8000-0026BB765291", name = "Air Quality", tag = "airQuality")
@NonNullByDefault
public class HomekitCurrentAirQualityCharacteristic extends HomekitEnumCharacteristic {
    public enum AirQuality {
        UNKNOWN(0),
        EXCELLENT(1),
        GOOD(2),
        FAIR(3),
        INFERIOR(4),
        POOR(5);
        
        private final int code;
        
        AirQuality(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static AirQuality fromCode(int code) {
            for (AirQuality s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return UNKNOWN;
        }
    }

    public HomekitCurrentAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, AirQuality.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Air Quality");
    }

    public HomekitCurrentAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < AirQuality.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            AirQuality.UNKNOWN.getCode(),
            AirQuality.EXCELLENT.getCode(),
            AirQuality.GOOD.getCode(),
            AirQuality.FAIR.getCode(),
            AirQuality.INFERIOR.getCode(),
            AirQuality.POOR.getCode()
        );
    }
} 