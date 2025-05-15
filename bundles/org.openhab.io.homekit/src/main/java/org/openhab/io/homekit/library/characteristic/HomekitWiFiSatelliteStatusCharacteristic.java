package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * HomeKit WiFi Satellite Status Characteristic.
 * <p>
 * This characteristic represents the status of a WiFi satellite device, as defined by the HAP specification.
 * <p>
 * See the HomeKit Accessory Protocol (HAP) specification for details: https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitCharacteristicType(type = "0000021E-0000-1000-8000-0026BB765291", name = "WiFi Satellite Status", tag = "wifiSatelliteStatus", acceptedItemTypes = {
        "Number", "String" })
@NonNullByDefault
public class HomekitWiFiSatelliteStatusCharacteristic extends HomekitEnumCharacteristic {
    /**
     * Enum representing the possible WiFi satellite statuses.
     */
    public enum WiFiSatelliteStatus {
        CONNECTED(0),
        DISCONNECTED(1),
        CONNECTING(2),
        DISCONNECTING(3);

        private final int code;

        WiFiSatelliteStatus(int code) {
            this.code = code;
        }

        /**
         * Returns the integer code for this status.
         * 
         * @return the code
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the WiFiSatelliteStatus enum for a given code.
         * 
         * @param code the code
         * @return the WiFiSatelliteStatus
         */
        public static WiFiSatelliteStatus fromCode(int code) {
            for (WiFiSatelliteStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            return DISCONNECTED;
        }
    }

    public HomekitWiFiSatelliteStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            long instanceId) {
        super(service, eventManager, WiFiSatelliteStatus.values().length);
        withInstanceId(instanceId).withPairedRead(true).withPairedWrite(false).withEvents(true)
                .withDescription("WiFi Satellite Status");
    }

    public HomekitWiFiSatelliteStatusCharacteristic(HomekitService service, HomekitEventManager eventManager,
            JsonValue value) {
        super(service, eventManager, value);
    }

    /**
     * Checks if the given value is an allowed WiFi satellite status.
     * 
     * @param value the value to check
     * @return true if allowed, false otherwise
     */
    @Override
    public boolean isAllowedValue(Integer value) {
        return value != null && value >= 0 && value < WiFiSatelliteStatus.values().length;
    }

    /**
     * Returns the set of allowed WiFi satellite status values.
     * 
     * @return the set of allowed values
     */
    @Override
    public java.util.Set<Integer> getAllowedValues() {
        return java.util.Set.of(WiFiSatelliteStatus.CONNECTED.getCode(), WiFiSatelliteStatus.DISCONNECTED.getCode(),
                WiFiSatelliteStatus.CONNECTING.getCode(), WiFiSatelliteStatus.DISCONNECTING.getCode());
    }
}
