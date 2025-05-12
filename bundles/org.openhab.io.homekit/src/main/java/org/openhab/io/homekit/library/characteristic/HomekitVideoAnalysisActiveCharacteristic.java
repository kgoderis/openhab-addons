package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitBooleanCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Video Analysis Active Characteristic.
 * This characteristic represents whether video analysis is active.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/VideoAnalysisActive">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000229-0000-1000-8000-0026BB765291", name = "Video Analysis Active", tag = "videoAnalysisActive")
@NonNullByDefault
public class HomekitVideoAnalysisActiveCharacteristic extends HomekitBooleanCharacteristic {
    public HomekitVideoAnalysisActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(true)
            .withEvents(true)
            .withDescription("Video Analysis Active");
    }

    public HomekitVideoAnalysisActiveCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
} 