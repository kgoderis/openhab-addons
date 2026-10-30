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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.config.AtsObjectConfiguration;
import org.openhab.binding.atsadvanced.internal.handler.AreaHandler;
import org.openhab.binding.atsadvanced.internal.handler.DgpHandler;
import org.openhab.binding.atsadvanced.internal.handler.NamedReadOnlyHandler;
import org.openhab.binding.atsadvanced.internal.handler.OutputHandler;
import org.openhab.binding.atsadvanced.internal.handler.PanelHandler;
import org.openhab.binding.atsadvanced.internal.handler.RasHandler;
import org.openhab.binding.atsadvanced.internal.handler.StatObjectHandler;
import org.openhab.binding.atsadvanced.internal.handler.ZoneHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi {@link ThingHandlerFactory} for all {@link ATSadvancedBindingConstants} thing types.
 * <p>
 * The panel bridge uses {@link PanelHandler}; child things hang off the bridge and delegate protocol work to it.
 * Discovery is not created here — {@link PanelHandler#getServices()} registers
 * {@link org.openhab.binding.atsadvanced.internal.discovery.ATSadvancedDiscoveryService}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.atsadvanced")
public class ATSadvancedHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(ATSadvancedBindingConstants.THING_TYPE_PANEL, ATSadvancedBindingConstants.THING_TYPE_AREA,
                    ATSadvancedBindingConstants.THING_TYPE_ZONE, ATSadvancedBindingConstants.THING_TYPE_OUTPUT,
                    ATSadvancedBindingConstants.THING_TYPE_RAS, ATSadvancedBindingConstants.THING_TYPE_DGP,
                    ATSadvancedBindingConstants.THING_TYPE_SYS, ATSadvancedBindingConstants.THING_TYPE_PCC,
                    ATSadvancedBindingConstants.THING_TYPE_CS, ATSadvancedBindingConstants.THING_TYPE_TRIGGER,
                    ATSadvancedBindingConstants.THING_TYPE_USER, ATSadvancedBindingConstants.THING_TYPE_USERGROUP,
                    ATSadvancedBindingConstants.THING_TYPE_FILTER, ATSadvancedBindingConstants.THING_TYPE_DIALLER)
            .collect(Collectors.toSet());

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
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "Area"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "Zone"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_OUTPUT.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "Output"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_RAS.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "RAS"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_DGP.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "DGP"), bridgeUID);
        }
        if (ATSadvancedBindingConstants.THING_TYPE_DIALLER.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration,
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, "Dialler"), bridgeUID);
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
                    getChildUID(thingTypeUID, thingUID, configuration, bridgeUID, prefix), bridgeUID);
        }
        throw new IllegalArgumentException(
                "The thing type " + thingTypeUID + " is not supported by the ATS Advanced binding.");
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID type = thing.getThingTypeUID();
        if (type.equals(ATSadvancedBindingConstants.THING_TYPE_PANEL)) {
            return new PanelHandler((Bridge) thing);
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

    private @Nullable ThingUID getPanelUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            Configuration configuration) {
        if (thingUID == null) {
            Object ipAddress = configuration.get("ipAddress");
            if (ipAddress instanceof String s && !s.isBlank()) {
                return new ThingUID(thingTypeUID, s);
            }
        }
        return thingUID;
    }

    private ThingUID getChildUID(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID, Configuration configuration,
            @Nullable ThingUID bridgeUID, String prefix) {
        if (thingUID != null) {
            return thingUID;
        }
        if (bridgeUID == null) {
            throw new IllegalArgumentException("Child thing of type " + thingTypeUID + " requires a bridge.");
        }
        Object raw = configuration.get(AtsObjectConfiguration.NUMBER);
        BigDecimal id = raw instanceof BigDecimal bd ? bd
                : raw instanceof Number n ? BigDecimal.valueOf(n.longValue()) : new BigDecimal(String.valueOf(raw));
        return new ThingUID(thingTypeUID, bridgeUID.getId(), prefix + id);
    }
}
