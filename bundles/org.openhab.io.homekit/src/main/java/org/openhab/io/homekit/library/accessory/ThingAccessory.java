package org.openhab.io.homekit.library.accessory;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.io.homekit.HomekitCommunicationManager;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.internal.accessory.AbstractAccessory;

public class ThingAccessory extends AbstractAccessory {

    private ThingUID thingUID;

    public ThingAccessory(HomekitCommunicationManager manager, AccessoryServer server, long instanceId, boolean extend)
            throws Exception {
        super(manager, server, instanceId, extend);
    }

    @Override
    public @NonNull String getLabel() {
        Thing theThing = manager.getThing(thingUID);
        if (theThing != null) {
            String label = theThing.getLabel();
            if (label != null) {
                return label;
            }
        }
        return getUID().getId();
    }

    public ThingUID getThingUID() {
        return thingUID;
    }

    public void setThingUID(ThingUID thingUID) {
        this.thingUID = thingUID;
    }

    @Override
    public String getManufacturer() {
        return "openHAB";
    }

}
