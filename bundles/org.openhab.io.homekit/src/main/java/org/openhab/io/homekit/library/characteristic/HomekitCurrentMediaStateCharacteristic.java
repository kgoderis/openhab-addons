package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Current Media State Characteristic.
 * This characteristic represents the current state for a media device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CurrentMediaState">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000E0-0000-1000-8000-0026BB765291", name = "Current Media State", tag = "currentMediaState")
@NonNullByDefault
public class HomekitCurrentMediaStateCharacteristic extends HomekitIntegerCharacteristic {
    public enum CurrentMediaState {
        PLAY(0),
        PAUSE(1),
        STOP(2),
        LOADING(4),
        INTERRUPTED(5);
        
        private final int code;
        
        CurrentMediaState(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static CurrentMediaState fromCode(int code) {
            for (CurrentMediaState s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return STOP;
        }
    }

    public HomekitCurrentMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 5, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Current Media State");
    }

    public HomekitCurrentMediaStateCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == CurrentMediaState.PLAY.getCode() 
            || value == CurrentMediaState.PAUSE.getCode() 
            || value == CurrentMediaState.STOP.getCode() 
            || value == CurrentMediaState.LOADING.getCode() 
            || value == CurrentMediaState.INTERRUPTED.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            CurrentMediaState.PLAY.getCode(),
            CurrentMediaState.PAUSE.getCode(),
            CurrentMediaState.STOP.getCode(),
            CurrentMediaState.LOADING.getCode(),
            CurrentMediaState.INTERRUPTED.getCode()
        );
    }
} 