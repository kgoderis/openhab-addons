package org.openhab.io.homekit.api.listener;

import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;

/**
 * Interface for listening to changes in the HomeKit accessory registry.
 *
 * This interface defines the contract for components that need to be notified of changes
 * in the HomeKit accessory registry, including the addition and removal of accessories.
 *
 * The interface provides:
 * - Accessory addition notifications
 * - Accessory removal notifications
 * - Registry change tracking
 *
 * Key implementation details:
 * - Registry-based event routing
 * - Thread-safe event handling
 * - Component state tracking
 * - Event propagation
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.RegistryChangeListener} for registry change events
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory events
 * - {@link org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry} for registry management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
public interface HomekitAccessoryRegistryChangeListener extends RegistryChangeListener<HomekitAccessory> {

}
