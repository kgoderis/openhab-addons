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
public class OutletInUseCharacteristic extends BooleanCharacteristic {

    public OutletInUseCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId) {
        super(manager, service, instanceId, false, true, true, "Plugged-in State");
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(Boolean value) throws Exception {
        throw new Exception("Can not modify a readonly characteristic");
    }

    public static String getType() {
        return "00000026-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

}
