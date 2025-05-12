package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Remote Key Characteristic.
 * This characteristic represents the remote key commands for a device.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "000000E1-0000-1000-8000-0026BB765291", name = "Remote Key", tag = "remoteKey")
@NonNullByDefault
public class HomekitRemoteKeyCharacteristic extends HomekitIntegerCharacteristic {
    public enum RemoteKey {
        REWIND(0),
        FAST_FORWARD(1),
        NEXT_TRACK(2),
        PREVIOUS_TRACK(3),
        ARROW_UP(4),
        ARROW_DOWN(5),
        ARROW_LEFT(6),
        ARROW_RIGHT(7),
        SELECT(8),
        BACK(9),
        EXIT(10),
        PLAY_PAUSE(11),
        INFORMATION(15);
        
        private final int code;
        
        RemoteKey(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static RemoteKey fromCode(int code) {
            for (RemoteKey k : values()) {
                if (k.code == code) {
                    return k;
                }
            }
            return null;
        }
    }

    public HomekitRemoteKeyCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 15, "");
        withInstanceId(instanceId)
            .withPairedRead(false)
            .withPairedWrite(true)
            .withEvents(false)
            .withDescription("Remote Key");
    }

    public HomekitRemoteKeyCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && RemoteKey.fromCode(value) != null;
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(
            RemoteKey.REWIND.getCode(),
            RemoteKey.FAST_FORWARD.getCode(),
            RemoteKey.NEXT_TRACK.getCode(),
            RemoteKey.PREVIOUS_TRACK.getCode(),
            RemoteKey.ARROW_UP.getCode(),
            RemoteKey.ARROW_DOWN.getCode(),
            RemoteKey.ARROW_LEFT.getCode(),
            RemoteKey.ARROW_RIGHT.getCode(),
            RemoteKey.SELECT.getCode(),
            RemoteKey.BACK.getCode(),
            RemoteKey.EXIT.getCode(),
            RemoteKey.PLAY_PAUSE.getCode(),
            RemoteKey.INFORMATION.getCode()
        );
    }
} 