package org.openhab.io.homekit.api;

import java.util.Collection;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Base interface for all \. You can implement this interface directly, but most
 * users will prefer to use the more full featured interfaces in {@link
 * io.github.hapjava.accessories} which include a default implementation of {@link #getServices()}.
 *
 * @author Andy Lintner
 */
@NonNullByDefault
public interface Accessory {

    /**
     * Accessory Instance IDs are assigned from the same number pool that is global across entire
     * AccessoryServer. For example, if the first Accessory object has an Instance ID of “1”, then no
     * other Accessory object can have an Instance ID of “1” within the AccessoryServer.
     *
     * @return the Accessory Instance ID.
     */
    long getId();

    void addService(Service service);

    @Nullable
    Service getService(String serviceType);

    /**
     * Accessory should list one of its Services as the primary Service. The primary Service
     * must match the primary function of the Accessory and must also match with the accessory category. An
     * Accessory must expose only one primary Service from its list of available Services
     *
     * @return the primary Services.
     */
    @Nullable
    Service getPrimaryService();

    /**
     * The collection of Services this single Accessory supports. Services are the primary way to
     * interact with the Accessory via the Homekit Accessory Protocol. Besides the Services offered here,
     * the accessory will automatically include the Required Accessory Information Services.
     *
     * The Services contained within an Accessory object must be collocated. For example, a fan with a
     * light on it would expose singleAccessory object with three Services: the Required Accessory
     * Information Services, a Fan Services, and a Light Bulb Services. Conversely, a HomekitBridge
     * that bridges two independent lights that may be in different physical locations must expose an Accessory
     * object for each independent light
     *
     * <p>
     * This method will only be useful if you're implementing your own accessory type. For the
     * standard accessories, use the default implementation provided by the interfaces in {@link
     * io.github.hapjava.accessories}.
     *
     * @return the collection of Services.
     */
    Collection<Service> getServices();

    /**
     * Creates the JSON representation of the Accessory, in accordance with the Homekit Accessory
     * Protocol.
     *
     * @return the resulting JSON.
     */
    JsonObject toJson();

    JsonObject toReducedJson();

}
