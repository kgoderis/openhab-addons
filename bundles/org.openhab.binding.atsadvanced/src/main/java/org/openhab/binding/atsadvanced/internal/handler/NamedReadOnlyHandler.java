/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal.handler;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
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
 * Read-only discovered object (e.g. dialler) — name/status text only, no control.
 */
public class NamedReadOnlyHandler extends BaseThingHandler implements PanelStatusListener {

    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(NamedReadOnlyHandler.class);
    private PanelHandler bridgeHandler;

    public NamedReadOnlyHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE);
        try {
            getBridgeHandler().registerPanelStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Named readonly init failed: {}", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("Named readonly dispose failed: {}", e.getMessage());
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

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (command instanceof RefreshType) {
            publishName();
        }
    }

    @Override
    public void onBridgeDisconnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(@NonNull PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        publishName();
    }

    private void publishName() {
        Object name = getConfig().get(NAME);
        String text = name != null ? String.valueOf(name) : ("#" + getConfig().get(NUMBER));
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(text));
    }

    public static int numberOf(Thing thing) {
        Object n = thing.getConfiguration().get(NUMBER);
        if (n instanceof BigDecimal bd) {
            return bd.intValue();
        }
        if (n instanceof Number num) {
            return num.intValue();
        }
        return Integer.parseInt(String.valueOf(n));
    }
}
