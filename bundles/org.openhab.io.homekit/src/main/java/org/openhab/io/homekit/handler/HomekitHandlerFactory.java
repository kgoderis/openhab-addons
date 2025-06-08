/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.factory.HomekitCharacteristicFactory;
import org.openhab.io.homekit.api.factory.HomekitServiceFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitAccessoryServerRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
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
 * Factory for creating HomeKit thing handlers and managing their lifecycle.
 *
 * This class is responsible for creating and managing HomeKit thing handlers, which are the core components
 * that bridge OpenHAB items with HomeKit accessories. It handles the creation of both accessory and service
 * handlers, manages their registration with the HomeKit server, and ensures proper initialization of all
 * required components.
 *
 * The factory integrates with:
 * - {@link HomekitAccessoryRegistry} for accessory registration and management
 * - {@link HomekitPairingRegistry} for handling device pairing
 * - {@link HomekitAccessoryServerRegistry} for server instance management
 * - {@link HomekitThingTypeProvider} for thing type definitions
 * - {@link HomekitChannelTypeProvider} for channel type definitions
 * - {@link HomekitEventManager} for event handling
 * - {@link HomekitServiceFactory} for service creation
 * - {@link HomekitCharacteristicFactory} for characteristic creation
 * - {@link HomekitChannelGroupTypeProvider} for channel group definitions
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
@Component(configurationPid = "io.homekit", service = ThingHandlerFactory.class)
public class HomekitHandlerFactory extends BaseThingHandlerFactory {

    /** Logger instance for this class */
    private final Logger logger = LoggerFactory.getLogger(HomekitHandlerFactory.class);

    /** Log message prefixes */
    protected static final String LOG_PREFIX = "HomeKit Handler Factory: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State Change - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /** Collection of supported thing types */
    public static Collection<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.emptySet();

    protected final HomekitAccessoryRegistry accessoryRegistry;
    protected final HomekitPairingRegistry pairingRegistry;
    protected final HomekitAccessoryServerRegistry serverRegistry;
    protected final BundleContext bundleContext;
    protected final HomekitThingTypeProvider homekitThingTypeProvider;
    protected final HomekitChannelTypeProvider homekitChannelTypeProvider;
    protected final HomekitEventManager eventManager;
    protected final HomekitServiceFactory serviceFactory;
    protected final HomekitCharacteristicFactory characteristicFactory;
    protected final HomekitChannelGroupTypeProvider channelGroupTypeProvider;

    @Activate
    public HomekitHandlerFactory(ComponentContext componentContext,
            @Reference HomekitAccessoryRegistry accessoryRegistry, @Reference HomekitPairingRegistry pairingRegistry,
            @Reference HomekitAccessoryServerRegistry serverRegistry,
            @Reference HomekitThingTypeProvider homekitThingTypeProvider,
            @Reference HomekitChannelTypeProvider homekitChannelTypeProvider,
            @Reference HomekitEventManager eventManager, @Reference HomekitServiceFactory serviceFactory,
            @Reference HomekitCharacteristicFactory characteristicFactory,
            @Reference HomekitChannelGroupTypeProvider channelGroupTypeProvider) {
        super.activate(componentContext);
        this.bundleContext = componentContext.getBundleContext();
        this.accessoryRegistry = accessoryRegistry;
        this.pairingRegistry = pairingRegistry;
        this.serverRegistry = serverRegistry;
        this.homekitThingTypeProvider = homekitThingTypeProvider;
        this.homekitChannelTypeProvider = homekitChannelTypeProvider;
        this.eventManager = eventManager;
        this.serviceFactory = serviceFactory;
        this.characteristicFactory = characteristicFactory;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
        SUPPORTED_THING_TYPES = Collections.unmodifiableSet(homekitThingTypeProvider.getThingTypes(null).stream()
                .map(ThingType::getUID).collect(Collectors.toSet()));
    }

    /**
     * Creates a handler for a thing.
     * 
     * This method creates an appropriate handler for the given thing based on its type.
     * It supports both accessory and service thing types.
     *
     * @param thing The thing to create a handler for
     * @return A handler for the thing, or null if the thing type is not supported
     */
    @Override
    public @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (HomekitBindingConstants.THING_TYPE_ACCESSORY.equals(thingTypeUID)) {
            logger.debug("{}Creating accessory handler for thing {}", LOG_INIT, thing.getUID());
            return new HomekitAccessoryThingHandler(thing, serverRegistry, accessoryRegistry,
                    homekitChannelTypeProvider, channelGroupTypeProvider, homekitThingTypeProvider, eventManager,
                    serviceFactory, characteristicFactory);
        } else if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            logger.debug("{}Creating service handler for thing {}", LOG_INIT, thing.getUID());
            return new HomekitServiceThingHandler(thing, serverRegistry, accessoryRegistry, homekitChannelTypeProvider,
                    homekitThingTypeProvider, eventManager, serviceFactory, characteristicFactory);
        }
        logger.debug("{}Unsupported thing type {}", LOG_WARN, thing.getThingTypeUID());
        return null;
    }

    /**
     * Checks if a thing type is supported.
     * 
     * This method determines if the factory can create a handler for the given thing type.
     * It supports both accessory and service thing types.
     *
     * @param thingTypeUID The thing type to check
     * @return true if the thing type is supported, false otherwise
     */
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    /**
     * Creates a new thing instance.
     * 
     * This method creates a new thing instance for the given thing type and configuration.
     * It validates that the thing type is supported before creating the thing.
     *
     * @param thingTypeUID The type of thing to create
     * @param configuration The configuration for the thing
     * @param thingUID The unique identifier for the thing, or null to generate one
     * @param bridgeUID The unique identifier of the bridge this thing belongs to, or null if not bridged
     * @return The created thing instance
     * @throws IllegalArgumentException if the thing type is not supported
     */
    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {

        if (SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the Homekit binding");
    }
}
