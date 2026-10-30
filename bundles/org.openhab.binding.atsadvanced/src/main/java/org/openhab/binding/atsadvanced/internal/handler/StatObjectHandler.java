/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.AtsProse;
import org.openhab.binding.atsadvanced.internal.StatObjectType;
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
 * Read-only (or trigger-control) status thing backed by getSTAT.* / getCOS.*.
 */
public class StatObjectHandler extends BaseThingHandler implements PanelStatusListener, ObjectStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(StatObjectHandler.class);
    private final StatObjectType type;
    private PanelHandler bridgeHandler;

    public StatObjectHandler(Thing thing, StatObjectType type) {
        super(thing);
        this.type = type;
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerObjectStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("{} init failed: {}", type, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterObjectStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("{} dispose failed: {}", type, e.getMessage());
        }
    }

    private synchronized PanelHandler getBridgeHandler() {
        if (bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof PanelHandler ph) {
                bridgeHandler = ph;
            }
        }
        return bridgeHandler;
    }

    private int number() {
        Object n = getConfig().get(NUMBER);
        if (n instanceof BigDecimal bd) {
            return bd.intValue();
        }
        if (n instanceof Number num) {
            return num.intValue();
        }
        if (n instanceof String s) {
            return Integer.parseInt(s);
        }
        return 1;
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
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
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onObjectStateChanged(type, number());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void onObjectStateChanged(StatObjectType changedType, int objectNumber) {
        if (changedType != type || objectNumber != number() || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        ArrayList flags = getBridgeHandler().getObjectStatus(type, objectNumber);
        if (flags == null) {
            return;
        }
        String prose = AtsProse.getInstance().flagsSummary(
                ((ArrayList<?>) flags).stream().map(f -> ((Enum<?>) f).name()).collect(Collectors.toList()));
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(prose));
        if (type == StatObjectType.TRIGG) {
            boolean remote = flags.contains(ATSadvancedBindingConstants.TriggStatusFlags.TRGEV_REMOTEOUT);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SWITCH),
                    remote ? OnOffType.ON : OnOffType.OFF);
        }
        if (type == StatObjectType.PCC) {
            boolean running = flags.contains(ATSadvancedBindingConstants.PccStatusFlags.PCCEV_RUNNING);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                    running ? OnOffType.ON : OnOffType.OFF);
        }
        if (type == StatObjectType.SYS) {
            boolean fault = flags.contains(ATSadvancedBindingConstants.SysStatusFlags.SYSEV_FAULT);
            boolean tamper = flags.contains(ATSadvancedBindingConstants.SysStatusFlags.SYSEV_TAMPER);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.FAULT),
                    fault ? OnOffType.ON : OnOffType.OFF);
            updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.TAMPER),
                    tamper ? OnOffType.ON : OnOffType.OFF);
        }
    }
}
