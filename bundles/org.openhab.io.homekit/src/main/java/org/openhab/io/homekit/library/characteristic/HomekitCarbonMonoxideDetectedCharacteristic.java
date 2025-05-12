package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Carbon Monoxide Detected Characteristic.
 * This characteristic represents whether carbon monoxide has been detected.
 * The value is an enumeration with two states: NOT_DETECTED and DETECTED.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristictypecarbonmonoxidedetected">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000069-0000-1000-8000-0026BB765291", name = "Carbon Monoxide Detected", tag = "carbonMonoxideDetected")
@NonNullByDefault
public class HomekitCarbonMonoxideDetectedCharacteristic extends HomekitEnumCharacteristic {

    public enum CarbonMonoxideDetected {
        NOT_DETECTED(0),
        DETECTED(1);

        private final int code;

        CarbonMonoxideDetected(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CarbonMonoxideDetected fromCode(int code) {
            for (CarbonMonoxideDetected state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_DETECTED;
        }
    }

    /**
     * Creates a new Carbon Monoxide Detected characteristic.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param instanceId The instance ID for this characteristic
     */
    public HomekitCarbonMonoxideDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, CarbonMonoxideDetected.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Carbon Monoxide Detected");
    }

    /**
     * Creates a new Carbon Monoxide Detected characteristic from a JSON value.
     *
     * @param service The HomeKit service this characteristic belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param value The JSON value to initialize the characteristic with
     */
    public HomekitCarbonMonoxideDetectedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == CarbonMonoxideDetected.NOT_DETECTED.getCode() || 
                                value == CarbonMonoxideDetected.DETECTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(CarbonMonoxideDetected.NOT_DETECTED.getCode(), 
                              CarbonMonoxideDetected.DETECTED.getCode());
    }
} 