package org.openhab.io.homekit.api.registry;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.provider.HomekitPairingProvider;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * Registry for managing HomeKit pairing information in the system.
 *
 * This interface defines the contract for components that need to track and manage HomeKit
 * pairing configurations from different providers. It extends the OpenHAB Registry interface to provide
 * a centralized registry for all HomeKit pairing information in the system.
 *
 * The registry provides:
 * - Centralized pairing management
 * - Provider-based pairing discovery
 * - Change notification support
 * - Pairing lifecycle tracking
 * - Pairing ID lookup
 *
 * Key implementation details:
 * - Thread-safe registry operations
 * - Provider-based discovery system
 * - Change listener support
 * - UID-based pairing identification
 * - Pairing ID validation
 *
 * The interface integrates with:
 * - {@link org.openhab.core.common.registry.Registry} for registry functionality
 * - {@link org.openhab.io.homekit.protocol.pairing.HomekitPairing} for pairing management
 * - {@link org.openhab.io.homekit.api.provider.HomekitPairingProvider} for pairing discovery
 * - {@link org.openhab.io.homekit.api.listener.HomekitPairingChangeListener} for change notifications
 * - {@link org.openhab.io.homekit.api.uid.HomekitPairingUID} for pairing identification
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitPairingRegistry extends Registry<HomekitPairing, HomekitPairingUID> {

    /**
     * Gets all pairing configurations for a specific accessory pairing ID.
     * This method is used to find all pairing configurations associated with a particular
     * accessory, which is useful for managing multiple pairings for the same device.
     *
     * @param pairingId the pairing ID of the accessory
     * @return a collection of pairing configurations, or an empty collection if none are found
     * @since 1.0.0
     */
    Collection<HomekitPairing> get(byte[] pairingId);
}
