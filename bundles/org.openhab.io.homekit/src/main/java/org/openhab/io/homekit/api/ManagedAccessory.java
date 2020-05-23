package org.openhab.io.homekit.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Base interface for all \. You can implement this interface directly, but most
 * users will prefer to use the more full featured interfaces in {@link
 * io.github.hapjava.accessories} which include a default implementation of {@link #getServices()}.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface ManagedAccessory extends Accessory {

    /**
     * Characteristic Instance IDs are assigned from the same number pool that is unique within each
     * Accessory object. For example, if the first Characteristic object has an Instance ID of “1”, then
     * no other Characteristic object can have an Instance ID of “1” within the parent Accessory object.
     * After a firmware update, Characteristic types that remain unchanged must retain their previous instance
     * IDs, newly added Characteristic must not reuse Instance IDs from Characteristics that were removed
     * in the firmware update.
     *
     * @return the next available unique identifier for a characteristic
     */
    long getInstanceId();

    long getCurrentInstanceId();

    void addServices();

}
