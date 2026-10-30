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
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.OutputStatusFlags;
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

/**
 * Output control via {@code createCC.OUTPUT} (all areas) → ACTIVATE/DEACTIVATE.
 */
public class OutputHandler extends BaseThingHandler implements PanelStatusListener, OutputStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(OutputHandler.class);
    private PanelHandler bridgeHandler;
    private ArrayList<OutputStatusFlags> lastStatus = new ArrayList<>();

    public OutputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerOutputStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Output init failed: {}", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterOutputStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Output dispose failed: {}", e.getMessage());
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
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        onOutputStateChanged(number());
    }

    @Override
    public void onOutputStateChanged(int outputNumber) {
        if (number() != outputNumber || getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        ArrayList<OutputStatusFlags> result = getBridgeHandler().getOutputStatus(outputNumber);
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
