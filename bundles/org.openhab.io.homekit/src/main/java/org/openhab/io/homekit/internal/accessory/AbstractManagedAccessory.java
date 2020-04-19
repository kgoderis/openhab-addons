package org.openhab.io.homekit.internal.accessory;

import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.library.service.AccessoryInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedAccessory extends GenericAccessory implements ManagedAccessory {

    private final Logger logger = LoggerFactory.getLogger(AbstractManagedAccessory.class);

    private final HomekitCommunicationManager manager;
    private long instanceIdPool = 1;

    public AbstractManagedAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId,
            boolean extend) {
        super(server, instanceId);
        this.manager = manager;

        if (extend) {
            addServices();
        }
    }

    @Override
    public void addServices() {
        addService(new AccessoryInformationService(getManager(), this, getInstanceId(), true, getLabel()));
    }

    @Override
    public long getInstanceId() {
        return instanceIdPool++;
    }

    @Override
    public long getCurrentInstanceId() {
        return instanceIdPool;
    }

    @Override
    public String getSerialNumber() {
        return getUID().getId();
    }

    @Override
    public String getModel() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void identify() {
    }

    @Override
    public boolean isExtensible() {
        return true;
    }

    public HomekitCommunicationManager getManager() {
        return manager;
    }
}
