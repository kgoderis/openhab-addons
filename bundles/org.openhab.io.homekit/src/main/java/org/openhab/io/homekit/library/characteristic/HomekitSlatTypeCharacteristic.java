package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Slat Type Characteristic.
 * <p>
 * This characteristic represents the type of slat (horizontal or vertical) for a window covering or similar device. The value is an integer corresponding to a specific slat type as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "000000C0-0000-1000-8000-0026BB765291", name = "Slat Type", tag = "slatType")
@NonNullByDefault
public class HomekitSlatTypeCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible slat types.
     */
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

    /**
     * Constructs a new Slat Type characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitSlatTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(false)
            .withDescription("Slat Type");
    }

    /**
     * Constructs a new Slat Type characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitSlatTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed slat type.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == SlatType.HORIZONTAL.getCode() || value == SlatType.VERTICAL.getCode());
    }

    /**
     * Returns the set of allowed slat type values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(SlatType.HORIZONTAL.getCode(), SlatType.VERTICAL.getCode());
    }
} 