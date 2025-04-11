package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.EnumCharacteristic;

public class ContactSensorStateCharacteristic extends EnumCharacteristic {

    public ContactSensorStateCharacteristic(Service service, long instanceId) {
        super(service, instanceId, false, true, true, "State of a door/window contact sensor", 1);
    }

    public static String getType() {
        return "0000006A-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public Integer toValue(State state) {
        if (state instanceof OpenClosedType) {
            return ((OpenClosedType) state) == OpenClosedType.OPEN ? 0 : 1;
        }
        return null;
    }

    @Override
    public State toState(Integer value) {
        return value == 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
    }

    public static String getTag() {
        return ContactSensorStateCharacteristic.class.getSimpleName().replace("Characteristic", "");
    }
}
