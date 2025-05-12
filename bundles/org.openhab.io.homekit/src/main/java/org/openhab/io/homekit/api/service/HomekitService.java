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
import org.openhab.io.homekit.core.service.HomekitServiceUID;

/**
 * Interface for a HomekitService offered by an HomekitAccessory.
 *
 * @author Andy Lintner
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
     * Not all Services provide user-visible or user-interactive functionality. Services which provide either
     * user-visible or user-interactive functionality must include the Name characteristic; All other Services must not
     * include this characteristic. This convention is used by iOS clients to determine which Services to display to
     * users.
     *
     * Note that the HomekitAccessory Information service is an exception and always includes the Name characteristic
     * even
     * though it is not typically user-visible or user-interactive
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
     *
     * @return the type identifier string
     */
    String getType();

    /**
     * Gets the tag associated with this service.
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
     *
     * @return true if the service is extensible
     */
    boolean isExtensible();

    /**
     * Checks if this service is hidden from user interfaces.
     * Accessories may specify the Services that are to be hidden from users by a generic Homekit application.
     * Accessories may expose several Services that could be used to configure the HomekitAccessory or to update
     * firmware on
     * the HomekitAccessory, these Services should be marked as hidden. When all Characteristics in a HomekitService are
     * marked hidden
     * then the HomekitService must also be marked as hidden.
     *
     * @return true if the service is hidden
     */
    boolean isHidden();

    /**
     * Checks if this service is the primary service.
     * The primary service must match the primary function of the accessory and must also match with
     * the accessory category. An accessory must expose only one primary service from its list of available
     * services.
     *
     * @return true if this is the primary service
     */
    boolean isPrimary();

    /**
     * Gets the services linked to this service.
     * Linked Services allows Accessories to specify logical relationship between Services. A HomekitService can link to
     * one or
     * more Services. A HomekitService must not link to itself. HomekitService links have context and meaning only to
     * the first level
     * of Services that it links to. For example if HomekitService A links to HomekitService B, and HomekitService B
     * links to HomekitService C, this
     * does not imply any relation between HomekitService A to HomekitService C. If HomekitService A also relates to
     * HomekitService C then HomekitService
     * Aʼs linked services must include both HomekitService B and HomekitService C. Linked services allows applications
     * to display
     * logically grouped HomekitAccessory controls in the UI.
     *
     * @return the collection of linked services
     */
    Collection<HomekitService> getLinkedServices();

    // Characteristic management methods
    /**
     * Adds a characteristic to this service.
     *
     * @param characteristic the characteristic to add
     */
    void addCharacteristic(HomekitCharacteristic<?> characteristic);

    /**
     * Adds all required characteristics to this service.
     */
    void addCharacteristics();

    /**
     * Removes a specific characteristic from this service.
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
     *
     * @param characteristicType the type to look up
     * @return an Optional containing the characteristic if found
     */
    Optional<HomekitCharacteristic<?>> getCharacteristic(String characteristicType);

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
