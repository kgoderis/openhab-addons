package org.openhab.io.homekit.internal.handler;

import java.util.Collections;
import java.util.Set;

import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitAccessoryHandler extends AbstractHomekitAccessoryHandler {

    private final Logger logger = LoggerFactory.getLogger(HomekitAccessoryHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(HomekitBindingConstants.THING_TYPE_ACCESSORY);

    public HomekitAccessoryHandler(Thing thing) {
        super(thing);
    }

}
