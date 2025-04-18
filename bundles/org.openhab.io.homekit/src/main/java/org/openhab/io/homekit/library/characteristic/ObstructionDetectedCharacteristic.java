/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class ObstructionDetectedCharacteristic extends BooleanCharacteristic {

    public ObstructionDetectedCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Obstruction detected");
    }

    public ObstructionDetectedCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000024-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return ObstructionDetectedCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
