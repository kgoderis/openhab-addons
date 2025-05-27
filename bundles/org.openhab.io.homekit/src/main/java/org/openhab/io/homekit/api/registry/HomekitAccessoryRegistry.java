package org.openhab.io.homekit.api.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;

/**
 * Registry for managing HomeKit accessories in the system.
 *
 * This interface defines the contract for components that need to track and manage HomeKit
 * accessories from different providers. It extends the OpenHAB Registry interface to provide
 * a centralized registry for all HomeKit accessories in the system.
 *
 * The registry provides:
 * - Centralized accessory management
 * - Provider-based accessory discovery
 * - Change notification support
 * - Accessory lifecycle tracking
 *
 * Key implementation details:
 * - Thread-safe registry operations
 * - Provider-based discovery system
 * - Change listener support
 * - UID-based accessory identification
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Registry} for registry functionality
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management
 * - {@link org.openhab.io.homekit.api.provider.HomekitAccessoryProvider} for accessory discovery
 * - {@link org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener} for change notifications
 * - {@link org.openhab.io.homekit.api.uid.HomekitAccessoryUID} for accessory identification
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryRegistry extends Registry<HomekitAccessory, HomekitAccessoryUID> {

    // /**
    // * Returns a list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
    // *
    // * @param serverId the id uniquely identifying the HomekitServer
    // * @return list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
    // */
    // Collection<HomekitAccessory> get(String serverId);
}
