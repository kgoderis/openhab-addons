/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import org.openhab.binding.atsadvanced.internal.discovery.ATSadvancedDiscoveryService;
import org.openhab.binding.atsadvanced.internal.handler.AreaHandler;
import org.openhab.binding.atsadvanced.internal.handler.DgpHandler;
import org.openhab.binding.atsadvanced.internal.handler.NamedReadOnlyHandler;
import org.openhab.binding.atsadvanced.internal.handler.OutputHandler;
import org.openhab.binding.atsadvanced.internal.handler.PanelHandler;
import org.openhab.binding.atsadvanced.internal.handler.RasHandler;
import org.openhab.binding.atsadvanced.internal.handler.StatObjectHandler;
import org.openhab.binding.atsadvanced.internal.handler.ZoneHandler;
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

@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.atsadvanced")
public class ATSadvancedHandlerFactory extends BaseThingHandlerFactory {

    @SuppressWarnings("null")
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(Stream
            .of(ATSadvancedBindingConstants.THING_TYPE_PANEL, ATSadvancedBindingConstants.THING_TYPE_AREA,
                    ATSadvancedBindingConstants.THING_TYPE_ZONE, ATSadvancedBindingConstants.THING_TYPE_OUTPUT,
                    ATSadvancedBindingConstants.THING_TYPE_RAS, ATSadvancedBindingConstants.THING_TYPE_DGP,
                    ATSadvancedBindingConstants.THING_TYPE_SYS, ATSadvancedBindingConstants.THING_TYPE_PCC,
                    ATSadvancedBindingConstants.THING_TYPE_CS, ATSadvancedBindingConstants.THING_TYPE_TRIGGER,
                    ATSadvancedBindingConstants.THING_TYPE_USER, ATSadvancedBindingConstants.THING_TYPE_USERGROUP,
                    ATSadvancedBindingConstants.THING_TYPE_FILTER, ATSadvancedBindingConstants.THING_TYPE_DIALLER)
            .collect(Collectors.toSet()));

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private String monoPath = "";
    private String atsPath = "";

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        Object mono = properties.get("mono");
        Object ats = properties.get("ats");
        monoPath = mono instanceof String s ? s : "";
        atsPath = ats instanceof String s ? s : "";
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        if (ATSadvancedBindingConstants.THING_TYPE_PANEL.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, getPanelUID(thingTypeUID, thingUID, configuration),
                    null);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_AREA.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, AreaHandler.NUMBER, "Area"),
                    bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, ZoneHandler.NUMBER, "Zone"),
                    bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_OUTPUT.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, OutputHandler.NUMBER, "Output"),
                    bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_RAS.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, RasHandler.NUMBER, "RAS"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_DGP.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, DgpHandler.NUMBER, "DGP"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_DIALLER.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, getChildUID(thingTypeUID, thingUID, configuration,
                    bridgeUID, NamedReadOnlyHandler.NUMBER, "Dialler"), bridgeUID);
        }
        StatObjectType statType = StatObjectType.forThingType(thingTypeUID);
        if (statType != null) {
            String prefix = switch (statType) {
                case SYS -> "Sys";
                case PCC -> "PCC";
                case CS -> "CS";
                case TRIGG -> "Trigger";
                case USER -> "User";
                case UG -> "UserGroup";
                case FILTER -> "Filter";
            };
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, StatObjectHandler.NUMBER, prefix),
                    bridgeUID);
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the ATS Advanced binding.");
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID type = thing.getThingTypeUID();
        if (type.equals(ATSadvancedBindingConstants.THING_TYPE_PANEL)) {
            PanelHandler panel = new PanelHandler((Bridge) thing, monoPath, atsPath);
            registerATSadvancedDiscoveryService(panel);
            return panel;
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_AREA)) {
            return new AreaHandler(thing);
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_ZONE)) {
            return new ZoneHandler(thing);
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_OUTPUT)) {
            return new OutputHandler(thing);
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_RAS)) {
            return new RasHandler(thing);
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_DGP)) {
            return new DgpHandler(thing);
        } else if (type.equals(ATSadvancedBindingConstants.THING_TYPE_DIALLER)) {
            return new NamedReadOnlyHandler(thing);
        }
        StatObjectType statType = StatObjectType.forThingType(type);
        if (statType != null) {
            return new StatObjectHandler(thing, statType);
        }
        return null;
    }

    @SuppressWarnings("null")
    private synchronized void registerATSadvancedDiscoveryService(PanelHandler bridgeHandler) {
        ATSadvancedDiscoveryService discoveryService = new ATSadvancedDiscoveryService(bridgeHandler);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof PanelHandler) {
            @Nullable
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private @Nullable ThingUID getPanelUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            Configuration configuration) {
        if (thingUID == null) {
            @Nullable
            String ipAddress = (String) configuration.get(PanelHandler.IP_ADDRESS);
            if (ipAddress != null) {
                return new ThingUID(thingTypeUID, ipAddress);
            }
        }
        return thingUID;
    }

    private ThingUID getChildUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID, String numberKey, String prefix) {
        if (thingUID != null) {
            return thingUID;
        }
        BigDecimal id = (BigDecimal) configuration.get(numberKey);
        return new ThingUID(thingTypeUID, prefix + id, bridgeUID.getId());
    }
}
