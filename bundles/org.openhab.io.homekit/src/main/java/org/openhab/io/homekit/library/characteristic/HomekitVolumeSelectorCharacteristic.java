package org.openhab.io.homekit.library.characteristic;

import java.util.Set;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Volume Selector Characteristic.
 * <p>
 * This characteristic represents the volume selector for a device, allowing increment or decrement actions. The value
 * is an integer corresponding to a specific action as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "000000EA-0000-1000-8000-0026BB765291", name = "Volume Selector", tag = "volumeSelector", acceptedItemTypes = {"Number", "String"})
@NonNullByDefault
public class HomekitVolumeSelectorCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible volume selector actions.
     */
    public enum VolumeSelector {
        INCREMENT(0),
        DECREMENT(1);

        private final int code;

        VolumeSelector(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static VolumeSelector fromCode(int code) {
            for (VolumeSelector s : values()) {
                if (s.code == code)
                    return s;
            }
            return INCREMENT;
        }
    }

    /**
     * Constructs a new Volume Selector characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitVolumeSelectorCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, VolumeSelector.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
                .withDescription("Volume Selector");
    }

    /**
     * Constructs a new Volume Selector characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitVolumeSelectorCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed volume selector action.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < VolumeSelector.values().length;
    }

    /**
     * Returns the set of allowed volume selector actions.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(VolumeSelector.INCREMENT.getCode(), VolumeSelector.DECREMENT.getCode());
    }
}
