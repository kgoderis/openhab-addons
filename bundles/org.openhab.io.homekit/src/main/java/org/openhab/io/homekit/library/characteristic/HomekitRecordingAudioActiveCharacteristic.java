package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import java.util.Set;

/**
 * HomeKit Recording Audio Active Characteristic.
 * This characteristic represents whether audio recording is active or not.
 *
 * See the official HomeKit documentation for details.
 */
@HomekitCharacteristicType(type = "00000226-0000-1000-8000-0026BB765291", name = "Recording Audio Active", tag = "recordingAudioActive")
@NonNullByDefault
public class HomekitRecordingAudioActiveCharacteristic extends HomekitIntegerCharacteristic {
    public static final int DISABLE = 0;
    public static final int ENABLE = 1;

    public HomekitRecordingAudioActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0, 1, "");
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withTimedWrite(true)
            .withDescription("Recording Audio Active");
    }

    public HomekitRecordingAudioActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == DISABLE || value == ENABLE);
    }

    @Override
    public Set<Integer> getAllowedValues() {
        return Set.of(DISABLE, ENABLE);
    }
} 