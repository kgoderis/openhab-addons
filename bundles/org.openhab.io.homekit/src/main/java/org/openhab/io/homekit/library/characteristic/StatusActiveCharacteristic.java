/**
 *
 */
package org.openhab.io.homekit.library.characteristic;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.BooleanCharacteristic;

/**
 * @author kgoderis
 *
 */
public class StatusActiveCharacteristic extends BooleanCharacteristic {

    public StatusActiveCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "An accessoryʼs current working status");
    }

    public static String getType() {
        return "00000075-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }
}
