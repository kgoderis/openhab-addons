package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;

/**
 * Interface for providing HomeKit accessories to the system.
 *
 * This interface defines the contract for components that provide HomeKit accessories
 * to the system. It extends the OpenHAB Provider interface to enable dynamic discovery
 * and management of HomeKit accessories.
 *
 * The interface provides:
 * - Dynamic accessory discovery
 * - Accessory lifecycle management
 * - Accessory state tracking
 * - Accessory configuration management
 *
 * Key implementation details:
 * - Provider-based discovery system
 * - Thread-safe accessory management
 * - Dynamic accessory registration
 * - State synchronization
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Provider} for provider functionality
 * - {@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryProvider extends Provider<HomekitAccessory> {

}
