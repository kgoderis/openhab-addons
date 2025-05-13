package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitCryptoHashCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTapTypeCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitTokenCharacteristic;

/**
 * HomeKit TapManagement Service.
 * This service represents the TapManagement functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000022E-0000-1000-8000-0026BB765291", name = "TapManagement", tag = "TapManagement")
@NonNullByDefault
public class HomekitTapManagementService extends AbstractHomekitService {
    /**
     * Creates a new TapManagement service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for this service
     * @param characteristicFactory The factory for creating characteristics
     */
    public HomekitTapManagementService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("TapManagement")
            .withExtensible(true)
            .withPrimary(false)
            .withHidden(false);
    }

    /**
     * Adds the required characteristics for this service.
     * These characteristics are defined in the HomeKit Accessory Protocol specification.
     */
    @Override
    public void addCharacteristics() {
        // Add required characteristics based on HAP specification
            addCharacteristic(new HomekitActiveCharacteristic(this, eventManager,
                    getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitCryptoHashCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitTapTypeCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(new HomekitTokenCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
    }
}
