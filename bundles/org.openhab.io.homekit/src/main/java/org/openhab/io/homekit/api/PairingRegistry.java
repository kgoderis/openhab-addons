package org.openhab.io.homekit.api;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.internal.pairing.PairingUID;

/**
 * {@link PairingRegistry} tracks all {@link Pairing}s from different {@link PairingProvider}s and provides access to
 * them. The {@link PairingRegistry} supports adding of listeners (see {@link HomekitPairingChangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface PairingRegistry extends Registry<Pairing, PairingUID> {

    /**
     * Returns a list of HomekitPairing for a given accessory pairing id or an empty list if no HomekitPairing was found
     *
     * @param pairingId the pairing id of the accessory
     * @return list of HomekitPairing for a given accessory pairing id or an empty list if no HomekitPairing was found
     */
    Collection<Pairing> get(byte[] pairingId);
}
