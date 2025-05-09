package org.openhab.io.homekit.api.listener;

import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;

/**
 * {@link HomekitAccessoryChangeListener} can be implemented to listen for Accessories
 * being added or removed. The listener must be added and removed via
 * {@link HomekitAccessoryRegistry#addRegistryChangeListener(HomekitAccessoryChangeListener)} and
 * {@link HomekitAccessoryRegistry#removeRegistryChangeListener(HomekitAccessoryChangeListener)}.
 *
 * @author Karel Goderis - Initial contribution
 *
 * @see HomekitAccessoryRegistry
 */
public interface HomekitAccessoryRegistryChangeListener extends RegistryChangeListener<HomekitAccessory> {

}
