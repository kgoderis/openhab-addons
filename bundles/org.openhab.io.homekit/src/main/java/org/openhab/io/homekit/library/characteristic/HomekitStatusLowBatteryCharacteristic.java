/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Low Battery Characteristic.
 * This characteristic indicates if the accessory has a low battery.
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristicstatuslowbattery">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000079-0000-1000-8000-0026BB765291", name = "Status Low Battery", tag = "statusLowBattery")
@NonNullByDefault
public class HomekitStatusLowBatteryCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusLowBattery {
        BATTERY_LEVEL_NORMAL(0),
        BATTERY_LEVEL_LOW(1);

        private final int code;

        StatusLowBattery(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static StatusLowBattery fromCode(int code) {
            for (StatusLowBattery state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return BATTERY_LEVEL_NORMAL;
        }
    }

    public HomekitStatusLowBatteryCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, StatusLowBattery.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Status Low Battery");
    }

    public HomekitStatusLowBatteryCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Integer value) {
        return super.toValueJson(value);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType,
            boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }

    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < StatusLowBattery.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            StatusLowBattery.BATTERY_LEVEL_NORMAL.getCode(),
            StatusLowBattery.BATTERY_LEVEL_LOW.getCode()
        );
    }

    public void setValue(StatusLowBattery value) throws Exception {
        setValue(value.getCode());
    }
}
