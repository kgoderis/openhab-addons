package org.openhab.io.homekit.library;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.library.service.AbstractHomekitService;

/**
 * Factory for creating HomeKit characteristics.
 *
 * @author Karel Goderis
 */
@NonNullByDefault
public interface HomekitCharacteristicFactory {
    /**
     * Create a new characteristic.
     *
     * @param service The service this characteristic belongs to
     * @param name The name of the characteristic
     * @return The created characteristic
     */
    HomekitCharacteristic createCharacteristic(AbstractHomekitService service, String name);

    /**
     * Create a new required characteristic.
     *
     * @param service The service this characteristic belongs to
     * @param name The name of the characteristic
     * @return The created characteristic
     */
    HomekitRequiredCharacteristic createRequiredCharacteristic(AbstractHomekitService service, String name);
} 