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
package org.openhab.io.homekit.internal.handler;

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
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.AccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.internal.client.HomekitBindingConstants;
import org.openhab.io.homekit.internal.provider.HomekitChannelGroupTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitChannelTypeProvider;
import org.openhab.io.homekit.internal.provider.HomekitThingTypeProvider;
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

    protected final AccessoryRegistry accessoryRegistry;
    protected final PairingRegistry pairingRegistry;
    protected final AccessoryServerRegistry serverRegistry;
    protected final @NonNullByDefault({}) BundleContext bundleContext;
    protected final HomekitThingTypeProvider homekitThingTypeProvider;
    protected final HomekitChannelTypeProvider homekitChannelTypeProvider;
    protected final HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider;

    @Activate
    public HomekitHandlerFactory(ComponentContext componentContext, @Reference AccessoryRegistry accessoryRegistry,
            @Reference PairingRegistry pairingRegistry, @Reference AccessoryServerRegistry serverRegistry,
            @Reference HomekitThingTypeProvider homekitThingTypeProvider,
            @Reference HomekitChannelTypeProvider homekitChannelTypeProvider,
            @Reference HomekitChannelGroupTypeProvider homekitChannelGroupTypeProvider) {
        super.activate(componentContext);
        this.bundleContext = componentContext.getBundleContext();
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.serverRegistry = serverRegistry;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.homekitChannelGroupTypeProvider = homekitChannelGroupTypeProvider;

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
            return new AccessoryThingHandler(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider
                    , homekitThingTypeProvider);
        }

        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new ServiceThingHandler(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider,
                    homekitThingTypeProvider);
        }

        logger.debug("Unsupported thing {}", thing.getThingTypeUID());
        return null;
    }
}
