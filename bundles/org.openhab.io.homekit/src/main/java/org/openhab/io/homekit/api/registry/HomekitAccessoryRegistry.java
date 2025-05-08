package org.openhab.io.homekit.api.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.api.hap.HomekitAccessory;
import org.openhab.io.homekit.api.listener.HomekitAccessoryChangeListener;
import org.openhab.io.homekit.api.provider.HomekitAccessoryProvider;
import org.openhab.io.homekit.internal.accessory.HomekitAccessoryUID;

/**
 * {@link HomekitAccessoryRegistry} tracks all {@link ManagedAccessory}s from different {@link HomekitAccessoryProvider}s and provides
 * access
 * to them. The {@link HomekitAccessoryRegistry} supports adding of listeners (see {@link HomekitAccessoryChangeListener})
 *
 * @author Karel Goderis - Initial contribution
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
