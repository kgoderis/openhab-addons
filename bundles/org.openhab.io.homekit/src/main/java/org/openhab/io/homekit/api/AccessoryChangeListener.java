package org.openhab.io.homekit.api;

import org.openhab.core.common.registry.RegistryChangeListener;

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
public interface AccessoryChangeListener extends RegistryChangeListener<ManagedAccessory> {

}
