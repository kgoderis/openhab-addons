package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Interface for providing HomeKit pairing information to the system.
 *
 * This interface defines the contract for components that provide HomeKit pairing
 * information to the system. It extends the OpenHAB Provider interface to enable
 * dynamic discovery and management of HomeKit pairing configurations.
 *
 * The interface provides:
 * - Dynamic pairing discovery
 * - Pairing lifecycle management
 * - Pairing state tracking
 * - Pairing configuration management
 *
 * Key implementation details:
 * - Provider-based discovery system
 * - Thread-safe pairing management
 * - Dynamic pairing registration
 * - State synchronization
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Provider} for provider functionality
 * - {@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing management
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitPairingProvider extends Provider<HomekitPairing> {

}
