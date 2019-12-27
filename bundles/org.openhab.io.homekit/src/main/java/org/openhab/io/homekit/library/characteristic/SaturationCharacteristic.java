package org.openhab.io.homekit.library.characteristic;

import java.math.BigDecimal;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.characteristic.FloatCharacteristic;

public class SaturationCharacteristic extends FloatCharacteristic {

    public SaturationCharacteristic(HomekitCommunicationManager manager, Service service, long instanceId) {
        super(manager, service, instanceId, true, true, true, "Adjust saturation of a light", 0, 100, 1, "%");
    }

    public static String getType() {
        return "0000002F-0000-1000-8000-0026BB765291";
    }

    @Override
    public String getInstanceType() {
        return getType();
    }

    @Override
    protected Double convert(State state) {
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
    protected State convert(Double value) {

        State state = manager.getValue(getChannelUID());

        if (state instanceof HSBType) {
            return new HSBType(((HSBType) state).getHue(), new PercentType(new BigDecimal(value)),
                    ((HSBType) state).getBrightness());
        } else {
            return new DecimalType(value);
        }
    }

}
