package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitMetricsBufferFullStateCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSupportedMetricsCharacteristic;

/**
 * HomeKit Accessory Metrics Service.
 * This service provides metrics and diagnostic information about the accessory.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000270-0000-1000-8000-0026BB765291", name = "AccessoryMetrics", tag = "accessoryMetrics")
@NonNullByDefault
public class HomekitAccessoryMetricsService extends AbstractHomekitService {

    /**
     * Creates a new Accessory Metrics service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitAccessoryMetricsService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Accessory Metrics").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Creates a new Accessory Metrics service from a JSON configuration.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitAccessoryMetricsService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: Active, MetricsBufferFullState, SupportedMetrics
     */
    @Override
    public void addCharacteristics() {
        // Required characteristics
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitMetricsBufferFullStateCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitSupportedMetricsCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
