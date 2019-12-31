/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class HoldPositionCharacteristic extends BooleanCharacteristic {

    public HoldPositionCharacteristic(HomekitCommunicationManager manager, ManagedService service, long instanceId) {
        super(manager, service, instanceId, true, false, false, "Stop at the current position");
    }

    public static String getType() {
        return "0000006F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
