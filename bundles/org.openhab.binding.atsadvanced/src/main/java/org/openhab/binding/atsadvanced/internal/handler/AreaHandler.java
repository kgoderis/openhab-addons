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
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.AreaStatusFlags;
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
 * Area / partition. Arming uses {@code createCC.A_SET} / {@code A_UNSET} / {@code A_PARTSET} sessions.
 */
public class AreaHandler extends BaseThingHandler implements PanelStatusListener, AreaStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(AreaHandler.class);

    private PanelHandler bridgeHandler;
    private ArrayList<AreaStatusFlags> previousStatus = new ArrayList<>();
    private ArrayList<AreaStatusFlags> lastStatus = new ArrayList<>();
    private boolean inErrorState = false;

    public AreaHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        previousStatus = new ArrayList<>();
        lastStatus = new ArrayList<>();
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerAreaStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while initialising Area '{}' : {}", getConfig().get(NAME),
                    e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterAreaStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while disposing Area '{}' : {}", getConfig().get(NAME),
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

    private int areaNumber() {
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
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        if (getThing().getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
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
        ArrayList<AreaStatusFlags> result = null;
        if (!inErrorState) {
            result = getBridgeHandler().getAreaStatus(areaNumber);
        } else {
            logger.warn("Area '{}' is in an error state, do you have access?", getConfig().get(NAME));
        }
        if (result == null) {
            inErrorState = true;
            return;
        }
        lastStatus = result;
        logger.debug("Area '{}' status → {}", getConfig().get(NAME), lastStatus);

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
        if (getConfig().get(NAME) == null && getThing().getStatus() == ThingStatus.ONLINE) {
            MessageResponse result = getBridgeHandler().getAreaNamesChunk(areaNumber());
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
}
