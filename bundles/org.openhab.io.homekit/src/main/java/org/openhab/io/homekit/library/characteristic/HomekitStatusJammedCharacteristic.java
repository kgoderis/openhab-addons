package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Status Jammed Characteristic.
 * This characteristic indicates if the accessory is jammed (e.g., a lock or door).
 *
 * @see <a href="https://developer.apple.com/documentation/homekit/hmcharacteristicstatusjammed">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000078-0000-1000-8000-0026BB765291", name = "Status Jammed", tag = "statusJammed")
@NonNullByDefault
public class HomekitStatusJammedCharacteristic extends HomekitEnumCharacteristic {

    public enum StatusJammed {
        NOT_JAMMED(0),
        JAMMED(1);

        private final int code;

        StatusJammed(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static StatusJammed fromCode(int code) {
            for (StatusJammed state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return NOT_JAMMED;
        }
    }

    public HomekitStatusJammedCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, StatusJammed.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Status Jammed");
    }

    public HomekitStatusJammedCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == StatusJammed.NOT_JAMMED.getCode() || 
                                value == StatusJammed.JAMMED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(StatusJammed.NOT_JAMMED.getCode(), 
                              StatusJammed.JAMMED.getCode());
    }
} 