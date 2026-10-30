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
package org.openhab.binding.atsadvanced.internal.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.binding.atsadvanced.internal.handler.AreaHandler;
import org.openhab.binding.atsadvanced.internal.handler.DgpHandler;
import org.openhab.binding.atsadvanced.internal.handler.NamedReadOnlyHandler;
import org.openhab.binding.atsadvanced.internal.handler.OutputHandler;
import org.openhab.binding.atsadvanced.internal.handler.PanelHandler;
import org.openhab.binding.atsadvanced.internal.handler.RasHandler;
import org.openhab.binding.atsadvanced.internal.handler.StatObjectHandler;
import org.openhab.binding.atsadvanced.internal.handler.ZoneHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers panel objects (zones, areas, outputs, …) via the logged-in {@link PanelHandler}.
 * Bound as a {@link org.openhab.core.thing.binding.ThingHandlerService} prototype.
 *
 * @author Karel Goderis - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = ATSadvancedDiscoveryService.class)
@NonNullByDefault
public class ATSadvancedDiscoveryService extends AbstractThingHandlerDiscoveryService<PanelHandler> {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(ATSadvancedBindingConstants.THING_TYPE_AREA, ATSadvancedBindingConstants.THING_TYPE_ZONE,
                    ATSadvancedBindingConstants.THING_TYPE_OUTPUT, ATSadvancedBindingConstants.THING_TYPE_RAS,
                    ATSadvancedBindingConstants.THING_TYPE_DGP, ATSadvancedBindingConstants.THING_TYPE_SYS,
                    ATSadvancedBindingConstants.THING_TYPE_PCC, ATSadvancedBindingConstants.THING_TYPE_CS,
                    ATSadvancedBindingConstants.THING_TYPE_TRIGGER, ATSadvancedBindingConstants.THING_TYPE_USER,
                    ATSadvancedBindingConstants.THING_TYPE_USERGROUP, ATSadvancedBindingConstants.THING_TYPE_FILTER,
                    ATSadvancedBindingConstants.THING_TYPE_DIALLER)
            .collect(Collectors.toSet());

    private static final int SEARCH_TIME = 60;
    private static final int DISCOVERY_THREAD_INTERVAL = 60;

    private final Logger logger = LoggerFactory.getLogger(ATSadvancedDiscoveryService.class);

    private @Nullable ScheduledFuture<?> discoveryJob;

    public ATSadvancedDiscoveryService() {
        super(PanelHandler.class, SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME, true);
    }

    @Override
    protected void startBackgroundDiscovery() {
        ScheduledFuture<?> job = discoveryJob;
        if (job == null || job.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::discoverAll, 1, DISCOVERY_THREAD_INTERVAL,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> job = discoveryJob;
        if (job != null) {
            job.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    protected void startScan() {
        discoverAll();
    }

    private void discoverAll() {
        PanelHandler panel = thingHandler;
        try {
            if (!(panel.isConnected() && panel.isLoggedIn() && panel.isClientsSetUp())) {
                return;
            }

            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_ZONE, "Zone", ZoneHandler.NUMBER,
                    ZoneHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_ZONES, panel::getZoneNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_AREA, "Area", AreaHandler.NUMBER,
                    AreaHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_AREAS, panel::getAreaNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_OUTPUT, "Output", OutputHandler.NUMBER,
                    OutputHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_OUTPUTS, panel::getOutputNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_RAS, "RAS", RasHandler.NUMBER, RasHandler.NAME,
                    ATSadvancedBindingConstants.MAX_NUMBER_RAS, panel::getRasNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_DGP, "DGP", DgpHandler.NUMBER, DgpHandler.NAME,
                    ATSadvancedBindingConstants.MAX_NUMBER_DGP, panel::getDgpNamesChunk);

            discoverSys();
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_PCC, "PCC", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_PCC, panel::getPccNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_CS, "CS", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_CS, panel::getCsNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_TRIGGER, "Trigger", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_TRIGGERS,
                    panel::getTriggerNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_USER, "User", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_USERS, panel::getUserNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_USERGROUP, "UserGroup", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_USERGROUPS,
                    panel::getUserGroupNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_FILTER, "Filter", StatObjectHandler.NUMBER,
                    StatObjectHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_FILTERS, panel::getFilterNamesChunk);
            discoverNamedChunks(ATSadvancedBindingConstants.THING_TYPE_DIALLER, "Dialler", NamedReadOnlyHandler.NUMBER,
                    NamedReadOnlyHandler.NAME, ATSadvancedBindingConstants.MAX_NUMBER_DIALLERS,
                    panel::getDiallerNamesChunk);
        } catch (Exception e) {
            logger.error("An exception occurred while discovering an ATS Advanced Panel: '{}'", e.getMessage());
        }
    }

    private void discoverSys() {
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        ThingUID uid = new ThingUID(ATSadvancedBindingConstants.THING_TYPE_SYS, bridgeUID, "Sys1");
        Map<String, Object> properties = new HashMap<>(2);
        properties.put(StatObjectHandler.NUMBER, "1");
        properties.put(StatObjectHandler.NAME, "System");
        thingDiscovered(DiscoveryResultBuilder.create(uid).withProperties(properties).withBridge(bridgeUID)
                .withRepresentationProperty(StatObjectHandler.NUMBER).withLabel("ATS Advanced System").build());
    }

    @FunctionalInterface
    private interface NameChunkFetcher {
        @Nullable
        MessageResponse fetch(int index);
    }

    private void discoverNamedChunks(ThingTypeUID type, String prefix, String numberKey, String nameKey, int max,
            NameChunkFetcher fetcher) {
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        int current = 1;
        while (current <= max) {
            MessageResponse result = fetcher.fetch(current);
            if (result == null) {
                break;
            }
            current = current + result.getProperties().size() - 1;
            int resultIndex = 0;
            for (Property property : result.getProperties()) {
                if ("index".equals(property.getId()) && property.getValue() != null) {
                    resultIndex = Integer.parseInt(Long.toString(((Number) property.getValue()).longValue()));
                }
            }
            for (Property property : result.getProperties()) {
                if (!"name".equals(property.getId())) {
                    continue;
                }
                if (property.getValue() instanceof String name && !name.isEmpty()) {
                    ThingUID uid = new ThingUID(type, bridgeUID, prefix + resultIndex);
                    Map<String, Object> properties = new HashMap<>(2);
                    properties.put(numberKey, String.valueOf(resultIndex));
                    properties.put(nameKey, name);
                    thingDiscovered(DiscoveryResultBuilder.create(uid).withProperties(properties).withBridge(bridgeUID)
                            .withRepresentationProperty(numberKey)
                            .withLabel("ATS Advanced " + prefix + " " + resultIndex).build());
                }
                resultIndex++;
            }
        }
    }
}
