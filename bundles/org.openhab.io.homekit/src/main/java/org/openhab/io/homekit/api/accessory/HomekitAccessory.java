package org.openhab.io.homekit.api.accessory;

import java.util.Collection;
import java.util.Optional;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.accessory.HomekitAccessoryUID;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;

/**
 * Base interface for all \. You can implement this interface directly, but most
 * users will prefer to use the more full featured interfaces in {@link
 * io.github.hapjava.accessories} which include a default implementation of {@link #getServices()}.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface HomekitAccessory extends Identifiable<HomekitAccessoryUID>, Comparable<HomekitAccessory> {

    /**
     * Returns the unique identifier for the accessory.
     *
     * @return the unique identifier for the accessory.
     */
    @Override
    HomekitAccessoryUID getUID();

    /**
     * HomekitAccessory Instance IDs are assigned from the same number pool that is global across entire
     * HomekitAccessoryServer. For example, if the first HomekitAccessory object has an Instance ID of "1", then no
     * other HomekitAccessory object can have an Instance ID of "1" within the HomekitAccessoryServer. The HAP accessory object with
     * an instance ID of 1 is considered the primary HAP accessory object. For bridges, this must be the bridge itself
     * 
     *
     * @return the HomekitAccessory Instance ID.
     */
    long getAccessoryId();

    /**
     * Returns a label to display
     *
     * @return the label.
     */
    String getLabel();

    /**
     * Returns a serial number
     *
     * @return the serial number, or null.
     */
    String getSerialNumber();

    /**
     * Returns a model name
     *
     * @return the model name, or null.
     */
    String getModel();

    /**
     * Returns a manufacturer name
     *
     * @return the manufacturer, or null.
     */
    String getManufacturer();

    // HomekitAccessoryServer getServer();

    boolean isExtensible();

    void addService(HomekitService service);

    void addServices();

    void removeService(HomekitService service);

    /**
     * The collection of Services this single HomekitAccessory supports. Services are the primary way to
     * interact with the HomekitAccessory via the Homekit HomekitAccessory Protocol. Besides the Services offered here,
     * the accessory will automatically include the Required HomekitAccessory Information Services.
     *
     * The Services contained within an HomekitAccessory object must be collocated. For example, a fan with a
     * light on it would expose singleAccessory object with three Services: the Required HomekitAccessory
     * Information Services, a Fan Services, and a Light Bulb Services. Conversely, a HomekitBridge
     * that bridges two independent lights that may be in different physical locations must expose an HomekitAccessory
     * object for each independent light
     *
     * <p>
     * This method will only be useful if you're implementing your own accessory type. For the
     * standard accessories, use the default implementation provided by the interfaces in {@link
     * io.github.hapjava.accessories}.
     *
     * @return the collection of Services.
     */
    Collection<HomekitService> getServices();

    Optional<HomekitService> getService(String serviceType);

    /**
     * HomekitAccessory should list one of its Services as the primary HomekitService. The primary HomekitService
     * must match the primary function of the HomekitAccessory and must also match with the accessory category. An
     * HomekitAccessory must expose only one primary HomekitService from its list of available Services
     *
     * @return the primary Services.
     */
    Optional<HomekitService> getPrimaryService();

    /**
     * Creates the JSON representation of the HomekitAccessory, in accordance with the Homekit HomekitAccessory
     * Protocol.
     *
     * @return the resulting JSON.
     */
    JsonObject toJson();

    JsonObject toReducedJson();

    /**
     * Performs an operation that can be used to identify the accessory. This action can be performed
     * without pairing.
     */
    void identify();

    /**
     * Gets the next available instance ID for this accessory.
     *
     * @return the next available instance ID
     */
    long getNextAvailableInstanceId();

    /**
     * Assigns this accessory to a server.
     *
     * @param server The server to assign to
     * @throws HomekitAccessoryOperationException If there is an error assigning the accessory
     */
    void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException;

    /**
     * Checks if this accessory is assigned to a server.
     *
     * @return true if assigned, false otherwise
     */
    boolean isAssigned();
}
