/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;
import org.openhab.io.homekit.library.service.BatteryService;

/**
 * @author kgoderis
 *
 */
public class StatusLowBatteryCharacteristic extends BooleanCharacteristic {

    public StatusLowBatteryCharacteristic(HomekitCommunicationManager manager, BatteryService service,
            long instanceId) {
        super(manager, service, instanceId, false, true, true, "Low Battery Status");
    }

    public static String getType() {
        return "00000079-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(Boolean value) throws Exception {
        throw new Exception("Can not modify a readonly characteristic");
    }
}
