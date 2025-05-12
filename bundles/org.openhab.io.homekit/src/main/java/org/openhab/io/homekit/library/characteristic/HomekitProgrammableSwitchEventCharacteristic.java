package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Programmable Switch Event Characteristic.
 * This characteristic represents the event for a programmable switch.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ProgrammableSwitchEvent">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000073-0000-1000-8000-0026BB765291", name = "Programmable Switch Event", tag = "programmableSwitchEvent")
@NonNullByDefault
public class HomekitProgrammableSwitchEventCharacteristic extends HomekitEnumCharacteristic {
    public enum ProgrammableSwitchEvent {
        SINGLE_PRESS(0),
        DOUBLE_PRESS(1),
        LONG_PRESS(2);
        private final int code;
        ProgrammableSwitchEvent(int code) { this.code = code; }
        public int getCode() { return code; }
        public static ProgrammableSwitchEvent fromCode(int code) {
            for (ProgrammableSwitchEvent s : values()) {
                if (s.code == code) return s;
            }
            return SINGLE_PRESS;
        }
    }
    public HomekitProgrammableSwitchEventCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, ProgrammableSwitchEvent.values().length);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
            .withDescription("Programmable Switch Event");
    }
    public HomekitProgrammableSwitchEventCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ProgrammableSwitchEvent.SINGLE_PRESS.getCode() || value == ProgrammableSwitchEvent.DOUBLE_PRESS.getCode() || value == ProgrammableSwitchEvent.LONG_PRESS.getCode());
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(ProgrammableSwitchEvent.SINGLE_PRESS.getCode(), ProgrammableSwitchEvent.DOUBLE_PRESS.getCode(), ProgrammableSwitchEvent.LONG_PRESS.getCode());
    }
} 