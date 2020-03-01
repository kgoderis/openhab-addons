package org.openhab.io.homekit.api;

import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.internal.service.ServiceUID;

/**
 * Interface for a Managed Service offered by an Accessory.
 *
 * @author Karel Goderis
 */
public interface ManagedService extends Identifiable<ServiceUID>, Service {

    void addCharacteristics();

    /**
     * Not all Services provide user-visible or user-interactive functionality. Services which provide either
     * user-visible or user-interactive functionality must include the Name characteristic; All other Services must not
     * include this characteristic. This convention is used by iOS clients to determine which Services to display to
     * users.
     *
     * Note that the Accessory Information service is an exception and always includes the Name characteristic even
     * though it is not typically user-visible or user-interactive
     *
     * @return a string representing the name of the service
     */
    String getName();

    boolean isExtensible();

}
