package org.openhab.io.homekit.internal.accessory;

import java.util.Collection;
import java.util.Optional;

import javax.json.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy accessory that bridges a remote accessory to a local server.
 * This class delegates all operations to the remote accessory while maintaining
 * its own AID and UID for the local server.
 */
@NonNullByDefault
public class BridgedAccessory implements Accessory {
    private static final Logger logger = LoggerFactory.getLogger(BridgedAccessory.class);
    private static final String LOG_PREFIX = "HomeKit BridgedAccessory: ";

    private final Accessory remoteAccessory;
    private final AccessoryServer localServer;
    private @Nullable Long accessoryId;
    private @Nullable AccessoryUID uid;

    /**
     * Creates a new BridgedAccessory.
     *
     * @param remoteAccessory The remote accessory to bridge
     * @param localServer The local server this accessory belongs to
     */
    public BridgedAccessory(Accessory remoteAccessory, AccessoryServer localServer) {
        this.remoteAccessory = remoteAccessory;
        this.localServer = localServer;
    }

    @Override
    public AccessoryUID getUID() {
        AccessoryUID currentUid = this.uid;
        if (currentUid == null) {
            throw new IllegalStateException("Accessory UID has not been set");
        }
        return currentUid;
    }

    @Override
    public long getAccessoryId() {
        Long aid = this.accessoryId;
        if (aid == null) {
            throw new IllegalStateException("Accessory ID has not been set");
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
    public Collection<Service> getServices() {
        return remoteAccessory.getServices();
    }

    @Override
    public Optional<Service> getService(String serviceType) {
        return remoteAccessory.getService(serviceType);
    }

    @Override
    public void addService(Service service) {
        // Services are managed by the remote accessory
        throw new UnsupportedOperationException("Cannot add services to a bridged accessory");
    }

    @Override
    public void removeService(Service service) {
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
    public Optional<Service> getPrimaryService() {
        return remoteAccessory.getPrimaryService();
    }

    @Override
    public long getNextAvailableInstanceId() {
        return remoteAccessory.getNextAvailableInstanceId();
    }

    @Override
    public int compareTo(Accessory other) {
        if (other instanceof BridgedAccessory) {
            return remoteAccessory.compareTo(((BridgedAccessory) other).remoteAccessory);
        }
        return remoteAccessory.compareTo(other);
    }

    @Override
    public void assignToServer(AccessoryServer server) throws HomekitAccessoryOperationException {
        if (server != localServer) {
            throw new HomekitAccessoryOperationException("BridgedAccessory can only be assigned to its local server");
        }
        if (accessoryId != null) {
            throw new HomekitAccessoryOperationException("BridgedAccessory is already assigned to a server");
        }
        this.accessoryId = server.getNextAvailableAccessoryId();
        this.uid = new AccessoryUID(server.getUID().getPairingId(), accessoryId);
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
    public Accessory getRemoteAccessory() {
        return remoteAccessory;
    }

    /**
     * Gets the local server this bridged accessory belongs to.
     *
     * @return The local server
     */
    public AccessoryServer getLocalServer() {
        return localServer;
    }
} 