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
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy accessory that bridges a remote accessory to a local server.
 * This class delegates all operations to the remote accessory while maintaining
 * its own AID and UID for the local server.
 */
@NonNullByDefault
@HomekitAccessoryType(name = "Bridged Accessory", type = "1110002-0000-1000-8000-0026BB765291", tag = "bridged")
public class HomekitBridgedAccessory implements HomekitAccessory {
    private static final Logger logger = LoggerFactory.getLogger(HomekitBridgedAccessory.class);
    private static final String LOG_PREFIX = "Homekit HomekitBridgedAccessory: ";

    private final HomekitAccessory remoteAccessory;
    private final HomekitAccessoryServer localServer;
    private @Nullable Long accessoryId;
    private @Nullable HomekitAccessoryUID uid;

    /**
     * Creates a new HomekitBridgedAccessory.
     *
     * @param remoteAccessory The remote accessory to bridge
     * @param localServer The local server this accessory belongs to
     */
    public HomekitBridgedAccessory(HomekitAccessory remoteAccessory, HomekitAccessoryServer localServer) {
        this.remoteAccessory = remoteAccessory;
        this.localServer = localServer;
    }

    @Override
    public HomekitAccessoryUID getUID() {
        HomekitAccessoryUID currentUid = this.uid;
        if (currentUid == null) {
            throw new IllegalStateException("HomekitAccessory UID has not been set");
        }
        return currentUid;
    }

    @Override
    public long getAccessoryId() {
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("HomekitAccessory ID has not been set");
        }
        return aid;
    }

    @Override
    public String getLabel() {
        return remoteAccessory.getLabel();
    }

    @Override
    public String getManufacturer() {
        return remoteAccessory.getManufacturer();
    }

    @Override
    public String getModel() {
        return remoteAccessory.getModel();
    }

    @Override
    public String getSerialNumber() {
        return remoteAccessory.getSerialNumber();
    }

    @Override
    public Collection<HomekitService> getServices() {
        return remoteAccessory.getServices();
    }

    @Override
    public Optional<HomekitService> getService(String serviceType) {
        return remoteAccessory.getService(serviceType);
    }

    @Override
    public void addService(HomekitService service) {
        // Services are managed by the remote accessory
        throw new UnsupportedOperationException("Cannot add services to a bridged accessory");
    }

    @Override
    public void removeService(HomekitService service) {
        // Services are managed by the remote accessory
        throw new UnsupportedOperationException("Cannot remove services from a bridged accessory");
    }

    @Override
    public boolean isExtensible() {
        return false; // Bridged accessories are not extensible
    }

    @Override
    public JsonObject toJson() {
        return remoteAccessory.toJson();
    }

    @Override
    public JsonObject toReducedJson() {
        return remoteAccessory.toReducedJson();
    }

    @Override
    public void identify() {
        remoteAccessory.identify();
    }

    @Override
    public void addServices() {
        // Services are managed by the remote accessory
        throw new UnsupportedOperationException("Cannot add services to a bridged accessory");
    }

    @Override
    public Optional<HomekitService> getPrimaryService() {
        return remoteAccessory.getPrimaryService();
    }

    @Override
    public long getNextAvailableInstanceId() {
        return remoteAccessory.getNextAvailableInstanceId();
    }

    @Override
    public int compareTo(HomekitAccessory other) {
        if (other instanceof HomekitBridgedAccessory) {
            return remoteAccessory.compareTo(((HomekitBridgedAccessory) other).remoteAccessory);
        }
        return remoteAccessory.compareTo(other);
    }

    @Override
    public void assignToServer(HomekitAccessoryServer server) throws HomekitAccessoryOperationException {
        if (server != localServer) {
            throw new HomekitAccessoryOperationException(
                    "HomekitBridgedAccessory can only be assigned to its local server");
        }
        if (accessoryId != null) {
            throw new HomekitAccessoryOperationException("HomekitBridgedAccessory is already assigned to a server");
        }
        this.accessoryId = server.getNextAvailableAccessoryId();
        this.uid = new HomekitAccessoryUID(server.getUID().getPairingId(), accessoryId);
        logger.debug("{}Assigned bridged accessory to local server with AID: {}", LOG_PREFIX, accessoryId);
    }

    @Override
    public boolean isAssigned() {
        return accessoryId != null;
    }

    /**
     * Gets the remote accessory that this bridged accessory represents.
     *
     * @return The remote accessory
     */
    public HomekitAccessory getRemoteAccessory() {
        return remoteAccessory;
    }

    /**
     * Gets the local server this bridged accessory belongs to.
     *
     * @return The local server
     */
    public HomekitAccessoryServer getLocalServer() {
        return localServer;
    }

    @Override
    public HomekitAccessory withLabel(String label) {
        return remoteAccessory.withLabel(label);
    }

    @Override
    public HomekitAccessory withSerialNumber(String serialNumber) {
        return remoteAccessory.withSerialNumber(serialNumber);
    }

    @Override
    public HomekitAccessory withModel(String model) {
        return remoteAccessory.withModel(model);
    }

    @Override
    public HomekitAccessory withManufacturer(String manufacturer) {
        return remoteAccessory.withManufacturer(manufacturer);
    }

    @Override
    public HomekitAccessory withExtensible(boolean isExtensible) {
        return remoteAccessory.withExtensible(isExtensible);
    }
}
