package org.openhab.io.homekit.api;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.internal.accessory.AccessoryUID;

/**
 * {@link AccessoryRegistry} tracks all {@link Accessory}s from different {@link AccessoryProvider}s and provides access
 * to them. The {@link AccessoryRegistry} supports adding of listeners (see {@link AccessoryChangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface AccessoryRegistry extends Registry<Accessory, AccessoryUID> {

    /**
     * Returns a list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
     *
     * @param serverId the id uniquely identifying the HomekitServer
     * @return list of HomekitAccessories for a given serverId or an empty list if no HomekitAccessory was found
     */
    Collection<Accessory> get(String serverId);

}
