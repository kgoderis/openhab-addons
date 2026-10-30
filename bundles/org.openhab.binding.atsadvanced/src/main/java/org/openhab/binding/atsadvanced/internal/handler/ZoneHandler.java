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
package org.openhab.binding.atsadvanced.internal.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.ZoneStatusFlags;
import org.openhab.binding.atsadvanced.internal.AtsProse;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zone thing. Inhibit/isolate commands open a {@code createCC.ZONE} control session
 * (not panel programming).
 */
public class ZoneHandler extends BaseThingHandler implements PanelStatusListener, ZoneStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(ZoneHandler.class);

    private PanelHandler bridgeHandler;
    private ArrayList<ZoneStatusFlags> previousStatus = new ArrayList<>();
    private ArrayList<ZoneStatusFlags> lastStatus = new ArrayList<>();
    private boolean inErrorState = false;

    public ZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        previousStatus = new ArrayList<>();
        lastStatus = new ArrayList<>();
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerZoneStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while initialising Zone '{}' : {}", getConfig().get(NAME),
                    e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterZoneStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while disposing Zone '{}' : {}", getConfig().get(NAME),
                    e.getMessage());
        }
    }

    private synchronized PanelHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof PanelHandler) {
                this.bridgeHandler = (PanelHandler) handler;
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    private int zoneNumber() {
        return ((BigDecimal) getConfig().get(NUMBER)).intValue();
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            logger.warn("ATS Advanced Panel handler not found. Cannot handle command without bridge.");
            return;
        }
        if (command instanceof RefreshType) {
            onZoneStateChanged(zoneNumber());
            return;
        }
        if (!(command instanceof OnOffType onOff)) {
            return;
        }
        String id = channelUID.getId();
        if (ATSadvancedBindingConstants.INHIBIT.equals(id)) {
            panel.inhibitZone(zoneNumber(), onOff == OnOffType.ON);
            onZoneStateChanged(zoneNumber());
        } else if (ATSadvancedBindingConstants.ISOLATE.equals(id)) {
            panel.isolateZone(zoneNumber(), onOff == OnOffType.ON);
            onZoneStateChanged(zoneNumber());
        }
    }

    @Override
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        updateName();
        onZoneStateChanged(zoneNumber());
    }

    private void updateName() {
        if (getConfig().get(NAME) == null && getThing().getStatus() == ThingStatus.ONLINE) {
            MessageResponse result = getBridgeHandler().getZoneNamesChunk(zoneNumber());
            if (result != null) {
                for (Property property : result.getProperties()) {
                    if ("name".equals(property.getId()) && property.getValue() instanceof String s && !s.isEmpty()) {
                        getThing().getConfiguration().put(NAME, s);
                        break;
                    }
                }
            }
        }
    }

    private boolean has(ZoneStatusFlags flag) {
        return lastStatus.contains(flag);
    }

    @Override
    public void onZoneStateChanged(int zoneNumber) {
        if (zoneNumber() != zoneNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        previousStatus = lastStatus;
        ArrayList<ZoneStatusFlags> result = null;
        if (!inErrorState) {
            result = getBridgeHandler().getZoneStatus(zoneNumber);
        } else {
            logger.warn("Zone '{}' is in an error state, do you have access?", getConfig().get(NAME));
        }
        if (result == null) {
            inErrorState = true;
            return;
        }
        lastStatus = result;
        logger.debug("Zone '{}' status → {}", getConfig().get(NAME), lastStatus);

        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                has(ZoneStatusFlags.ZNEV_ACTIVE) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ALARM),
                has(ZoneStatusFlags.ZNEV_ALARM) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.INHIBIT),
                has(ZoneStatusFlags.ZNEV_INHIBIT) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ISOLATE),
                has(ZoneStatusFlags.ZNEV_ISOLATE) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.TAMPER),
                has(ZoneStatusFlags.ZNEV_TAMPER) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.FAULT),
                has(ZoneStatusFlags.ZNEV_FAULT) ? OnOffType.ON : OnOffType.OFF);
        String prose = AtsProse.getInstance()
                .flagsSummary(lastStatus.stream().map(Enum::name).collect(Collectors.toList()));
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(prose));
    }
}
