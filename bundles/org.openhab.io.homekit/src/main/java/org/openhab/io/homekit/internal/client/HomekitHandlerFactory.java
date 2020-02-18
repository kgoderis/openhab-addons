/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.internal.client;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.io.homekit.api.PairingRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomekitHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "io.homekit", service = ThingHandlerFactory.class)
public class HomekitHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(HomekitHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(HomekitBindingConstants.THING_TYPE_BRIDGE, HomekitBindingConstants.THING_TYPE_ACCESSORY)
                    .collect(Collectors.toSet()));

    protected final PairingRegistry pairingRegistry;

    @Activate
    public HomekitHandlerFactory(ComponentContext componentContext, @Reference PairingRegistry pairingRegistry) {
        super.activate(componentContext);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (HomekitBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new HomekitBridgeHandler((Bridge) thing);
        }

        if (HomekitBindingConstants.THING_TYPE_ACCESSORY.equals(thingTypeUID)) {
            return new HomekitAccessoryHandler(thing, pairingRegistry);
        }

        logger.debug("Unsupported thing {}.", thing.getThingTypeUID());

        return null;
    }
}
