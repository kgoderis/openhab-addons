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
public class OutletInUseCharacteristic extends BooleanCharacteristic {

    public OutletInUseCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Outlet in use");
    }

    public OutletInUseCharacteristic(Service service, JsonValue value) {
        super(service, value);
    }

    public static String getType() {
        return "00000026-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return OutletInUseCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
