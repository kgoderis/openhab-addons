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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.io.homekit.api.PairingRegistry;
import org.osgi.framework.ServiceRegistration;
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

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.unmodifiableSet(Stream
            .of(HomekitAccessoryHandler.SUPPORTED_THING_TYPES.stream(),
                    HomekitAccessoryBridgeHandler.SUPPORTED_THING_TYPES.stream())
            .flatMap(i -> i).collect(Collectors.toSet()));

    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private final Map<ThingUID, @Nullable ServiceRegistration<?>> mdnsServiceRegs = new HashMap<>();
    protected final PairingRegistry pairingRegistry;

    @Activate
    public HomekitHandlerFactory(ComponentContext componentContext, @Reference PairingRegistry pairingRegistry) {
        super.activate(componentContext);
        this.pairingRegistry = pairingRegistry;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        if (HomekitAccessoryBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, null);
        } else if (HomekitAccessoryHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }

        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the Homekit binding");
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (HomekitBindingConstants.THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            HomekitAccessoryBridgeHandler handler = new HomekitAccessoryBridgeHandler((Bridge) thing, pairingRegistry);
            registerHomekitDiscoveryService(handler);
            registerHomekitMDNSParticipant(handler);
            return handler;
        }

        if (HomekitBindingConstants.THING_TYPE_ACCESSORY.equals(thingTypeUID)) {
            return new HomekitAccessoryHandler(thing);
        }

        logger.debug("Unsupported thing {}", thing.getThingTypeUID());

        return null;
    }

    private synchronized void registerHomekitDiscoveryService(HomekitAccessoryBridgeHandler bridgeHandler) {
        HomekitAccessoryBridgeDiscoveryService discoveryService = new HomekitAccessoryBridgeDiscoveryService(bridgeHandler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    private synchronized void registerHomekitMDNSParticipant(HomekitAccessoryBridgeHandler bridgeHandler) {
        HomekitAccessoryBridgeParticipant mdnsParticipant = new HomekitAccessoryBridgeParticipant(bridgeHandler);
        this.mdnsServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext.registerService(
                MDNSDiscoveryParticipant.class.getName(), mdnsParticipant, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof HomekitAccessoryBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                HomekitAccessoryBridgeDiscoveryService service = (HomekitAccessoryBridgeDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                if (service != null) {
                    service.deactivate();
                }
            }

            serviceReg = this.mdnsServiceRegs.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                HomekitAccessoryBridgeParticipant service = (HomekitAccessoryBridgeParticipant) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
            }
        }
    }
}
