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
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.DgpStatusFlags;
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
 * Read-only status for a data gathering panel (DGP/expander): {@code getSTAT.DGP} via {@link PanelHandler}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class DgpHandler extends BaseThingHandler implements PanelStatusListener, DgpStatusListener {

    private final Logger logger = LoggerFactory.getLogger(DgpHandler.class);
    private @Nullable PanelHandler bridgeHandler;
    private @NonNullByDefault({}) AtsObjectConfiguration config;

    public DgpHandler(Thing thing) {
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
            panel.registerDgpStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("DGP init failed: {}", e.getMessage());
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
            panel.unregisterDgpStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("DGP dispose failed: {}", e.getMessage());
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
        if (command instanceof RefreshType) {
            onDgpStateChanged(number());
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onDgpStateChanged(number());
    }

    @Override
    public void onDgpStateChanged(int dgpNumber) {
        if (number() != dgpNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        ArrayList<DgpStatusFlags> result = panel.getDgpStatus(dgpNumber);
        if (result == null) {
            return;
        }
        boolean online = !result.contains(DgpStatusFlags.DGPEV_OFFLINE);
        boolean fault = result.contains(DgpStatusFlags.DGPEV_MAINSFAIL)
                || result.contains(DgpStatusFlags.DGPEV_BATTFAIL) || result.contains(DgpStatusFlags.DGPEV_PU_FAIL);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ONLINE),
                online ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.TAMPER),
                result.contains(DgpStatusFlags.DGPEV_TAMPER) ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.FAULT),
                fault ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(
                AtsProse.getInstance().flagsSummary(result.stream().map(Enum::name).collect(Collectors.toList()))));
    }
}
