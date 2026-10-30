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
package org.openhab.binding.atsadvanced.internal.handler;

import static org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.MAX_NUMBER_AREAS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.AreaStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.ControlSessionState;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.DgpStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.OutputStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.RasStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSadvancedBindingConstants.ZoneStatusFlags;
import org.openhab.binding.atsadvanced.internal.AtsEventProse;
import org.openhab.binding.atsadvanced.internal.FaultMessages;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanel;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanelResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Message;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.binding.atsadvanced.internal.StatObjectType;
import org.openhab.binding.atsadvanced.internal.discovery.ATSadvancedDiscoveryService;
import org.openhab.binding.atsadvanced.internal.gateway.AtsGatewayClient;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PanelHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public class PanelHandler extends BaseBridgeHandler {

    // List of Configuration constants
    public static final String IP_ADDRESS = "ipAddress";
    public static final String PORT_NUMBER = "portNumber";
    public static final String GATEWAY_URL = "gatewayUrl";
    public static final String PIN = "pin";
    /** Default encryption password accepted by the gateway (52 zeros). */
    private static final String PANEL_PASSWORD = "0".repeat(52);

    private final Logger logger = LoggerFactory.getLogger(PanelHandler.class);

    private static int CONNECTION_THREAD_INTERVAL = 15;
    private static int POLLING_THREAD_INTERVAL = 30;
    private static int HEART_BEAT = 10000;
    private static int RETRIES = 1;
    private static int TIME_OUT = 2000;

    private ScheduledFuture<?> connectionJob;
    private ScheduledFuture<?> pollingJob;
    private AtsGatewayClient client;
    private String lastError = "";
    private String gatewayURL;
    private boolean gatewayReady = false;
    private boolean panelConnected = false;
    private boolean userLoggedIn = false;
    private boolean monitorStarted = false;
    private boolean logsOpened = false;
    private boolean clientsSetUp = false;

    private List<PanelStatusListener> panelStatusListeners = new CopyOnWriteArrayList<>();
    private List<ZoneStatusListener> zoneStatusListeners = new CopyOnWriteArrayList<>();
    private List<AreaStatusListener> areaStatusListeners = new CopyOnWriteArrayList<>();
    private List<OutputStatusListener> outputStatusListeners = new CopyOnWriteArrayList<>();
    private List<RasStatusListener> rasStatusListeners = new CopyOnWriteArrayList<>();
    private List<DgpStatusListener> dgpStatusListeners = new CopyOnWriteArrayList<>();
    private List<ObjectStatusListener> objectStatusListeners = new CopyOnWriteArrayList<>();

    /** Serialize C&amp;C sessions (createCC.* → fnCC.* → destroyCC.SESSION). */
    private final ReentrantLock controlSessionLock = new ReentrantLock();
    /** Active walk-test session (0 = none). Destroyed on stop or bridge disconnect. */
    private int walkTestSessionId = 0;
    /** After first log fetch uses LATEST; subsequent use OLDER until reset. */
    private boolean logBrowsing = false;
    /** Newest-first buffer for {@code logHistory} (live monitor + panel log reload). */
    private final Deque<String> logHistoryBuffer = new ArrayDeque<>();

    public PanelHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(ATSadvancedDiscoveryService.class);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        String id = channelUID.getId();
        if (command instanceof RefreshType) {
            if (ATSadvancedBindingConstants.LOG_HISTORY.equals(id)
                    || ATSadvancedBindingConstants.FETCH_LOG_HISTORY.equals(id)) {
                reloadLogHistory();
            }
            return;
        }
        if (!(command instanceof OnOffType onOff)) {
            return;
        }
        if (ATSadvancedBindingConstants.RESET.equals(id) && onOff == OnOffType.ON) {
            engineerReset();
        } else if (ATSadvancedBindingConstants.CONFIRM_ALARM.equals(id) && onOff == OnOffType.ON) {
            confirmAlarms();
        } else if (ATSadvancedBindingConstants.BATTERY_TEST.equals(id)) {
            if (onOff == OnOffType.ON) {
                startBatteryTest();
            } else {
                cancelBatteryTest();
            }
        } else if (ATSadvancedBindingConstants.WALK_TEST.equals(id)) {
            if (onOff == OnOffType.ON) {
                startWalkTest(0); // all areas
            } else {
                stopWalkTest();
            }
        } else if (ATSadvancedBindingConstants.FETCH_LOG.equals(id) && onOff == OnOffType.ON) {
            String entry = fetchNextLogEntry();
            if (entry != null) {
                publishTranslatedLog(entry, true);
            }
            updateState(channelUID, OnOffType.OFF);
        } else if (ATSadvancedBindingConstants.FETCH_LOG_HISTORY.equals(id) && onOff == OnOffType.ON) {
            reloadLogHistory();
            updateState(channelUID, OnOffType.OFF);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing ATS Advanced Panel handler.");

        updateStatus(ThingStatus.OFFLINE);

        if (connectionJob == null || connectionJob.isCancelled()) {
            connectionJob = scheduler.scheduleWithFixedDelay(connectionRunnable, 0, CONNECTION_THREAD_INTERVAL,
                    TimeUnit.SECONDS);
        }

        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, POLLING_THREAD_INTERVAL,
                    POLLING_THREAD_INTERVAL, TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing ATS Advanced Panel handler.");

        if (connectionJob != null && !connectionJob.isCancelled()) {
            connectionJob.cancel(true);
            connectionJob = null;
        }

        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }

        logout();

        if (client != null) {
            client.close();
            client = null;
            clientsSetUp = false;
        }

        gatewayReady = false;

        super.dispose();
    }

    public boolean registerPanelStatusListener(PanelStatusListener panelStatusListener) throws ATSAdvancedException {
        if (panelStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null PanelStatusListener.");
        }
        return panelStatusListeners.add(panelStatusListener);
    }

    public boolean registerZoneStatusListener(ZoneStatusListener zoneStatusListener) throws ATSAdvancedException {
        if (zoneStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null ZoneStatusListener.");
        }
        return zoneStatusListeners.add(zoneStatusListener);
    }

    public boolean registerAreaStatusListener(AreaStatusListener areaStatusListener) throws ATSAdvancedException {
        if (areaStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null AreaStatusListener.");
        }
        return areaStatusListeners.add(areaStatusListener);
    }

    public boolean registerOutputStatusListener(OutputStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null OutputStatusListener.");
        }
        return outputStatusListeners.add(listener);
    }

    public boolean registerRasStatusListener(RasStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null RasStatusListener.");
        }
        return rasStatusListeners.add(listener);
    }

    public boolean registerDgpStatusListener(DgpStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null DgpStatusListener.");
        }
        return dgpStatusListeners.add(listener);
    }

    public boolean registerObjectStatusListener(ObjectStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null ObjectStatusListener.");
        }
        return objectStatusListeners.add(listener);
    }

    public boolean unregisterPanelStatusListener(PanelStatusListener panelStatusListener) throws ATSAdvancedException {
        if (panelStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null PanelStatusListener.");
        }
        return panelStatusListeners.remove(panelStatusListener);
    }

    public boolean unregisterZoneStatusListener(ZoneStatusListener zoneStatusListener) throws ATSAdvancedException {
        if (zoneStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null ZoneStatusListener.");
        }
        return zoneStatusListeners.remove(zoneStatusListener);
    }

    public boolean unregisterAreaStatusListener(AreaStatusListener areaStatusListener) throws ATSAdvancedException {
        if (areaStatusListener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null AreaStatusListener.");
        }
        return areaStatusListeners.remove(areaStatusListener);
    }

    public boolean unregisterOutputStatusListener(OutputStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null OutputStatusListener.");
        }
        return outputStatusListeners.remove(listener);
    }

    public boolean unregisterRasStatusListener(RasStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null RasStatusListener.");
        }
        return rasStatusListeners.remove(listener);
    }

    public boolean unregisterDgpStatusListener(DgpStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null DgpStatusListener.");
        }
        return dgpStatusListeners.remove(listener);
    }

    public boolean unregisterObjectStatusListener(ObjectStatusListener listener) throws ATSAdvancedException {
        if (listener == null) {
            throw new ATSAdvancedException("It's not allowed to pass a null ObjectStatusListener.");
        }
        return objectStatusListeners.remove(listener);
    }

    public boolean isConnected() {
        return panelConnected;
    }

    public boolean isLoggedIn() {
        return userLoggedIn;
    }

    public boolean isMonitorStarted() {
        return monitorStarted;
    }

    public boolean isLogsOpened() {
        return logsOpened;
    }

    public boolean isClientsSetUp() {
        return clientsSetUp;
    }

    public boolean isGatewayStarted() {
        return gatewayReady;
    }

    public String getLastError() {
        return lastError;
    }

    private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                logger.debug("Polling the ATS Advanced Panel");
                if (getThing().getStatus() == ThingStatus.ONLINE) {
                    if (isGatewayStarted() && isClientsSetUp() && isConnected() && isLoggedIn() && isMonitorStarted()
                            && isLogsOpened()) {
                        updateChangedZonesStatus();
                        updateChangedAreasStatus();
                        updateChangedOutputsStatus();
                        updateChangedRasStatus();
                        updateChangedDgpStatus();
                        updateChangedStatObjects();
                    }
                }
            } catch (Exception e) {
                logger.warn("An exception occurred while polling the ATS Advanced Panel: '{}'", e.getMessage());
            }
        }
    };

    private Runnable connectionRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                logger.trace("connectionRunnable invoked");
                if (getThing().getStatus() == ThingStatus.OFFLINE) {

                    if (!isGatewayStarted()) {
                        ensureGatewayAvailable();
                    }

                    if (isGatewayStarted() && !isClientsSetUp()) {
                        setupClients();
                    }

                    if (!isConnected() && isClientsSetUp()) {
                        // instruct the gateway to connect to the ATS panel
                        establishConnection();
                    }

                    if (!isLoggedIn() && isConnected()) {
                        // login into the ATS panel with the PIN of a user that is authorised to monitor and control the
                        // panel
                        login();
                        if (!isLoggedIn()) {
                            logout();
                            onConnectionLost();
                        }
                    }

                    if (!isMonitorStarted() && isLoggedIn()) {
                        startMonitor();
                    }

                    if (!isLogsOpened() && isLoggedIn()) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            logger.debug(
                                    "An exception occurred while putting the ATS Advanced execute thread to sleep");
                        }
                        openLogs();
                    }

                    if (isClientsSetUp() && isConnected() && isLoggedIn() && isMonitorStarted() && isLogsOpened()) {
                        onConnectionResumed();
                    }
                }
            } catch (Exception e) {
                logger.warn("An exception occurred while setting up the ATS Advanced Panel: '{}'", e.getMessage());
                handleError(e.getMessage());
            }
        }
    };

    public void onConnectionLost() {

        // let's start over again
        panelConnected = false;
        userLoggedIn = false;
        monitorStarted = false;
        logsOpened = false;
        walkTestSessionId = 0;
        logBrowsing = false;
        synchronized (logHistoryBuffer) {
            logHistoryBuffer.clear();
        }

        for (PanelStatusListener listener : panelStatusListeners) {
            listener.onBridgeDisconnected(this);
        }
        updateStatus(ThingStatus.OFFLINE);
    }

    public void onConnectionResumed() {
        updateStatus(ThingStatus.ONLINE);
        for (PanelStatusListener listener : panelStatusListeners) {
            listener.onBridgeConnected(this);
        }
    }


    private void ensureGatewayAvailable() {
        if (gatewayReady) {
            return;
        }

        Object url = getConfig().get(GATEWAY_URL);
        if (!(url instanceof String s) || s.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "gatewayUrl is required (e.g. http://127.0.0.1:18137)");
            return;
        }

        gatewayURL = s.trim();
        logger.info("Using ATS Advanced gateway at '{}'", gatewayURL);
        try {
            try (AtsGatewayClient probe = new AtsGatewayClient(gatewayURL)) {
                if (!probe.isHealthy()) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Gateway health check failed at " + gatewayURL);
                    return;
                }
            }
            gatewayReady = true;
        } catch (Exception e) {
            logger.warn("Gateway not reachable at '{}': {}", gatewayURL, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void setupClients() {
        try {
            if (client != null) {
                client.close();
            }
            client = new AtsGatewayClient(gatewayURL);
            client.startEventStream(this::handleMessageResponse);
            clientsSetUp = true;
            logger.debug("Gateway HTTP/SSE client ready for '{}'", gatewayURL);
        } catch (Exception e) {
            logger.error("An exception occurred while setting up the Gateway Client: {}", e.getMessage(), e);
            clientsSetUp = false;
        }
    }

    private void establishConnection() throws Exception {
        if (isClientsSetUp()) {
            logger.debug("Configure Panel");
            ConfigurePanel configurePanel = new ConfigurePanel();
            configurePanel.setHearbeat(HEART_BEAT);
            configurePanel.setHostaddress((String) getConfig().get(IP_ADDRESS));
            configurePanel.setPassword(PANEL_PASSWORD);
            configurePanel.setPort(((BigDecimal) getConfig().get(PORT_NUMBER)).intValue());
            configurePanel.setRetries(RETRIES);
            configurePanel.setTimeout(TIME_OUT);

            ConfigurePanelResponse panelResponse = client.configure(configurePanel);

            if (panelResponse != null) {
                if (panelResponse.getResult() != null) {
                    logger.debug("Configure Panel : Gateway : Result : {}", panelResponse.getResult());
                    if (!panelResponse.getResult().equals("1")) {
                        handleError(panelResponse.getResult());
                    } else {
                        panelConnected = true;
                    }
                }
            }
        }
    }

    private void login() {
        if (isClientsSetUp() && isConnected()) {
            logger.debug("Login : Log into the ATS Advanced Panel with PIN: {}", getConfig().get(PIN));
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("userPIN");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = ((BigDecimal) getConfig().get(PIN)).toString();
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            Property ProgramProperty2 = new Property();
            ProgramProperty2.setId("userAction_LOGREAD");
            ProgramProperty2.setIndex(0);
            java.lang.Object ProgramProperty2Value = "1";
            ProgramProperty2.setValue(ProgramProperty2Value);
            properties.add(ProgramProperty2);

            Property ProgramProperty3 = new Property();
            ProgramProperty3.setId("userAction_CTRL");
            ProgramProperty3.setIndex(0);
            java.lang.Object ProgramProperty3Value = "1";
            ProgramProperty3.setValue(ProgramProperty3Value);
            properties.add(ProgramProperty3);

            Property ProgramProperty4 = new Property();
            ProgramProperty4.setId("userAction_MONITOR");
            ProgramProperty4.setIndex(0);
            java.lang.Object ProgramProperty4Value = "1";
            ProgramProperty4.setValue(ProgramProperty4Value);
            properties.add(ProgramProperty4);

            Property ProgramProperty5 = new Property();
            ProgramProperty5.setId("userAction_DIAG");
            ProgramProperty5.setIndex(0);
            java.lang.Object ProgramProperty5Value = "1";
            ProgramProperty5.setValue(ProgramProperty5Value);
            properties.add(ProgramProperty5);

            Property ProgramProperty6 = new Property();
            ProgramProperty6.setId("userAction_UPLOAD");
            ProgramProperty6.setIndex(0);
            java.lang.Object ProgramProperty6Value = "0";
            ProgramProperty6.setValue(ProgramProperty6Value);
            properties.add(ProgramProperty6);

            Property ProgramProperty7 = new Property();
            ProgramProperty7.setId("userAction_DOWNLOAD");
            ProgramProperty7.setIndex(0);
            java.lang.Object ProgramProperty7Value = "0";
            ProgramProperty7.setValue(ProgramProperty7Value);
            properties.add(ProgramProperty7);

            message.setProperties(properties);
            message.setName("device.getConnect");
            MessageResponse response = client.message(message);

            logger.debug("Login : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            // request get.UserInfo to check if login is successful
            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                userLoggedIn = true;
            }
        }
    }

    private void logout() {
        if (isClientsSetUp()) {
            logger.debug("Logout");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("device.disconnect");
            MessageResponse response = client.message(message);

            logger.debug("Logout : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                String error = (String) response.getProperties().get(0).getValue();
                if (StringUtils.contains(error, "FAULT_NO_ACCESS")
                        || StringUtils.contains(error, "Object reference not set to an instance of an object")) {
                    userLoggedIn = false;
                    panelConnected = false;
                }
                handleError(error);
            } else {
                panelConnected = false;
                userLoggedIn = false;
            }
        }
    }

    private void openLogs() {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            logger.debug("Logs : Open");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("open.LOG");
            MessageResponse response = client.message(message);

            logger.debug("Logs : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                logsOpened = true;
            }
        }
    }

    private void startMonitor() {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            logger.debug("Monitor : Start");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("start.MONITOR");
            MessageResponse response = client.message(message);

            logger.debug("Monitor : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.bool")) {
                monitorStarted = true;
            } else {
                handleError((String) response.getProperties().get(0).getValue());
            }
        }
    }

    public boolean isAlive() {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            logger.debug("Is Alive");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("is.Alive");
            MessageResponse response = client.message(message);

            logger.debug("Is Alive : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.void")) {
                return true;
            } else {
                handleError((String) response.getProperties().get(0).getValue());
            }
        }

        return false;
    }

    public void getUserPrivileges() {
        for (int i = 0; i < 9; i++) {
            getUserPrivilege(i);
        }
    }

    public boolean getUserPrivilege(int index) {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            logger.debug("Verify User Priveleges : Index: {}", index);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("areaID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = index;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("get.privileges");
            MessageResponse response = client.message(message);

            logger.debug("Verify User Priveleges : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }

        return false;
    }

    private void updateChangedZonesStatus() {
        BitSet changedSet = getChangedZones();
        if (changedSet != null) {
            for (int i = 0; i < changedSet.length(); i++) {
                if (changedSet.get(i)) {
                    for (ZoneStatusListener listener : zoneStatusListeners) {
                        listener.onZoneStateChanged(i + 1);
                    }
                }
            }
        }
    }

    private void updateChangedAreasStatus() {
        BitSet changedSet = getChangedAreas();
        if (changedSet != null) {
            for (int i = 0; i < changedSet.length(); i++) {
                if (changedSet.get(i)) {
                    for (AreaStatusListener listener : areaStatusListeners) {
                        listener.onAreaStateChanged(i + 1);
                    }
                }
            }
        }
    }

    private void updateChangedOutputsStatus() {
        notifyBitSet(getChangedCos("getCOS.OUT", "returnCOS.OUT"), outputStatusListeners,
                OutputStatusListener::onOutputStateChanged);
    }

    private void updateChangedRasStatus() {
        notifyBitSet(getChangedCos("getCOS.RAS", "returnCOS.RAS"), rasStatusListeners,
                RasStatusListener::onRasStateChanged);
    }

    private void updateChangedDgpStatus() {
        notifyBitSet(getChangedCos("getCOS.DGP", "returnCOS.DGP"), dgpStatusListeners,
                DgpStatusListener::onDgpStateChanged);
    }

    private void updateChangedStatObjects() {
        for (StatObjectType type : StatObjectType.values()) {
            if (type.getCos == null || type.returnCos == null) {
                continue;
            }
            BitSet changed = getChangedCos(type.getCos, type.returnCos);
            if (changed == null) {
                continue;
            }
            for (int i = 0; i < changed.length(); i++) {
                if (changed.get(i)) {
                    notifyObjectListeners(type, i + 1);
                }
            }
        }
    }

    private void notifyObjectListeners(StatObjectType type, int objectNumber) {
        for (ObjectStatusListener listener : objectStatusListeners) {
            listener.onObjectStateChanged(type, objectNumber);
        }
    }

    private void refreshSysStatus() {
        notifyObjectListeners(StatObjectType.SYS, 1);
    }

    private <T> void notifyBitSet(BitSet changedSet, List<T> listeners, java.util.function.ObjIntConsumer<T> notify) {
        if (changedSet == null) {
            return;
        }
        for (int i = 0; i < changedSet.length(); i++) {
            if (changedSet.get(i)) {
                for (T listener : listeners) {
                    notify.accept(listener, i + 1);
                }
            }
        }
    }

    private BitSet getChangedCos(String getName, String returnName) {
        if (!isClientsSetUp()) {
            return null;
        }
        Message message = new Message();
        message.setProperties(new ArrayList<>());
        message.setName(getName);
        MessageResponse response = client.message(message);
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return null;
        }
        if (!returnName.equals(response.getName())) {
            return null;
        }
        for (Property property : response.getProperties()) {
            if ("bitSet".equals(property.getId()) && property.getValue() instanceof String s) {
                return BitSet.valueOf(Base64.getDecoder().decode(s));
            }
        }
        return null;
    }

    public ArrayList<ZoneStatusFlags> getZoneStatus(int index) {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("objectID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = index;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("getSTAT.ZONE");
            MessageResponse response = client.message(message);

            ArrayList<ZoneStatusFlags> status = new ArrayList<ZoneStatusFlags>();

            if (response.getName().equals("returnSTAT.ZONE")) {
                for (Property property : response.getProperties()) {
                    if (!property.getId().equals("objectID")) {
                        if ((boolean) property.getValue()) {
                            status.add(ZoneStatusFlags.valueOf(property.getId()));
                        }
                    }
                }
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
                return null;
            } else {
                return status;
            }
        }

        return null;
    }

    public ArrayList<AreaStatusFlags> getAreaStatus(int index) {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("objectID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = index;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("getSTAT.AREA");
            MessageResponse response = client.message(message);

            ArrayList<AreaStatusFlags> status = new ArrayList<AreaStatusFlags>();

            for (Property property : response.getProperties()) {
                if (!property.getId().equals("objectID")) {
                    if ((boolean) property.getValue()) {
                        status.add(AreaStatusFlags.valueOf(property.getId()));
                    }
                }
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
                return null;
            } else {
                return status;
            }
        }

        return null;
    }

    public MessageResponse getZoneNamesChunk(int index) {
        try {
            if (isClientsSetUp() && isConnected() && isLoggedIn()) {
                Message message = new Message();
                ArrayList<Property> properties = new ArrayList<Property>();

                Property ProgramProperty1 = new Property();
                ProgramProperty1.setId("index");
                ProgramProperty1.setIndex(0);
                java.lang.Object ProgramProperty1Value = index;
                ProgramProperty1.setValue(ProgramProperty1Value);
                properties.add(ProgramProperty1);

                message.setProperties(properties);
                message.setName("select.ZoneNames");
                MessageResponse response = client.message(message);

                if (response.getName().equals("return.ZoneNames")) {
                    return response;
                } else {
                    handleError((String) response.getProperties().get(0).getValue());
                }
            }
        } catch (Exception e) {
            logger.error("An exception occurred while getting zone names : '{}'", e.getMessage(), e);
        }
        return null;
    }

    public MessageResponse getAreaNamesChunk(int index) {
        if (isClientsSetUp() && isConnected() && isLoggedIn()) {
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("index");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = index;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("select.AreaNames");
            MessageResponse response = client.message(message);

            if (response.getName().equals("return.AreaNames")) {
                return response;
            } else {
                handleError((String) response.getProperties().get(0).getValue());
            }
        }
        return null;
    }

    private BitSet getChangedAreas() {
        if (isClientsSetUp()) {
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("getCOS.AREA");
            MessageResponse response = client.message(message);

            BitSet bitSet = null;

            for (Property property : response.getProperties()) {
                if (property.getId().equals("bitSet")) {
                    bitSet = BitSet.valueOf(Base64.getDecoder().decode((String) property.getValue()));
                }
            }

            if (response.getName().equals("returnCOS.AREA")) {
                return bitSet;
            } else if (response.getName().equals("return.void")) {
                return null;
            } else {
                handleError((String) response.getProperties().get(0).getValue());
            }
        }
        return null;
    }

    private BitSet getChangedZones() {
        if (isClientsSetUp()) {
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("getCOS.ZONE");
            MessageResponse response = client.message(message);

            BitSet bitSet = null;

            for (Property property : response.getProperties()) {
                if (property.getId().equals("bitSet")) {
                    bitSet = BitSet.valueOf(Base64.getDecoder().decode((String) property.getValue()));
                }
            }

            if (response.getName().equals("returnCOS.ZONE")) {
                return bitSet;
            } else if (response.getName().equals("return.void")) {
                return null;
            } else {
                handleError((String) response.getProperties().get(0).getValue());
            }
        }
        return null;
    }

    private Property createBooleanProperty(String id, boolean value) {
        Property programProperty = new Property();
        programProperty.setId(id);
        programProperty.setIndex(0);
        // java.lang.Object ProgramProperty1Value = value;
        if (value) {
            // programProperty.setValue(ProgramProperty1Value);
            programProperty.setValue(Boolean.TRUE);
        } else {
            programProperty.setValue(Boolean.FALSE);
        }
        return programProperty;
    }

    private MessageResponse getLiveEvent(boolean next) {
        if (isClientsSetUp()) {
            logger.debug("Fetch Live Events");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            properties.add(createBooleanProperty("area.1", true));
            properties.add(createBooleanProperty("area.2", true));
            properties.add(createBooleanProperty("area.3", true));
            properties.add(createBooleanProperty("area.4", true));
            properties.add(createBooleanProperty("area.5", true));
            properties.add(createBooleanProperty("area.6", true));
            properties.add(createBooleanProperty("area.7", true));
            properties.add(createBooleanProperty("area.8", true));
            properties.add(createBooleanProperty("evCatFAULT", true));
            properties.add(createBooleanProperty("evCatMAINS", true));
            properties.add(createBooleanProperty("evCatACTZN", true));
            properties.add(createBooleanProperty("evCatACT24H", true));
            properties.add(createBooleanProperty("evCatACTLCD", true));
            properties.add(createBooleanProperty("evCatACTDEV", true));
            properties.add(createBooleanProperty("evCatALARMS_NCNF", true));
            properties.add(createBooleanProperty("evCatFAULTS_CNF", true));
            properties.add(createBooleanProperty("evCatWALK_REQ", true));
            properties.add(createBooleanProperty("evCatWALK_OK", true));
            properties.add(createBooleanProperty("evCatSYSTEM", true));
            properties.add(createBooleanProperty("next", next));

            message.setProperties(properties);
            message.setName("get.liveEvents");
            MessageResponse response = client.message(message);

            logger.debug("Fetch Live Events : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return response;
            }
        }
        return null;
    }

    public boolean getLiveEvents() throws ATSAdvancedException {
        if (isClientsSetUp()) {
            MessageResponse response = getLiveEvent(false);

            while (response.getName().equals("return.sysevent")) {
                response = getLiveEvent(true);
            }

            return true;

        } else {
            return false;
        }
    }

    public boolean unsetArea(int area) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateUnsetControlSession(area);
            ControlSessionState state = getControlSessionState(sessionID);

            if (state == ControlSessionState.CSMS_UC_Ready) {
                unArmAreas(sessionID);
            } else {
                return false;
            }

            state = getControlSessionState(sessionID);

            while (state == ControlSessionState.CSMS_UC_Unsetting) {
                state = getControlSessionState(sessionID);
            }

            if (state == ControlSessionState.CSMS_UC_CnfAlarms) {
                skipAlarms(sessionID);
            }

            if (state == ControlSessionState.CSMS_UC_CnfFaults) {
                skipFaults(sessionID);
            }

            if (state == ControlSessionState.CSMS_UC_Unset) {
                finishControlSession(sessionID);
                return true;
            }

            return false;
        } finally {
            controlSessionLock.unlock();
        }
    }

    public boolean setArea(int area) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateSetControlSession(area);

            if (sessionID == 0) {
                if (StringUtils.contains(lastError, "FAULT_CC_BUSY_AREAS")) {
                    // unlock before nested unsetArea which takes the same lock (non-reentrant would deadlock —
                    // ReentrantLock is reentrant so OK)
                    unsetArea(area);
                }
                return false;
            } else {
                ControlSessionState state = getControlSessionState(sessionID);

                if (state == ControlSessionState.CSMS_FC_Ready) {
                    armAreas(sessionID);
                } else {
                    return false;
                }

                state = getControlSessionState(sessionID);

                while (state != ControlSessionState.CSMS_FC_Setting) {
                    if (state == ControlSessionState.CSMS_FC_Faults) {
                        logger.debug(
                                "Set Area : There are detected faults in the system that prevents setting selected areas");
                        getFaults(sessionID);
                    }
                    if (state == ControlSessionState.CSMS_FC_ActiveStates) {
                        logger.debug(
                                "Set Area : There are detected active states in zones or devices that prevents setting selected areas");
                        getActiveZones(sessionID);
                    }

                    state = getControlSessionState(sessionID);
                }

                finishControlSession(sessionID);
                return true;
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Force-arm via the same {@code createCC.A_SET} session, using {@code fnCC.A_SET_FORCEDSET}
     * instead of {@code fnCC.A_SET_SETAREAS}.
     */
    public boolean forceSetArea(int area) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateSetControlSession(area);
            if (sessionID == 0) {
                return false;
            }
            ControlSessionState state = getControlSessionState(sessionID);
            if (state == ControlSessionState.CSMS_FC_Ready) {
                callSessionFn("fnCC.A_SET_FORCEDSET", sessionID, null);
            } else {
                finishControlSession(sessionID);
                return false;
            }
            state = getControlSessionState(sessionID);
            while (state != ControlSessionState.CSMS_FC_Setting && state != ControlSessionState.CSMS_FC_Set
                    && state != ControlSessionState.CSMS_UNKNOWN) {
                if (state == ControlSessionState.CSMS_FC_Faults) {
                    getFaults(sessionID);
                }
                if (state == ControlSessionState.CSMS_FC_ActiveStates) {
                    getActiveZones(sessionID);
                }
                state = getControlSessionState(sessionID);
            }
            finishControlSession(sessionID);
            return true;
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Part-set: {@code createCC.A_PARTSET} then the same arm progression as full set.
     */
    public boolean partSetArea(int area) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.A_PARTSET", false, area);
            if (sessionID == 0) {
                return false;
            }
            ControlSessionState state = getControlSessionState(sessionID);
            if (state == ControlSessionState.CSMS_FC_Ready || state == ControlSessionState.CSMS_CC_Ready) {
                armAreas(sessionID);
            } else {
                finishControlSession(sessionID);
                return false;
            }
            state = getControlSessionState(sessionID);
            while (state != ControlSessionState.CSMS_FC_Setting && state != ControlSessionState.CSMS_FC_Set
                    && state != ControlSessionState.CSMS_UNKNOWN) {
                if (state == ControlSessionState.CSMS_FC_Faults) {
                    getFaults(sessionID);
                }
                if (state == ControlSessionState.CSMS_FC_ActiveStates) {
                    getActiveZones(sessionID);
                }
                state = getControlSessionState(sessionID);
            }
            finishControlSession(sessionID);
            return true;
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Zone inhibit/uninhibit/isolate — PDF {@code createCC.ZONE} is a <em>control session</em>
     * (state {@code ZIC_Active}), not programming a new zone.
     */
    public boolean inhibitZone(int zoneNumber, boolean inhibit) {
        return zoneControl(zoneNumber, inhibit ? "fnCC.ZONE_INHIBIT" : "fnCC.ZONE_UNINHIBIT");
    }

    public boolean isolateZone(int zoneNumber, boolean isolate) {
        return zoneControl(zoneNumber, isolate ? "fnCC.ZONE_ISOLATE" : "fnCC.ZONE_UNISOLATE");
    }

    private boolean zoneControl(int zoneNumber, String fnName) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.ZONE", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                return callSessionFn(fnName, sessionID, zoneNumber);
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    public boolean setOutput(int outputNumber, boolean on) {
        controlSessionLock.lock();
        try {
            // PDF: createCC.OUTPUT requires all areas
            int sessionID = initiateControlSession("createCC.OUTPUT", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                return callSessionFn(on ? "fnCC.OUTPUT_ACTIVATE" : "fnCC.OUTPUT_DEACTIVATE", sessionID, outputNumber);
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Trigger / remote-output control via {@code createCC.OUT_TRIG}.
     *
     * @param scheduled when true uses {@code fnCC.OUT_SCHED_TRIG_*}, else {@code fnCC.OUT_TRIG_*}
     */
    public boolean setTrigger(int triggerNumber, boolean on, boolean scheduled) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.OUT_TRIG", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                String fn;
                if (scheduled) {
                    fn = on ? "fnCC.OUT_SCHED_TRIG_ACTIVATE" : "fnCC.OUT_SCHED_TRIG_DEACTIVATE";
                } else {
                    fn = on ? "fnCC.OUT_TRIG_ACTIVATE" : "fnCC.OUT_TRIG_DEACTIVATE";
                }
                return callSessionFn(fn, sessionID, triggerNumber);
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Start area walk-test ({@code createCC.A_WALKTST} → {@code fnCC.A_WALKTST_START}).
     *
     * @param area area number, or {@code 0} for all areas
     */
    public boolean startWalkTest(int area) {
        controlSessionLock.lock();
        try {
            if (walkTestSessionId != 0) {
                finishControlSession(walkTestSessionId);
                walkTestSessionId = 0;
            }
            int sessionID = area <= 0 ? initiateControlSession("createCC.A_WALKTST", true, 0)
                    : initiateControlSession("createCC.A_WALKTST", false, area);
            if (sessionID == 0) {
                return false;
            }
            if (!callSessionFn("fnCC.A_WALKTST_START", sessionID, null)) {
                finishControlSession(sessionID);
                return false;
            }
            walkTestSessionId = sessionID;
            return true;
        } finally {
            controlSessionLock.unlock();
        }
    }

    public boolean stopWalkTest() {
        controlSessionLock.lock();
        try {
            if (walkTestSessionId == 0) {
                return true;
            }
            boolean ok = finishControlSession(walkTestSessionId);
            walkTestSessionId = 0;
            return ok;
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Arm-readiness preview: lists inhibitable and non-inhibitable blockers ({@code createCC.A_STATE}).
     */
    public String armPreview(int area) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.A_STATE", false, area);
            if (sessionID == 0) {
                return "arm preview failed";
            }
            try {
                StringBuilder sb = new StringBuilder();
                appendSyseventList(sb, "inhibitable", "fnCC.A_STATE_GET_INH", sessionID);
                appendSyseventList(sb, "blocking", "fnCC.A_STATE_GET_UNINH", sessionID);
                return sb.length() == 0 ? "ready to set" : sb.toString().trim();
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    private void appendSyseventList(StringBuilder sb, String label, String fnName, int sessionID) {
        MessageResponse response = callSessionFnNext(fnName, sessionID, false);
        boolean first = true;
        while (response != null && "return.sysevent".equals(response.getName())) {
            String detail = formatSysevent(response);
            if (first) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(label).append(": ");
                first = false;
            } else {
                sb.append("; ");
            }
            sb.append(detail);
            response = callSessionFnNext(fnName, sessionID, true);
        }
    }

    private @Nullable MessageResponse callSessionFnNext(String fnName, int sessionID, boolean next) {
        if (!isClientsSetUp()) {
            return null;
        }
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        properties.add(intProp("sessionID", sessionID));
        properties.add(createBooleanProperty("next", next));
        message.setProperties(properties);
        message.setName(fnName);
        MessageResponse response = client.message(message);
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return null;
        }
        return response;
    }

    private static String formatSysevent(MessageResponse response) {
        String eventText = null;
        int eventId = 0;
        int sourceId = 0;
        for (Property p : response.getProperties()) {
            if (p.getId() == null) {
                continue;
            }
            switch (p.getId()) {
                case "event_text", "eventText" ->
                    eventText = p.getValue() != null ? String.valueOf(p.getValue()) : null;
                case "event_ID", "eventID" -> eventId = toInt(p.getValue());
                case "source_ID", "sourceID", "objectID" -> sourceId = toInt(p.getValue());
                default -> {
                }
            }
        }
        if (eventText != null && !eventText.isBlank()) {
            return eventText;
        }
        return "event " + eventId + " source " + sourceId;
    }

    /**
     * Read one event from the panel log ({@code select.getLOG}). First call = latest; then older.
     */
    public @Nullable String fetchNextLogEntry() {
        int direction = logBrowsing ? 3 /* LOG_OLDER_EV */ : 0 /* LOG_LATEST_EV */;
        String entry = fetchLogEntry(direction);
        if (entry != null) {
            logBrowsing = true;
        }
        return entry;
    }

    public void resetLogBrowse() {
        logBrowsing = false;
    }

    /**
     * Reload up to {@link ATSadvancedBindingConstants#LOG_HISTORY_SIZE} newest panel log events
     * into {@code logHistory} (newest first).
     */
    public void reloadLogHistory() {
        resetLogBrowse();
        ArrayList<String> entries = new ArrayList<>();
        for (int i = 0; i < ATSadvancedBindingConstants.LOG_HISTORY_SIZE; i++) {
            String entry = fetchNextLogEntry();
            if (entry == null || entry.isBlank()) {
                break;
            }
            entries.add(entry);
        }
        synchronized (logHistoryBuffer) {
            logHistoryBuffer.clear();
            for (String entry : entries) {
                logHistoryBuffer.addLast(entry);
            }
        }
        publishLogHistoryChannel();
        for (String entry : entries) {
            logger.info("ATS panel: {}", entry);
        }
        logger.info("ATS panel: loaded {} log history entr{}", entries.size(), entries.size() == 1 ? "y" : "ies");
    }

    /**
     * Log translated text to openHAB, update {@code logEntry}, and keep {@code logHistory} (newest first).
     */
    private void publishTranslatedLog(String entry, boolean prependToHistory) {
        logger.info("ATS panel: {}", entry);
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.LOG_ENTRY), new StringType(entry));
        if (prependToHistory) {
            synchronized (logHistoryBuffer) {
                logHistoryBuffer.addFirst(entry);
                while (logHistoryBuffer.size() > ATSadvancedBindingConstants.LOG_HISTORY_SIZE) {
                    logHistoryBuffer.removeLast();
                }
            }
            publishLogHistoryChannel();
        }
    }

    private void publishLogHistoryChannel() {
        String text;
        synchronized (logHistoryBuffer) {
            text = logHistoryBuffer.stream().collect(Collectors.joining("\n"));
        }
        updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.LOG_HISTORY), new StringType(text));
    }

    public @Nullable String fetchLogEntry(int logReadingDirection) {
        if (!(isClientsSetUp() && isConnected() && isLoggedIn() && isLogsOpened())) {
            return null;
        }
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        properties.add(intProp("logReadingDirection", logReadingDirection));
        message.setProperties(properties);
        message.setName("select.getLOG");
        MessageResponse response = client.message(message);
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return null;
        }
        if (!"return.getLOG".equals(response.getName())) {
            return null;
        }
        return formatMonitorLike(response.getProperties());
    }

    private String formatMonitorLike(ArrayList<Property> properties) {
        return AtsEventProse.getInstance().formatProperties(properties);
    }

    /**
     * Engineer reset: {@code createCC.ENG_RES} → {@code GETCODE} → {@code DORESET} → destroy.
     */
    public boolean engineerReset() {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.ENG_RES", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                int resCode = 0;
                MessageResponse codeResp = callSessionFnRaw("fnCC.ENG_RES_GETCODE", sessionID, null);
                if (codeResp != null && "return.short".equals(codeResp.getName())) {
                    for (Property p : codeResp.getProperties()) {
                        if ("result".equals(p.getId()) && p.getValue() != null) {
                            resCode = toInt(p.getValue());
                        }
                    }
                }
                Message message = new Message();
                ArrayList<Property> properties = new ArrayList<>();
                properties.add(intProp("sessionID", sessionID));
                properties.add(intProp("resCode", resCode));
                message.setProperties(properties);
                message.setName("fnCC.ENG_RES_DORESET");
                MessageResponse response = client.message(message);
                if ("return.error".equals(response.getName())) {
                    handleError(String.valueOf(response.getProperties().get(0).getValue()));
                    return false;
                }
                return true;
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    /**
     * Confirm alarms: {@code createCC.A_CONFAL} → START → GETALARM/CONFALARM loop → destroy.
     */
    public boolean confirmAlarms() {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.A_CONFAL", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                callSessionFn("fnCC.A_CONFAL_START", sessionID, null);
                ControlSessionState state = getControlSessionState(sessionID);
                while (state == ControlSessionState.CSMS_CC_CnfAlarms) {
                    MessageResponse alarm = callSessionFnRaw("fnCC.A_CONFAL_GETALARM", sessionID, null);
                    if (alarm == null || !"return.sysevent".equals(alarm.getName())) {
                        break;
                    }
                    Integer eventId = null;
                    for (Property p : alarm.getProperties()) {
                        if ("eventUniqueID".equals(p.getId()) && p.getValue() != null) {
                            eventId = toInt(p.getValue());
                        }
                    }
                    if (eventId != null) {
                        Message message = new Message();
                        ArrayList<Property> properties = new ArrayList<>();
                        properties.add(intProp("sessionID", sessionID));
                        properties.add(intProp("eventUniqueID", eventId));
                        message.setProperties(properties);
                        message.setName("fnCC.A_CONFAL_CONFALARM");
                        client.message(message);
                    }
                    state = getControlSessionState(sessionID);
                }
                return true;
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    public boolean startBatteryTest() {
        return batteryTest("fnCC.BATTERY_TEST_START");
    }

    public boolean cancelBatteryTest() {
        return batteryTest("fnCC.BATTERY_TEST_CANCEL");
    }

    private boolean batteryTest(String fnName) {
        controlSessionLock.lock();
        try {
            int sessionID = initiateControlSession("createCC.DEVICE", true, 0);
            if (sessionID == 0) {
                return false;
            }
            try {
                Message message = new Message();
                ArrayList<Property> properties = new ArrayList<>();
                properties.add(intProp("sessionID", sessionID));
                // APPOBJ_DGP = 4 — battery test is expander-scoped per PDF
                properties.add(intProp("classID", 4));
                message.setProperties(properties);
                message.setName(fnName);
                MessageResponse response = client.message(message);
                if ("return.error".equals(response.getName())) {
                    handleError(String.valueOf(response.getProperties().get(0).getValue()));
                    return false;
                }
                return true;
            } finally {
                finishControlSession(sessionID);
            }
        } finally {
            controlSessionLock.unlock();
        }
    }

    private void getActiveZones(int sessionID) {
        if (isClientsSetUp()) {
            MessageResponse response = getActiveZones(sessionID, false);

            while (response.getName().equals("return.sysevent")) {
                // inhibit the fault
                // TODO add parameter to switch enable this - we might want to skip setting if there are active zones
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("eventUniqueID")) {
                        inhibitActiveZone(sessionID, (int) property.getValue());
                    }
                }

                response = getActiveZones(sessionID, true);
            }
        }
    }

    private boolean inhibitActiveZone(int sessionID, int eventID) {
        if (isClientsSetUp()) {
            logger.debug("Inhibit Active Zone : Fault with ID {} for Session : {}", eventID, sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            Property ProgramProperty2 = new Property();
            ProgramProperty2.setId("eventUniqueID");
            ProgramProperty2.setIndex(0);
            java.lang.Object ProgramProperty2Value = eventID;
            ProgramProperty2.setValue(ProgramProperty2Value);
            properties.add(ProgramProperty2);

            message.setProperties(properties);
            message.setName("fnCC.A_SET_INHACTIVE");
            MessageResponse response = client.message(message);

            logger.debug("Inhibit Active Zone : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("result")) {
                        return (boolean) property.getValue();
                    }
                }
            }
        }
        return false;
    }

    private MessageResponse getActiveZones(int sessionID, boolean next) {
        if (isClientsSetUp()) {
            logger.debug("Get Active Zones : Faults for Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            properties.add(createBooleanProperty("next", next));

            message.setProperties(properties);
            message.setName("fnCC.A_SET_GETACTIVE");
            MessageResponse response = client.message(message);

            logger.debug("Get Active Zones : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return response;
            }
        }

        return null;
    }

    private void getFaults(int sessionID) {
        if (isClientsSetUp()) {
            MessageResponse response = getFaults(sessionID, false);

            while (response.getName().equals("return.sysevent")) {

                for (Property property : response.getProperties()) {
                    if (property.getId().equals("eventUniqueID")) {
                        inhibitFault(sessionID, (int) property.getValue());
                    }
                }
                response = getFaults(sessionID, true);
            }
        }
    }

    private boolean inhibitFault(int sessionID, int eventID) {
        if (isClientsSetUp()) {
            logger.debug("Inhibit Fault : ID {} for Session : {}", eventID, sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            Property ProgramProperty2 = new Property();
            ProgramProperty2.setId("eventUniqueID");
            ProgramProperty2.setIndex(0);
            java.lang.Object ProgramProperty2Value = eventID;
            ProgramProperty2.setValue(ProgramProperty2Value);
            properties.add(ProgramProperty2);

            message.setProperties(properties);
            message.setName("fnCC.A_SET_INHFAULT");
            MessageResponse response = client.message(message);

            logger.debug("Inhibit Fault : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("result")) {
                        return (boolean) property.getValue();
                    }
                }
            }
        }
        return false;
    }

    private MessageResponse getFaults(int sessionID, boolean next) {
        if (isClientsSetUp()) {
            logger.debug("Get Faults : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            properties.add(createBooleanProperty("next", next));

            message.setProperties(properties);
            message.setName("fnCC.A_SET_GETFAULT");
            MessageResponse response = client.message(message);

            logger.debug("Get Faults : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return response;
            }
        }
        return null;
    }

    private boolean armAreas(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Arm Areas : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("fnCC.A_SET_SETAREAS");
            MessageResponse response = client.message(message);

            logger.debug("Arm Areas : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean skipAlarms(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Skip Alarms : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("fnCC.A_UNSET_SKIP");
            MessageResponse response = client.message(message);

            logger.debug("Skip Alarms : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean skipFaults(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Skip Faults : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("fnCC.A_UNSET_SKIP");
            MessageResponse response = client.message(message);

            logger.debug("Skip Faults : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean unArmAreas(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Arm Areas : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("fnCC.A_UNSET_UNSETAREAS");
            MessageResponse response = client.message(message);

            logger.debug("Unarm Areas : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }
        return false;
    }

    private ControlSessionState getControlSessionState(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Get Control Session State : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("statusCC.SESSION");
            MessageResponse response = client.message(message);

            logger.debug("Get Control Session State : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("stateID")) {
                        return ControlSessionState.forValue((int) (double) property.getValue());
                    }
                }
            }
        }
        return ControlSessionState.CSMS_UNKNOWN;
    }

    private boolean finishControlSession(int sessionID) {
        if (isClientsSetUp()) {
            logger.debug("Finish Control Session : Session : {}", sessionID);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            Property ProgramProperty1 = new Property();
            ProgramProperty1.setId("sessionID");
            ProgramProperty1.setIndex(0);
            java.lang.Object ProgramProperty1Value = sessionID;
            ProgramProperty1.setValue(ProgramProperty1Value);
            properties.add(ProgramProperty1);

            message.setProperties(properties);
            message.setName("destroyCC.SESSION");
            MessageResponse response = client.message(message);

            logger.debug("Finish Control Session : Gateway : Message Type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                return true;
            }
        }
        return false;
    }

    private int initiateSetControlSession(int area) {
        return initiateControlSession("createCC.A_SET", false, area);
    }

    private int initiateUnsetControlSession(int area) {
        return initiateControlSession("createCC.A_UNSET", false, area);
    }

    /**
     * Opens a C&amp;C session. {@code createCC.*} returns {@code return.short} with session id in {@code result}.
     *
     * @param allAreas when true, set every {@code area.N} (required for OUTPUT; used for ZONE/ENG_RES)
     * @param singleArea used when {@code allAreas} is false
     */
    private int initiateControlSession(String createName, boolean allAreas, int singleArea) {
        if (!isClientsSetUp()) {
            return 0;
        }
        logger.debug("Initiate control session {} allAreas={} area={}", createName, allAreas, singleArea);
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        for (int i = 1; i < MAX_NUMBER_AREAS + 1; i++) {
            properties.add(createBooleanProperty("area." + i, allAreas || i == singleArea));
        }
        message.setProperties(properties);
        message.setName(createName);
        MessageResponse response = client.message(message);
        logger.debug("Initiate {}: {}", createName, response.getName());
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return 0;
        }
        for (Property property : response.getProperties()) {
            if ("result".equals(property.getId()) && property.getValue() != null) {
                return toInt(property.getValue());
            }
        }
        return 0;
    }

    private boolean callSessionFn(String fnName, int sessionID, @Nullable Integer objectId) {
        MessageResponse response = callSessionFnRaw(fnName, sessionID, objectId);
        if (response == null) {
            return false;
        }
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return false;
        }
        for (Property property : response.getProperties()) {
            if ("result".equals(property.getId()) && property.getValue() instanceof Boolean b) {
                return b;
            }
        }
        return !"return.error".equals(response.getName());
    }

    private @Nullable MessageResponse callSessionFnRaw(String fnName, int sessionID, @Nullable Integer objectId) {
        if (!isClientsSetUp()) {
            return null;
        }
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        properties.add(intProp("sessionID", sessionID));
        if (objectId != null) {
            properties.add(intProp("objectID", objectId));
        }
        message.setProperties(properties);
        message.setName(fnName);
        MessageResponse response = client.message(message);
        logger.debug("{} → {}", fnName, response.getName());
        return response;
    }

    private static Property intProp(String id, int value) {
        Property p = new Property();
        p.setId(id);
        p.setIndex(0);
        p.setValue(value);
        return p;
    }

    public ArrayList<OutputStatusFlags> getOutputStatus(int index) {
        return getFlagStatus("getSTAT.OUT", "returnSTAT.OUT", index, OutputStatusFlags.class);
    }

    public ArrayList<RasStatusFlags> getRasStatus(int index) {
        return getFlagStatus("getSTAT.RAS", "returnSTAT.RAS", index, RasStatusFlags.class);
    }

    public ArrayList<DgpStatusFlags> getDgpStatus(int index) {
        return getFlagStatus("getSTAT.DGP", "returnSTAT.DGP", index, DgpStatusFlags.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ArrayList getObjectStatus(StatObjectType type, int index) {
        return getFlagStatus(type.getStat, type.returnStat, index, (Class) type.flagType);
    }

    public MessageResponse getUserNamesChunk(int index) {
        return getNamesChunk("select.UserNames", "return.UserNames", index);
    }

    public MessageResponse getUserGroupNamesChunk(int index) {
        return getNamesChunk("select.UserGroupNames", "return.UserGroupNames", index);
    }

    public MessageResponse getCsNamesChunk(int index) {
        return getNamesChunk("select.CSNames", "return.CSNames", index);
    }

    public MessageResponse getDiallerNamesChunk(int index) {
        return getNamesChunk("select.DLNames", "return.DLNames", index);
    }

    public MessageResponse getPccNamesChunk(int index) {
        return getNamesChunk("select.PCCNames", "return.PCCNames", index);
    }

    public MessageResponse getTriggerNamesChunk(int index) {
        return getNamesChunk("select.TriggerNames", "return.TriggerNames", index);
    }

    public MessageResponse getFilterNamesChunk(int index) {
        return getNamesChunk("select.CEvFilterNames", "return.CEvFilterNames", index);
    }

    private <E extends Enum<E>> ArrayList<E> getFlagStatus(String getName, String returnName, int index,
            Class<E> type) {
        if (!(isClientsSetUp() && isConnected() && isLoggedIn())) {
            return null;
        }
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        properties.add(intProp("objectID", index));
        message.setProperties(properties);
        message.setName(getName);
        MessageResponse response = client.message(message);
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
            return null;
        }
        if (!returnName.equals(response.getName())) {
            return null;
        }
        ArrayList<E> status = new ArrayList<>();
        for (Property property : response.getProperties()) {
            if ("objectID".equals(property.getId()) || property.getId() == null) {
                continue;
            }
            try {
                if (isTruthy(property.getValue())) {
                    status.add(Enum.valueOf(type, property.getId()));
                }
            } catch (IllegalArgumentException ignored) {
                // unknown flag for this protocol revision
            }
        }
        return status;
    }

    public MessageResponse getOutputNamesChunk(int index) {
        return getNamesChunk("select.OutputNames", "return.OutputNames", index);
    }

    public MessageResponse getRasNamesChunk(int index) {
        return getNamesChunk("select.RASNames", "return.RASNames", index);
    }

    public MessageResponse getDgpNamesChunk(int index) {
        return getNamesChunk("select.DGPNames", "return.DGPNames", index);
    }

    private MessageResponse getNamesChunk(String selectName, String returnName, int index) {
        if (!(isClientsSetUp() && isConnected() && isLoggedIn())) {
            return null;
        }
        Message message = new Message();
        ArrayList<Property> properties = new ArrayList<>();
        properties.add(intProp("index", index));
        message.setProperties(properties);
        message.setName(selectName);
        MessageResponse response = client.message(message);
        if (returnName.equals(response.getName())) {
            return response;
        }
        if ("return.error".equals(response.getName())) {
            handleError(String.valueOf(response.getProperties().get(0).getValue()));
        }
        return null;
    }

    /**
     * All gateway SSE {@code panelevent} payloads land here (panel pushes + gateway logMessage).
     * Always log, then dispatch — dispatch panel push events to status listeners.
     */
    public void handleMessageResponse(MessageResponse message) {
        if (message == null || message.getName() == null) {
            return;
        }

        ArrayList<Property> properties = message.getProperties();
        logPanelEvent(message.getName(), properties);

        switch (message.getName()) {
            case "msg.info": {
                String text = gatewayLogText(properties);
                logger.debug("Gateway [DEBUG]: '{}'", text);
                if (isTransportFailure(text)) {
                    handleError(text);
                }
                break;
            }
            case "msg.warn": {
                String text = gatewayLogText(properties);
                logger.warn("Gateway [WARNING]: '{}'", text);
                if (isTransportFailure(text)) {
                    handleError(text);
                }
                break;
            }
            case "msg.error": {
                String errorText = gatewayLogText(properties);
                logger.error("Gateway [ERROR]: '{}'", errorText);
                handleError(errorText);
                break;
            }
            case "msgCOS.ALL": {
                for (Property aProperty : properties) {
                    if (aProperty.getId() == null) {
                        continue;
                    }
                    switch (aProperty.getId()) {
                        case "APPOBJ_ZN": {
                            if (isTruthy(aProperty.getValue())) {
                                updateChangedZonesStatus();
                            }
                            break;
                        }
                        case "APPOBJ_AREA": {
                            if (isTruthy(aProperty.getValue())) {
                                updateChangedAreasStatus();
                            }
                            break;
                        }
                        case "APPOBJ_OUT": {
                            if (isTruthy(aProperty.getValue())) {
                                updateChangedOutputsStatus();
                            }
                            break;
                        }
                        case "APPOBJ_RAS": {
                            if (isTruthy(aProperty.getValue())) {
                                updateChangedRasStatus();
                            }
                            break;
                        }
                        case "APPOBJ_DGP": {
                            if (isTruthy(aProperty.getValue())) {
                                updateChangedDgpStatus();
                            }
                            break;
                        }
                        case "APPOBJ_SYS": {
                            if (isTruthy(aProperty.getValue())) {
                                refreshSysStatus();
                            }
                            break;
                        }
                        default: {
                            StatObjectType type = StatObjectType.forCosFlag(aProperty.getId());
                            if (type != null && type != StatObjectType.SYS && isTruthy(aProperty.getValue())) {
                                if (type.getCos != null && type.returnCos != null) {
                                    BitSet changed = getChangedCos(type.getCos, type.returnCos);
                                    if (changed != null) {
                                        for (int i = 0; i < changed.length(); i++) {
                                            if (changed.get(i)) {
                                                notifyObjectListeners(type, i + 1);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                break;
            }
            case "msg.MONITOR": {
                for (Property aProperty : properties) {
                    if (aProperty.getId() != null) {
                        logger.trace("\t Property {} : {}", aProperty.getId(), aProperty.getValue());
                    }
                }
                String result = formatMonitorLike(properties);
                updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.MONITOR),
                        new StringType(result));
                publishTranslatedLog(result, true);
                break;
            }
            default:
                logger.debug("Unhandled panelevent '{}' (logged only)", message.getName());
                break;
        }
    }

    private void logPanelEvent(String name, ArrayList<Property> properties) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("panelevent ").append(name);
        if (properties != null) {
            for (Property p : properties) {
                sb.append(" | ").append(p.getId()).append('=').append(p.getValue());
            }
        }
        logger.debug("{}", sb);
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        return false;
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            return (int) Double.parseDouble(s);
        }
        return 0;
    }

    /**
     * Gateway emits XML/ISO via XmlConvert (e.g. {@code 2026-10-30T09:28:05+01:00}).
     * Also accepts older locale/InvariantCulture strings from unpatched gateways.
     */
    private static @Nullable Date parseMonitorTimeStamp(String ts) {
        try {
            return Date.from(OffsetDateTime.parse(ts).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return Date.from(LocalDateTime.parse(ts).atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        DateTimeFormatter[] legacy = { DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US),
                DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss", Locale.US),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.UK),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT) };
        for (DateTimeFormatter fmt : legacy) {
            try {
                return Date.from(LocalDateTime.parse(ts, fmt).atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private boolean handlingError = false;

    private static boolean isTransportFailure(String error) {
        return StringUtils.contains(error, "Connection refused")
                || StringUtils.contains(error, "The socket has been shut down")
                || StringUtils.contains(error, "Connection reset by peer")
                || StringUtils.contains(error, "Object reference not set to an instance of an object");
    }

    private boolean handleError(String error) {
        // handle errors returned from the panel / gateway
        try {
            if (!handlingError) {
                String prose = FaultMessages.getInstance().fromGatewayMessage(error);
                logger.debug("ATS Advanced Panel or Gateway : Error : {} ({})", prose, error);
                if (isTransportFailure(error)) {
                    handlingError = true;
                    logout();
                    // tearDown();
                    onConnectionLost();
                    handlingError = false;
                }
                lastError = error;
                return true;
            }
        } catch (Exception e) {
            logger.error("An exception ocurred while handling an error at the panel or gateway: '{}'", e.getMessage());
        }
        return false;
    }

    private static String gatewayLogText(ArrayList<Property> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        Object value = properties.get(0).getValue();
        return value != null ? String.valueOf(value) : "";
    }

}
