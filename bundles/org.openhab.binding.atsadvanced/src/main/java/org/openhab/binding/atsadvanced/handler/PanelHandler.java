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

import static org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.MAX_NUMBER_AREAS;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.AreaStatusFlags;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.ControlSessionState;
import org.openhab.binding.atsadvanced.ATSadvancedBindingConstants.ZoneStatusFlags;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanel;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanelResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Message;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Property;
import org.openhab.binding.atsadvanced.internal.PanelLogProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.servicestack.client.JsonServiceClient;
import net.servicestack.client.JsonUtils;
import net.servicestack.client.Log;
import net.servicestack.client.WebServiceException;
import net.servicestack.client.sse.ServerEventsClient;

/**
 * The {@link PanelHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public class PanelHandler extends BaseBridgeHandler {

    // List of Configuration constants
    public static final String IP_ADDRESS = "ipAddress";
    public static final String OPENHAB_HOST = "openhab";
    public static final String PORT_NUMBER = "portNumber";
    public static final String GATEWAY_PORT = "gatewayPortNumber";
    public static final String EVENT_LOGGER_PORT = "eventLoggerPortNumber";
    public static final String PIN = "pin";

    private Logger logger = LoggerFactory.getLogger(PanelHandler.class);

    private static int CONNECTION_THREAD_INTERVAL = 5;
    private static int POLLING_THREAD_INTERVAL = 30;
    private static int HEART_BEAT = 10000;
    private static int RETRIES = 1;
    private static int TIME_OUT = 2000;

    private ScheduledFuture<?> connectionJob;
    private ScheduledFuture<?> pollingJob;
    private JsonServiceClient client;
    private ServerEventsClient sseclient;
    private String lastError = "";
    private String gatewayURL;
    private String monoPath;
    private String atsPath;
    private boolean gatewayProcessStarted = false;
    private boolean panelConnected = false;
    private boolean userLoggedIn = false;
    private boolean monitorStarted = false;
    private boolean logsOpened = false;
    private boolean clientsSetUp = false;

    private final ProcessDestroyer shutdownHookProcessDestroyer = new LoggingShutdownHookProcessDestroyer();
    private final DefaultExecuteResultHandler resultHandler = new GatewayExecuteResultHandler();
    private final ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);

    private List<PanelStatusListener> panelStatusListeners = new CopyOnWriteArrayList<>();
    private List<ZoneStatusListener> zoneStatusListeners = new CopyOnWriteArrayList<>();
    private List<AreaStatusListener> areaStatusListeners = new CopyOnWriteArrayList<>();

    public PanelHandler(Bridge bridge, String monoPath, String atsPath) {
        super(bridge);
        this.monoPath = monoPath;
        this.atsPath = atsPath;
        Log.setInstance(new PanelLogProvider());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // nothing to do here
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

        if (sseclient != null) {
            sseclient.close();
        }

        stopGatewayProcess();

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
        return gatewayProcessStarted;
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
                    }
                    // if (sseclient != null) {
                    // sseclient.Heartbeat();
                    // }
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
                if (getThing().getStatus() == ThingStatus.OFFLINE) {

                    if (!isGatewayStarted()) {
                        startGatewayProcess();
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
            } catch (WebServiceException w) {
                logger.debug("Error code {}", w.getErrorCode());
                logger.debug("Error message {}", w.getErrorMessage());
                logger.debug("Status Code {}", w.getStatusCode());
                logger.debug("Status Desc {}", w.getStatusDescription());
                logger.debug("Servce trace {}", w.getServerStackTrace());
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

    private void startGatewayProcess() {

        if (!gatewayProcessStarted) {

            gatewayURL = "http://" + (String) getConfig().get(OPENHAB_HOST) + ":" + getConfig().get(GATEWAY_PORT);
            logger.debug("The ATS Advanced Panel handler will contact the gateway via '{}'", gatewayURL);

            DefaultExecutor executor = new DefaultExecutor();
            executor.setExitValue(0);

            PumpStreamHandler psh = new PumpStreamHandler(new GatewayLogHandler(logger, 0),
                    new GatewayLogHandler(logger, 1));
            // PumpStreamHandler psh = new PumpStreamHandler(new GatewayLogHangler(logger, 0));
            executor.setStreamHandler(psh);
            executor.setProcessDestroyer(shutdownHookProcessDestroyer);
            executor.setExitValue(0);
            executor.setWatchdog(watchDog);

            File file = new File(atsPath);
            executor.setWorkingDirectory(file);

            CommandLine commandLine = new CommandLine(monoPath);
            commandLine.addArgument("--debug");
            commandLine.addArgument("ATSAdvancedGateway.exe");
            commandLine.addArgument("-p");
            commandLine.addArgument(((BigDecimal) getConfig().get(GATEWAY_PORT)).toString());
            commandLine.addArgument("-d");
            commandLine.addArgument("ats.advanced.drv");
            Map<String, String> environment = new HashMap<String, String>();

            checkAndKillExistingProcessRunning(commandLine);

            try {
                logger.debug("Starting the ATS Advanced Panel gateway process : '{}'", commandLine.toString());
                executor.execute(commandLine, environment, resultHandler);
            } catch (IOException e) {
                logger.error("An exception occurred while starting the gateway process : '{}'", e.getMessage());
            }
            // gatewayProcessStarted = true;
        }
    }

    private void checkAndKillExistingProcessRunning(CommandLine commandline) {
        try {
            String line;
            DefaultExecutor executor = new DefaultExecutor();
            CommandLine psCmd = new CommandLine("ps");
            psCmd.addArgument("aux");

            ByteArrayOutputStream err = new ByteArrayOutputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PumpStreamHandler p = new PumpStreamHandler(out, err);
            executor.setStreamHandler(p);

            executor.execute(psCmd);

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));

            while ((line = input.readLine()) != null) {
                if (line.contains(commandline.toString())) {

                    String[] entry = StringUtils.split(line);

                    CommandLine commandLine = new CommandLine("/bin/kill");
                    commandLine.addArgument("-9");
                    commandLine.addArgument(entry[0]);
                    int result = executor.execute(commandLine);
                    if (result == 0) {
                        logger.info("Killed a running ATS Advanced Panel gateway process");
                    } else {
                        logger.warn("Killing a running ATS Advanced Panel gateway process exited with code : '{}'",
                                result);
                    }
                }
            }
            input.close();

        } catch (Exception err) {
            logger.warn(
                    "An exception occurred while checking and killing existing ATS Advanced Panel gateway processes: '{}'",
                    err.getMessage());
        }
    }

    private void stopGatewayProcess() {
        if (!gatewayProcessStarted) {
            logger.error("The gateway process was already stopped (or never started)");
        }

        logger.debug("Destroying the gateway process");
        watchDog.destroyProcess();

        try {
            // Safer to waitFor() after destroy()
            resultHandler.waitFor();
        } catch (InterruptedException e) {
            logger.error("The gateway process was interrupted. This should not occur");
        }

        gatewayProcessStarted = false;
    }

    @SuppressWarnings("resource")
    private void setupClients() {
        try {
            logger.trace("The Json Service Stack client will use base URL '{}'", gatewayURL);
            client = new JsonServiceClient(gatewayURL);
            if (sseclient != null) {
                sseclient.close();
            }
            sseclient = new ServerEventsClient(gatewayURL).registerHandler("panelevent", (sseclient, e) -> {
                handleMessageResponse((MessageResponse) JsonUtils.fromJson(e.getJson(), MessageResponse.class));
            }).setOnConnect(sub -> {
                logger.debug("The SSE Hartbeat is set at {}ms", sub.getHeartbeatIntervalMs());
            }).start();
            clientsSetUp = true;
        } catch (Exception e) {
            logger.error("An exception occurred while setting up clients: {}", e.getMessage(), e);
            clientsSetUp = false;
        }
    }

    private void establishConnection() throws Exception {
        if (isClientsSetUp()) {
            ConfigurePanel configurePanel = new ConfigurePanel();
            configurePanel.setHearbeat(HEART_BEAT);
            configurePanel.setHostaddress((String) getConfig().get(IP_ADDRESS));
            configurePanel.setPassword("0000");
            configurePanel.setPort(((BigDecimal) getConfig().get(PORT_NUMBER)).intValue());
            configurePanel.setRetries(RETRIES);
            configurePanel.setTimeout(TIME_OUT);

            ConfigurePanelResponse panelResponse = client.post(configurePanel);

            if (panelResponse != null) {
                if (panelResponse.getResult() != null) {
                    logger.debug("The gateway returned: {}", panelResponse.getResult());
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
            logger.debug("The gateway will log into the panel with PIN: {}", getConfig().get(PIN));
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will logout");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("device.disconnect");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will open the logs");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("open.LOG");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will start the monitor");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("start.MONITOR");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will check if the panel is alive");
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            message.setProperties(properties);
            message.setName("is.Alive");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will verify the user privileges with index: {}", index);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            MessageResponse response = client.post(message);

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
            MessageResponse response = client.post(message);

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
                MessageResponse response = client.post(message);

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
            MessageResponse response = client.post(message);

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
            MessageResponse response = client.post(message);

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
            MessageResponse response = client.post(message);

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
            logger.debug("The gateway will fetch live events");
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
        int sessionID = initiateUnsetControlSession(area);
        ControlSessionState state = getControlSessionState(sessionID);

        if (state == ControlSessionState.CSMS_UC_Ready) {
            unArmAreas(sessionID);
        } else {
            // wrong state
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
    }

    public boolean setArea(int area) {
        int sessionID = initiateSetControlSession(area);

        if (sessionID == 0) {
            if (StringUtils.contains(lastError, "FAULT_CC_BUSY_AREAS")) {
                unsetArea(area);
            }
            return false;
        } else {
            ControlSessionState state = getControlSessionState(sessionID);

            if (state == ControlSessionState.CSMS_FC_Ready) {
                armAreas(sessionID);
            } else {
                // wrong state
                return false;
            }

            state = getControlSessionState(sessionID);

            while (state != ControlSessionState.CSMS_FC_Setting) {
                if (state == ControlSessionState.CSMS_FC_Faults) {
                    logger.debug("There are detected faults in the system that prevents setting selected areas");
                    getFaults(sessionID);
                }
                if (state == ControlSessionState.CSMS_FC_ActiveStates) {
                    logger.debug(
                            "There are detected active states in zones or devices that prevents setting selected areas");
                    getActiveZones(sessionID);
                }

                state = getControlSessionState(sessionID);
            }

            finishControlSession(sessionID);
            return true;
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
            logger.debug("The gateway will inhibit the fault with ID {} for session : {}", eventID, sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will fetch the faults for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will inhibit the fault with ID {} for session : {}", eventID, sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will fetch the faults for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will arm the areas for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will arm the areas for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will arm the areas for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will unarm the areas for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will fetch the control session state for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
            logger.debug("The gateway will finish the control session for session : {}", sessionID);
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
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

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
        if (isClientsSetUp()) {
            logger.debug("The gateway will start the set control session for area: {}", area);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            for (int i = 1; i < MAX_NUMBER_AREAS + 1; i++) {
                properties.add(createBooleanProperty("area." + i, i == area));
            }

            message.setProperties(properties);
            message.setName("createCC.A_SET");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("result")) {
                        return (int) (double) property.getValue();
                    }
                }
            }
        }
        return 0;
    }

    private int initiateUnsetControlSession(int area) {
        if (isClientsSetUp()) {
            logger.debug("The gateway will start the unset control session for area: {}", area);
            Message message = new Message();
            ArrayList<Property> properties = new ArrayList<Property>();

            for (int i = 1; i < MAX_NUMBER_AREAS + 1; i++) {
                properties.add(createBooleanProperty("area." + i, i == area));
            }

            message.setProperties(properties);
            message.setName("createCC.A_UNSET");
            MessageResponse response = client.post(message);

            logger.debug("The gateway returned a message of type : {}", response.getName());

            for (Property property : response.getProperties()) {
                logger.debug("\t Property {} : {} : {}",
                        new Object[] { property.getId(), property.getIndex(), property.getValue() });
            }

            if (response.getName().equals("return.error")) {
                handleError((String) response.getProperties().get(0).getValue());
            } else {
                for (Property property : response.getProperties()) {
                    if (property.getId().equals("result")) {
                        return (int) (double) property.getValue();
                    }
                }
            }
        }
        return 0;
    }

    public void handleMessageResponse(MessageResponse message) {

        ArrayList<Property> properties = message.getProperties();

        switch (message.getName()) {
            case "msg.error": {
                handleError((String) properties.get(0).getValue());
                break;
            }
            case "msgCOS.ALL": {
                for (Property aProperty : properties) {
                    switch (aProperty.getId()) {
                        case "APPOBJ_ZN": {
                            if ((boolean) aProperty.getValue()) {
                                updateChangedZonesStatus();
                            }
                            break;
                        }
                        case "APPOBJ_AREA": {
                            if ((boolean) aProperty.getValue()) {
                                updateChangedAreasStatus();
                            }
                            break;
                        }
                        default:
                            break;
                    }
                    ;
                }
                break;
            }
            case "msg.MONITOR": {
                Date timeStamp = null;
                int uniqueID = 0;
                int eventID = 0;
                int eventSource = 0;
                int sourceID = 0;
                int area = 0;
                String eventText = null;

                for (Property aProperty : properties) {
                    logger.trace("\t property {} : {}", aProperty.getId(), aProperty.getValue());
                    if (aProperty.getId().equals("timeStamp")) {
                        Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime((String) aProperty.getValue());
                        timeStamp = cal.getTime();
                    }
                    if (aProperty.getId().equals("unique_id")) {
                        uniqueID = (int) (double) aProperty.getValue();
                    }
                    if (aProperty.getId().equals("event_ID")) {
                        eventID = (int) (double) aProperty.getValue();
                    }
                    if (aProperty.getId().equals("event_source")) {
                        eventSource = (int) (double) aProperty.getValue();
                    }
                    if (aProperty.getId().equals("source_ID")) {
                        sourceID = (int) (double) aProperty.getValue();
                    }
                    if (aProperty.getId().equals("Area")) {
                        if (aProperty.getValue() != null) {
                            area = (int) (double) aProperty.getValue();
                        }
                    }
                    if (aProperty.getId().equals("event_text")) {
                        eventText = (String) aProperty.getValue();
                    }
                }

                String result = timeStamp != null
                        ? timeStamp.toString() + ": " + "id " + uniqueID + " :" + " event type " + eventID + " :"
                                + " source " + eventSource + " :" + " sourceID " + sourceID + ": " + " area" + area
                                + " :" + " detail :" + eventText
                        : null;

                updateState(new ChannelUID(getThing().getUID(), ATSadvancedBindingConstants.MONITOR),
                        (result != null) ? new StringType(result) : UnDefType.UNDEF);
            }
                break;
        }
    }

    private boolean handlingError = false;

    private boolean handleError(String error) {
        // handle errors returned from the panel
        try {
            if (!handlingError) {
                logger.debug("Handling a panel or gateway error: {}", error);
                if (StringUtils.contains(error, "Connection refused")
                        || StringUtils.contains(error, "The socket has been shut down")
                        || StringUtils.contains(error, "Connection reset by peer")
                        || StringUtils.contains(error, "Object reference not set to an instance of an object")) {
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

    class GatewayLogHandler extends LogOutputStream {

        Logger log;
        int level;

        public GatewayLogHandler(Logger log, int level) {
            super(level);
            this.log = log;
        }

        @Override
        protected void processLine(String line, int level) {
            switch (level) {
                case 0: {
                    logger.debug("Gateway [DEBUG]: '{}'", line);
                    if (line.contains("AppHost Created")) {
                        gatewayProcessStarted = true;
                    }
                    break;
                }
                case 1: {
                    logger.error("Gateway [ERROR]: '{}'", line);
                    break;
                }
            }
        }
    }

    public class LoggingShutdownHookProcessDestroyer extends ShutdownHookProcessDestroyer {

        @Override
        public void run() {
            logger.info("Shutdown Hook: JVM is about to exit! Going to kill ATS gateway processes...");
            super.run();
        }
    }

    public class GatewayExecuteResultHandler extends DefaultExecuteResultHandler {

        @Override
        public void onProcessComplete(int exitValue) {
            super.onProcessComplete(exitValue);
            logger.info("The gateway process just exited, with value '{}'", exitValue);
            gatewayProcessStarted = false;
            onConnectionLost();
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
            super.onProcessFailed(e);
            if (!watchDog.killedProcess()) {
                logger.error("The gateway process just failed unexpectedly with value '{}'", e.getExitValue());
                logger.error("The gateway process just failed unexpectedly with message '{}'", e.getMessage());
            }
            gatewayProcessStarted = false;
            onConnectionLost();
        }
    }
}
