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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.config.AtsObjectConfiguration;
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
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class NamedReadOnlyHandler extends BaseThingHandler implements PanelStatusListener {

    private final Logger logger = LoggerFactory.getLogger(NamedReadOnlyHandler.class);
    private @Nullable PanelHandler bridgeHandler;
    private @NonNullByDefault({}) AtsObjectConfiguration config;

    public NamedReadOnlyHandler(Thing thing) {
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
        } catch (ATSAdvancedException e) {
            logger.error("Named readonly init failed: {}", e.getMessage());
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
        } catch (ATSAdvancedException e) {
            logger.error("Named readonly dispose failed: {}", e.getMessage());
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

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            publishName();
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        publishName();
    }

    private void publishName() {
        String name = config.name;
        String text = name != null ? name : ("#" + config.number);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.STATUS), new StringType(text));
    }
}
