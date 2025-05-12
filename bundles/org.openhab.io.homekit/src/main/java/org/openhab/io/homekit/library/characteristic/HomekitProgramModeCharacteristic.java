package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Program Mode Characteristic.
 * @see <a href="https://developers.homebridge.io/#/characteristic/ProgramMode">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000D1-0000-1000-8000-0026BB765291", name = "Program Mode", tag = "programMode")
@NonNullByDefault
public class HomekitProgramModeCharacteristic extends HomekitIntegerCharacteristic {
    public enum ProgramMode {
        NO_PROGRAM_SCHEDULED(0),
        PROGRAM_SCHEDULED(1),
        PROGRAM_SCHEDULED_MANUAL_MODE(2);
        
        private final int code;
        
        ProgramMode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static ProgramMode fromCode(int code) {
            for (ProgramMode s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return NO_PROGRAM_SCHEDULED;
        }
    }

    public HomekitProgramModeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 2, "");
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Program Mode");
    }

    public HomekitProgramModeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == ProgramMode.NO_PROGRAM_SCHEDULED.getCode() 
            || value == ProgramMode.PROGRAM_SCHEDULED.getCode() 
            || value == ProgramMode.PROGRAM_SCHEDULED_MANUAL_MODE.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            ProgramMode.NO_PROGRAM_SCHEDULED.getCode(),
            ProgramMode.PROGRAM_SCHEDULED.getCode(),
            ProgramMode.PROGRAM_SCHEDULED_MANUAL_MODE.getCode()
        );
    }
} 