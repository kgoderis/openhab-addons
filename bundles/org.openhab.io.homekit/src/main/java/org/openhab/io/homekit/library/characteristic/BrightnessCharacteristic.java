package org.openhab.io.homekit.library.characteristic;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.internal.characteristic.IntegerCharacteristic;

public class BrightnessCharacteristic extends IntegerCharacteristic {

    public BrightnessCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust brightness of a light", 0, 100, "percentage");
    }

    public static String getType() {
        return "00000008-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public Integer toValue(State state) {
        if (state instanceof HSBType) {
            PercentType brightness = ((HSBType) state).getBrightness();
            return brightness.intValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return null;
            }
            return convertedState.intValue();
        }
    }

    @Override
    public State toState(Integer value) {
        return new PercentType(value);

        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        //     return new HSBType(((HSBType) state).getHue(), ((HSBType) state).getSaturation(), new PercentType(value));
        // } else {
        //     return new DecimalType(value);
        // }
    }
}
