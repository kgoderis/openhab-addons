package org.openhab.io.homekit.api.listener;

import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;

/**
 * {@link AccessoryChangeListener} can be implemented to listen for Accessories
 * being added or removed. The listener must be added and removed via
 * {@link AccessoryRegistry#addRegistryChangeListener(AccessoryChangeListener)} and
 * {@link AccessoryRegistry#removeRegistryChangeListener(AccessoryChangeListener)}.
 *
 * @author Karel Goderis - Initial contribution
 *
 * @see AccessoryRegistry
 */
public interface AccessoryRegistryChangeListener extends RegistryChangeListener<Accessory> {

}
