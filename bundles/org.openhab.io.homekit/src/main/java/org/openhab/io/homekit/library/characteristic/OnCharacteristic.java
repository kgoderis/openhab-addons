/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;
import javax.json.JsonValue;

/**
 * @author kgoderis
 *
 */
public class OnCharacteristic extends BooleanCharacteristic {

    public OnCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "On");
    }

    public OnCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000025-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return OnCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
