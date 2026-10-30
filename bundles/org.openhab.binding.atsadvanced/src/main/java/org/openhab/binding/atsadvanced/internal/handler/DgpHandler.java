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
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.DgpStatusFlags;
import org.openhab.binding.atsadvanced.internal.AtsProse;
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

public class DgpHandler extends BaseThingHandler implements PanelStatusListener, DgpStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(DgpHandler.class);
    private PanelHandler bridgeHandler;

    public DgpHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerDgpStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("DGP init failed: {}", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterDgpStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("DGP dispose failed: {}", e.getMessage());
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
        return ((BigDecimal) getConfig().get(NUMBER)).intValue();
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (command instanceof RefreshType) {
            onDgpStateChanged(number());
        }
    }

    @Override
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onDgpStateChanged(number());
    }

    @Override
    public void onDgpStateChanged(int dgpNumber) {
        if (number() != dgpNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        ArrayList<DgpStatusFlags> result = getBridgeHandler().getDgpStatus(dgpNumber);
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
