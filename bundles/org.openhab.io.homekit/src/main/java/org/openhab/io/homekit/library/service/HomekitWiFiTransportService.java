package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitCurrentTransportCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitWiFiCapabilitiesCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitWiFiConfigurationControlCharacteristic;

/**
 * HomeKit WiFi Transport Service.
 * This service provides WiFi transport functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000022A-0000-1000-8000-0026BB765291", name = "WiFiTransport", tag = "wifiTransport")
@NonNullByDefault
public class HomekitWiFiTransportService extends AbstractHomekitService {

    /**
     * Creates a new WiFi Transport service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitWiFiTransportService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("WiFi Transport").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new WiFi Transport service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitWiFiTransportService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: WiFiTransportConfiguration
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(new HomekitCurrentTransportCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitWiFiCapabilitiesCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitWiFiConfigurationControlCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }
}
