/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class OutletInUseCharacteristic extends BooleanCharacteristic {

    public OutletInUseCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "Outlet in use");
    }

    public static String getType() {
        return "00000026-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
