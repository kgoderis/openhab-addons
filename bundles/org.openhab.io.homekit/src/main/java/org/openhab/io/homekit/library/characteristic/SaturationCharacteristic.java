package org.openhab.io.homekit.library.characteristic;

import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;
import java.math.BigDecimal;
public class SaturationCharacteristic extends FloatCharacteristic {

    public SaturationCharacteristic(Service service, long instanceId) {
        super(service, instanceId, true, true, true, "Adjust saturation of the light", 0, 100, 1, "percentage");
    }

    public static String getType() {
        return "0000002F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            PercentType saturation = ((HSBType) state).getSaturation();
            return saturation.doubleValue();
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
        return new HSBType(new PercentType(0), new PercentType(new BigDecimal(value)), new PercentType(100));
        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        //     return new HSBType(((HSBType) state).getHue(), new PercentType(new BigDecimal(value)),
        //             ((HSBType) state).getBrightness());
        // } else {
        //     return new DecimalType(value);
        
    }
}
