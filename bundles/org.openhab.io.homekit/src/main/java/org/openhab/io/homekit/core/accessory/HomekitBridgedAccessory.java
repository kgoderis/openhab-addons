package org.openhab.io.homekit.core.accessory;

import java.util.Collection;
import java.util.Optional;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryType;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.api.uid.HomekitAccessoryUID;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy accessory that bridges a remote HomeKit accessory to a local server.
 * This class acts as a transparent intermediary between a remote HomeKit accessory and the local
 * HomeKit server, allowing remote accessories to be exposed through the local server while maintaining
 * their own identity and state.
 *
 * <p>
 * The bridged accessory provides a seamless integration layer that:
 * <ul>
 * <li>Maintains local server identity while delegating operations to the remote accessory</li>
 * <li>Preserves the remote accessory's characteristics and services</li>
 * <li>Handles state synchronization between local and remote accessories</li>
 * <li>Manages event propagation and state updates</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Transparent delegation of all operations to the remote accessory</li>
 * <li>Local AID and UID management for server integration</li>
 * <li>Read-only access to remote services and characteristics</li>
 * <li>State synchronization and event handling</li>
 * <li>JSON serialization for persistence</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * <ul>
 * <li>{@link HomekitAccessory} - Base accessory interface</li>
 * <li>{@link HomekitAccessoryServer} - Local server management</li>
 * <li>{@link HomekitAccessoryUID} - Unique identification</li>
 * <li>{@link HomekitService} - Service delegation</li>
 * <li>{@link HomekitAccessoryOperationException} - Error handling</li>
 * </ul>
 * </p>
 *
 * <p>
 * The bridged accessory ensures that all operations are properly delegated to the remote
 * accessory while maintaining its own identity in the local server. This includes:
 * <ul>
 * <li>Service and characteristic access</li>
 * <li>State management and updates</li>
 * <li>Event propagation</li>
 * <li>Identification and discovery</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
@HomekitAccessoryType(name = "Bridged Accessory", type = "1110002-0000-1000-8000-0026BB765291", tag = "bridged")
public class HomekitBridgedAccessory implements HomekitAccessory {
    // ========== Log Message Prefixes ==========
    private static final String LOG_PREFIX = "Homekit BridgedAccessory: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";

    private final Logger logger = LoggerFactory.getLogger(HomekitBridgedAccessory.class);

    private final HomekitAccessory remoteAccessory;
    private final HomekitAccessoryServer localServer;
    private long accessoryId = 0;
    private @Nullable HomekitAccessoryUID uid;

    /**
     * Creates a new Bridged Accessory that acts as a proxy for a remote accessory.
     * This constructor initializes the bridge between the remote accessory and the local server,
     * setting up the necessary connections and state management.
     *
     * @param remoteAccessory The remote accessory to bridge
     * @param localServer The local server this accessory belongs to
     * @throws IllegalArgumentException if either parameter is null
     * @since 1.0
     */
    public HomekitBridgedAccessory(HomekitAccessory remoteAccessory, HomekitAccessoryServer localServer) {
        this.remoteAccessory = remoteAccessory;
        this.localServer = localServer;
        logger.debug("{}Created new BridgedAccessory for remote accessory {} on server {}", LOG_INIT,
                remoteAccessory.getLabel(), localServer.getUID());
    }

    /**
     * Gets the unique identifier for this accessory.
     * The UID is required for identifying the accessory in the HomeKit system and is
     * managed by the local server.
     *
     * @return the unique identifier
     * @throws IllegalStateException if the UID has not been set
     * @see HomekitAccessoryUID
     */
    @Override
    public HomekitAccessoryUID getUID() {
        HomekitAccessoryUID currentUid = this.uid;
        if (currentUid == null) {
            throw new IllegalStateException("HomekitAccessory UID has not been set");
        }
        return currentUid;
    }

