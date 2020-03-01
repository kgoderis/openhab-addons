package org.openhab.io.homekit.api;

import java.util.Collection;
import java.util.List;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface for a Service offered by an Accessory.
 *
 * @author Andy Lintner
 */
public interface Service {

    /**
     * Service Instance Ids are assigned from the same number pool that is unique within each Accessory.
     * For example, if the first Service has an Instance Id of “1”, then no other Service
     * can have an Instance Id of “1” within the parent Accessory. The Required Accessory
     * Information Service must have a service Instance Id of 1. After a firmware update, Services types
     * that remain unchanged must retain their previous Instance Ids, newly added Service must not reuse Instance
     * IDs from Services that were removed in the firmware update.
     *
     * @return the unique identifier.
     */
    long getId();

    Accessory getAccessory();

    /**
     * Linked Services allows Accessories to specify logical relationship between Services. A Service can link to one or
     * more Services. A Service must not link to itself. Service links have context and meaning only to the first level
     * of Services that it links to. For example if Service A links to Service B, and Service B links to Service C, this
     * does not imply any relation between Service A to Service C. If Service A also relates to Service C then Service
     * Aʼs linked services must include both Service B and Service C. Linked services allows applications to display
     * logically grouped Accessory controls in the UI.
     *
     * @return the collection of linked Dervices
     */
    Collection<Service> getLinkedServices();

    void addCharacteristic(Characteristic characteristic);

    Characteristic getCharacteristic(long iid);

    Characteristic getCharacteristic(String characteristicType);

    Characteristic getCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass);

    /**
     * Characteristics are the variables offered for reading, updating, and eventing by the Service
     * over the Homekit Accessory Protocol.
     *
     *
     * @return the list of Characteristics.
     */
    @NonNull
    List<Characteristic> getCharacteristics();

    void removeCharacteristic(@NonNull Class<@NonNull ? extends Characteristic> characteristicClass);

    String getInstanceType();

    boolean isType(String aType);

    /**
     * Accessories may specify the Services that are to be hidden from users by a generic HomeKit application.
     * Accessories may expose several Services that could be used to configure the Accessory or to update firmware on
     * the Accessory, these Services should be marked as hidden. When all Characteristics in a Service are marked hidden
     * then the Service must also be marked as hidden.
     *
     * @return true of the service is to be hidden
     */
    boolean isHidden();

    /**
     *
     * @return true of if the Service is the primary Service
     */
    boolean isPrimary();

    void setPrimary(boolean isPrimary);

    /**
     * Creates the JSON representation of the Service, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON.
     */
    JsonObject toJson();

    JsonObject toReducedJson();

}
