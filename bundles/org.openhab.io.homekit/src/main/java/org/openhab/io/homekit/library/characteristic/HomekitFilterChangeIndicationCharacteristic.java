package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Filter Change Indication Characteristic.
 * <p>
 * This characteristic indicates if a filter needs to be changed in an air purifier or similar device. The value is an integer corresponding to a specific indication as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "000000AC-0000-1000-8000-0026BB765291", name = "Filter Change Indication", tag = "filterChangeIndication")
@NonNullByDefault
public class HomekitFilterChangeIndicationCharacteristic extends HomekitIntegerCharacteristic {
    /**
     * Enum representing the possible filter change indications.
     */
    public enum FilterChangeIndication {
        FILTER_OK(0),
        CHANGE_FILTER(1);
        private final int code;
        FilterChangeIndication(int code) { this.code = code; }
        public int getCode() { return code; }
        public static FilterChangeIndication fromCode(int code) {
            for (FilterChangeIndication s : values()) {
                if (s.code == code) return s;
            }
            return FILTER_OK;
        }
    }
    /**
     * Constructs a new Filter Change Indication characteristic.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param instanceId the instance ID for this characteristic
     */
    public HomekitFilterChangeIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Filter Change Indication");
    }
    /**
     * Constructs a new Filter Change Indication characteristic from a JSON value.
     *
     * @param service the HomeKit service this characteristic belongs to
     * @param eventManager the event manager for handling HomeKit events
     * @param value the JSON value to initialize the characteristic with
     */
    public HomekitFilterChangeIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    /**
     * Checks if the given value is an allowed filter change indication.
     *
     * @param value the value to check
     * @return true if the value is allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == FilterChangeIndication.FILTER_OK.getCode() || value == FilterChangeIndication.CHANGE_FILTER.getCode());
    }
    /**
     * Returns the set of allowed filter change indication values.
     *
     * @return the set of allowed values
     */
    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(FilterChangeIndication.FILTER_OK.getCode(), FilterChangeIndication.CHANGE_FILTER.getCode());
    }
} 