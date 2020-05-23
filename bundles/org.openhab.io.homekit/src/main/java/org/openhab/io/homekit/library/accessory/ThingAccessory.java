package org.openhab.io.homekit.library.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.api.ManagedCharacteristic;
import org.openhab.io.homekit.api.Service;
import org.openhab.io.homekit.internal.accessory.AbstractManagedAccessory;
import org.openhab.io.homekit.library.characteristic.NameCharacteristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingAccessory extends AbstractManagedAccessory {

    protected static final Logger logger = LoggerFactory.getLogger(ThingAccessory.class);

    private ThingUID thingUID;

    public ThingAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId,
            boolean extend) {
        super(manager, server, instanceId, extend);
    }

    @Override
    public @NonNull String getLabel() {
        Thing theThing = getManager().getThing(thingUID);
        if (theThing != null) {
            String label = theThing.getLabel();
            String location = theThing.getLocation();
            if (label != null || location != null) {
                String result = "";
                if (label != null) {
                    result = label;
                } else {
                    result = theThing.getUID().toString();
                }

                if (result != "" && location != null) {
                    result = result + " @ ";
                    result = result + location;
                }
                return result;
            } else {
                return theThing.getUID().toString();
            }
        }
        return getUID().toString();
    }

    public ThingUID getThingUID() {
        return thingUID;
    }

    public void setThingUID(ThingUID thingUID) {
        this.thingUID = thingUID;

        for (Service aService : getServices()) {
            ManagedCharacteristic<?> nc = (ManagedCharacteristic<?>) aService
                    .getCharacteristic(NameCharacteristic.getType());
            if (nc != null) {
                logger.debug("setThingUID - Setting name of {} to {}", nc.getUID(), getLabel());
                ((NameCharacteristic) nc).setReadOnlyValue(getLabel());
            }
        }
    }

}
