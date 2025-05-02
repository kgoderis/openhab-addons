/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;
import org.openhab.io.homekit.internal.events.HomekitEventManager;

/**
 * @author kgoderis
 *
 */
@NonNullByDefault
public class OnCharacteristic extends BooleanCharacteristic {

    private static final String TYPE = "00000025-0000-1000-8000-0026BB765291";

    public OnCharacteristic(Service service, long instanceId, HomekitEventManager eventManager) {
        super(service, instanceId, true, true, true, "On", TYPE, eventManager);
    }

    public OnCharacteristic(Service service, JsonValue value, HomekitEventManager eventManager) {
        super(service, value, eventManager);
    }

    public static String getType() {
        return TYPE;
    }

    public static String getTag() {
        return OnCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }

    @Override
    public JsonObject toEventJson(@Nullable Boolean value) {
        return super.toEventJson(value != null ? value : false);
    }

    @Override
    public JsonObject toEventJson() {
        return super.toEventJson();
    }

    @Override
    public JsonValue toValueJson(@Nullable Boolean value) {
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
    public State toState(@Nullable Boolean value) {
        return OnOffType.from(value != null && value);
    }
}
