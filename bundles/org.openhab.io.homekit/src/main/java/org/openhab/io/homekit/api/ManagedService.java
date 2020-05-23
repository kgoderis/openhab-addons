package org.openhab.io.homekit.api;

/**
 * Interface for a Managed Service offered by an Accessory.
 *
 * @author Karel Goderis
 */
public interface ManagedService extends Service {

    void addCharacteristics();

}
