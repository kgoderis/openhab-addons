package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit Valve Type Characteristic.
 * This characteristic represents the type of valve (e.g., irrigation, shower).
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/ValveType">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "000000D5-0000-1000-8000-0026BB765291", name = "Valve Type", tag = "valveType")
@NonNullByDefault
public class HomekitValveTypeCharacteristic extends HomekitEnumCharacteristic {
    public enum ValveType {
        GENERIC_VALVE(0),
        IRRIGATION(1),
        SHOWER_HEAD(2),
        WATER_FAUCET(3);
        private final int code;
        ValveType(int code) { this.code = code; }
        public int getCode() { return code; }
        public static ValveType fromCode(int code) {
            for (ValveType s : values()) {
                if (s.code == code) return s;
            }
            return GENERIC_VALVE;
        }
    }
    public HomekitValveTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, ValveType.values().length);
        withInstanceId(instanceId)
            .withPairedWrite(false)
            .withPairedRead(true)
            .withEvents(true)
            .withDescription("Valve Type");
    }
    public HomekitValveTypeCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < ValveType.values().length;
    }
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            ValveType.GENERIC_VALVE.getCode(),
            ValveType.IRRIGATION.getCode(),
            ValveType.SHOWER_HEAD.getCode(),
            ValveType.WATER_FAUCET.getCode()
        );
    }
} 