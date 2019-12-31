/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

/**
 * @author kgoderis
 *
 */
public class StatusLowBatteryCharacteristic extends EnumCharacteristic {

    public StatusLowBatteryCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "State of an accessoryʼs battery", 1);
    }

    public static String getType() {
        return "00000079-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
