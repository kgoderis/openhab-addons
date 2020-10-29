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
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.AreaStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Karel Goderis - Initial contribution
 */
public class AreaHandler extends BaseThingHandler implements PanelStatusListener, AreaStatusListener {

    // List of Configuration constants
    public static final String NUMBER = "number";
    public static final String NAME = "name";

    private Logger logger = LoggerFactory.getLogger(AreaHandler.class);

    private PanelHandler bridgeHandler;
    private ArrayList<AreaStatusFlags> previousStatus = new ArrayList<AreaStatusFlags>();
    private ArrayList<AreaStatusFlags> lastStatus = new ArrayList<AreaStatusFlags>();
    private boolean inErrorState = false;

    public AreaHandler(Thing thing) {
        super(thing);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        previousStatus = new ArrayList<AreaStatusFlags>();
        lastStatus = new ArrayList<AreaStatusFlags>();

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

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        PanelHandler panel = getBridgeHandler();

        if (panel == null) {
            logger.warn("ATS Advanced Panel handler not found. Cannot handle command without bridge.");
            return;
        }

        if (command instanceof RefreshType) {
            onAreaStateChanged(((BigDecimal) this.getThing().getConfiguration().get(AreaHandler.NUMBER)).intValue());
        } else {
            if (channelUID.getId().equals(ATSadvancedBindingConstants.SET)) {
                if (command instanceof OnOffType) {
                    if (command == OnOffType.ON) {
                        panel.setArea(((BigDecimal) getConfig().get(NUMBER)).intValue());
                    } else {
                        panel.unsetArea(((BigDecimal) getConfig().get(NUMBER)).intValue());
                    }
                    onAreaStateChanged(
                            ((BigDecimal) this.getThing().getConfiguration().get(AreaHandler.NUMBER)).intValue());
                }
            }
        }
    }

    @Override
    public void onBridgeDisconnected(PanelHandler bridge) {
        if (getThing().getStatus() == ThingStatus.ONLINE) {
            logger.debug("Updating thing '{}' status to OFFLINE.", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onBridgeConnected(PanelHandler bridge) {
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            logger.debug("Updating thing '{}' status to ONLINE.", this.getThing().getUID());
            updateStatus(ThingStatus.ONLINE);
        }
        updateName();
        onAreaStateChanged(((BigDecimal) this.getThing().getConfiguration().get(AreaHandler.NUMBER)).intValue());
    }

    public boolean isAlarm() {
        return lastStatus.contains(AreaStatusFlags.AREV_ALARM);
    }

    public boolean isSet() {
        return lastStatus.contains(AreaStatusFlags.AREV_FULLSET);
    }

    public boolean wasAlarm() {
        return previousStatus.contains(AreaStatusFlags.AREV_ALARM);
    }

    public boolean wasSet() {
        return previousStatus.contains(AreaStatusFlags.AREV_FULLSET);
    }

    public boolean isExit() {
        return lastStatus.contains(AreaStatusFlags.AREV_EXIT);
    }

    public boolean wasExit() {
        return previousStatus.contains(AreaStatusFlags.AREV_EXIT);
    }

    @Override
    public void onAreaStateChanged(int areaNumber) {
        if (((BigDecimal) this.getThing().getConfiguration().get(AreaHandler.NUMBER)).intValue() == areaNumber) {
            if (getThing().getStatus() == ThingStatus.ONLINE) {
                previousStatus = lastStatus;
                ArrayList<AreaStatusFlags> result = null;
                if (!inErrorState) {
                    result = getBridgeHandler().getAreaStatus(((BigDecimal) getConfig().get(NUMBER)).intValue());
                } else {
                    logger.warn("Area '{}' is in an error state, do you have access?", getConfig().get(NAME));
                }

                if (result != null) {

                    lastStatus = result;

                    logger.debug("Area '{}' has changed status from '{}' to '{}'",
                            new Object[] { (String) getConfig().get(NAME), getPreviousStatus().toString(),
                                    getLastStatus().toString() });

                    if (isSet() && !wasSet()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SET), OnOffType.ON);
                    }

                    if (!isSet() && wasSet()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SET),
                                OnOffType.OFF);
                    }

                    if (!isSet() && !wasSet()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.SET),
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

                    if (isExit() && !wasExit()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.EXIT),
                                OnOffType.ON);
                    }

                    if (!isExit() && wasExit()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.EXIT),
                                OnOffType.OFF);
                    }

                    if (!isExit() && !wasExit()) {
                        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.EXIT),
                                OnOffType.OFF);
                    }
                } else {
                    inErrorState = true;
                }
            }
        }
    }

    public ArrayList<AreaStatusFlags> getPreviousStatus() {
        return previousStatus;
    }

    public ArrayList<AreaStatusFlags> getLastStatus() {
        return lastStatus;
    }

    private void updateName() {
        if ((String) getConfig().get(NAME) == null && getThing().getStatus() == ThingStatus.ONLINE) {
            PanelHandler panel = getBridgeHandler();

            MessageResponse result = panel.getAreaNamesChunk(((BigDecimal) getConfig().get(NUMBER)).intValue());

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
}
