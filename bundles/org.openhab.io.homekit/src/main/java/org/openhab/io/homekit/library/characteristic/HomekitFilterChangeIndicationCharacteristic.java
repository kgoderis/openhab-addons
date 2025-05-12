package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Filter Change Indication Characteristic.
 * This characteristic indicates if a filter needs to be changed.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/FilterChangeIndication">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000AC-0000-1000-8000-0026BB765291", name = "Filter Change Indication", tag = "filterChangeIndication")
@NonNullByDefault
public class HomekitFilterChangeIndicationCharacteristic extends HomekitIntegerCharacteristic {
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
    public HomekitFilterChangeIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Filter Change Indication");
    }
    public HomekitFilterChangeIndicationCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == FilterChangeIndication.FILTER_OK.getCode() || value == FilterChangeIndication.CHANGE_FILTER.getCode());
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(FilterChangeIndication.FILTER_OK.getCode(), FilterChangeIndication.CHANGE_FILTER.getCode());
    }
} 