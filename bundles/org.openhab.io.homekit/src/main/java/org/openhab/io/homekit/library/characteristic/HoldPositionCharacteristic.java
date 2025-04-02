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
public class HoldPositionCharacteristic extends BooleanCharacteristic {

    public HoldPositionCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Hold position");
    }

    public static String getType() {
        return "0000006F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
