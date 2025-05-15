package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit CCA Signal Detect Threshold Characteristic.
 * This characteristic represents the CCA signal detect threshold in dBm.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/CCASignalDetectThreshold">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000245-0000-1000-8000-0026BB765291", name = "CCA Signal Detect Threshold", tag = "ccaSignalDetectThreshold", acceptedItemTypes = {
        "Number" })
@NonNullByDefault
public class HomekitCCASignalDetectThresholdCharacteristic extends HomekitIntegerCharacteristic {
    public HomekitCCASignalDetectThresholdCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, -128, 127, "dBm");
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("CCA Signal Detect Threshold");
    }

    public HomekitCCASignalDetectThresholdCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -128 && value <= 127;
    }
}
