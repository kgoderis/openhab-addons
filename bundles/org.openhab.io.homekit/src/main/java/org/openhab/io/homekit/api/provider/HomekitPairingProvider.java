package org.openhab.io.homekit.api.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Interface for providing HomeKit pairing information to the system.
 * <p>
 * This interface defines the contract for components that provide HomeKit pairing
 * information to the system. It extends the OpenHAB Provider interface to enable
 * dynamic discovery and management of HomeKit pairing configurations.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Dynamic pairing discovery</li>
 * <li>Pairing lifecycle management</li>
 * <li>Pairing state tracking</li>
 * <li>Pairing configuration management</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Provider-based discovery system</li>
 * <li>Thread-safe pairing management</li>
 * <li>Dynamic pairing registration</li>
 * <li>State synchronization</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.core.common.registry.Provider} for provider functionality</li>
 * <li>{@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing management</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitPairingProvider extends Provider<HomekitPairing> {

}
