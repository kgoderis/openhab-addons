/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

/**
 * @author kgoderis
 *
 */
public class StatusLowBatteryCharacteristic extends EnumCharacteristic {

    public StatusLowBatteryCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Status low battery", 1);
    }

    public static String getType() {
        return "00000079-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    public static String getTag() {
        return StatusLowBatteryCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
