package org.openhab.io.homekit.internal.handler;

import java.util.Collections;
import java.util.Set;

import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.io.homekit.api.Accessory;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitAccessoryHandler extends AbstractHomekitAccessoryHandler {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_ACCESSORY);

    public HomekitAccessoryHandler(Thing thing, BundleContext context) {
        super(thing, context);
    }

    @Override
    public void initialize() {
        HomekitAccessoryBridgeHandler bridgeHandler = getBridge() != null
                ? (HomekitAccessoryBridgeHandler) getBridge().getHandler()
                : null;

        if (bridgeHandler != null) {
            Accessory accessory = bridgeHandler.getAccessory(Long.parseLong(getThing().getUID().getId()));

            if (accessory != null) {
                this.configureThing(accessory);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("{}' : The Homekit Accessory Bridge Handler does not manage an Accessory with Id {}",
                        getThing().getUID(), getThing().getUID().getId());
            }
        } else {
            logger.warn("{}' : There is no Homekit Accessory Bridge Handler", getThing().getUID());
        }
    }

}
