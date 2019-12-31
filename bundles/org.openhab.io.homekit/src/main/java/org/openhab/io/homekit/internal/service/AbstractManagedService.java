package org.openhab.io.homekit.internal.service;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.ManagedAccessory;
import org.openhab.io.homekit.api.ManagedService;
import org.openhab.io.homekit.hap.accessories.BatteryStatusAccessory;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.openhab.io.homekit.library.characteristic.StatusLowBatteryCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagedService extends GenericService implements ManagedService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractManagedService.class);

    private final HomekitCommunicationManager manager;
    private final boolean isExtensible;

    /**
     * Creates a new instance of this class with the specified UUID and {@link ManagedAccessory}.
     * Download and install <i>HomeKit Accessory Simulator</i> to discover the corresponding UUID for
     * the specific service.
     *
     * <p>
     * The new service will automatically add {@link Name} characteristic. If the accessory is
     * battery operated then it must implement {@link BatteryStatusAccessory} and {@link
     * StatusLowBatteryCharacteristic} will be added too.
     *
     * @param type unique UUID of the service. This information can be obtained from HomeKit Accessory
     *            Simulator.
     * @param accessory HomeKit accessory exposed as a service.
     * @param name name of the service. This information is usually the name of the accessory.
     * @throws Exception
     */
    public AbstractManagedService(@NonNull HomekitCommunicationManager manager, @NonNull ManagedAccessory accessory,
            long instanceId, boolean extend, String name) {
        super(accessory, instanceId, name);
        this.isExtensible = extend;
        this.manager = manager;

        if (isExtensible()) {
            addCharacteristics();
        }

        // If battery operated accessory then add LowBatteryStatusAccessory
        // if (accessory instanceof BatteryStatusAccessory) {
        // addCharacteristic(new StatusLowBatteryCharacteristic(manager, (BatteryService) this));
        // }
    }

    @Override
    public boolean isExtensible() {
        return isExtensible;
    }

    @Override
    public void addCharacteristics() {
        addCharacteristic(
                new NameCharacteristic(getManager(), this, ((ManagedAccessory) getAccessory()).getInstanceId()));
    }

    @Override
    public abstract String getInstanceType();

    protected HomekitCommunicationManager getManager() {
        return manager;
    }

}
