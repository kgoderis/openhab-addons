package org.openhab.io.homekit.library.service;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.service.HomekitServiceType;
import org.openhab.io.homekit.core.service.AbstractHomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.library.characteristic.HomekitActiveCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitIdentifierCharacteristic;
import org.openhab.io.homekit.library.characteristic.HomekitNameCharacteristic;

/**
 * HomeKit Assistant Service.
 * This service represents the Assistant functionality in HomeKit.
 * For more information, see https://developer.apple.com/documentation/HomeKit
 *
 * @author Karel Goderis
 */
@HomekitServiceType(type = "0000026A-0000-1000-8000-0026BB765291", name = "Assistant", tag = "Assistant")
@NonNullByDefault
public class HomekitAssistantService extends AbstractHomekitService {
    /**
     * Creates a new Assistant service.
     *
     * @param accessory The accessory this service belongs to
     * @param eventManager The event manager for this service
     * @param characteristicFactory The factory for creating characteristics
     */
    public HomekitAssistantService(HomekitAccessory accessory, HomekitEventManager eventManager,
            HomekitCharacteristicFactory characteristicFactory) {
        super(accessory, eventManager, characteristicFactory);
        withName("Assistant").withExtensible(true).withPrimary(false).withHidden(false);
    }

    /**
     * Adds the required characteristics for this service.
     * Required: Active, Identifier, Name
     */
    @Override
    public void addCharacteristics() {
        addCharacteristic(
                new HomekitActiveCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(
                new HomekitIdentifierCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                        .withMandatory(true));
        addCharacteristic(new HomekitNameCharacteristic(this, eventManager, getAccessory().getNextAvailableInstanceId())
                .withMandatory(true));
    }
}
