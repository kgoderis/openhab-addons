package org.openhab.io.homekit.api.hap;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.io.homekit.internal.service.HomekitServiceUID;

/**
 * Interface for a HomekitService offered by an HomekitAccessory.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface HomekitService extends Identifiable<HomekitServiceUID>, Comparable<HomekitService> {

    @Override
    @NonNull
    HomekitServiceUID getUID();

    /**
     * HomekitService Instance Ids are assigned from the same number pool that is unique within each HomekitAccessory.
     * For example, if the first HomekitService has an Instance Id of "1", then no other HomekitService
     * can have an Instance Id of "1" within the parent HomekitAccessory. The Required HomekitAccessory
     * Information HomekitService must have a service Instance Id of 1. After a firmware update, Services types
     * that remain unchanged must retain their previous Instance Ids, newly added HomekitService must not reuse Instance
     * IDs from Services that were removed in the firmware update.
     *
     * @return the unique identifier.
     */
    long getInstanceId();

    /**
     * Not all Services provide user-visible or user-interactive functionality. Services which provide either
     * user-visible or user-interactive functionality must include the Name characteristic; All other Services must not
     * include this characteristic. This convention is used by iOS clients to determine which Services to display to
     * users.
     *
     * Note that the HomekitAccessory Information service is an exception and always includes the Name characteristic even
     * though it is not typically user-visible or user-interactive
     *
     * @return a string representing the name of the service
     */
    String getName();

    HomekitAccessory getAccessory();

    /**
     * Linked Services allows Accessories to specify logical relationship between Services. A HomekitService can link to one or
     * more Services. A HomekitService must not link to itself. HomekitService links have context and meaning only to the first level
     * of Services that it links to. For example if HomekitService A links to HomekitService B, and HomekitService B links to HomekitService C, this
     * does not imply any relation between HomekitService A to HomekitService C. If HomekitService A also relates to HomekitService C then HomekitService
     * Aʼs linked services must include both HomekitService B and HomekitService C. Linked services allows applications to display
     * logically grouped HomekitAccessory controls in the UI.
     *
     * @return the collection of linked Dervices
     */
    Collection<HomekitService> getLinkedServices();

    boolean isExtensible();

    void addCharacteristic(HomekitCharacteristic<?> characteristic);

    void addCharacteristics();

    void removeCharacteristic(HomekitCharacteristic<?> characteristic);

    Optional<HomekitCharacteristic<?>> getCharacteristic(long iid);

    Optional<HomekitCharacteristic<?>> getCharacteristic(String characteristicType);

    Optional<HomekitCharacteristic<?>> getCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass);

    /**
     * Characteristics are the variables offered for reading, updating, and eventing by the HomekitService
     * over the Homekit HomekitAccessory Protocol.
     *
     *
     * @return the list of Characteristics.
     */
    Set<HomekitCharacteristic<?>> getCharacteristics();

    void removeCharacteristic(Class<? extends HomekitCharacteristic<?>> characteristicClass);

    String getInstanceType();

    boolean isType(String aType);

    /**
     * Accessories may specify the Services that are to be hidden from users by a generic HomeKit application.
     * Accessories may expose several Services that could be used to configure the HomekitAccessory or to update firmware on
     * the HomekitAccessory, these Services should be marked as hidden. When all Characteristics in a HomekitService are marked hidden
     * then the HomekitService must also be marked as hidden.
     *
     * @return true of the service is to be hidden
     */
    boolean isHidden();

    /**
     *
     * @return true of if the HomekitService is the primary HomekitService. Accessories should list one of its services as the primary
     *         service. The primary service must match the primary function of the accessory and must also match with
     *         the accessory category. An accessory must expose only one primary service from its list of available
     *         services.
     * 
     */
    boolean isPrimary();

    void setPrimary(boolean isPrimary);

    /**
     * Creates the JSON representation of the HomekitService, in accordance with the Homekit HomekitAccessory
     * Protocol.
     *
     * @return the resulting JSON.
     */
    JsonObject toJson();

    JsonObject toReducedJson();
}
