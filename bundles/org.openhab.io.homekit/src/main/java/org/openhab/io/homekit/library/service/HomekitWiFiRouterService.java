package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitRouterStatusCharacteristic;

/**
 * HomeKit WiFi Router Service.
 * This service provides WiFi router functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000020A-0000-1000-8000-0026BB765291", name = "WiFiRouter", tag = "wifiRouter")
@NonNullByDefault
public class HomekitWiFiRouterService extends AbstractHomekitService {
    /**
     * Creates a new WiFi Router service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitWiFiRouterService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("WiFi Router").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new WiFi Router service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitWiFiRouterService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: RouterStatus (UUID: 0000020E-0000-1000-8000-0026BB765291)
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(
                new HomekitRouterStatusCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
    }
}
