package org.openhab.io.homekit.library.service;

import javax.json.JsonValue;

import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitFilterChangeIndicationCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitFilterLifeLevelCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitResetFilterIndicationCharacteristic;

/**
 * Service that represents a filter maintenance in HomeKit.
 * This service provides information about filter status and maintenance.
 */
@HomekitServiceType(type = "000000BA-0000-1000-8000-0026BB765291", name = "FilterMaintenance", tag = "FilterMaintenance")
public class HomekitFilterMaintenanceService extends AbstractHomekitService {

    /**
     * Creates a new HomekitFilterMaintenanceService.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     */
    public HomekitFilterMaintenanceService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
    }

    /**
     * Creates a new HomekitFilterMaintenanceService from a JSON value.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for handling HomeKit events
     * @param characteristicFactory Factory for creating HomeKit characteristics
     * @param value JSON value containing service configuration
     */
    public HomekitFilterMaintenanceService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory, JsonValue value) {
        super(accessory, eventManager, characteristicFactory, value);
    }

    @Override
    public void addCharacteristics() {
        super.addCharacteristics();
        addCharacteristic(new HomekitFilterChangeIndicationCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
        addCharacteristic(new HomekitFilterLifeLevelCharacteristic(this, getAccessory().getNextAvailableInstanceId(),
                eventManager));
        addCharacteristic(new HomekitResetFilterIndicationCharacteristic(this,
                getAccessory().getNextAvailableInstanceId(), eventManager));
    }

    @Override
    public boolean isExtensible() {
        return false;
    }
}
