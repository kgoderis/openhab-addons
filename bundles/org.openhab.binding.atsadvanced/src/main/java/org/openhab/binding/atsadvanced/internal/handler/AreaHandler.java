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

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.AreaStatusFlags;
import org.openhab.binding.atsadvanced.internal.AtsProse;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.binding.atsadvanced.internal.config.AtsObjectConfiguration;
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
 * Area / partition. Arming uses {@code createCC.A_SET} / {@code A_UNSET} / {@code A_PARTSET} sessions.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class AreaHandler extends BaseThingHandler implements PanelStatusListener, AreaStatusListener {

    private final Logger logger = LoggerFactory.getLogger(AreaHandler.class);

    private @Nullable PanelHandler bridgeHandler;
    private @NonNullByDefault({}) AtsObjectConfiguration config;
    private ArrayList<AreaStatusFlags> previousStatus = new ArrayList<>();
    private ArrayList<AreaStatusFlags> lastStatus = new ArrayList<>();
    private boolean inErrorState = false;

    public AreaHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(AtsObjectConfiguration.class);
        previousStatus = new ArrayList<>();
        lastStatus = new ArrayList<>();
        updateStatus(ThingStatus.OFFLINE);
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        try {
            panel.registerPanelStatusListener(this);
            panel.registerAreaStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while initialising Area '{}' : {}", config.name, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        try {
            panel.unregisterPanelStatusListener(this);
            panel.unregisterAreaStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while disposing Area '{}' : {}", config.name, e.getMessage());
        }
    }

    private synchronized @Nullable PanelHandler getBridgeHandler() {
        PanelHandler handler = this.bridgeHandler;
        if (handler != null) {
            return handler;
        }
        Bridge bridge = getBridge();
        if (bridge == null) {
            return null;
        }
        ThingHandler thingHandler = bridge.getHandler();
        if (thingHandler instanceof PanelHandler panelHandler) {
            this.bridgeHandler = panelHandler;
            return panelHandler;
        }
        return null;
    }

    private int areaNumber() {
        return config.number;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            logger.warn("ATS Advanced Panel handler not found. Cannot handle command without bridge.");
            return;
        }
        if (command instanceof RefreshType) {
            if (ATSadvancedBindingConstants.ARM_PREVIEW.equals(channelUID.getId())) {
                String preview = panel.armPreview(areaNumber());
                updateState(channelUID, new StringType(preview != null ? preview : ""));
            } else {
                onAreaStateChanged(areaNumber());
            }
            return;
        }
        if (!(command instanceof OnOffType onOff)) {
            return;
        }
        String id = channelUID.getId();
        if (ATSadvancedBindingConstants.SET.equals(id)) {
            if (onOff == OnOffType.ON) {
                panel.setArea(areaNumber());
            } else {
                panel.unsetArea(areaNumber());
            }
        } else if (ATSadvancedBindingConstants.FORCE_SET.equals(id) && onOff == OnOffType.ON) {
            panel.forceSetArea(areaNumber());
        } else if (ATSadvancedBindingConstants.PART_SET.equals(id)) {
            if (onOff == OnOffType.ON) {
                panel.partSetArea(areaNumber());
            } else {
                panel.unsetArea(areaNumber());
            }
        } else if (ATSadvancedBindingConstants.WALK_TEST.equals(id)) {
            if (onOff == OnOffType.ON) {
                panel.startWalkTest(areaNumber());
            } else {
                panel.stopWalkTest();
            }
        }
        onAreaStateChanged(areaNumber());
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        if (getThing().getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
        updateName();
        onAreaStateChanged(areaNumber());
    }

    private boolean has(AreaStatusFlags flag) {
        return lastStatus.contains(flag);
    }

    @Override
    public void onAreaStateChanged(int areaNumber) {
        if (areaNumber() != areaNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        previousStatus = lastStatus;
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        ArrayList<AreaStatusFlags> result = null;
        if (!inErrorState) {
            result = panel.getAreaStatus(areaNumber);
        } else {
            logger.warn("Area '{}' is in an error state, do you have access?", config.name);
        }
        if (result == null) {
            inErrorState = true;
            return;
        }
        lastStatus = result;
        logger.debug("Area '{}' status → {}", config.name, lastStatus);

        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SET),
                has(AreaStatusFlags.AREV_FULLSET) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.PART_SET),
                has(AreaStatusFlags.AREV_PARTSET) || has(AreaStatusFlags.AREV_PARTSET2) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.FORCE_SET), OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ALARM),
                has(AreaStatusFlags.AREV_ALARM) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.EXIT),
                has(AreaStatusFlags.AREV_EXIT) ? OnOffType.ON : OnOffType.OFF);
        String prose = AtsProse.getInstance()
                .flagsSummary(lastStatus.stream().map(Enum::name).collect(Collectors.toList()));
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(prose));
    }

    private void updateName() {
        if (config.name != null || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        MessageResponse result = panel.getAreaNamesChunk(areaNumber());
        if (result == null) {
            return;
        }
        for (Property property : result.getProperties()) {
            if ("name".equals(property.getId()) && property.getValue() instanceof String s && !s.isEmpty()) {
                getThing().getConfiguration().put(AtsObjectConfiguration.NAME, s);
                config.name = s;
                break;
            }
        }
    }
}
