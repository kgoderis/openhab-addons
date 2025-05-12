package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit WiFi Satellite Status Characteristic.
 * This characteristic represents the status of a WiFi satellite device.
 *
 * @see <a href="https://developers.homebridge.io/#/characteristic/WiFiSatelliteStatus">HomeKit Documentation</a>
 */
@HomekitCharacteristicType(type = "0000021E-0000-1000-8000-0026BB765291", name = "WiFi Satellite Status", tag = "wifiSatelliteStatus")
@NonNullByDefault
public class HomekitWiFiSatelliteStatusCharacteristic extends HomekitEnumCharacteristic {
    public enum WiFiSatelliteStatus {
        CONNECTED(0),
        DISCONNECTED(1),
        CONNECTING(2),
        DISCONNECTING(3);

        private final int code;

        WiFiSatelliteStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static WiFiSatelliteStatus fromCode(int code) {
            for (WiFiSatelliteStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            return DISCONNECTED;
        }
    }

    public HomekitWiFiSatelliteStatusCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager, WiFiSatelliteStatus.values().length);
        withInstanceId(instanceId)
            .withPairedRead(true)
            .withPairedWrite(false)
            .withEvents(true)
            .withDescription("WiFi Satellite Status");
    }

    public HomekitWiFiSatelliteStatusCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < WiFiSatelliteStatus.values().length;
    }

    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(
            WiFiSatelliteStatus.CONNECTED.getCode(),
            WiFiSatelliteStatus.DISCONNECTED.getCode(),
            WiFiSatelliteStatus.CONNECTING.getCode(),
            WiFiSatelliteStatus.DISCONNECTING.getCode()
        );
    }
} 