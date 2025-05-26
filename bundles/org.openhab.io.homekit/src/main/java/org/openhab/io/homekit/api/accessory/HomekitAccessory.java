package org.openhab.io.homekit.api.accessory;

import java.util.Collection;
import java.util.Optional;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;

/**
 * Base interface for all HomeKit accessories. This interface defines the core functionality that all HomeKit
 * accessories must implement. While you can implement this interface directly, most users will prefer to use the more
 * full-featured interfaces in the accessories package which include default implementations of common methods.
 *
 * <p>
 * A HomeKit accessory represents a physical or virtual device that can be controlled through the HomeKit protocol.
 * Each accessory:
 * <ul>
 *   <li>Has a unique identifier</li>
 *   <li>Contains basic information (model, manufacturer, etc.)</li>
 *   <li>Can have one or more services that define its functionality</li>
 *   <li>Can be assigned to a HomeKit server</li>
 * </ul>
 * </p>
 *
 * <p>
 * Accessories are the primary way to represent devices in HomeKit. They can be simple (like a light bulb) or complex
 * (like a thermostat with multiple sensors and controls). Each accessory must have at least one service, and the
 * primary service must match the accessory's category.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public interface HomekitAccessory extends Identifiable<HomekitAccessoryUID>, Comparable<HomekitAccessory> {

    /**
     * Gets the unique identifier for this accessory.
     * The UID is used to identify the accessory within the HomeKit ecosystem.
     *
     * @return the unique identifier for this accessory
     * @since 1.0
     */
    @Override
    HomekitAccessoryUID getUID();

    /**
     * Gets the accessory instance ID.
     * 
     * <p>
     * This ID is assigned from a global pool across the entire HomeKit server.
     * For example, if the first accessory has an ID of "1", no other accessory can have ID "1" within the server.
     * The accessory with ID 1 is considered the primary accessory. For bridges, this must be the bridge itself.
     * </p>
     *
     * @return the accessory instance ID
     * @since 1.0
     */
    long getAccessoryId();

    /**
     * Gets the display label for this accessory.
     * This is the name that will be shown to users in the Home app.
     *
     * @return the display label
     * @since 1.0
     */
    String getLabel();

    /**
     * Gets the serial number for this accessory.
     * This should be a unique identifier for the physical device.
     *
     * @return the serial number
     * @since 1.0
     */
    String getSerialNumber();

    /**
     * Gets the model name for this accessory.
     * This should identify the specific model of the device.
     *
     * @return the model name
     * @since 1.0
     */
    String getModel();

    /**
     * Gets the manufacturer name for this accessory.
     * This should identify the company that made the device.
     *
     * @return the manufacturer name
     * @since 1.0
     */
    String getManufacturer();

    /**
     * Checks if this accessory can be extended with additional services.
     * If true, new services can be added to the accessory after creation.
     *
     * @return true if the accessory is extensible, false otherwise
     * @since 1.0
     */
    boolean isExtensible();

    /**
     * Checks if this accessory is assigned to a server.
     * An accessory must be assigned to a server before it can be used.
     *
     * @return true if assigned, false otherwise
     * @since 1.0
     */
    boolean isAssigned();

    /**
     * Adds a service to this accessory.
     * The service must be compatible with the accessory type and not duplicate existing services.
     *
     * @param service the service to add
     * @since 1.0
     */
    void addService(HomekitService service);

    /**
     * Adds the default set of services to this accessory.
     * This typically includes the required accessory information service.
     *
     * @since 1.0
     */
    void addServices();

    /**
     * Removes a service from this accessory.
     *
     * @param service the service to remove
     * @since 1.0
     */
    void removeService(HomekitService service);

    /**
     * Gets all services supported by this accessory.
     * 
     * <p>
     * Services are the primary way to interact with the accessory via the HomeKit protocol.
     * Besides the services returned here, the accessory will automatically include the required
     * accessory information service.
     * </p>
     *
     * <p>
     * The services contained within an accessory must be collocated. For example, a fan with a
     * light would expose a single accessory with three services: the required accessory information
     * service, a fan service, and a light bulb service.
     * </p>
     *
     * @return the collection of services
     * @since 1.0
     */
    Collection<HomekitService> getServices();

    /**
     * Gets a service by its type.
     *
     * @param serviceType the type of service to find
     * @return an Optional containing the service if found, empty otherwise
     * @since 1.0
     */
    Optional<HomekitService> getService(String serviceType);

    /**
     * Gets the primary service for this accessory.
     * 
     * <p>
     * The primary service must match the primary function of the accessory and must also
     * match the accessory category. An accessory must expose only one primary service.
     * </p>
     *
     * @return an Optional containing the primary service if found, empty otherwise
     * @since 1.0
     */
    Optional<HomekitService> getPrimaryService();

    /**
     * Assigns this accessory to a server.
     * This method should be called by the server when adding the accessory.
     *
     * @param server The server to assign to
     * @throws HomekitAccessoryOperationException If there is an error assigning the accessory
     * @since 1.0
     */
    void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException;

    /**
     * Creates the JSON representation of the accessory, in accordance with the HomeKit protocol.
     * This includes all services and their characteristics.
     *
     * @return the JSON representation
     * @since 1.0
     */
    JsonObject toJson();

    /**
     * Creates a reduced JSON representation of the accessory.
     * This typically includes only essential information needed for basic operations.
     *
     * @return the reduced JSON representation
     * @since 1.0
     */
    JsonObject toReducedJson();

    /**
     * Performs an operation that can be used to identify the accessory.
     * This action can be performed without pairing and is typically used
     * to help users identify which physical device corresponds to this accessory.
     *
     * @since 1.0
     */
    void identify();

    /**
     * Gets the next available instance ID for this accessory.
     * Instance IDs are used to uniquely identify services and characteristics
     * within the accessory.
     *
     * @return the next available instance ID
     * @since 1.0
     */
    long getNextAvailableInstanceId();

    // Builder pattern methods
    /**
     * Sets the display label for this accessory.
     *
     * @param label the new label
     * @return this accessory for method chaining
     */
    HomekitAccessory withLabel(String label);

    /**
     * Sets the serial number for this accessory.
     *
     * @param serialNumber the new serial number
     * @return this accessory for method chaining
     */
    HomekitAccessory withSerialNumber(String serialNumber);

    /**
     * Sets the model name for this accessory.
     *
     * @param model the new model name
     * @return this accessory for method chaining
     */
    HomekitAccessory withModel(String model);

    /**
     * Sets the manufacturer name for this accessory.
     *
     * @param manufacturer the new manufacturer name
     * @return this accessory for method chaining
     */
    HomekitAccessory withManufacturer(String manufacturer);

    /**
     * Sets whether this accessory is extensible.
     *
     * @param isExtensible true if the accessory should be extensible, false otherwise
     * @return this accessory for method chaining
     */
    HomekitAccessory withExtensible(boolean isExtensible);

    /**
     * Sets whether this accessory is orphaned (its source item/thing has been removed).
     * Orphaned accessories are kept in the registry to prevent HomeKit controllers from deleting them.
     *
     * @param orphaned true if the accessory is orphaned, false otherwise
     */
    void setOrphaned(boolean orphaned);

    /**
     * Checks if this accessory is orphaned.
     * Orphaned accessories are those whose source items/things have been removed.
     *
     * @return true if the accessory is orphaned, false otherwise
     */
    boolean isOrphaned();
}
