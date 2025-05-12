package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "00000008-0000-1000-8000-0026BB765291", name = "Brightness", tag = "brightness")
@NonNullByDefault
public class HomekitBrightnessCharacteristic extends HomekitFloatCharacteristic {

    public HomekitBrightnessCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, 0.0, 100.0, 1.0, "%");
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Brightness");
    }

    public HomekitBrightnessCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Double value) {
        return value != null && value >= 0.0 && value <= 100.0;
    }

    @Override
    public java.util.Set<Double> getAllowedValues() {
        return java.util.Collections.emptySet();
    }

    @Override
    public Double toValue(State state) {
        if (state instanceof HSBType) {
            PercentType brightness = ((HSBType) state).getBrightness();
            return brightness.doubleValue();
        } else {
            DecimalType convertedState = state.as(DecimalType.class);
            if (convertedState == null) {
                return minValue;
            }
            return convertedState.doubleValue();
        }
    }

    @Override
    public State toState(Double value) {
        return new PercentType(value.intValue());

        // State state = manager.getState(getChannelUID());

        // if (state instanceof HSBType) {
        // return new HSBType(((HSBType) state).getHue(), ((HSBType) state).getSaturation(), new PercentType(value));
        // } else {
        // return new DecimalType(value);
        // }
    }


}
