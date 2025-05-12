package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

@HomekitCharacteristicType(type = "00000036-0000-1000-8000-0026BB765291", name = "Temperature Display Units", tag = "temperatureDisplayUnits")
@NonNullByDefault
public class HomekitTemperatureDisplayUnitsCharacteristic extends HomekitEnumCharacteristic {

    public enum TemperatureDisplayUnits {
        CELSIUS(0),
        FAHRENHEIT(1);
        private final int code;
        TemperatureDisplayUnits(int code) { this.code = code; }
        public int getCode() { return code; }
        public static TemperatureDisplayUnits fromCode(int code) {
            for (TemperatureDisplayUnits v : values()) {
                if (v.code == code) return v;
            }
            return CELSIUS;
        }
    }

    public HomekitTemperatureDisplayUnitsCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, TemperatureDisplayUnits.values().length);
        withInstanceId(instanceId).withPairedWrite(true).withPairedRead(true).withEvents(true)
            .withDescription("Temperature Display Units");
    }

    public HomekitTemperatureDisplayUnitsCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && (value == TemperatureDisplayUnits.CELSIUS.getCode() || value == TemperatureDisplayUnits.FAHRENHEIT.getCode());
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(TemperatureDisplayUnits.CELSIUS.getCode(), TemperatureDisplayUnits.FAHRENHEIT.getCode());
    }
}
