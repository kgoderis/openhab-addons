package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitReadOnlyStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Hardware Revision Characteristic.
 * This characteristic represents the hardware revision string.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/HardwareRevision">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "00000053-0000-1000-8000-0026BB765291", name = "Hardware Revision", tag = "hardwareRevision")
@NonNullByDefault
public class HomekitHardwareRevisionCharacteristic extends HomekitReadOnlyStringCharacteristic {
    public HomekitHardwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
            .withDescription("Hardware Revision");
    }
    public HomekitHardwareRevisionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public State toState(String value) {
        return super.toState(value);
    }
    @Override
    public JsonObject toJson(boolean includeMeta, boolean includePermissions, boolean includeType, boolean includeEvent) {
        return super.toJson(includeMeta, includePermissions, includeType, includeEvent);
    }
    @Override
    public JsonObject toJson() {
        return super.toJson();
    }
    @Override
    public JsonObject toReducedJson() {
        return super.toReducedJson();
    }
    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }
    @Override
    public JsonObject toEventJson(String value) {
        return super.toEventJson(value);
    }
} 