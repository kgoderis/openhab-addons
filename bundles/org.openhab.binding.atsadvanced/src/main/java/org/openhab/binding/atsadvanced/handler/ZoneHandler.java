/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.atsadvanced.handler;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.ZoneStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Karel Goderis - Initial contribution
 */
public class ZoneHandler extends BaseThingHandler implements PanelStatusListener, ZoneStatusListener {

    // List of Configuration constants
    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private Logger logger = LoggerFactory.getLogger(ZoneHandler.class);

    private PanelHandler bridgeHandler;
    private ArrayList<ZoneStatusFlags> previousStatus = new ArrayList<ZoneStatusFlags>();
    private ArrayList<ZoneStatusFlags> lastStatus = new ArrayList<ZoneStatusFlags>();
    private boolean inErrorState = false;

    public ZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        previousStatus = new ArrayList<ZoneStatusFlags>();
        lastStatus = new ArrayList<ZoneStatusFlags>();

        updateStatus(ThingStatus.OFFLINE);

        try {
            getBridgeHandler().registerPanelStatusListener(this);
            getBridgeHandler().registerZoneStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while initialising Zone '{}' : {}", getConfig().get(NAME),
                    e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            getBridgeHandler().unregisterPanelStatusListener(this);
            getBridgeHandler().unregisterZoneStatusListener(this);
        } catch (ATSAdvancedException e) {
            logger.error("An exception has occured while disposing Zone '{}' : {}", getConfig().get(NAME),
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

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        PanelHandler panel = getBridgeHandler();

        if (panel == null) {
            logger.warn("ATS Advanced Panel handler not found. Cannot handle command without bridge.");
            return;
        }

        if (command instanceof RefreshType) {
            onZoneStateChanged(((BigDecimal) this.getThing().getConfiguration().get(ZoneHandler.NUMBER)).intValue());
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        updateStatus(ThingStatus.ONLINE);
        updateName();
        onZoneStateChanged(((BigDecimal) this.getThing().getConfiguration().get(ZoneHandler.NUMBER)).intValue());
    }

    private void updateName() {

        if ((String) getConfig().get(NAME) == null && getThing().getStatus() == ThingStatus.ONLINE) {
            PanelHandler panel = getBridgeHandler();

            MessageResponse result = panel.getZoneNamesChunk(((BigDecimal) getConfig().get(NUMBER)).intValue());

            if (result != null) {
                for (Property property : result.getProperties()) {
                    if (property.getId().equals("name")) {
                        if (!(((String) property.getValue()).equals(""))) {
                            getThing().getConfiguration().put(NAME, property.getValue());
                        }
                        break;
                    }
                }
            }
        }
    }

    public boolean isActive() {
        return lastStatus.contains(ZoneStatusFlags.ZNEV_ACTIVE);
    }

    public boolean isAlarm() {
        return lastStatus.contains(ZoneStatusFlags.ZNEV_ALARM);
    }

    public boolean wasActive() {
        return previousStatus.contains(ZoneStatusFlags.ZNEV_ACTIVE);
    }

    public boolean wasAlarm() {
        return previousStatus.contains(ZoneStatusFlags.ZNEV_ALARM);
    }

    @Override
    public void onZoneStateChanged(int zoneNumber) {
        if (((BigDecimal) this.getThing().getConfiguration().get(ZoneHandler.NUMBER)).intValue() == zoneNumber) {
            if (getThing().getStatus() == ThingStatus.ONLINE) {
                previousStatus = lastStatus;
                ArrayList<ZoneStatusFlags> result = null;
                if (!inErrorState) {
                    result = getBridgeHandler().getZoneStatus(((BigDecimal) getConfig().get(NUMBER)).intValue());
                } else {
                    logger.warn("Zone '{}' is in an error state, do you have access?", getConfig().get(NAME));
                }

                if (result != null) {

                    lastStatus = result;

                    logger.debug("Zone '{}' has changed status from '{}' to '{}'",
                            new Object[] { (String) getConfig().get(NAME), getPreviousStatus().toString(),
                                    getLastStatus().toString() });

                    if (isActive() && !wasActive()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                                OnOffType.ON);
                    }

                    if (!isActive() && wasActive()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                                OnOffType.OFF);
                    }

                    if (!isActive() && !wasActive()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ACTIVE),
                                OnOffType.OFF);
                    }

                    if (isAlarm() && !wasAlarm()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ALARM),
                                OnOffType.ON);
                    }

                    if (!isAlarm() && wasAlarm()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ALARM),
                                OnOffType.OFF);
                    }

                    if (!isAlarm() && !wasAlarm()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.ALARM),
                                OnOffType.OFF);
                    }
                } else {
                    inErrorState = true;
                }
            }
        }
    }

    public ArrayList<ZoneStatusFlags> getPreviousStatus() {
        return previousStatus;
    }

    public ArrayList<ZoneStatusFlags> getLastStatus() {
        return lastStatus;
    }
}
