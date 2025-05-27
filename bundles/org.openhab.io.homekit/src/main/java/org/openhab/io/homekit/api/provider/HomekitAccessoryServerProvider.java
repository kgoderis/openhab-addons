package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;

/**
 * Interface for providing HomeKit accessory servers to the system.
 *
 * This interface defines the contract for components that provide HomeKit accessory servers
 * to the system. It extends the OpenHAB Provider interface to enable dynamic discovery
 * and management of HomeKit accessory servers.
 *
 * The interface provides:
 * - Dynamic server discovery
 * - Server lifecycle management
 * - Server state tracking
 * - Server configuration management
 *
 * Key implementation details:
 * - Provider-based discovery system
 * - Thread-safe server management
 * - Dynamic server registration
 * - State synchronization
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Provider} for provider functionality
 * - {@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for server management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitAccessoryServerProvider extends Provider<HomekitAccessoryServer> {

}
