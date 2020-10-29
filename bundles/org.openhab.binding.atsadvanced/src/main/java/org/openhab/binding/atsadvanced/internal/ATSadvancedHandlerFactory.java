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
package org.openhab.binding.atsadvanced.internal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.discovery.ATSadvancedDiscoveryService;
import org.openhab.binding.atsadvanced.handler.AreaHandler;
import org.openhab.binding.atsadvanced.handler.PanelHandler;
import org.openhab.binding.atsadvanced.handler.ZoneHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ATSadvancedHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.atsadvanced")
public class ATSadvancedHandlerFactory extends BaseThingHandlerFactory {

    @SuppressWarnings("null")
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(ATSadvancedBindingConstants.THING_TYPE_PANEL, ATSadvancedBindingConstants.THING_TYPE_AREA,
                    ATSadvancedBindingConstants.THING_TYPE_ZONE).collect(Collectors.toSet()));

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private String monoPath = "";
    private String atsPath = "";

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        monoPath = (String) properties.get("mono");
        atsPath = (String) properties.get("ats");
    };

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        if (ATSadvancedBindingConstants.THING_TYPE_PANEL.equals(thingTypeUID)) {
            ThingUID panelUID = getPanelUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, panelUID, null);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_AREA.equals(thingTypeUID)) {
            ThingUID blasterUID = getAreaUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, blasterUID, bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            ThingUID blasterUID = getZoneUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, blasterUID, bridgeUID);
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the ATS Advanced binding.");
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        if (thing.getThingTypeUID().equals(ATSadvancedBindingConstants.THING_TYPE_PANEL)) {
            PanelHandler panel = new PanelHandler((Bridge) thing, monoPath, atsPath);
            registerATSadvancedDiscoveryService(panel);
            return panel;
        } else if (thing.getThingTypeUID().equals(ATSadvancedBindingConstants.THING_TYPE_AREA)) {
            return new AreaHandler(thing);
        } else if (thing.getThingTypeUID().equals(ATSadvancedBindingConstants.THING_TYPE_ZONE)) {
            return new ZoneHandler(thing);
        } else {
            return null;
        }
    }

    @SuppressWarnings("null")
    private synchronized void registerATSadvancedDiscoveryService(PanelHandler bridgeHandler) {
        ATSadvancedDiscoveryService discoveryService = new ATSadvancedDiscoveryService(bridgeHandler);
        // discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof PanelHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            serviceReg.unregister();
            discoveryServiceRegs.remove(thingHandler.getThing().getUID());
        }
    }

    private ThingUID getPanelUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String ipAddress = (String) configuration.get(PanelHandler.IP_ADDRESS);
            return new ThingUID(thingTypeUID, ipAddress);
        }
        return thingUID;
    }

    private ThingUID getAreaUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID) {
        BigDecimal areaID = (BigDecimal) configuration.get(AreaHandler.NUMBER);

        if (thingUID == null) {
            return new ThingUID(thingTypeUID, "Area" + areaID, bridgeUID.getId());
        }
        return thingUID;
    }

    private ThingUID getZoneUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID) {
        BigDecimal zoneID = (BigDecimal) configuration.get(ZoneHandler.NUMBER);

        if (thingUID == null) {
            return new ThingUID(thingTypeUID, "Zone" + zoneID, bridgeUID.getId());
        }
        return thingUID;
    }
}
