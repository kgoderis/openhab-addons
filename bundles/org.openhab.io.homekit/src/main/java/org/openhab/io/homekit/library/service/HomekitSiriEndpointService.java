package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveIdentifierCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitManuallyDisabledCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitSiriEndpointSessionStatusCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitVersionCharacteristic;

/**
 * HomeKit SiriEndpoint Service.
 * This service represents the SiriEndpoint functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "00000253-0000-1000-8000-0026BB765291", name = "SiriEndpoint", tag = "SiriEndpoint")
@NonNullByDefault
public class HomekitSiriEndpointService extends AbstractHomekitService {
    /**
     * Creates a new SiriEndpoint service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for this service
     * @param characteristicFactory The factory for creating characteristics
     */
    public HomekitSiriEndpointService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("SiriEndpoint").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Adds the required characteristics for this service.
     * These characteristics are defined in the HomeKit Accessory Protocol specification.
     */
    @Override
    public void addCharacteristics() {
        // Add required characteristics based on HAP specification
        addCharacteristic(new HomekitSiriEndpointSessionStatusCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(true));
        addCharacteristic(
                new HomekitVersionCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitActiveIdentifierCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
        addCharacteristic(new HomekitManuallyDisabledCharacteristic(this, eventManager,
                getAccessory().getNextAvailableInstanceId()).withMandatory(false));
    }
}