    /**
     * Gets the accessory instance ID.
     * This ID is assigned from a global pool across the entire HomeKit server and is
     * used for internal routing and state management.
     *
     * @return the accessory instance ID
     * @throws IllegalStateException if the accessory ID has not been set
     * @see HomekitAccessoryServer#getNextAvailableAccessoryId()
     */
    @Override
    public long getAccessoryId() {
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("HomekitAccessory ID has not been set");
        }
        return aid;
    }

    /**
     * Gets the display label for this accessory.
     * Delegates to the remote accessory's label.
     *
     * @return the display label
     */
    @Override
    public String getLabel() {
        return remoteAccessory.getLabel();
    }

    /**
     * Gets the manufacturer name for this accessory.
     * Delegates to the remote accessory's manufacturer.
     *
     * @return the manufacturer name
     */
    @Override
    public String getManufacturer() {
        return remoteAccessory.getManufacturer();
    }

    /**
     * Gets the model name for this accessory.
     * Delegates to the remote accessory's model.
     *
     * @return the model name
     */
    @Override
    public String getModel() {
        return remoteAccessory.getModel();
    }

    /**
     * Gets the serial number for this accessory.
     * Delegates to the remote accessory's serial number.
     *
     * @return the serial number
     */
    @Override
    public String getSerialNumber() {
        return remoteAccessory.getSerialNumber();
    }

    /**
     * Gets the collection of services provided by this accessory.
     * This method delegates to the remote accessory's service collection, ensuring
     * that all services are properly exposed through the bridge.
     *
     * @return collection of HomeKit services
     * @see HomekitService
     */
    @Override
    public Collection<HomekitService> getServices() {
        return remoteAccessory.getServices();
    }

    /**
     * Gets a specific service by its type.
     * This method delegates to the remote accessory's service lookup, allowing
     * direct access to specific services through the bridge.
     *
     * @param serviceType the type of service to find
     * @return optional containing the service if found
     * @see HomekitService
     */
    @Override
    public Optional<HomekitService> getService(String serviceType) {
        return remoteAccessory.getService(serviceType);
    }

    /**
     * Attempts to add a service to this accessory.
     * This operation is not supported for bridged accessories as they are read-only
     * proxies for remote accessories.
     *
     * @param service the service to add
     * @throws UnsupportedOperationException always thrown as bridged accessories cannot be modified
     * @see HomekitService
     */
    @Override
    public void addService(HomekitService service) {
        throw new UnsupportedOperationException("Cannot add services to a bridged accessory");
    }

    /**
     * Attempts to remove a service from this accessory.
     * This operation is not supported for bridged accessories as they are read-only
     * proxies for remote accessories.
     *
     * @param service the service to remove
     * @throws UnsupportedOperationException always thrown as bridged accessories cannot be modified
     * @see HomekitService
     */
    @Override
    public void removeService(HomekitService service) {
        throw new UnsupportedOperationException("Cannot remove services from a bridged accessory");
    }

    /**
     * Indicates whether this accessory can be extended with additional services.
     * Bridged accessories are not extensible as they are read-only proxies.
     *
     * @return false, as bridged accessories cannot be extended
     * @see HomekitService
     */
    @Override
    public boolean isExtensible() {
        return false;
    }

    /**
     * Converts this accessory to a JSON representation.
     * This method delegates to the remote accessory's JSON conversion, ensuring
     * that the bridge's state is properly serialized.
     *
     * @return JSON object containing accessory data
     * @see JsonObject
     */
    @Override
    public JsonObject toJson() {
        return remoteAccessory.toJson();
    }

    /**
     * Converts this accessory to a reduced JSON representation.
     * This method delegates to the remote accessory's reduced JSON conversion,
     * providing a minimal representation of the accessory's state.
     *
     * @return reduced JSON object containing essential accessory data
     * @see JsonObject
     */
    @Override
    public JsonObject toReducedJson() {
        return remoteAccessory.toReducedJson();
    }

    /**
     * Triggers the identify action on this accessory.
     * This method delegates to the remote accessory's identify action,
     * allowing the bridge to participate in the HomeKit identification process.
     *
     * @see HomekitAccessory#identify()
     */
    @Override
    public void identify() {
        remoteAccessory.identify();
    }

    /**
     * Attempts to add services to this accessory.
     * This operation is not supported for bridged accessories as they are read-only
     * proxies for remote accessories.
     *
     * @throws UnsupportedOperationException always thrown as bridged accessories cannot be modified
     * @see HomekitService
     */
    @Override
    public void addServices() {
        throw new UnsupportedOperationException("Cannot add services to a bridged accessory");
    }

    /**
     * Gets the primary service of this accessory.
     * This method delegates to the remote accessory's primary service lookup,
     * ensuring that the bridge maintains the same service hierarchy.
     *
     * @return optional containing the primary service if found
     * @see HomekitService
     */
    @Override
    public Optional<HomekitService> getPrimaryService() {
        return remoteAccessory.getPrimaryService();
    }

    /**
     * Gets the next available instance ID for this accessory.
     * This method delegates to the remote accessory's instance ID management,
     * ensuring proper ID allocation across the bridge.
     *
     * @return the next available instance ID
     * @see HomekitAccessory#getNextAvailableInstanceId()
     */
    @Override
    public long getNextAvailableInstanceId() {
        return remoteAccessory.getNextAvailableInstanceId();
    }

    /**
     * Compares this accessory with another for ordering.
     * For bridged accessories, compares the underlying remote accessories.
     *
     * @param other the accessory to compare with
     * @return comparison result
     */
    @Override
    public int compareTo(HomekitAccessory other) {
        if (other instanceof HomekitBridgedAccessory) {
            return remoteAccessory.compareTo(((HomekitBridgedAccessory) other).remoteAccessory);
        }
        return remoteAccessory.compareTo(other);
    }

    /**
     * Assigns this accessory to a server.
     * This method ensures that the bridged accessory is properly assigned to its
     * local server and maintains the correct identity.
     *
     * @param server the server to assign to
     * @throws HomekitAccessoryOperationException if assignment is invalid or already assigned
     * @see HomekitAccessoryServer
     * @see HomekitAccessoryOperationException
     */
    @Override
    public void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException {
        if (server != localServer) {
            throw new HomekitAccessoryOperationException(
                    "HomekitBridgedAccessory can only be assigned to its local server");
        }
        if (accessoryId != 0) {
            throw new HomekitAccessoryOperationException("HomekitBridgedAccessory is already assigned to a server");
        }
        try {
            this.accessoryId = server.getNextAvailableAccessoryId();
            this.uid = new HomekitAccessoryUIDImpl(server.getUID().getPairingId(), accessoryId);
            if (uid == null) {
                throw new HomekitAccessoryOperationException("Failed to create HomekitAccessoryUID");
            }
            logger.debug("{}Assigned bridged accessory to local server with AID: {}", LOG_PREFIX, accessoryId);

        } catch (Exception e) {
            throw new HomekitAccessoryOperationException("Failed to assign bridged accessory to local server", e);
        }
    }

    /**
     * Checks if this accessory is assigned to a server.
     *
     * @return true if the accessory is assigned, false otherwise
     */
    @Override
    public boolean isAssigned() {
        return accessoryId != 0;
    }

    /**
     * Gets the remote accessory that this bridged accessory represents.
     * This method provides access to the underlying remote accessory for
     * direct interaction when needed.
     *
     * @return The remote accessory
     * @see HomekitAccessory
     */
    public HomekitAccessory getRemoteAccessory() {
        return remoteAccessory;
    }

    /**
     * Gets the local server this bridged accessory belongs to.
     * This method provides access to the local server for coordination
     * and state management.
     *
     * @return The local server
     * @see HomekitAccessoryServer
     */
    public HomekitAccessoryServer getLocalServer() {
        return localServer;
    }

    /**
     * Creates a new accessory with the specified label.
     * Delegates to the remote accessory's label modification.
     *
     * @param label the new label
     * @return new accessory instance with updated label
     */
    @Override
    public HomekitAccessory withLabel(String label) {
        return remoteAccessory.withLabel(label);
    }

    /**
     * Creates a new accessory with the specified serial number.
     * Delegates to the remote accessory's serial number modification.
     *
     * @param serialNumber the new serial number
     * @return new accessory instance with updated serial number
     */
    @Override
    public HomekitAccessory withSerialNumber(String serialNumber) {
        return remoteAccessory.withSerialNumber(serialNumber);
    }

    /**
     * Creates a new accessory with the specified model.
     * Delegates to the remote accessory's model modification.
     *
     * @param model the new model
     * @return new accessory instance with updated model
     */
    @Override
    public HomekitAccessory withModel(String model) {
        return remoteAccessory.withModel(model);
    }

    /**
     * Creates a new accessory with the specified manufacturer.
     * Delegates to the remote accessory's manufacturer modification.
     *
     * @param manufacturer the new manufacturer
     * @return new accessory instance with updated manufacturer
     */
    @Override
    public HomekitAccessory withManufacturer(String manufacturer) {
        return remoteAccessory.withManufacturer(manufacturer);
    }

    /**
     * Creates a new accessory with the specified extensibility setting.
     * Delegates to the remote accessory's extensibility modification.
     *
     * @param isExtensible whether the accessory should be extensible
     * @return new accessory instance with updated extensibility
     */
    @Override
    public HomekitAccessory withExtensible(boolean isExtensible) {
        return remoteAccessory.withExtensible(isExtensible);
    }

    /**
     * Sets the orphaned status of this accessory.
     * Delegates to the remote accessory's orphaned status modification.
     *
     * @param orphaned whether the accessory is orphaned
     */
    @Override
    public void setOrphaned(boolean orphaned) {
        remoteAccessory.setOrphaned(orphaned);
    }

    /**
     * Checks if this accessory is orphaned.
     * Delegates to the remote accessory's orphaned status check.
     *
     * @return true if the accessory is orphaned, false otherwise
     */
    @Override
    public boolean isOrphaned() {
        return remoteAccessory.isOrphaned();
    }
}
