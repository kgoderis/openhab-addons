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
package org.openhab.binding.atsadvanced.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.handler.AreaHandler;
import org.openhab.binding.atsadvanced.handler.PanelHandler;
import org.openhab.binding.atsadvanced.handler.ZoneHandler;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ATSadvancedDiscoveryService} is responsible for discovering a Area and Zone Things
 * of defined on the ATS Advanced Panel
 *
 * @author Karel Goderis - Initial contribution
 */
public class ATSadvancedDiscoveryService extends AbstractDiscoveryService {

    private Logger logger = LoggerFactory.getLogger(ATSadvancedDiscoveryService.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();

    static {
        SUPPORTED_THING_TYPES_UIDS.add(ATSadvancedBindingConstants.THING_TYPE_AREA);
        SUPPORTED_THING_TYPES_UIDS.add(ATSadvancedBindingConstants.THING_TYPE_ZONE);
    }

    private static int DISCOVERY_THREAD_INTERVAL = 60;

    private PanelHandler panel;
    private ScheduledFuture<?> discoveryJob;

    public ATSadvancedDiscoveryService(PanelHandler panel) {
        super(SUPPORTED_THING_TYPES_UIDS, 10);
        this.panel = panel;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    private void discoverAreasAndZones() {

        try {
            if (panel != null && panel.isConnected() && panel.isLoggedIn() && panel.isClientsSetUp()) {

                int current = 1;
                logger.debug("The gateway will fetch a list of zone names");

                while (current <= ATSadvancedBindingConstants.MAX_NUMBER_ZONES) {
                    MessageResponse result = panel.getZoneNamesChunk(current);
                    // result should contain number of zones + 1 field for "index"
                    if (result != null) {
                        current = current + result.getProperties().size() - 1;

                        int resultIndex = 0;

                        // do something with the result
                        for (Property property : result.getProperties()) {
                            if (property.getId().equals("index")) {
                                resultIndex = Integer.parseInt(Long.toString((long) property.getValue()));
                            }
                        }

                        for (Property property : result.getProperties()) {
                            if (property.getId().equals("name")) {

                                if (!(((String) property.getValue()).equals(""))) {

                                    ThingUID uid = new ThingUID(ATSadvancedBindingConstants.THING_TYPE_ZONE,
                                            "Zone" + resultIndex);

                                    Map<String, Object> properties = new HashMap<>(1);
                                    properties.put(ZoneHandler.NUMBER, String.valueOf(resultIndex));
                                    properties.put(ZoneHandler.NAME, property.getValue());
                                    DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                                            .withProperties(properties).withBridge(panel.getThing().getUID())
                                            .withLabel("ATS Advanced Panel Zone " + resultIndex).build();
                                    thingDiscovered(discoveryResult);

                                }

                                resultIndex++;
                            }
                        }
                    }
                }

                current = 1;
                logger.debug("The gateway will fetch a list of area names");

                while (current <= ATSadvancedBindingConstants.MAX_NUMBER_AREAS) {
                    MessageResponse result = panel.getAreaNamesChunk(current);
                    // result should contain number of zones + 1 field for "index"
                    if (result != null) {
                        current = current + result.getProperties().size() - 1;

                        int resultIndex = 0;

                        // do something with the result
                        for (Property property : result.getProperties()) {
                            if (property.getId().equals("index")) {
                                resultIndex = Integer.parseInt(Long.toString((long) property.getValue()));
                            }
                        }

                        for (Property property : result.getProperties()) {
                            if (property.getId().equals("name")) {

                                if (!(((String) property.getValue()).equals(""))) {

                                    ThingUID uid = new ThingUID(ATSadvancedBindingConstants.THING_TYPE_AREA,
                                            "Area" + resultIndex);
                                    Map<String, Object> properties = new HashMap<>(1);
                                    properties.put(AreaHandler.NUMBER, String.valueOf(resultIndex));
                                    properties.put(AreaHandler.NAME, property.getValue());
                                    DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                                            .withProperties(properties).withBridge(panel.getThing().getUID())
                                            .withLabel("ATS Advanced Panel Area " + resultIndex).build();
                                    thingDiscovered(discoveryResult);
                                }
                                resultIndex++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("An exception occurred while discovering an ATS Advanced Panel: '{}'", e.getMessage());
        }
    }

    private Runnable discoveryRunnable = new Runnable() {
        @Override
        public void run() {
            discoverAreasAndZones();
        }
    };

    @Override
    protected void startBackgroundDiscovery() {
        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleAtFixedRate(discoveryRunnable, 1, DISCOVERY_THREAD_INTERVAL,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void startScan() {
        discoverAreasAndZones();
    }
}
