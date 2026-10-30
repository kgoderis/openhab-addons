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
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.OutputStatusFlags;
import org.openhab.binding.atsadvanced.internal.AtsProse;
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
 * Output control via {@code createCC.OUTPUT} (all areas) → ACTIVATE/DEACTIVATE.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class OutputHandler extends BaseThingHandler implements PanelStatusListener, OutputStatusListener {

    private final Logger logger = LoggerFactory.getLogger(OutputHandler.class);
    private @Nullable PanelHandler bridgeHandler;
    private @NonNullByDefault({}) AtsObjectConfiguration config;
    private ArrayList<OutputStatusFlags> lastStatus = new ArrayList<>();

    public OutputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(AtsObjectConfiguration.class);
        updateStatus(ThingStatus.OFFLINE);
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        try {
            panel.registerPanelStatusListener(this);
            panel.registerOutputStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Output init failed: {}", e.getMessage());
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
            panel.unregisterOutputStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Output dispose failed: {}", e.getMessage());
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

    private int number() {
        return config.number;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        if (command instanceof RefreshType) {
            onOutputStateChanged(number());
            return;
        }
        if (ATSadvancedBindingConstants.SWITCH.equals(channelUID.getId()) && command instanceof OnOffType onOff) {
            panel.setOutput(number(), onOff == OnOffType.ON);
            onOutputStateChanged(number());
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onOutputStateChanged(number());
    }

    @Override
    public void onOutputStateChanged(int outputNumber) {
        if (number() != outputNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        ArrayList<OutputStatusFlags> result = panel.getOutputStatus(outputNumber);
        if (result == null) {
            return;
        }
        lastStatus = result;
        boolean on = lastStatus.contains(OutputStatusFlags.OUTEV_ON)
                || lastStatus.contains(OutputStatusFlags.OUTEV_ACTIVE);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SWITCH),
                on ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(
                AtsProse.getInstance().flagsSummary(lastStatus.stream().map(Enum::name).collect(Collectors.toList()))));
    }
}
