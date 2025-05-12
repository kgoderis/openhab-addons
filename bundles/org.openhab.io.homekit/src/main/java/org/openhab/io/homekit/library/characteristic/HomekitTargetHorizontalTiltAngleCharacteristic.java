package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitIntegerCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Target Horizontal Tilt Angle Characteristic.
 * This characteristic represents the target horizontal tilt angle for a device (e.g., window covering).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/TargetHorizontalTiltAngle">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000007B-0000-1000-8000-0026BB765291", name = "Target Horizontal Tilt Angle", tag = "targetHorizontalTiltAngle")
@NonNullByDefault
public class HomekitTargetHorizontalTiltAngleCharacteristic extends HomekitIntegerCharacteristic {

    public HomekitTargetHorizontalTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, -90, 90, "°");
        withInstanceId(instanceId)
            .withPairedWrite(true)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Target Horizontal Tilt Angle");
    }

    public HomekitTargetHorizontalTiltAngleCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public State toState(Integer value) {
        return super.toState(value);
    }

    @Override
    public JsonObject toEventJson(Integer value) {
        return super.toEventJson(value);
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
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= -90 && value <= 90;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Collections.emptySet();
    }
}
