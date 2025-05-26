package org.openhab.io.homekit.api.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.uid.HomekitServiceUID;

/**
 * Base interface for all HomeKit services. This interface defines the core functionality that all HomeKit services
 * must implement. While you can implement this interface directly, most users will prefer to use the more full-featured
 * interfaces in the services package which include default implementations of common methods.
 *
 * <p>
 * A HomeKit service represents a specific functionality or feature of an accessory. Each service:
 * <ul>
 *   <li>Has a unique type identifier</li>
 *   <li>Contains one or more characteristics</li>
 *   <li>Can be linked to other services</li>
 *   <li>Can be marked as primary, hidden, or extensible</li>
 * </ul>
 * </p>
 *
 * <p>
 * Services are the primary way to interact with accessories via the HomeKit protocol. They define what an accessory
 * can do and how it can be controlled. For example, a light bulb accessory might have a LightBulb service that
 * contains characteristics for power state, brightness, and color.
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public interface HomekitService extends Identifiable<HomekitServiceUID>, Comparable<HomekitService> {

    // Core identification and type methods
    /**
     * Gets the unique identifier for this service.
     *
     * @return the HomekitServiceUID that uniquely identifies this service
     */
    @Override
    @NonNull
    HomekitServiceUID getUID();

    /**
     * Gets the instance ID of this service.
     * HomekitService Instance Ids are assigned from the same number pool that is unique within each HomekitAccessory.
     * For example, if the first HomekitService has an Instance Id of "1", then no other HomekitService
     * can have an Instance Id of "1" within the parent HomekitAccessory. The Required HomekitAccessory
     * Information HomekitService must have a service Instance Id of 1. After a firmware update, Services types
     * that remain unchanged must retain their previous Instance Ids, newly added HomekitService must not reuse Instance
     * IDs from Services that were removed in the firmware update.
     *
     * @return the unique instance identifier
     */
    long getInstanceId();

    /**
     * Gets the name of this service.
     * 
     * <p>
     * Not all Services provide user-visible or user-interactive functionality. Services which provide either
     * user-visible or user-interactive functionality must include the Name characteristic; All other Services must not
     * include this characteristic. This convention is used by iOS clients to determine which Services to display to
     * users.
     * </p>
     *
     * <p>
     * Note that the HomekitAccessory Information service is an exception and always includes the Name characteristic
     * even though it is not typically user-visible or user-interactive.
     * </p>
     *
     * @return the name of the service
     */
    String getName();

    /**
     * Gets the parent accessory that contains this service.
     *
     * @return the parent HomekitAccessory
     */
    HomekitAccessory getAccessory();

    /**
     * Gets the type identifier of this service.
     * The type identifier is a unique string that identifies the kind of service.
     *
     * @return the type identifier string
     */
    String getType();

    /**
     * Gets the tag associated with this service.
     * The tag is a shorter, more user-friendly identifier for the service type.
     *
     * @return the tag string
     */
    String getTag();

    /**
     * Checks if this service is of the specified type.
     *
     * @param aType the type to check against
     * @return true if the service is of the specified type
     */
    boolean isType(String aType);

    // Service property methods
    /**
     * Checks if this service is extensible.
     * An extensible service can have additional characteristics added to it.
     *
     * @return true if the service is extensible
     */
    boolean isExtensible();

    /**
     * Checks if this service is hidden from user interfaces.
     * 
     * <p>
     * Accessories may specify the Services that are to be hidden from users by a generic Homekit application.
     * Accessories may expose several Services that could be used to configure the HomekitAccessory or to update
     * firmware on the HomekitAccessory, these Services should be marked as hidden. When all Characteristics in a
     * HomekitService are marked hidden then the HomekitService must also be marked as hidden.
     * </p>
     *
     * @return true if the service is hidden
     */
    boolean isHidden();

    /**
     * Checks if this service is the primary service.
     * 
     * <p>
     * The primary service must match the primary function of the accessory and must also match with
     * the accessory category. An accessory must expose only one primary service from its list of available
     * services.
     * </p>
     *
     * @return true if this is the primary service
     */
    boolean isPrimary();

    /**
     * Gets the services linked to this service.
     * 
     * <p>
     * Linked Services allows Accessories to specify logical relationship between Services. A HomekitService can link to
     * one or more Services. A HomekitService must not link to itself. HomekitService links have context and meaning
     * only to the first level of Services that it links to.
     * </p>
     *
     * <p>
     * For example:
     * <ul>
     *   <li>If Service A links to Service B, and Service B links to Service C, this does not imply any relation
     *       between Service A and Service C.</li>
     *   <li>If Service A also relates to Service C then Service A's linked services must include both Service B and
     *       Service C.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Linked services allows applications to display logically grouped HomekitAccessory controls in the UI.
     * </p>
     *
     * @return the collection of linked services
     */
    Collection<HomekitService> getLinkedServices();

    // Characteristic management methods
    /**
     * Adds a characteristic to this service.
     * The service must be extensible to add characteristics.
     *
     * @param characteristic the characteristic to add
     */
    void addCharacteristic(HomekitCharacteristic<?> characteristic);

    /**
     * Adds all required characteristics to this service.
     */
    void addCharacteristics();

    /**
     * Removes a characteristic from this service.
     *
     * @param characteristic the characteristic to remove
     */
    void removeCharacteristic(HomekitCharacteristic<?> characteristic);

    /**
     * Removes a characteristic of the specified class from this service.
     *
     * @param characteristicClass the class of characteristic to remove
     */
    void removeCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass);

    /**
     * Gets a characteristic by its instance ID.
     *
     * @param iid the instance ID to look up
     * @return an Optional containing the characteristic if found
     */
    Optional<HomekitCharacteristic<?>> getCharacteristic(long iid);

    /**
     * Gets a characteristic by its type.
     * The type must match exactly.
     *
     * @param type the type of characteristic to find
     * @return the characteristic if found, null otherwise
     */
    Optional<HomekitCharacteristic<?>> getCharacteristic(String type);

    /**
     * Gets a characteristic by its class.
     *
     * @param characteristicClass the class to look up
     * @return an Optional containing the characteristic if found
     */
    Optional<HomekitCharacteristic<?>> getCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass);

    /**
     * Gets all characteristics of this service.
     * Characteristics are the variables offered for reading, updating, and eventing by the HomekitService
     * over the Homekit HomekitAccessory Protocol.
     *
     * @return the set of characteristics
     */
    Set<HomekitCharacteristic<?>> getCharacteristics();

    // JSON conversion methods
    /**
     * Creates the JSON representation of the service, in accordance with the Homekit HomekitAccessory Protocol.
     *
     * @return the resulting JSON object
     */
    JsonObject toJson();

    /**
     * Creates a reduced JSON representation of the service, excluding some optional fields.
     *
     * @return the resulting JSON object
     */
    JsonObject toReducedJson();

    // Builder pattern methods
    /**
     * Sets the instance ID for this service.
     *
     * @param instanceId the instance ID to set
     * @return this instance for method chaining
     */
    HomekitService withInstanceId(long instanceId);

    /**
     * Sets whether this service is hidden.
     *
     * @param isHidden whether the service is hidden
     * @return this instance for method chaining
     */
    HomekitService withHidden(boolean isHidden);

    /**
     * Sets whether this service is primary.
     *
     * @param isPrimary whether the service is primary
     * @return this instance for method chaining
     */
    HomekitService withPrimary(boolean isPrimary);

    /**
     * Sets whether this service is extensible.
     *
     * @param isExtensible whether the service is extensible
     * @return this instance for method chaining
     */
    HomekitService withExtensible(boolean isExtensible);

    /**
     * Sets the name for this service.
     *
     * @param name the name to set
     * @return this instance for method chaining
     */
    HomekitService withName(String name);
}
