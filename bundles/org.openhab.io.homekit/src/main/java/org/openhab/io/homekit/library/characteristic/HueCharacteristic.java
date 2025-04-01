package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class HueCharacteristic extends FloatCharacteristic {

    protected HueCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust hue of the light", 0, 360, 1, "arcdegrees");
    }

    public static String getType() {
        return "00000013-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            DecimalType hue = ((HSBType) state).getHue();
            return hue.doubleValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return null;
            }
            return convertedState.doubleValue();
        }
    }

    @Override
    public State toState(Double value) {
        return new DecimalType(value);
        // return new HSBType(value, 100, 100);

        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        //     return new HSBType(new DecimalType(value), ((HSBType) state).getSaturation(),
        //             ((HSBType) state).getBrightness());
        // } else {
        //     return new DecimalType(value);
        // }
    }
}
