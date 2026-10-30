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
import org.openhab.binding.atsadvanced.internal.AtsProse;
import org.openhab.binding.atsadvanced.internal.StatObjectType;
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
 * Handler for {@link StatObjectType} things (system, user, trigger, filter, …): polls {@code getSTAT.*} through
 * {@link PanelHandler#getObjectStatus(StatObjectType, int)} and listens for COS via {@link ObjectStatusListener}.
 * Trigger things may expose control channels that map to {@link PanelHandler#setTrigger(int, boolean, boolean)}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class StatObjectHandler extends BaseThingHandler implements PanelStatusListener, ObjectStatusListener {

    private final Logger logger = LoggerFactory.getLogger(StatObjectHandler.class);
    private final StatObjectType type;
    private @Nullable PanelHandler bridgeHandler;
    private @NonNullByDefault({}) AtsObjectConfiguration config;

    public StatObjectHandler(Thing thing, StatObjectType type) {
        super(thing);
        this.type = type;
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
            panel.registerObjectStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("{} init failed: {}", type, e.getMessage());
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
            panel.unregisterObjectStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("{} dispose failed: {}", type, e.getMessage());
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
            onObjectStateChanged(type, number());
            return;
        }
        if (type == StatObjectType.TRIGG && command instanceof OnOffType onOff) {
            String id = channelUID.getId();
            if (ATSadvancedBindingConstants.SWITCH.equals(id)) {
                panel.setTrigger(number(), onOff == OnOffType.ON, false);
                onObjectStateChanged(type, number());
            } else if (ATSadvancedBindingConstants.SCHED_TRIG.equals(id)) {
                panel.setTrigger(number(), onOff == OnOffType.ON, true);
                onObjectStateChanged(type, number());
            }
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onObjectStateChanged(type, number());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void onObjectStateChanged(StatObjectType type, int objectNumber) {
        if (type != this.type || objectNumber != number() || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        PanelHandler panel = getBridgeHandler();
        if (panel == null) {
            return;
        }
        ArrayList flags = panel.getObjectStatus(this.type, objectNumber);
        if (flags == null) {
            return;
        }
        String prose = AtsProse.getInstance().flagsSummary(
                ((ArrayList<?>) flags).stream().map(f -> ((Enum<?>) f).name()).collect(Collectors.toList()));
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(prose));
        if (this.type == StatObjectType.TRIGG) {
            boolean remote = flags.contains(ATSadvancedBindingConstants.TriggStatusFlags.TRGEV_REMOTEOUT);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SWITCH),
                    remote ? OnOffType.ON : OnOffType.OFF);
        }
        if (this.type == StatObjectType.PCC) {
            boolean running = flags.contains(ATSadvancedBindingConstants.PccStatusFlags.PCCEV_RUNNING);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                    running ? OnOffType.ON : OnOffType.OFF);
        }
        if (this.type == StatObjectType.SYS) {
            boolean fault = flags.contains(ATSadvancedBindingConstants.SysStatusFlags.SYSEV_FAULT);
            boolean tamper = flags.contains(ATSadvancedBindingConstants.SysStatusFlags.SYSEV_TAMPER);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.FAULT),
                    fault ? OnOffType.ON : OnOffType.OFF);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.TAMPER),
                    tamper ? OnOffType.ON : OnOffType.OFF);
        }
    }
}
