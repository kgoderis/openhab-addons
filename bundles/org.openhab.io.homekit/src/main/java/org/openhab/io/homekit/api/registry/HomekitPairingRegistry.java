package org.openhab.io.homekit.api.registry;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.provider.HomekitPairingProvider;
import org.openhab.io.homekit.api.uid.HomekitPairingUID;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;

/**
 * {@link HomekitPairingRegistry} tracks all {@link HomekitPairing}s from different {@link HomekitPairingProvider}s and
 * provides access to
 * them. The {@link HomekitPairingRegistry} supports adding of listeners (see {@link HomekitPairingChangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface HomekitPairingRegistry extends Registry<HomekitPairing, HomekitPairingUID> {

    /**
     * Returns a list of HomekitPairing for a given accessory pairing id or an empty list if no HomekitPairing was found
     *
     * @param pairingId the pairing id of the accessory
     * @return list of HomekitPairing for a given accessory pairing id or an empty list if no HomekitPairing was found
     */
    Collection<HomekitPairing> get(byte[] pairingId);
}
