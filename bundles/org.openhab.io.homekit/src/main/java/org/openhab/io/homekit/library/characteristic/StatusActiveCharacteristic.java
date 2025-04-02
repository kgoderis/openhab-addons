/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class StatusActiveCharacteristic extends BooleanCharacteristic {

    public StatusActiveCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Status active");
    }

    public static String getType() {
        return "00000075-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
