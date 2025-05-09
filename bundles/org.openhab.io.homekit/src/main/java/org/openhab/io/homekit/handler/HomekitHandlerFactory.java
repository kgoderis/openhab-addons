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
package org.openhab.io.homekit.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.type.ThingType;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.network.discovery.HomekitBindingConstants;
import org.openhab.io.homekit.provider.HomekitChannelGroupTypeProvider;
import org.openhab.io.homekit.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.provider.HomekitThingTypeProvider;
import org.osgi.framework.BundleContext;
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

    public static Collection<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.emptySet();

    protected final HomekitAccessoryRegistry accessoryRegistry;
    protected final HomekitPairingRegistry pairingRegistry;
    protected final HomekitAccessoryServerRegistry serverRegistry;
    protected final @NonNullByDefault({}) BundleContext bundleContext;
    protected final HomekitThingTypeProvider homekitThingTypeProvider;
    protected final HomekitChannelTypeProvider homekitChannelTypeProvider;
    protected final HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider;
    protected final HomekitEventManager eventManager;

    @Activate
    public HomekitHandlerFactory(ComponentContext componentContext, @Reference HomekitAccessoryRegistry accessoryRegistry,
            @Reference HomekitPairingRegistry pairingRegistry, @Reference HomekitAccessoryServerRegistry serverRegistry,
            @Reference HomekitThingTypeProvider homekitThingTypeProvider,
            @Reference HomekitChannelTypeProvider homekitChannelTypeProvider,
            @Reference HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider,
            @Reference HomekitEventManager eventManager) {
        super.activate(componentContext);
        this.bundleContext = componentContext.getBundleContext();
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.serverRegistry = serverRegistry;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.homekitChannelGroupTypeProvider = homekitChannelGroupTypeProvider;
        this.eventManager = eventManager;

        SUPPORTED_THING_TYPES = Collections.unmodifiableSet(homekitThingTypeProvider.getThingTypes(null).stream()
                .map(ThingType::getUID).collect(Collectors.toSet()));
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {

        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the Homekit binding");
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (HomekitBindingConstants.THING_TYPE_ACCESSORY.equals(thingTypeUID)) {
            return new HomekitAccessoryThingHandler(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider,
                    homekitThingTypeProvider, eventManager);
        }

        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new HomekitServiceThingHandler(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider,
                    homekitThingTypeProvider, eventManager);
        }

        logger.debug("Unsupported thing {}", thing.getThingTypeUID());
        return null;
    }
}
