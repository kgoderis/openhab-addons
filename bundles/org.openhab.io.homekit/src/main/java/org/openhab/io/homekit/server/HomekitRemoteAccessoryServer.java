/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

package org.openhab.io.homekit.server;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.HomekitBindingConstants;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.listener.HomekitCharacteristicChangeListener;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.server.HomekitAccessoryServerState;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.manager.HomekitEventManager.HomekitEventHandler;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;
import org.openhab.io.homekit.event.model.server.HomekitAccessoryServerEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.HomekitFactoryException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.network.http.HomekitHttpClientTransport;
import org.openhab.io.homekit.network.http.HomekitHttpDestination;
import org.openhab.io.homekit.network.http.HomekitProtocolHandler;
import org.openhab.io.homekit.protocol.crypto.HomekitChachaDecoder;
import org.openhab.io.homekit.protocol.crypto.HomekitChachaEncoder;
import org.openhab.io.homekit.protocol.crypto.HomekitClientSRP6Session;
import org.openhab.io.homekit.protocol.crypto.HomekitEdsaSigner;
import org.openhab.io.homekit.protocol.crypto.HomekitEdsaVerifier;
import org.openhab.io.homekit.protocol.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.openhab.io.homekit.protocol.method.HomekitMethod;
import org.openhab.io.homekit.protocol.pairing.HomekitPairing;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

import djb.Curve25519;

/**
 * Represents a remote HomeKit accessory server that connects to a HomeKit
 * accessory on the network.
 * This server handles remote HomeKit accessories and manages their lifecycle,
 * including:
 * - Server initialization and connection
 * - Remote accessory discovery and management
 * - Event subscription and handling
 * - State synchronization
 * - Security and pairing
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitRemoteAccessoryServer extends HomekitAbstractAccessoryServer
        implements HomekitCharacteristicChangeListener {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(HomekitRemoteAccessoryServer.class);
    private static final String HTTP_SCHEME = "http";
    protected static final String LOG_PREFIX = "Homekit RemoteAccessoryServer";
    protected static final String LOG_INIT = "Init";
    protected static final String LOG_STATE = "State";
    protected static final String LOG_CONFIG = "Config";
    protected static final String LOG_ACCESSORY = "Accessory";
    protected static final String LOG_ERROR = "Error";
    protected static final String LOG_WARN = "Warning";
    protected static final String LOG_PAIRING = "Pairing";
    protected static final String LOG_EVENT = "Event";
    protected static final String LOG_SERVER = "Server";

    // Timeout for pairing stage operations (30 seconds)
    private static final long PAIRING_STAGE_TIMEOUT_SECONDS = 5;

    // ========== Core Dependencies ==========
    private final ScheduledExecutorService scheduler;
    private final HomekitAccessoryFactory accessoryFactory;

    // ========== Component References and Locks ==========
    private Optional<HomekitClientSRP6Session> SRP6Session = Optional.empty();
    @Nullable
    private HttpClient httpClient;

    // ========== State Management ==========
    private byte[] sessionKey = new byte[0];
    private byte[] sharedSecret = new byte[0];
    private byte[] clientPublicKey = new byte[0];
    private byte[] clientPrivateKey = new byte[0];
    private @Nullable ScheduledFuture<?> connectionMonitorJob;

    private final Set<HomekitEventSubscription> eventSubscriptions = new HashSet<>();

    /**
     * Creates a new remote HomeKit accessory server with the specified
     * configuration.
     *
     * @param category The category of accessories this server will host
     * @param serverId The unique identifier for this server
     * @param address The network address of the remote server
     * @param port The port the remote server is listening on
     * @param pairingIdentifier The unique pairing identifier for this server
     * @param secretKey The secret key used for encryption
     * @param accessoryRegistry The registry for managing accessories
     * @param pairingRegistry The registry for managing pairings
     * @param eventManager The manager for handling events
     * @param accessoryFactory The factory for creating accessories
     * @throws HomekitConfigurationException if the configuration is invalid
     */
    public HomekitRemoteAccessoryServer(HomekitAccessoryCategory category, String serverId, InetAddress address,
            int port, byte[] pairingIdentifier, byte[] secretKey, HomekitAccessoryRegistry accessoryRegistry,
            HomekitPairingRegistry pairingRegistry, HomekitEventManager eventManager,
            HomekitAccessoryFactory accessoryFactory) throws HomekitConfigurationException {
        super(category, serverId, address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry,
                eventManager);
        logger.debug("{} [{}] : {} - Initializing remote server - Category: {}, ServerId: {}, Address: {}, Port: {}",
                LOG_PREFIX, getUID(), LOG_INIT, category, serverId, address, port);
        this.accessoryFactory = accessoryFactory;
        this.setupCode = "";
        this.scheduler = org.openhab.core.common.ThreadPoolManager
                .getScheduledPool(HomekitBindingConstants.THREAD_POOL_NAME);
        logger.debug("{} [{}] : {} - Remote server initialization completed", LOG_PREFIX, getUID(), LOG_INIT);
    }

    /**
     * Creates a new remote HomeKit accessory server with auto-generated pairing ID
     * and secret key.
     *
     * @param category The category of accessories this server will host
     * @param serverId The unique identifier for this server
     * @param address The network address of the remote server
     * @param port The port the remote server is listening on
     * @param accessoryRegistry The registry for managing accessories
     * @param pairingRegistry The registry for managing pairings
     * @param eventManager The manager for handling events
     * @param accessoryFactory The factory for creating accessories
     * @throws HomekitConfigurationException if the configuration is invalid
     * @throws HomekitServerException if server creation fails
     */
    public HomekitRemoteAccessoryServer(HomekitAccessoryCategory category, String serverId, InetAddress address,
            int port, HomekitAccessoryRegistry accessoryRegistry, HomekitPairingRegistry pairingRegistry,
            HomekitEventManager eventManager, HomekitAccessoryFactory accessoryFactory)
            throws HomekitConfigurationException, HomekitServerException {
        this(category, serverId, address, port, generatePairingId(), generateSecretKey(), accessoryRegistry,
                pairingRegistry, eventManager, accessoryFactory);
        logger.debug("{} [{}] : {} - Created new remote server with auto-generated credentials", LOG_PREFIX, getUID(),
                LOG_INIT);
    }

    // ========== Core Lifecycle Methods ==========
    @Override
    @SuppressWarnings("null")
    protected void initializeResources() throws HomekitServerException {
        super.initializeResources();
        try {
            httpClient = new HttpClient(new HomekitHttpClientTransport(), null);

            if (httpClient != null) {
                logger.debug("{} [{}] : {} - Starting HTTP client initialization", LOG_PREFIX, getUID(), LOG_INIT);
                try {
                    httpClient.start();
                    ProtocolHandlers handlers = httpClient.getProtocolHandlers();
                    handlers.clear();
                    handlers.put(new HomekitProtocolHandler(this));
                    // Don't set state to CONNECTED here - let the base class set it to READY first
                    // The CONNECTED state will be set later when the connection is actually established
                    logger.debug("{} [{}] : {} - HTTP client initialized successfully", LOG_PREFIX, getUID(), LOG_INIT);
                } catch (Exception e) {
                    logger.error("{} [{}] : {} - Failed to start HTTP client - Error: {}", LOG_PREFIX, getUID(),
                            LOG_ERROR, e.getMessage());
                    logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
                    // Only set state to DISCONNECTED if we're not in UNKNOWN state
                    // UNKNOWN can only transition to READY, not to DISCONNECTED
                    if (currentState != HomekitAccessoryServerState.UNKNOWN) {
                        setState(HomekitAccessoryServerState.DISCONNECTED);
                    }
                    throw new HomekitServerException("Failed to start HTTP client", e);
                }
            }

            SRP6Session = Optional.of(new HomekitClientSRP6Session());

            SRP6Session.ifPresent(session -> {
                session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
                session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
                session.setXRoutine(new XRoutineWithUserIdentity());
            });

        } catch (HomekitServerException e) {
            logger.error("{} [{}] : {} - Failed to start HTTP client - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            // Only set state to DISCONNECTED if we're not in UNKNOWN state
            // UNKNOWN can only transition to READY, not to DISCONNECTED
            if (currentState != HomekitAccessoryServerState.UNKNOWN) {
                try {
                    setState(HomekitAccessoryServerState.DISCONNECTED);
                } catch (HomekitServerException ex) {
                    logger.error("{} [{}] : {} - Failed to set state to DISCONNECTED: {}", LOG_PREFIX, getUID(),
                            LOG_ERROR, ex.getMessage());
                }
            }
        }

        startConnectionMonitor();

        // testHttpConnection();
    }

    @Override
    protected void cleanupResources() throws HomekitServerException {
        super.cleanupResources();
    }

    @Override
    public void start() throws HomekitServerException {
        super.start();

        // After the base class has set the state to READY, we can now set it to CONNECTED
        // since the HTTP client has been initialized successfully
        try {
            setState(HomekitAccessoryServerState.CONNECTED);
            logger.debug("{} [{}] : {} - Server state set to CONNECTED after successful initialization", LOG_PREFIX,
                    getUID(), LOG_STATE);
        } catch (HomekitServerException e) {
            logger.error("{} [{}] : {} - Failed to set state to CONNECTED: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            throw e;
        }

        // try {
        // // Create a test request to check connection using the address member
        // final String url = String.format("http://%s:%d", address.getHostAddress(), port);
        // logger.debug("{} [{}] : {} - Testing connection to {}", LOG_PREFIX, getUID(), LOG_INIT, url);
        // final HttpClient currentClient = httpClient;
        // if (currentClient != null) {
        // final Request request = currentClient.newRequest(url);
        // request.onRequestFailure((req, failure) -> {
        // logger.warn("{} [{}] : {} - Connection failed", LOG_PREFIX, getUID(), LOG_STATE);
        // logger.debug("{} [{}] : {} - Failure details: {}", LOG_PREFIX, getUID(), LOG_STATE,
        // failure.getMessage());
        // try {
        // // Only set state to DISCONNECTED if we're not in UNKNOWN state
        // // UNKNOWN can only transition to READY, not to DISCONNECTED
        // if (currentState != HomekitAccessoryServerState.UNKNOWN) {
        // setState(HomekitAccessoryServerState.DISCONNECTED);
        // }
        // } catch (HomekitServerException e) {
        // logger.error("{} [{}] : {} - Failed to set state to DISCONNECTED: {}", LOG_PREFIX, getUID(),
        // LOG_ERROR, e.getMessage());
        // }
        // });
        // request.send();
        // }
        // } catch (InterruptedException | ExecutionException | TimeoutException e) {
        // logger.error("{} [{}] : {} - Failed to start HTTP client - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
        // e.getMessage());
        // logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
        // try {
        // // Only set state to DISCONNECTED if we're not in UNKNOWN state
        // // UNKNOWN can only transition to READY, not to DISCONNECTED
        // if (currentState != HomekitAccessoryServerState.UNKNOWN) {
        // setState(HomekitAccessoryServerState.DISCONNECTED);
        // }
        // } catch (HomekitServerException ex) {
        // logger.error("{} [{}] : {} - Failed to set state to DISCONNECTED: {}", LOG_PREFIX, getUID(), LOG_ERROR,
        // ex.getMessage());
        // }
        // }

        startConnectionMonitor();
    }

    @Override
    public void stop() throws HomekitServerException {
        stopConnectionMonitor();

        logger.info("{} [{}] : {} - Stopping server", LOG_PREFIX, getUID(), LOG_STATE);
        try {
            if (isPaired()) {
                pairRemove();
            }
        } catch (HomekitServerException e) {
            logger.warn("{} [{}] : {} - Error removing pairing during stop - Error: {}", LOG_PREFIX, getUID(), LOG_WARN,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_WARN, e);
            throw e;
        }

        try {
            final HttpClient currentClient = httpClient;
            if (currentClient != null) {
                currentClient.stop();
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} - Error stopping HTTP client - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
        }

        super.stop();
    }

    @Override
    @SuppressWarnings("null")
    public void close() throws Exception {
        logger.info("{} [{}] : {} - Closing server", LOG_PREFIX, getUID(), LOG_STATE);

        try {
            // First stop the server to clean up active connections
            stop();

            // Clean up HTTP client resources
            HttpClient currentClient = httpClient;
            if (currentClient != null) {
                logger.debug("{} [{}] : {} - Destroying HTTP client", LOG_PREFIX, getUID(), LOG_CONFIG);
                currentClient.destroy();
                httpClient = null;
            }

            // Reset pairing state
            resetPairingState();

            // Call super.close() last to ensure proper cleanup of base class resources
            super.close();

            logger.debug("{} [{}] : {} - Server closed successfully", LOG_PREFIX, getUID(), LOG_STATE);
        } catch (Exception e) {
            logger.error("{} [{}] : {} - Error during server close - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            throw e;
        }
    }

    // ========== State Management Methods ==========
    protected HomekitAccessoryServerState getState() {
        return currentState;
    }

    public boolean isPairVerified() {
        return currentState == HomekitAccessoryServerState.PAIR_VERIFIED;
    }

    @Override
    public boolean isPaired() {
        return currentState == HomekitAccessoryServerState.PAIRED
                || currentState == HomekitAccessoryServerState.PAIR_VERIFIED;
    }

    public boolean isConnected() {
        return currentState != HomekitAccessoryServerState.DISCONNECTED;
    }

    @Override
    public boolean isSecure() {
        if (httpClient != null && port != 0) {
            // Null Pointer Access Warning Checked
            // httpClient is explicitly checked for null above, but static analysis still flags the
            // potential null access. We use the non-null variable for clarity and to make the code more robust.
            HttpClient client = Objects.requireNonNull(httpClient);
            Destination destination = client.getDestination(HTTP_SCHEME, address.getHostAddress(), port);

            if (destination instanceof HomekitHttpDestination) {
                return ((HomekitHttpDestination) destination).hasEncryptionKeys();
            }
        }

        return false;
    }

    // ========== Connection Management Methods ==========
    protected void startConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
            logger.debug("{} [{}] : {} - Cancelled existing connection monitor", LOG_PREFIX, getUID(), LOG_STATE);
        }

        // Schedule periodic connection monitoring
        connectionMonitorJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.debug("{} [{}] : {} - Starting connection monitoring cycle", LOG_PREFIX, getUID(), LOG_STATE);
                monitorConnection();
            } catch (Exception e) {
                logger.warn("{} [{}] : {} - Connection monitoring failed - Error: {}", LOG_PREFIX, getUID(), LOG_STATE,
                        e.getMessage());
                logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_STATE, e);
                try {
                    // Only set state to DISCONNECTED if we're not in UNKNOWN state
                    // UNKNOWN can only transition to READY, not to DISCONNECTED
                    if (currentState != HomekitAccessoryServerState.UNKNOWN) {
                        setState(HomekitAccessoryServerState.DISCONNECTED);
                    }
                } catch (HomekitServerException ex) {
                    logger.error("{} [{}] : {} - Failed to set disconnected state: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                            ex.getMessage());
                }
            }
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS);
        logger.debug("{} [{}] : {} - Connection monitor scheduled", LOG_PREFIX, getUID(), LOG_STATE);
    }

    protected void stopConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
            connectionMonitorJob = null;
            logger.debug("{} [{}] : {} - Connection monitor stopped", LOG_PREFIX, getUID(), LOG_STATE);
        }
    }

    private void monitorConnection() throws HomekitServerException, IOException {
        logger.debug("{} [{}] : {} - Monitoring connection state", LOG_PREFIX, getUID(), LOG_STATE);

        // Check if we were previously disconnected
        if (currentState == HomekitAccessoryServerState.DISCONNECTED) {
            logger.info("{} [{}] : {} - Connection was lost, attempting to re-establish", LOG_PREFIX, getUID(),
                    LOG_STATE);

            // If we're not paired at all, proceed with pairing
            if (!isPaired()) {
                logger.info("{} [{}] : {} - Setting up new pairing", LOG_PREFIX, getUID(), LOG_STATE);
                try {
                    pairSetup();
                } catch (HomekitServerException e) {
                    logger.warn("{} [{}] : {} - HomekitPairing setup failed - Error: {}", LOG_PREFIX, getUID(),
                            LOG_STATE, e.getMessage());
                    logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_STATE, e);
                    setState(HomekitAccessoryServerState.MISSING_SETUP_CODE);
                    return;
                }
            }
        }

        // If connected but not paired, attempt to pair
        if (currentState == HomekitAccessoryServerState.CONNECTED && !isPaired()) {
            logger.info("{} [{}] : {} - Connected but not paired, attempting to pair", LOG_PREFIX, getUID(), LOG_STATE);
            try {
                pairSetup();
                if (isPaired()) {
                    pairVerify();
                }
            } catch (HomekitServerException e) {
                logger.warn("{} [{}] : {} - HomekitPairing setup failed - Error: {}", LOG_PREFIX, getUID(), LOG_STATE,
                        e.getMessage());
                logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_STATE, e);
                setState(HomekitAccessoryServerState.MISSING_SETUP_CODE);
                return;
            }
        }

        // If paired, verify the connection
        if (isPaired()) {
            logger.debug("{} [{}] : {} - Verifying connection", LOG_PREFIX, getUID(), LOG_STATE);
            if (!isPairVerified()) {
                logger.info("{} [{}] : {} - Connection not verified, attempting verification", LOG_PREFIX, getUID(),
                        LOG_STATE);
                if (!pairVerify()) {
                    logger.warn("{} [{}] : {} - Connection verification failed", LOG_PREFIX, getUID(), LOG_STATE);
                    setState(HomekitAccessoryServerState.DISCONNECTED);
                    return;
                }

            }

            // After successful verification, check pairing status with accessory
            if (isPairVerified()) {
                checkPairingStatus();
            }

            // Check if connection is still secure
            if (!isSecure()) {
                logger.warn("{} [{}] : {} - Connection is no longer secure", LOG_PREFIX, getUID(), LOG_STATE);
                setState(HomekitAccessoryServerState.DISCONNECTED);
                return;
            }

            setState(HomekitAccessoryServerState.CONNECTED);
            logger.debug("{} [{}] : {} - Connection verified and secure", LOG_PREFIX, getUID(), LOG_STATE);
        }
    }

    // ========== HomekitPairing Methods ==========
    @Override
    public void pairSetup() throws HomekitServerException {
        logger.info("{} [{}] : {} : Starting pair setup process", LOG_PREFIX, getUID(), LOG_STATE);
        logger.debug("{} [{}] : {} : Current state: {}", LOG_PREFIX, getUID(), LOG_STATE, currentState);

        // Check if already in pairing process
        if (currentState == HomekitAccessoryServerState.PAIR_SETUP_INITIAL
                || currentState == HomekitAccessoryServerState.PAIR_SETUP_SRP
                || currentState == HomekitAccessoryServerState.PAIR_SETUP_VERIFY
                || currentState == HomekitAccessoryServerState.PAIR_SETUP_EXCHANGE) {
            logger.debug("{} [{}] : {} : Already in pairing process (state: {}), skipping", LOG_PREFIX, getUID(),
                    LOG_STATE, currentState);
            return;
        }

        // Ensure server is in proper state before pairing
        if (currentState == HomekitAccessoryServerState.UNKNOWN) {
            logger.info("{} [{}] : {} : Server in UNKNOWN state, starting server first", LOG_PREFIX, getUID(),
                    LOG_STATE);
            try {
                start();
                logger.debug("{} [{}] : {} : Server started successfully, current state: {}", LOG_PREFIX, getUID(),
                        LOG_STATE, currentState);
            } catch (HomekitServerException e) {
                logger.error("{} [{}] : {} : Failed to start server before pairing: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, e.getMessage());
                throw e;
            }
        }

        // Validate setup code
        if (setupCode.isEmpty()) {
            logger.warn("{} [{}] : {} : Unable to pair with {}:{} because no setup code is set", LOG_PREFIX, getUID(),
                    LOG_STATE, address.getHostAddress(), port);
            setState(HomekitAccessoryServerState.MISSING_SETUP_CODE);
            return;
        }

        // Initialize pairing state
        resetPairingState();
        setState(HomekitAccessoryServerState.PAIR_SETUP_INITIAL);
        logger.debug("{} [{}] : {} : HomekitPairing state reset, starting authentication", LOG_PREFIX, getUID(),
                LOG_STATE);

        try {
            // Stage 0: Initial Setup
            logger.debug("{} [{}] : {} : Stage {} : Starting - Initial Setup", LOG_PREFIX, getUID(), LOG_STATE, 0);
            StageResult stage0Result = executePairingSetupStage(0, () -> doPairSetupStage0());
            if (stage0Result.isFailure()) {
                handlePairingFailure(0, stage0Result);
                return;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 0);

            // Stage 1: SRP Protocol Exchange
            logger.debug("{} [{}] : {} : Stage {} : Starting - SRP Protocol Exchange", LOG_PREFIX, getUID(), LOG_STATE,
                    1);
            setState(HomekitAccessoryServerState.PAIR_SETUP_SRP);
            StageResult stage1Result = executePairingSetupStage(1, () -> doPairSetupStage1(stage0Result));
            if (stage1Result.isFailure()) {
                handlePairingFailure(1, stage1Result);
                return;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 1);

            // Stage 2: Verify Proof
            logger.debug("{} [{}] : {} : Stage {} : Starting - Verify Proof", LOG_PREFIX, getUID(), LOG_STATE, 2);
            setState(HomekitAccessoryServerState.PAIR_SETUP_VERIFY);
            StageResult stage2Result = executePairingSetupStage(2, () -> doPairSetupStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handlePairingFailure(2, stage2Result);
                return;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 2);

            // Stage 3: Exchange Keys
            logger.debug("{} [{}] : {} : Stage {} : Starting - Exchange Keys", LOG_PREFIX, getUID(), LOG_STATE, 3);
            setState(HomekitAccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage3Result = executePairingSetupStage(3, () -> doPairSetupStage3(stage2Result));
            if (stage3Result.isFailure()) {
                handlePairingFailure(3, stage3Result);
                return;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 3);

            // HomekitPairing completed successfully
            setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
            logger.info("{} [{}] : {} : Pair setup completed successfully", LOG_PREFIX, getUID(), LOG_STATE);
            logger.debug("{} [{}] : {} : Final state: {}", LOG_PREFIX, getUID(), LOG_STATE, currentState);

        } catch (HomekitServerException e) {
            logger.error("{} [{}] : {} : Pair setup failed with error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} : Homekit exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            resetPairingState();
            setState(HomekitAccessoryServerState.UNPAIRED);
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Unexpected error during pair setup: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            resetPairingState();
            setState(HomekitAccessoryServerState.UNPAIRED);
        }
    }

    @Override
    public boolean pairVerify() throws HomekitServerException {
        logger.info("{} [{}] : {} : Starting pair verify process", LOG_PREFIX, getUID(), LOG_STATE);
        logger.debug("{} [{}] : {} : Current state: {}", LOG_PREFIX, getUID(), LOG_STATE, currentState);

        // Check if already in verification process
        if (currentState == HomekitAccessoryServerState.PAIR_SETUP_VERIFY
                || currentState == HomekitAccessoryServerState.PAIR_SETUP_EXCHANGE) {
            logger.debug("{} [{}] : {} : Already in verification process (state: {}), skipping", LOG_PREFIX, getUID(),
                    LOG_STATE, currentState);
            return true; // Assume verification is in progress and will complete
        }

        // Ensure server is in proper state before verification
        if (currentState == HomekitAccessoryServerState.UNKNOWN) {
            logger.info("{} [{}] : {} : Server in UNKNOWN state, starting server first", LOG_PREFIX, getUID(),
                    LOG_STATE);
            try {
                start();
                logger.debug("{} [{}] : {} : Server started successfully, current state: {}", LOG_PREFIX, getUID(),
                        LOG_STATE, currentState);
            } catch (HomekitServerException e) {
                logger.error("{} [{}] : {} : Failed to start server before verification: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, e.getMessage());
                throw e;
            }
        }

        // Reset verification state
        resetVerificationState();
        setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("{} [{}] : {} : Verification state reset", LOG_PREFIX, getUID(), LOG_STATE);

        try {
            // Stage 0: Initial Verification
            logger.debug("{} [{}] : {} : Stage {} : Starting - Initial Verification", LOG_PREFIX, getUID(), LOG_STATE,
                    0);
            StageResult stage0Result = executePairingVerifyStage(0, () -> doPairVerifyStage0());
            if (stage0Result.isFailure()) {
                handleVerificationFailure(0, stage0Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 0);

            // Stage 1: Exchange Keys
            logger.debug("{} [{}] : {} : Stage {} : Starting - Exchange Keys", LOG_PREFIX, getUID(), LOG_STATE, 1);
            setState(HomekitAccessoryServerState.PAIR_SETUP_VERIFY);

            // Handle stage 1 with authentication error handling
            final StageResult stage1Result = handleStage1Verification(stage0Result);
            if (stage1Result.isFailure()) {
                handleVerificationFailure(1, stage1Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 1);

            // Stage 2: Final Verification
            logger.debug("{} [{}] : {} : Stage {} : Starting - Final Verification", LOG_PREFIX, getUID(), LOG_STATE, 2);
            setState(HomekitAccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage2Result = executePairingVerifyStage(2, () -> doPairVerifyStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handleVerificationFailure(2, stage2Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{} [{}] : {} : Stage {} : Completed successfully", LOG_PREFIX, getUID(), LOG_STATE, 2);

            // Verification completed successfully
            handlePairingVerification(true);
            logger.info("{} [{}] : {} : Pair verification completed successfully", LOG_PREFIX, getUID(), LOG_STATE);
            logger.debug("{} [{}] : {} : Final state: {}", LOG_PREFIX, getUID(), LOG_STATE, currentState);
            return true;

        } catch (HomekitServerException e) {
            logger.error("{} [{}] : {} : Pair verification failed with error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} : Homekit exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            handlePairingVerification(false);
            return false;
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Unexpected error during pair verification: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, e.getMessage());
            logger.debug("{} [{}] : {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            handlePairingVerification(false);
            return false;
        }
    }

    @Override
    public void pairRemove() throws HomekitServerException {
        logger.debug("{} [{}] : {} - Starting pair remove", LOG_PREFIX, getUID(), LOG_STATE);

        try {
            // Send pair remove request
            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            try {
                encoder.add(HomekitMessage.STATE, (short) 0x01);
                encoder.add(HomekitMessage.METHOD, HomekitMethod.REMOVE_PAIRING.getKey());
                encoder.add(HomekitMessage.IDENTIFIER, getPairingId());
            } catch (IOException e) {
                logger.error("{} [{}] : {} - Failed to prepare remove pairing request - Error: {}", LOG_PREFIX,
                        getUID(), LOG_ERROR, e.getMessage());
                throw new HomekitServerException("Failed to prepare remove pairing request", e);
            }

            Future<StageResult> stageFuture = sendPairing(encoder.toByteArray());
            StageResult stageResult = Objects.requireNonNull(stageFuture.get(), "StageResult is null");

            DecodeResult decodeResult = stageResult.getDecodeResult()
                    .orElseThrow(() -> new HomekitServerException("No decode result in response"));

            // Verify response state
            short state = decodeResult.getByte(HomekitMessage.STATE);
            if (state != 2) {
                logger.error("{} [{}] : {} - Invalid state in remove pairing response: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, state);
                throw new HomekitServerException("Wrong STATE");
            }

            // Check for errors in response
            if (decodeResult.getBytes(HomekitMessage.ERROR).length > 0) {
                HomekitErrorCode error = HomekitErrorCode.fromCode(decodeResult.getByte(HomekitMessage.ERROR));
                logger.warn("{} [{}] : {} - HomekitAccessory failed to remove pairing: {}", LOG_PREFIX, getUID(),
                        LOG_STATE, error);
                setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                return;
            }

            // Remove all pairings
            logger.debug("{} [{}] : {} - Removing all pairings", LOG_PREFIX, getUID(), LOG_STATE);
            for (HomekitPairing pairing : getPairings()) {
                removePairing(pairing.getDestinationId());
            }

            // Update state
            setState(HomekitAccessoryServerState.UNPAIRED);
            logger.info("{} [{}] : {} - Successfully removed pairing", LOG_PREFIX, getUID(), LOG_STATE);
            logger.debug("{} [{}] : {} - Final state: {}", LOG_PREFIX, getUID(), LOG_STATE, currentState);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("{} [{}] : {} - Error during pair remove: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
            throw new HomekitServerException("Failed to remove pairing", e);
        }
    }

    // ========== HomekitPairing Stage Methods ==========
    private void resetPairingState() {
        logger.debug("{} [{}] : {} - Resetting pairing state", LOG_PREFIX, getUID(), LOG_STATE);
        sessionKey = new byte[0];
        sharedSecret = new byte[0];
        clientPublicKey = new byte[0];
        clientPrivateKey = new byte[0];

        // Reset SRP6 session to allow fresh pairing attempts
        SRP6Session = Optional.empty();
        logger.debug("{} [{}] : {} - SRP6 session cleared for fresh pairing", LOG_PREFIX, getUID(), LOG_STATE);

        logger.debug("{} [{}] : {} - HomekitPairing state reset completed", LOG_PREFIX, getUID(), LOG_STATE);
    }

    private void resetVerificationState() {
        logger.debug("{} [{}] : {} - Resetting verification state", LOG_PREFIX, getUID(), LOG_STATE);
        sessionKey = new byte[0];
        sharedSecret = new byte[0];
        clientPublicKey = new byte[0];
        clientPrivateKey = new byte[0];
        logger.debug("{} [{}] : {} - Verification state reset completed", LOG_PREFIX, getUID(), LOG_STATE);
    }

    private StageResult executePairingSetupStage(int stage, PairingStageExecutor executor)
            throws HomekitServerException, InterruptedException, ExecutionException, IOException {
        logger.debug("{} [{}] : {} - Executing pair setup Stage {} :  preparing payload", LOG_PREFIX, getUID(),
                LOG_STATE, stage);

        long startTime = System.currentTimeMillis();
        byte[] payload = executor.execute();

        logger.debug("{} [{}] : {} : Stage {} :  sending payload", LOG_PREFIX, getUID(), LOG_STATE, stage);
        Future<StageResult> stageFuture = sendPairSetupStage(payload);

        try {
            StageResult result = Objects.requireNonNull(
                    stageFuture.get(PAIRING_STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS), "StageResult is null");

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("{} [{}] : {} : Stage {} :  received response in {}ms, success: {}", LOG_PREFIX, getUID(),
                    LOG_STATE, stage, duration, !result.isFailure());

            return result;
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("{} [{}] : {} : Stage {} :  timed out after {}ms (limit: {}s)", LOG_PREFIX, getUID(),
                    LOG_ERROR, stage, duration, PAIRING_STAGE_TIMEOUT_SECONDS);

            // Cancel the future to prevent resource leaks
            stageFuture.cancel(true);

            // Return a failure result with timeout message
            return new StageResult(
                    "Pairing setup stage " + stage + " timed out after " + PAIRING_STAGE_TIMEOUT_SECONDS + " seconds");
        }
    }

    private StageResult executePairingVerifyStage(int stage, PairingStageExecutor executor)
            throws HomekitServerException, InterruptedException, ExecutionException, IOException {
        logger.debug("{} [{}] : {} - Executing pair verify Stage {} :  preparing payload", LOG_PREFIX, getUID(),
                LOG_STATE, stage);

        long startTime = System.currentTimeMillis();
        byte[] payload = executor.execute();

        logger.debug("{} [{}] : {} : Stage {} :  sending payload", LOG_PREFIX, getUID(), LOG_STATE, stage);
        Future<StageResult> stageFuture = sendPairVerifyStage(payload);

        try {
            StageResult result = Objects.requireNonNull(
                    stageFuture.get(PAIRING_STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS), "StageResult is null");

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("{} [{}] : {} : Stage {} :  received response in {}ms, success: {}", LOG_PREFIX, getUID(),
                    LOG_STATE, stage, duration, !result.isFailure());

            return result;
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("{} [{}] : {} : Stage {} :  timed out after {}ms (limit: {}s)", LOG_PREFIX, getUID(),
                    LOG_ERROR, stage, duration, PAIRING_STAGE_TIMEOUT_SECONDS);

            // Cancel the future to prevent resource leaks
            stageFuture.cancel(true);

            // Return a failure result with timeout message
            return new StageResult(
                    "Pairing verify stage " + stage + " timed out after " + PAIRING_STAGE_TIMEOUT_SECONDS + " seconds");
        }
    }

    private void handlePairingFailure(int stage, StageResult result) throws HomekitServerException {
        logger.debug("{} [{}] : {} - Handling failure for stage {}", LOG_PREFIX, getUID(), LOG_STATE, stage);

        // Reset pairing state to clear SRP6 session for retry attempts
        resetPairingState();

        if (result.error.isPresent()) {
            if (result.error.get() == HomekitErrorCode.UNAVAILABLE) {
                logger.warn(
                        "{} [{}] : {} - Pair setup failed - accessory is not available for pairing (already paired)",
                        LOG_PREFIX, getUID(), LOG_STATE);
                logger.debug("{} [{}] : {} - HomekitAccessory reported UNAVAILABLE error", LOG_PREFIX, getUID(),
                        LOG_STATE);
            } else {
                logger.warn("{} [{}] : {} - Pair setup failed at stage {} with error: {}", LOG_PREFIX, getUID(),
                        LOG_STATE, stage, result.error.get());
                logger.debug("{} [{}] : {} : Stage {} : error details: {}", LOG_PREFIX, getUID(), LOG_STATE, stage,
                        result.error.get());
            }
        } else {
            logger.warn("{} [{}] : {} - Pair setup failed at stage {} with message: {}", LOG_PREFIX, getUID(), LOG_WARN,
                    stage, result.message.orElse(""));
            logger.debug("{} [{}] : {} : Stage {} : failure message details: {}", LOG_PREFIX, getUID(), LOG_STATE,
                    stage, result.message.orElse(""));
        }
        setState(HomekitAccessoryServerState.UNPAIRED);
        logger.debug("{} [{}] : {} - State set to UNPAIRED after failure", LOG_PREFIX, getUID(), LOG_STATE);
    }

    @FunctionalInterface
    private interface PairingStageExecutor {
        byte[] execute() throws IOException, HomekitServerException;
    }

    protected byte[] doPairSetupStage0() throws IOException, HomekitServerException {
        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x01);
        encoder.add(HomekitMessage.METHOD, HomekitMethod.PAIR_SETUP_WITH_AUTH.getKey());

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage1(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair setup stage", LOG_PREFIX, getUID(), LOG_STATE, 1);
        DecodeResult decodeResult = stageResult.getDecodeResult()
                .orElseThrow(() -> new HomekitServerException("Missing decode result for stage 1"));

        short state = decodeResult.getByte(HomekitMessage.STATE);
        if (state != 2) {
            throw new HomekitServerException("Wrong STATE");
        }

        BigInteger serverPublicKey = decodeResult.getBigInt(HomekitMessage.PUBLIC_KEY);
        logger.debug("{} [{}] : {} : Stage {} : Public key received", LOG_PREFIX, getUID(), LOG_STATE, 1);

        BigInteger salt = decodeResult.getBigInt(HomekitMessage.SALT);
        logger.debug("{} [{}] : {} : Stage {} : Salt received", LOG_PREFIX, getUID(), LOG_STATE, 1);

        if (SRP6Session.isEmpty()) {
            logger.debug("{} [{}] : {} : Stage {} : Creating new SRP6 session", LOG_PREFIX, getUID(), LOG_STATE, 1);
            SRP6Session = Optional.of(new HomekitClientSRP6Session());
            SRP6Session.ifPresent(session -> {
                session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
                session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
                session.setXRoutine(new XRoutineWithUserIdentity());
            });
        } else {
            logger.debug("{} [{}] : {} : Stage {} : Using existing SRP6 session - State: {}", LOG_PREFIX, getUID(),
                    LOG_STATE, 1, SRP6Session.map(session -> session.getState().toString()).orElse("UNKNOWN"));
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();

        // Ensure we have a valid session and call step1
        if (SRP6Session.isPresent()) {
            HomekitClientSRP6Session session = SRP6Session.get();
            logger.debug("{} [{}] : {} : Stage {} : Calling SRP6 step1 - Current state: {}", LOG_PREFIX, getUID(),
                    LOG_STATE, 1, session.getState());

            // Check if session is in wrong state and reset if needed
            if (session.getState() != HomekitClientSRP6Session.State.INIT) {
                logger.warn("{} [{}] : {} : Stage {} : SRP6 session in wrong state ({}), resetting session", LOG_PREFIX,
                        getUID(), LOG_STATE, 1, session.getState());
                session = new HomekitClientSRP6Session();
                session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
                session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
                session.setXRoutine(new XRoutineWithUserIdentity());
                SRP6Session = Optional.of(session);
                logger.debug("{} [{}] : {} : Stage {} : Created fresh SRP6 session", LOG_PREFIX, getUID(), LOG_STATE,
                        1);
            }

            // Call step1 on the session
            session.step1("Pair-Setup", setupCode);
            logger.debug("{} [{}] : {} : Stage {} : SRP6 step1 completed - New state: {}", LOG_PREFIX, getUID(),
                    LOG_STATE, 1, session.getState());

            SRP6ClientCredentials clientCredentials = null;
            try {
                clientCredentials = session.step2(HomekitEncryptionEngine.SRP6Params, salt, serverPublicKey);

            } catch (SRP6Exception e) {
                logger.error("{} [{}] : {} : Stage {} : SRP6 step 2 failed - Error: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, 1,
                        e.getMessage());
                logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 1, e);
                throw new HomekitServerException("SRP6 step 2 failed", e);
            }

            BigInteger clientPublicKey = clientCredentials.A;
            logger.debug("{} [{}] : {} : Stage {} : Client public key generated", LOG_PREFIX, getUID(), LOG_STATE, 1);

            BigInteger clientProof = clientCredentials.M1;
            logger.debug("{} [{}] : {} : Stage {} : Client proof generated", LOG_PREFIX, getUID(), LOG_STATE, 1);

            encoder.add(HomekitMessage.STATE, (short) 0x03);
            encoder.add(HomekitMessage.PUBLIC_KEY, clientPublicKey);
            encoder.add(HomekitMessage.PROOF, clientProof);
        } else {
            throw new HomekitServerException("SRP6 session not found");
        }

        return encoder.toByteArray();
    }

    private StageResult handleStage1Verification(StageResult stage0Result)
            throws HomekitServerException, InterruptedException, ExecutionException, IOException {
        try {
            return executePairingSetupStage(1, () -> doPairVerifyStage1(stage0Result));
        } catch (HomekitServerException e) {
            logger.error("{} [{}] : {} - Authentication error in stage 1: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Sending authentication error to accessory", LOG_PREFIX, getUID(), LOG_STATE);

            // Send authentication error to accessory
            Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(HomekitMessage.STATE, (short) 0x03);
            encoder.add(HomekitMessage.ERROR, HomekitErrorCode.AUTHENTICATION);

            Future<StageResult> errorFuture = sendPairVerifyStage(encoder.toByteArray());
            return Objects.requireNonNull(errorFuture.get(), "StageResult is null");
        }
    }

    protected byte[] doPairSetupStage2(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair setup stage", LOG_PREFIX, getUID(), LOG_STATE, 2);
        DecodeResult decodeResult = stageResult.getDecodeResult()
                .orElseThrow(() -> new HomekitServerException("Missing decode result for stage 2"));

        short state = decodeResult.getByte(HomekitMessage.STATE);
        if (state != 4) {
            throw new HomekitServerException("Wrong STATE");
        }

        BigInteger serverProof = decodeResult.getBigInt(HomekitMessage.PROOF);
        logger.debug("{} [{}] : {} : Stage {} : Proof received", LOG_PREFIX, getUID(), LOG_STATE, 2);

        MessageDigest digest;
        BigInteger SRPSessionKey;

        // Step 3: Verify Proof
        if (SRP6Session.isPresent()) {
            @SuppressWarnings("null") // get() is safe after isPresent() check
            var session = SRP6Session.get();
            try {
                session.step3(serverProof);
                digest = session.getCryptoParams().getMessageDigestInstance();
                SRPSessionKey = session.getSessionKey(false);
                logger.debug("{} [{}] : {} : Stage {} : SRP session key generated", LOG_PREFIX, getUID(), LOG_STATE, 2);
            } catch (SRP6Exception e) {
                logger.error("{} [{}] : {} : Stage {} : SRP6 step 3 failed - Error: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, 2, e.getMessage());
                logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 2, e);
                throw new HomekitServerException("SRP6 step 3 failed", e);
            }
        } else {
            throw new HomekitServerException("SRP6 session not found");
        }

        sharedSecret = digest.digest(HomekitByte.toByteArray(SRPSessionKey));
        logger.debug("{} [{}] : {} : Stage {} : Shared secret generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.debug("{} [{}] : {} : Stage {} : Session key generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.debug("{} [{}] : {} : Stage {} : Client device X generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

        HomekitEdsaSigner signer = new HomekitEdsaSigner(secretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = HomekitByte.joinBytes(clientDeviceX, getPairingId(), clientLongtermPublicKey);
        byte[] clientSignature = null;
        try {
            clientSignature = signer.sign(clientDeviceInfo);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to sign client device info - Error: {}", LOG_PREFIX,
                    getUID(), LOG_ERROR, 2, e.getMessage());
            logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 2, e);
            throw new HomekitServerException("Failed to sign client device info", e);
        }
        logger.debug("{} [{}] : {} : Stage {} : Client signature generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.IDENTIFIER, getPairingId());
        encoder.add(HomekitMessage.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(HomekitMessage.SIGNATURE, clientSignature);

        HomekitChachaEncoder chachaEncoder = new HomekitChachaEncoder(sessionKey,
                "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] encrypedDataWithTag = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x05);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, encrypedDataWithTag);

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage3(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair setup stage", LOG_PREFIX, getUID(), LOG_STATE, 3);
        DecodeResult decodeResult = stageResult.getDecodeResult()
                .orElseThrow(() -> new HomekitServerException("Missing decode result for stage 3"));

        short state = decodeResult.getByte(HomekitMessage.STATE);
        if (state != 6) {
            throw new HomekitServerException("Wrong STATE");
        }

        byte[] encryptedData = new byte[decodeResult.getLength(HomekitMessage.ENCRYPTED_DATA) - 16];
        decodeResult.getBytes(HomekitMessage.ENCRYPTED_DATA, encryptedData, 0);
        logger.debug("{}Extracted {} bytes of encrypted data", LOG_STATE, encryptedData.length);
        assert encryptedData != null : "Encrypted data should not be null";

        byte[] tag = new byte[16];
        decodeResult.getBytes(HomekitMessage.ENCRYPTED_DATA, tag, encryptedData.length);
        logger.debug("{}Extracted 16-byte authentication tag", LOG_STATE);
        assert tag != null : "Authentication tag should not be null";

        HomekitChachaDecoder chachaDecoder = new HomekitChachaDecoder(sessionKey,
                "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(tag, encryptedData);
        logger.debug("{} [{}] : {} : Stage {} : Plaintext decoded", LOG_PREFIX, getUID(), LOG_STATE, 3);

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] serverPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
        byte[] serverLongTermPublicKey = d.getBytes(HomekitMessage.PUBLIC_KEY);
        byte[] serverSignature = d.getBytes(HomekitMessage.SIGNATURE);

        // Defensive validation of TLV-decoded data for robust error handling
        // Static analysis indicates these cannot be null at this point, but we maintain validation logic
        // for defensive programming
        assert serverPairingIdentifier != null : "Server pairing identifier should not be null";
        assert serverLongTermPublicKey != null : "Server long term public key should not be null";
        assert serverSignature != null : "Server signature should not be null";

        logger.trace("{}Validating pairing data - all required fields present", LOG_STATE);
        logger.debug("{} [{}] : {} : Stage {} : Server pairing identifier received", LOG_PREFIX, getUID(),
                LOG_STATE, 3);
        logger.debug("{} [{}] : {} : Stage {} : Server long term public key received", LOG_PREFIX, getUID(), LOG_STATE,
                3);
        logger.debug("{} [{}] : {} : Stage {} : Server signature received", LOG_PREFIX, getUID(), LOG_STATE,
                3);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret,
                "Pair-Setup-HomekitAccessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-HomekitAccessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] serverDeviceX = new byte[32];
        hkdf.generateBytes(serverDeviceX, 0, 32);

        byte[] serverDeviceInfo = HomekitByte.joinBytes(serverDeviceX, serverPairingIdentifier,
                serverLongTermPublicKey);
        logger.debug("{} [{}] : {} : Stage {} : HomekitAccessory device info generated", LOG_PREFIX, getUID(),
                LOG_STATE, 3);

        try {
            if (!new HomekitEdsaVerifier(serverLongTermPublicKey).verify(serverDeviceInfo, serverSignature)) {
                logger.error("{} [{}] : {} : Stage {} : Signature verification failed", LOG_PREFIX, getUID(), LOG_ERROR,
                        3);
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Stage {} : Signature verification failed - Error: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, 3, e.getMessage());
            logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 3, e);
            throw new HomekitServerException("Signature verification failed", e);
        }

        addPairing(serverPairingIdentifier, serverLongTermPublicKey);
        SRP6Session = Optional.empty();

        return new byte[0];
    }

    protected byte[] doPairVerifyStage0() throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Starting pair verify stage", LOG_PREFIX, getUID(), LOG_STATE, 0);

        clientPublicKey = new byte[32];
        clientPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(clientPrivateKey);
        Curve25519.keygen(clientPublicKey, null, clientPrivateKey);

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x01);
        encoder.add(HomekitMessage.PUBLIC_KEY, clientPublicKey);

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage1(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair verify stage", LOG_PREFIX, getUID(), LOG_STATE, 1);
        DecodeResult decodeResult = stageResult.getDecodeResult()
                .orElseThrow(() -> new HomekitServerException("Missing decode result for verify stage 1"));

        short state = decodeResult.getByte(HomekitMessage.STATE);
        if (state != 2) {
            throw new HomekitServerException("Wrong STATE");
        }

        byte[] destinationPublicKey = decodeResult.getBytes(HomekitMessage.PUBLIC_KEY);
        logger.debug("{} [{}] : {} : Stage {} : Destination public key received", LOG_PREFIX, getUID(), LOG_STATE, 1);

        byte[] messageData = new byte[decodeResult.getLength(HomekitMessage.ENCRYPTED_DATA) - 16];
        decodeResult.getBytes(HomekitMessage.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        decodeResult.getBytes(HomekitMessage.ENCRYPTED_DATA, authTagData, messageData.length);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, clientPrivateKey, destinationPublicKey);
        logger.debug("{} [{}] : {} : Stage {} : Shared secret generated", LOG_PREFIX, getUID(), LOG_STATE, 1);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.debug("{} [{}] : {} : Stage {} : Session key generated", LOG_PREFIX, getUID(), LOG_STATE, 1);

        byte[] plaintext = null;
        HomekitChachaDecoder chachaDecoder = new HomekitChachaDecoder(sessionKey,
                "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        try {
            plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
            logger.debug("{} [{}] : {} : Stage {} : Plaintext decoded", LOG_PREFIX, getUID(), LOG_STATE, 1);
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to decode ciphertext - Error: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, 1, e.getMessage());
            logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 1, e);
            throw new HomekitServerException("Ciphertext decoding failed", e);
        }

        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(HomekitMessage.IDENTIFIER);
        logger.debug("{} [{}] : {} : Stage {} : Destination pairing identifier received", LOG_PREFIX, getUID(),
                LOG_STATE, 1);
        byte[] accessorySignature = d.getBytes(HomekitMessage.SIGNATURE);
        logger.debug("{} [{}] : {} : Stage {} : HomekitAccessory signature received", LOG_PREFIX, getUID(), LOG_STATE,
                1);

        Optional<HomekitPairing> accessoryPairing = getPairing(destinationPairingIdentifier);

        if (accessoryPairing.isEmpty()) {
            logger.error("{} [{}] : {} : Stage {} : HomekitAccessory is not paired", LOG_PREFIX, getUID(), LOG_ERROR,
                    1);
            throw new HomekitServerException("HomekitAccessory is not paired");
        } else {
            logger.debug("{} [{}] : {} : Stage {} : HomekitAccessory pairing found", LOG_PREFIX, getUID(), LOG_STATE,
                    1);
        }

        byte[] accessoryDeviceInfo = HomekitByte.joinBytes(destinationPublicKey, destinationPairingIdentifier,
                clientPublicKey);

        try {
            @SuppressWarnings("null")
            HomekitPairing pairing = accessoryPairing
                    .orElseThrow(() -> new HomekitServerException("Accessory pairing not found"));
            byte[] publicKey = pairing.getPublicKey();
            boolean signatureVerification = new HomekitEdsaVerifier(publicKey).verify(accessoryDeviceInfo,
                    accessorySignature);
            if (!signatureVerification) {
                logger.error("{} [{}] : {} : Stage {} : Signature verification failed", LOG_PREFIX, getUID(), LOG_ERROR,
                        1);
                throw new HomekitServerException("Signature verification failed");
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} : Stage {} : Signature verification failed - Error: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, 1, e.getMessage());
            logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 1, e);
            throw new HomekitServerException("Signature verification failed", e);
        }

        byte[] clientDeviceInfo = HomekitByte.joinBytes(clientPublicKey, getPairingId(), destinationPublicKey);

        byte[] clientSignature = null;
        try {
            logger.debug("{} [{}] : {} : Stage {} : Signing client device info", LOG_PREFIX, getUID(), LOG_STATE, 1);
            clientSignature = new HomekitEdsaSigner(secretKey).sign(clientDeviceInfo);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{} [{}] : {} : Stage {} : Failed to sign client device info - Error: {}", LOG_PREFIX,
                    getUID(), LOG_ERROR, 1, e.getMessage());
            logger.debug("{} [{}] : {} : Stage {} : Exception details", LOG_PREFIX, getUID(), LOG_ERROR, 1, e);
            throw new HomekitServerException("Failed to sign client device info", e);
        }

        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.IDENTIFIER, getPairingId());
        encoder.add(HomekitMessage.SIGNATURE, clientSignature);
        plaintext = encoder.toByteArray();

        HomekitChachaEncoder chacha = new HomekitChachaEncoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 0x03);
        encoder.add(HomekitMessage.ENCRYPTED_DATA, ciphertext);

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage2(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{} [{}] : {} : Stage {} : Executing pair verify stage", LOG_PREFIX, getUID(), LOG_STATE, 2);
        DecodeResult decodeResult = stageResult.getDecodeResult()
                .orElseThrow(() -> new HomekitServerException("Missing decode result for verify stage 2"));

        if (stageResult.result.isPresent()) {
            short state = decodeResult.getByte(HomekitMessage.STATE);
            if (state != 4) {
                throw new HomekitServerException("Wrong STATE");
            }

            byte[] writeKey = HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret);
            logger.debug("{} [{}] : {} : Stage {} : Write key generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

            byte[] readKey = HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret);
            logger.debug("{} [{}] : {} : Stage {} : Read key generated", LOG_PREFIX, getUID(), LOG_STATE, 2);

            if (httpClient != null && stageResult.result.isPresent()) {
                Result result = stageResult.result.get();
                if (result.getRequest() != null) {
                    @SuppressWarnings("null")
                    Destination dest = httpClient.getDestination(result.getRequest().getScheme(),
                            result.getRequest().getHost(), result.getRequest().getPort());
                    if (dest != null && dest instanceof HomekitHttpDestination) {
                        @SuppressWarnings("resource")
                        HomekitHttpDestination destination = (HomekitHttpDestination) dest;
                        logger.debug("{} [{}] : {} : Stage {} : Setting encryption keys on destination", LOG_PREFIX,
                                getUID(), LOG_STATE, 2);
                        destination.setEncryptionKeys(readKey, writeKey);
                    }
                }
            }
        }

        return new byte[0];
    }

    // ========== Communication Methods ==========
    protected Future<StageResult> sendPairSetupStage(byte[] request)
            throws InterruptedException, HomekitServerException {
        return sendStage(request, "/pair-setup");
    }

    protected Future<StageResult> sendPairVerifyStage(byte[] request)
            throws InterruptedException, HomekitServerException {
        return sendStage(request, "/pair-verify");
    }

    protected Future<StageResult> sendPairing(byte[] request) throws InterruptedException, HomekitServerException {
        return sendStage(request, "/pairings");
    }

    protected Future<StageResult> sendStage(byte[] request, String url)
            throws InterruptedException, HomekitServerException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{} [{}] : {} - Failed to create URI - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e1.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e1);
        }

        if (uri == null) {
            throw new HomekitServerException("URI must not be null");
        }

        CompletableFuture<StageResult> completableFuture = new CompletableFuture<>();

        // Debug: Check HTTP client state
        logger.debug("{} [{}] : {} - sendStage called - httpClient null: {}, uri: {}", LOG_PREFIX, getUID(), LOG_STATE,
                httpClient == null, uri != null ? uri.toString() : "null");

        if (httpClient != null) {
            logger.debug("{} [{}] : {} - Sending HTTP POST request to: {}", LOG_PREFIX, getUID(), LOG_STATE,
                    uri.toString());
            httpClient.newRequest(uri.toString()).method(HttpMethod.POST)
                    .content(new BytesContentProvider(request), "application/pairing+tlv8")
                    .header(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString())
                    .timeout(30, TimeUnit.SECONDS) // Add explicit timeout
                    .onRequestBegin(req -> {
                        logger.debug("{} [{}] : {} - HTTP request begin: {}", LOG_PREFIX, getUID(), LOG_STATE,
                                req.getURI());
                    }).onRequestSuccess(req -> {
                        logger.debug("{} [{}] : {} - HTTP request sent successfully", LOG_PREFIX, getUID(), LOG_STATE);
                    }).onRequestFailure((req, failure) -> {
                        logger.error("{} [{}] : {} - HTTP request failed to send - Error: {}", LOG_PREFIX, getUID(),
                                LOG_ERROR, failure.getMessage());
                        logger.debug("{} [{}] : {} - HTTP request failure details", LOG_PREFIX, getUID(), LOG_ERROR,
                                failure);
                    }).send(new BufferingResponseListener(8 * 1024 * 1024) {

                        private volatile boolean skipContentProcessing = false;

                        @Override
                        public void onBegin(Response response) {
                            int status = response.getStatus();
                            logger.debug("{} [{}] : {} - HTTP response begun - Status: {}", LOG_PREFIX, getUID(),
                                    LOG_STATE, status);

                            // Handle 401 Unauthorized responses
                            if (status == HttpStatus.UNAUTHORIZED_401) {
                                logger.warn(
                                        "{} [{}] : {} - Received HTTP 401 Unauthorized - pairing may be invalid or expired",
                                        LOG_PREFIX, getUID(), LOG_PAIRING);
                                try {
                                    setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                                } catch (HomekitServerException e) {
                                    logger.error("{} [{}] : {} - Failed to set state after 401: {}", LOG_PREFIX,
                                            getUID(), LOG_ERROR, e.getMessage());
                                }

                                // Set flag to skip content processing and complete immediately
                                skipContentProcessing = true;
                                logger.debug("{} [{}] : {} - Setting skipContentProcessing=true for 401 response",
                                        LOG_PREFIX, getUID(), LOG_STATE);

                                // Complete the future immediately with 401 error
                                StageResult unauthorizedResult = new StageResult(
                                        "HTTP 401 Unauthorized - authentication failed");
                                completableFuture.complete(unauthorizedResult);
                                return; // Don't call super.onBegin() to avoid further processing
                            }

                            super.onBegin(response);
                        }

                        @Override
                        public void onContent(Response response, ByteBuffer content) {
                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping content processing due to 401 response - {} bytes ignored",
                                        LOG_PREFIX, getUID(), LOG_STATE, content.remaining());
                                return; // Skip content processing for 401 responses
                            }

                            // log the number of bytes in the content
                            logger.debug("{} [{}] : {} - Received {} bytes", LOG_PREFIX, getUID(), LOG_STATE,
                                    content.remaining());
                            super.onContent(response, content);
                        }

                        @Override
                        public void onFailure(Response response, Throwable failure) {
                            logger.error("{} [{}] : {} - HTTP request failed - Error: {}", LOG_PREFIX, getUID(),
                                    LOG_ERROR, failure.getMessage());
                            logger.debug("{} [{}] : {} - HTTP failure details", LOG_PREFIX, getUID(), LOG_ERROR,
                                    failure);
                            super.onFailure(response, failure);
                        }

                        @Override
                        public void onComplete(@Nullable Result result) {
                            // Null Pointer Access Warning Checked
                            // We explicitly check result for null before accessing its methods
                            // This prevents NPE and satisfies static analysis

                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping onComplete processing due to 401 response - future already completed",
                                        LOG_PREFIX, getUID(), LOG_STATE);
                                return; // Skip onComplete processing for 401 responses since we already completed the
                                        // future
                            }

                            logger.debug("{} [{}] : {} - onComplete called - result null: {}, failed: {}", LOG_PREFIX,
                                    getUID(), LOG_STATE, result == null, result != null ? result.isFailed() : "N/A");
                            if (result != null && !result.isFailed()) {
                                try {
                                    byte[] body = getContent();
                                    DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(body);

                                    if (d.getBytes(HomekitMessage.ERROR).length > 0) {
                                        SRP6Session = Optional.empty();
                                        StageResult stageResult = new StageResult(
                                                HomekitErrorCode.fromCode(d.getByte(HomekitMessage.ERROR)));
                                        completableFuture.complete(stageResult);
                                        return;
                                    }

                                    short state = d.getByte(HomekitMessage.STATE);
                                    logger.info("{} [{}] : {} - Received State {}", LOG_PREFIX, getUID(), LOG_STATE,
                                            state);

                                    StageResult stageResult = new StageResult(d, result);
                                    completableFuture.complete(stageResult);
                                } catch (IOException e) {
                                    SRP6Session = Optional.empty();
                                    logger.error("{} [{}] : {} - Failed to decode response - Error: {}", LOG_PREFIX,
                                            getUID(), LOG_ERROR, e.getMessage());
                                    logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR,
                                            e);
                                    completableFuture
                                            .complete(new StageResult("Failed to decode response: " + e.getMessage()));
                                }
                            } else {
                                // Safe handling of potentially null result
                                String failureMessage = "Unknown failure";
                                if (result != null) {
                                    Throwable failure = result.getResponseFailure();
                                    if (failure != null) {
                                        String message = failure.getMessage();
                                        if (message != null) {
                                            failureMessage = message;
                                        }
                                    }
                                }
                                StageResult stageResult = new StageResult(failureMessage);
                                completableFuture.complete(stageResult);
                            }
                        }
                    });
        } else {
            completableFuture.complete(new StageResult("HTTP client is not initialized"));
        }

        return completableFuture;
    }

    protected Future<ContentResult> getContent(String url) throws InterruptedException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{} [{}] : {} - Failed to create URI - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e1.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e1);
        }

        if (uri == null) {
            throw new IllegalStateException("URI must not be null");
        }

        CompletableFuture<ContentResult> completableFuture = new CompletableFuture<>();

        if (httpClient != null) {
            logger.debug("{} [{}] : {} - Sending HTTP GET request to: {}", LOG_PREFIX, getUID(), LOG_STATE,
                    uri.toString());
            httpClient.newRequest(uri.toString()).method(HttpMethod.GET).timeout(30, TimeUnit.SECONDS)
                    .onRequestBegin(req -> {
                        logger.debug("{} [{}] : {} - HTTP GET request begin: {}", LOG_PREFIX, getUID(), LOG_STATE,
                                req.getURI());
                    }).onRequestSuccess(req -> {
                        logger.debug("{} [{}] : {} - HTTP GET request sent successfully", LOG_PREFIX, getUID(),
                                LOG_STATE);
                    }).onRequestFailure((req, failure) -> {
                        logger.error("{} [{}] : {} - HTTP GET request failed to send - Error: {}", LOG_PREFIX, getUID(),
                                LOG_ERROR, failure.getMessage());
                    }).send(new BufferingResponseListener(8 * 1024 * 1024) {

                        private volatile boolean skipContentProcessing = false;

                        @Override
                        public void onBegin(Response response) {
                            int status = response.getStatus();
                            logger.debug("{} [{}] : {} - HTTP GET response begun - Status: {}", LOG_PREFIX, getUID(),
                                    LOG_STATE, status);

                            // Handle 401 Unauthorized responses
                            if (status == HttpStatus.UNAUTHORIZED_401) {
                                logger.warn(
                                        "{} [{}] : {} - GET request received HTTP 401 Unauthorized - pairing may be invalid or expired",
                                        LOG_PREFIX, getUID(), LOG_PAIRING);
                                try {
                                    setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                                } catch (HomekitServerException e) {
                                    logger.error("{} [{}] : {} - Failed to set state after 401: {}", LOG_PREFIX,
                                            getUID(), LOG_ERROR, e.getMessage());
                                }

                                // Set flag to skip content processing and complete immediately
                                skipContentProcessing = true;
                                logger.debug("{} [{}] : {} - Setting skipContentProcessing=true for GET 401 response",
                                        LOG_PREFIX, getUID(), LOG_STATE);

                                // Complete the future immediately with 401 error
                                ContentResult unauthorizedResult = new ContentResult(
                                        "HTTP 401 Unauthorized - authentication failed");
                                completableFuture.complete(unauthorizedResult);
                                return; // Don't call super.onBegin() to avoid further processing
                            }

                            super.onBegin(response);
                        }

                        @Override
                        public void onContent(Response response, ByteBuffer content) {
                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping GET content processing due to 401 response - {} bytes ignored",
                                        LOG_PREFIX, getUID(), LOG_STATE, content.remaining());
                                return; // Skip content processing for 401 responses
                            }

                            // log the number of bytes in the content
                            logger.debug("{} [{}] : {} - Received {} bytes", LOG_PREFIX, getUID(), LOG_STATE,
                                    content.remaining());
                            super.onContent(response, content);
                        }

                        @Override
                        public void onFailure(Response response, Throwable failure) {
                            logger.error("{} [{}] : {} - HTTP GET request failed - Error: {}", LOG_PREFIX, getUID(),
                                    LOG_ERROR, failure.getMessage());
                            logger.debug("{} [{}] : {} - HTTP GET failure details", LOG_PREFIX, getUID(), LOG_ERROR,
                                    failure);
                            super.onFailure(response, failure);
                        }

                        @Override
                        public void onComplete(@Nullable Result result) {
                            // Null Pointer Access Warning Checked
                            // We explicitly check result for null before accessing its methods
                            // This prevents NPE and satisfies static analysis

                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping GET onComplete processing due to 401 response - future already completed",
                                        LOG_PREFIX, getUID(), LOG_STATE);
                                return; // Skip onComplete processing for 401 responses since we already completed the
                                        // future
                            }

                            logger.debug("{} [{}] : {} - GET onComplete called - result null: {}, failed: {}",
                                    LOG_PREFIX, getUID(), LOG_STATE, result == null,
                                    result != null ? result.isFailed() : "N/A");
                            if (result != null && !result.isFailed()) {
                                byte[] body = getContent();
                                ContentResult stageResult = new ContentResult(body, result);
                                completableFuture.complete(stageResult);
                            } else {
                                // Safe handling of potentially null result
                                String failureMessage = "Unknown failure";
                                if (result != null) {
                                    Throwable failure = result.getResponseFailure();
                                    if (failure != null) {
                                        String message = failure.getMessage();
                                        if (message != null) {
                                            failureMessage = message;
                                        }
                                    }
                                }
                                ContentResult stageResult = new ContentResult(failureMessage);
                                completableFuture.complete(stageResult);
                            }
                        }
                    });
        } else {
            completableFuture.complete(new ContentResult("HTTP client is not initialized"));
        }

        return completableFuture;
    }

    protected Future<ContentResult> putContent(String url, byte[] body)
            throws InterruptedException, HomekitServerException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{} [{}] : {} - Failed to create URI - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e1.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e1);
        }

        if (uri == null) {
            throw new HomekitServerException("URI must not be null");
        }

        CompletableFuture<ContentResult> completableFuture = new CompletableFuture<>();

        if (httpClient != null) {
            logger.debug("{} [{}] : {} - Sending HTTP PUT request to: {}", LOG_PREFIX, getUID(), LOG_STATE,
                    uri.toString());
            httpClient.newRequest(uri.toString()).method(HttpMethod.PUT)
                    .content(new BytesContentProvider(body), "application/pairing+json")
                    .header(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString())
                    .send(new BufferingResponseListener(8 * 1024 * 1024) {

                        private volatile boolean skipContentProcessing = false;

                        @Override
                        public void onBegin(Response response) {
                            int status = response.getStatus();
                            logger.debug("{} [{}] : {} - HTTP PUT response begun - Status: {}", LOG_PREFIX, getUID(),
                                    LOG_STATE, status);

                            // Handle 401 Unauthorized responses
                            if (status == HttpStatus.UNAUTHORIZED_401) {
                                logger.warn(
                                        "{} [{}] : {} - PUT request received HTTP 401 Unauthorized - pairing may be invalid or expired",
                                        LOG_PREFIX, getUID(), LOG_PAIRING);
                                try {
                                    setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                                } catch (HomekitServerException e) {
                                    logger.error("{} [{}] : {} - Failed to set state after 401: {}", LOG_PREFIX,
                                            getUID(), LOG_ERROR, e.getMessage());
                                }

                                // Set flag to skip content processing and complete immediately
                                skipContentProcessing = true;
                                logger.debug("{} [{}] : {} - Setting skipContentProcessing=true for PUT 401 response",
                                        LOG_PREFIX, getUID(), LOG_STATE);

                                // Complete the future immediately with 401 error
                                ContentResult unauthorizedResult = new ContentResult(
                                        "HTTP 401 Unauthorized - authentication failed");
                                completableFuture.complete(unauthorizedResult);
                                return; // Don't call super.onBegin() to avoid further processing
                            }

                            super.onBegin(response);
                        }

                        @Override
                        public void onContent(Response response, ByteBuffer content) {
                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping PUT content processing due to 401 response - {} bytes ignored",
                                        LOG_PREFIX, getUID(), LOG_STATE, content.remaining());
                                return; // Skip content processing for 401 responses
                            }

                            // log the number of bytes in the content
                            logger.debug("{} [{}] : {} - PUT received {} bytes", LOG_PREFIX, getUID(), LOG_STATE,
                                    content.remaining());
                            super.onContent(response, content);
                        }

                        @Override
                        public void onFailure(Response response, Throwable failure) {
                            logger.error("{} [{}] : {} - HTTP PUT request failed - Error: {}", LOG_PREFIX, getUID(),
                                    LOG_ERROR, failure.getMessage());
                            logger.debug("{} [{}] : {} - HTTP PUT failure details", LOG_PREFIX, getUID(), LOG_ERROR,
                                    failure);
                            super.onFailure(response, failure);
                        }

                        @Override
                        public void onComplete(@Nullable Result result) {
                            // Null Pointer Access Warning Checked
                            // We explicitly check result for null before accessing its methods
                            // This prevents NPE and satisfies static analysis

                            if (skipContentProcessing) {
                                logger.debug(
                                        "{} [{}] : {} - Skipping PUT onComplete processing due to 401 response - future already completed",
                                        LOG_PREFIX, getUID(), LOG_STATE);
                                return; // Skip onComplete processing for 401 responses since we already completed the
                                        // future
                            }

                            logger.debug("{} [{}] : {} - PUT onComplete called - result null: {}, failed: {}",
                                    LOG_PREFIX, getUID(), LOG_STATE, result == null,
                                    result != null ? result.isFailed() : "N/A");
                            if (result != null && !result.isFailed()) {
                                byte[] responseBody = getContent();
                                ContentResult stageResult = new ContentResult(responseBody, result);
                                completableFuture.complete(stageResult);
                            } else {
                                // Safe handling of potentially null result
                                String failureMessage = "Unknown failure";
                                if (result != null) {
                                    Throwable failure = result.getResponseFailure();
                                    if (failure != null) {
                                        String message = failure.getMessage();
                                        if (message != null) {
                                            failureMessage = message;
                                        }
                                    }
                                }
                                ContentResult stageResult = new ContentResult(failureMessage);
                                completableFuture.complete(stageResult);
                            }
                        }
                    });
        } else {
            completableFuture.complete(new ContentResult("HTTP client is not initialized"));
        }

        return completableFuture;
    }

    // ========== Event Handling Methods ==========
    /**
     * Handles incoming events from the remote server.
     * This method processes event data and updates the local state accordingly.
     *
     * @param body The event data received from the remote server
     */
    public void handleEvent(byte[] body) {
        try {
            // getPairingId() returns non-null per the method implementation
            byte[] pairingId = getPairingId();
            logger.debug("{} [{}] : {} - Processing event", LOG_PREFIX, getUID(), LOG_STATE);
            HomekitByte.logBuffer(logger, "handleEvent", HomekitByte.toHexString(pairingId), ByteBuffer.wrap(body));
        } catch (IOException e) {
            logger.error("{} [{}] : {} - Failed to process event - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
        }
    }

    /**
     * Handles characteristic events from the remote server.
     * This method manages event subscriptions for characteristics and updates their
     * state.
     *
     * @param event The characteristic event to handle
     */
    @Override
    public void onCharacteristicEvent(HomekitCharacteristicEvent event) {
        // getPairingId() returns non-null per the method implementation
        byte[] pairingId = getPairingId();
        if (event.getType() == HomekitEventType.CHARACTERISTIC_START_EVENTS && event.getCharacteristic().isPresent()) {
            logger.debug("{} [{}] : {} - Starting events for characteristic", LOG_PREFIX, getUID(), LOG_STATE);
            subscribeEvents(event.getCharacteristic().get(), true);
        } else if (event.getType() == HomekitEventType.CHARACTERISTIC_STOP_EVENTS
                && event.getCharacteristic().isPresent()) {
            logger.debug("{} [{}] : {} - Stopping events for characteristic", LOG_PREFIX, getUID(), LOG_STATE);
            subscribeEvents(event.getCharacteristic().get(), false);
        }
    }

    // ========== HomekitAccessory Management Methods ==========
    @Override
    public long getNextAvailableAccessoryId() {
        return 0;
    }

    public Collection<HomekitAccessory> getRemoteAccessories() {
        Collection<HomekitAccessory> result = new HashSet<HomekitAccessory>();
        if (isPaired() && isPairVerified() && isSecure()) {
            Future<ContentResult> contentFuture;
            ContentResult contentResult = null;
            try {
                contentFuture = getContent("/accessories");
                contentResult = Objects.requireNonNull(contentFuture.get(), "ContentResult is null");
            } catch (InterruptedException | ExecutionException e) {
                logger.error("{} [{}] : {} - Error getting remote accessories - Error: {}", LOG_PREFIX, getUID(),
                        LOG_ERROR, e.getMessage());
                logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            }

            logger.info("{} [{}] : {} - Received accessories data", LOG_PREFIX, getUID(), LOG_STATE);

            if (contentResult != null && contentResult.result.isPresent()) {
                int status = contentResult.result.get().getResponse().getStatus();
                if (status == HttpStatus.UNAUTHORIZED_401) {
                    logger.warn(
                            "{} [{}] : {} - Received HTTP 401 Unauthorized when getting accessories - pairing invalid",
                            LOG_PREFIX, getUID(), LOG_PAIRING);
                    try {
                        setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                    } catch (HomekitServerException e) {
                        logger.error("{} [{}] : {} - Failed to set state after 401: {}", LOG_PREFIX, getUID(),
                                LOG_ERROR, e.getMessage());
                    }
                    return result;
                } else if (status == HttpStatus.OK_200) {
                    JsonArray accessories = Json
                            .createReader(new ByteArrayInputStream(contentResult.body.orElse(new byte[0]))).readObject()
                            .getJsonArray("accessories");
                    for (JsonValue value : accessories) {
                        try {
                            result.add(accessoryFactory.createAccessoryFromTagWithValue("generic", value));
                        } catch (HomekitFactoryException e) {
                            logger.error("{} [{}] : {} - Failed to create accessory from remote data: {}", LOG_PREFIX,
                                    getUID(), LOG_ERROR, e.getMessage());
                        }
                    }
                }
            }
        }
        return result;
    }

    public boolean subscribeEvents(HomekitCharacteristic<?> characteristic, boolean subscribe) {
        if (!isPairVerified()) {
            logger.debug("{} [{}] : {} - Cannot subscribe to events - not paired", LOG_PREFIX, getUID(), LOG_STATE);
            return false;
        }

        try {
            // Create the characteristic update request
            JsonObjectBuilder requestBuilder = Json.createObjectBuilder();
            JsonArrayBuilder characteristicsBuilder = Json.createArrayBuilder();
            JsonObjectBuilder characteristicBuilder = Json.createObjectBuilder()
                    .add("aid", characteristic.getService().getAccessory().getAccessoryId())
                    .add("iid", characteristic.getInstanceId()).add("ev", subscribe);

            characteristicsBuilder.add(characteristicBuilder);
            requestBuilder.add("characteristics", characteristicsBuilder);

            // Send the subscription request
            Future<ContentResult> contentFuture = putContent("/characteristics",
                    requestBuilder.build().toString().getBytes(StandardCharsets.UTF_8));
            ContentResult contentResult = Objects.requireNonNull(contentFuture.get(), "ContentResult is null");

            if (contentResult.result.isPresent()) {
                var result = contentResult.result.get();
                @SuppressWarnings("null")
                var response = result.getResponse();
                int status = response.getStatus();

                if (status == HttpStatus.UNAUTHORIZED_401) {
                    logger.warn(
                            "{} [{}] : {} - Received HTTP 401 Unauthorized when subscribing to events - pairing invalid",
                            LOG_PREFIX, getUID(), LOG_PAIRING);
                    try {
                        setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                    } catch (HomekitServerException e) {
                        logger.error("{} [{}] : {} - Failed to set state after 401: {}", LOG_PREFIX, getUID(),
                                LOG_ERROR, e.getMessage());
                    }
                    return false;
                } else if (status == HttpStatus.NO_CONTENT_204) {
                    logger.debug("{} [{}] : {} - Successfully subscribed to events for characteristic {}", LOG_PREFIX,
                            getUID(), LOG_STATE, characteristic.getUID());
                    characteristic.withEvents(true);
                    return true;
                } else {
                    logger.warn("{} [{}] : {} - Failed to subscribe to events for characteristic {} - Status: {}",
                            LOG_PREFIX, getUID(), LOG_WARN, characteristic.getUID(), status);
                    return false;
                }
            } else {
                logger.warn("{} [{}] : {} - No response received for event subscription", LOG_PREFIX, getUID(),
                        LOG_WARN);
                return false;
            }

        } catch (InterruptedException | ExecutionException | HomekitServerException e) {
            logger.error("{} [{}] : {} - Error subscribing to events for characteristic {}: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, characteristic.getUID(), e.getMessage());
            return false;
        }
    }

    /**
     * Updates the list of accessories by fetching remote accessories and comparing
     * with currently managed ones.
     * This method:
     * 1. Fetches the current list of remote accessories
     * 2. Compares with locally managed accessories
     * 3. Adds new accessories
     * 4. Removes accessories that no longer exist
     * 5. Updates services and characteristics for existing accessories
     *
     * @throws HomekitAccessoryOperationException if the update operation fails
     */
    @Override
    public void updateAccessories() throws HomekitAccessoryOperationException {
        if (!isPairVerified()) {
            logger.debug("{} [{}] : {} - Cannot update accessories - not paired", LOG_PREFIX, getUID(), LOG_STATE);
            return;
        }

        try {
            // Get current accessories
            Collection<HomekitAccessory> currentAccessories = new HashSet<>(getAccessories());

            // Get remote accessories
            Collection<HomekitAccessory> remoteAccessories = getRemoteAccessories();

            // Find new accessories to add
            for (HomekitAccessory remoteAccessory : remoteAccessories) {
                boolean found = false;
                for (HomekitAccessory currentAccessory : currentAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.info("{} [{}] : {} - Adding new accessory {}", LOG_PREFIX, getUID(), LOG_STATE,
                            remoteAccessory);
                    addAccessory(remoteAccessory);
                    eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.ACCESSORY_ADDED, this,
                            remoteAccessory, null, null));
                }
            }

            // Find accessories to remove
            for (HomekitAccessory currentAccessory : currentAccessories) {
                boolean found = false;
                for (HomekitAccessory remoteAccessory : remoteAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.info("{} [{}] : {} - Removing accessory {}", LOG_PREFIX, getUID(), LOG_STATE,
                            currentAccessory);
                    removeAccessory(currentAccessory);
                    eventManager.publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.ACCESSORY_REMOVED, this,
                            currentAccessory, null, null));
                }
            }

            // Compare services and characteristics for each accessory
            for (HomekitAccessory currentAccessory : getAccessories()) {
                for (HomekitAccessory remoteAccessory : remoteAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        // Compare services
                        Collection<HomekitService> currentServices = currentAccessory.getServices();
                        Collection<HomekitService> remoteServices = remoteAccessory.getServices();

                        // Find new services to add
                        for (HomekitService remoteService : remoteServices) {
                            boolean found = false;
                            for (HomekitService currentService : currentServices) {
                                if (Objects.equals(currentService.getInstanceId(), remoteService.getInstanceId())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                logger.info("{} [{}] : {} - Adding new service {} to accessory {}", LOG_PREFIX,
                                        getUID(), LOG_STATE, remoteService, currentAccessory);
                                currentAccessory.addService(remoteService);
                                eventManager.publishEvent(new HomekitAccessoryServerEvent(
                                        HomekitEventType.SERVICE_ADDED, this, currentAccessory, remoteService, null));
                            }
                        }

                        // Find services to remove
                        for (HomekitService currentService : currentServices) {
                            boolean found = false;
                            for (HomekitService remoteService : remoteServices) {
                                if (Objects.equals(currentService.getInstanceId(), remoteService.getInstanceId())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                logger.info("{} [{}] : {} - Removing service {} from accessory {}", LOG_PREFIX,
                                        getUID(), LOG_STATE, currentService, currentAccessory);
                                currentAccessory.removeService(currentService);
                                eventManager
                                        .publishEvent(new HomekitAccessoryServerEvent(HomekitEventType.SERVICE_REMOVED,
                                                this, currentAccessory, currentService, null));
                            }
                        }

                        // Compare characteristics for each service
                        for (HomekitService currentService : currentServices) {
                            for (HomekitService remoteService : remoteServices) {
                                if (Objects.equals(currentService.getInstanceId(), remoteService.getInstanceId())) {
                                    Collection<HomekitCharacteristic<?>> currentCharacteristics = currentService
                                            .getCharacteristics();
                                    Collection<HomekitCharacteristic<?>> remoteCharacteristics = remoteService
                                            .getCharacteristics();

                                    // Find new characteristics to add
                                    for (HomekitCharacteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                        boolean found = false;
                                        for (HomekitCharacteristic<?> currentCharacteristic : currentCharacteristics) {
                                            if (Objects.equals(currentCharacteristic.getInstanceId(),
                                                    remoteCharacteristic.getInstanceId())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            logger.info(
                                                    "{} [{}] : {} - Adding new characteristic {} to service {} of accessory {}",
                                                    LOG_PREFIX, getUID(), LOG_STATE, remoteCharacteristic,
                                                    currentService, currentAccessory);
                                            currentService.addCharacteristic(remoteCharacteristic);
                                            eventManager.publishEvent(new HomekitAccessoryServerEvent(
                                                    HomekitEventType.CHARACTERISTIC_ADDED, this, currentAccessory,
                                                    currentService, remoteCharacteristic));
                                        }
                                    }

                                    // Find characteristics to remove
                                    for (HomekitCharacteristic<?> currentCharacteristic : currentCharacteristics) {
                                        boolean found = false;
                                        for (HomekitCharacteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                            if (Objects.equals(currentCharacteristic.getInstanceId(),
                                                    remoteCharacteristic.getInstanceId())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            logger.info(
                                                    "{} [{}] : {} - Removing characteristic {} from service {} of accessory {}",
                                                    LOG_PREFIX, getUID(), LOG_STATE, currentCharacteristic,
                                                    currentService, currentAccessory);
                                            currentService.removeCharacteristic(currentCharacteristic);
                                            eventManager.publishEvent(new HomekitAccessoryServerEvent(
                                                    HomekitEventType.CHARACTERISTIC_REMOVED, this, currentAccessory,
                                                    currentService, currentCharacteristic));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("{} [{}] : {} - Error updating accessories: {}", LOG_PREFIX, getUID(), LOG_WARN,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_STATE, e);
            throw new HomekitAccessoryOperationException("Failed to update accessories", e);
        }
    }

    /**
     * Adds an accessory to this server and sets up event subscriptions for its
     * characteristics.
     *
     * @param accessory The accessory to add
     * @throws HomekitAccessoryOperationException if the operation fails
     */
    @Override
    public void addAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        super.addAccessory(accessory);
        for (HomekitService service : accessory.getServices()) {
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
                        (UID) characteristic.getUID(), (UID) getUID(),
                        (HomekitEventHandler) event -> onCharacteristicEvent((HomekitCharacteristicEvent) event)));
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_START_EVENTS,
                        (UID) characteristic.getUID(), (UID) getUID(),
                        event -> onCharacteristicEvent((HomekitCharacteristicEvent) event)));
                eventSubscriptions.add(eventManager.subscribe(HomekitEventType.CHARACTERISTIC_STOP_EVENTS,
                        (UID) characteristic.getUID(), (UID) getUID(),
                        event -> onCharacteristicEvent((HomekitCharacteristicEvent) event)));
                logger.debug("{} [{}] : {} - Subscribed to events for characteristic: {}", LOG_PREFIX, getUID(),
                        LOG_ACCESSORY, characteristic.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void removeAccessory(HomekitAccessory accessory) throws HomekitAccessoryOperationException {
        super.removeAccessory(accessory);
        // Collect all characteristic UIDs for this accessory
        Set<UID> characteristicUids = new HashSet<>();
        for (HomekitService service : accessory.getServices()) {
            for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                characteristicUids.add((UID) characteristic.getUID());
            }
        }

        // Remove and unsubscribe only those subscriptions that match
        eventSubscriptions.removeIf(subscription -> {
            if (characteristicUids.contains(subscription.getPublisherUID())) {
                eventManager.unsubscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, subscription.getPublisherUID(),
                        subscription.getSubscriber());
                logger.debug("{} [{}] : {} - Unsubscribed from events for sourceUid: {}", LOG_PREFIX, getUID(),
                        LOG_ACCESSORY, subscription.getPublisherUID());
                return true;
            }
            return false;
        });

        logger.info("{} [{}] : {} - HomekitAccessory removed successfully - ID: {}", LOG_PREFIX, getUID(),
                LOG_ACCESSORY, accessory.getAccessoryId());
    }

    // ========== Inner Classes ==========
    protected class StageResult {
        private final Optional<DecodeResult> decodeResult;
        private final Optional<Result> result;
        private final Optional<String> message;
        private final Optional<HomekitErrorCode> error;

        public StageResult(@Nullable DecodeResult decodeResult, @Nullable Result result) {
            this.decodeResult = Optional.ofNullable(decodeResult);
            this.result = Optional.ofNullable(result);
            this.message = Optional.empty();
            this.error = Optional.empty();
        }

        public StageResult(@Nullable String message) {
            this.decodeResult = Optional.empty();
            this.result = Optional.empty();
            this.message = Optional.ofNullable(message);
            this.error = Optional.empty();
        }

        public StageResult(@Nullable HomekitErrorCode error) {
            this.decodeResult = Optional.empty();
            this.result = Optional.empty();
            this.message = Optional.empty();
            this.error = Optional.ofNullable(error);
        }

        public boolean isFailure() {
            return message.isPresent() || error.isPresent();
        }

        public Optional<DecodeResult> getDecodeResult() {
            return decodeResult;
        }

        public Optional<Result> getResult() {
            return result;
        }

        public Optional<String> getMessage() {
            return message;
        }

        public Optional<HomekitErrorCode> getError() {
            return error;
        }
    }

    public static class ContentResult {
        private final Optional<Result> result;
        private final Optional<byte[]> body;
        private final Optional<String> message;

        public ContentResult(byte[] body, Result result) {
            this.result = Optional.ofNullable(result);
            this.body = Optional.ofNullable(body);
            this.message = Optional.empty();
        }

        public ContentResult(@Nullable String message) {
            this.result = Optional.empty();
            this.body = Optional.empty();
            this.message = Optional.ofNullable(message);
        }

        public Optional<Result> getResult() {
            return result;
        }

        public Optional<byte[]> getBody() {
            return body;
        }

        public Optional<String> getMessage() {
            return message;
        }
    }

    // ========== Utility Methods ==========
    // Note: These utility methods use comprehensive @SuppressWarnings("null") due to Eclipse's
    // overly conservative null analysis in complex JSON parsing and reflection operations
    @SuppressWarnings("unchecked") // Safe cast - decode method ensures type compatibility
    public static <T> T fromJson(String json, Class<T> beanClass) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonValue jsonValue = jsonReader.read();
            Optional<Object> decoded = decode(jsonValue, beanClass);
            return (T) decoded.orElseThrow(() -> new IllegalStateException("Failed to decode JSON to " + beanClass));
        }
    }

    private static Optional<Object> decode(JsonValue jsonValue, Type targetType) {
        if (jsonValue.getValueType() == ValueType.NULL) {
            return Optional.empty();
        } else if (jsonValue.getValueType() == ValueType.TRUE || jsonValue.getValueType() == ValueType.FALSE) {
            return Optional.of(decodeBoolean(jsonValue, targetType));
        } else if (jsonValue instanceof JsonNumber) {
            return Optional.of(decodeNumber((JsonNumber) jsonValue, targetType));
        } else if (jsonValue instanceof JsonString) {
            return Optional.of(decodeString((JsonString) jsonValue, targetType));
        } else if (jsonValue instanceof JsonArray) {
            return Optional.of(decodeArray((JsonArray) jsonValue, targetType));
        } else if (jsonValue instanceof JsonObject) {
            return Optional.of(decodeObject((JsonObject) jsonValue, targetType));
        } else {
            throw new UnsupportedOperationException("Unsupported json value: " + jsonValue);
        }
    }

    private static Optional<Object> decodeBoolean(JsonValue jsonValue, Type targetType) {
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Optional.of(Boolean.valueOf(jsonValue.toString()));
        } else {
            throw new UnsupportedOperationException("Unsupported boolean type: " + targetType);
        }
    }

    private static Optional<Object> decodeNumber(JsonNumber jsonNumber, Type targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return Optional.of(jsonNumber.intValue());
        } else if (targetType == long.class || targetType == Long.class) {
            return Optional.of(jsonNumber.longValue());
        } else {
            throw new UnsupportedOperationException("Unsupported number type: " + targetType);
        }
    }

    private static Optional<Object> decodeString(JsonString jsonString, Type targetType) {
        if (targetType == String.class) {
            return Optional.of(jsonString.getString());
        } else if (targetType == Date.class) {
            try {
                return Optional.of(
                        new SimpleDateFormat("MMM dd, yyyy H:mm:ss a", Locale.ENGLISH).parse(jsonString.getString()));
            } catch (ParseException e) {
                throw new UnsupportedOperationException("Unsupported date format: " + jsonString.getString());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported string type: " + targetType);
        }
    }

    @SuppressWarnings({ "null" }) // Needed for reflection type operations
    private static Optional<Object> decodeArray(JsonArray jsonArray, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (List.class.isAssignableFrom(targetClass)) {
            // Suppression for ParameterizedType operations - Eclipse overly conservative
            @SuppressWarnings({ "null" })
            ParameterizedType paramType = (ParameterizedType) targetType;
            @SuppressWarnings({ "null" })
            Class<?> elementClass = (Class<?>) paramType.getActualTypeArguments()[0];
            List<Object> list = new ArrayList<>();

            for (JsonValue item : jsonArray) {
                Optional<Object> decodedItem = decode(item, elementClass);
                decodedItem.ifPresent(list::add);
            }

            return Optional.of(list);
        } else if (targetClass.isArray()) {
            Class<?> elementClass = targetClass.getComponentType();
            if (elementClass == null) {
                throw new UnsupportedOperationException("Cannot determine element type for array: " + targetClass);
            }
            Object array = Array.newInstance(elementClass, jsonArray.size());

            @SuppressWarnings("null") // Comprehensive suppression for array element processing
            int size = jsonArray.size();
            for (int i = 0; i < size; i++) {
                // elementClass is guaranteed non-null after the null check above
                @SuppressWarnings("null") // null check performed above
                Class<?> nonNullElementClass = elementClass;
                Optional<Object> decodedItem = decode(jsonArray.get(i), nonNullElementClass);
                Array.set(array, i, decodedItem.orElse(null));
            }

            return Optional.of(array);
        } else {
            throw new UnsupportedOperationException("Unsupported array type: " + targetClass);
        }
    }

    @SuppressWarnings({ "null" }) // Needed for reflection & JSON operations
    private static Optional<Object> decodeObject(JsonObject object, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (Map.class.isAssignableFrom(targetClass)) {
            // Suppression for ParameterizedType operations - Eclipse overly conservative
            @SuppressWarnings({ "null" })
            ParameterizedType paramType = (ParameterizedType) targetType;
            @SuppressWarnings({ "null" })
            Class<?> valueClass = (Class<?>) paramType.getActualTypeArguments()[1];
            Map<String, Object> map = new LinkedHashMap<>();

            for (Entry<String, JsonValue> entry : object.entrySet()) {
                Optional<Object> decodedValue = decode(entry.getValue(), valueClass);
                decodedValue.ifPresent(value -> map.put(entry.getKey(), value));
            }

            return Optional.of(map);
        } else {
            try {
                Constructor<?>[] ctors = targetClass.getDeclaredConstructors();
                Constructor<?> ctor = null;
                for (int i = 0; i < ctors.length; i++) {
                    ctor = ctors[i];
                    if (ctor.getGenericParameterTypes().length == 0) {
                        break;
                    }
                }

                if (ctor == null) {
                    return Optional.empty();
                }
                // Object bean = targetClass.newInstance();
                // Constructor.newInstance(targetClass);
                @SuppressWarnings("null") // Constructor.newInstance() is safe with targetClass parameter
                Object bean = ctor.newInstance(targetClass);

                // Get PropertyDescriptors from BeanInfo and ensure resources are properly managed
                var beanInfo = Introspector.getBeanInfo(targetClass);
                PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
                // Flush the BeanInfo caches to prevent resource leaks
                Introspector.flushCaches();

                for (PropertyDescriptor property : properties) {
                    @SuppressWarnings("null")
                    java.lang.reflect.Method writeMethod = property.getWriteMethod();
                    @SuppressWarnings("null")
                    String propertyName = property.getName();
                    if (writeMethod != null && object.containsKey(propertyName)) {
                        // Use conditional logic to help compiler with null analysis
                        Type[] parameterTypes = writeMethod.getGenericParameterTypes();
                        if (parameterTypes != null && parameterTypes.length > 0) {
                            Type parameterType = parameterTypes[0];
                            if (parameterType != null) {
                                JsonValue jsonValue = object.get(propertyName);
                                if (jsonValue != null) {
                                    Optional<Object> decodedValue = decode(jsonValue, parameterType);
                                    if (decodedValue.isPresent()) {
                                        Object value = decodedValue.get();
                                        if (value != null) {
                                            // Comprehensive suppression for reflection invoke operation
                                            @SuppressWarnings("null") // null checks performed, reflection operation
                                                                      // safe
                                            Object nonNullValue = value;
                                            @SuppressWarnings("null") // bean and writeMethod guaranteed non-null in
                                                                      // this context
                                            java.lang.reflect.Method safeWriteMethod = writeMethod;
                                            @SuppressWarnings("null") // bean guaranteed non-null from
                                                                      // constructor.newInstance()
                                            Object safeBean = bean;
                                            safeWriteMethod.invoke(safeBean, nonNullValue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (bean == null) {
                    return Optional.empty();
                }

                return Optional.of(bean);
            } catch (Exception e) {
                throw new UnsupportedOperationException("Unsupported object type: " + targetClass, e);
            }
        }
    }

    /**
     * Checks the current pairing status by querying the remote accessory's pairing list.
     * This method determines if the accessory is paired with this controller or other controllers,
     * and updates the server state accordingly.
     * 
     * @throws HomekitServerException if there's an error communicating with the accessory
     * @throws IOException if there's an I/O error during the status check
     */
    protected void checkPairingStatus() throws HomekitServerException, IOException {
        logger.debug("{} [{}] : {} - Checking pairing status", LOG_PREFIX, getUID(), LOG_STATE);

        if (!isPairVerified() || !isSecure()) {
            logger.debug("{} [{}] : {} - Cannot check pairing status - connection not secure or verified", LOG_PREFIX,
                    getUID(), LOG_STATE);
            return;
        }

        try {
            Future<ContentResult> contentFuture = getContent("/pairings");
            ContentResult contentResult = Objects.requireNonNull(contentFuture.get(), "ContentResult is null");

            if (contentResult.result.isPresent()) {
                int status = contentResult.result.get().getResponse().getStatus();
                if (status == HttpStatus.UNAUTHORIZED_401) {
                    logger.warn(
                            "{} [{}] : {} - Received HTTP 401 Unauthorized when checking pairing status - pairing invalid",
                            LOG_PREFIX, getUID(), LOG_PAIRING);
                    setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
                } else if (status == HttpStatus.OK_200) {
                    processPairingStatusResponse(contentResult);
                } else {
                    logger.warn("{} [{}] : {} - Failed to retrieve pairing status - HTTP Status: {}", LOG_PREFIX,
                            getUID(), LOG_WARN, status);
                    setState(HomekitAccessoryServerState.UNPAIRED);
                }
            } else {
                logger.warn("{} [{}] : {} - Failed to retrieve pairing status - no response", LOG_PREFIX, getUID(),
                        LOG_WARN);
                setState(HomekitAccessoryServerState.UNPAIRED);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("{} [{}] : {} - Error checking pairing status - Error: {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            setState(HomekitAccessoryServerState.DISCONNECTED);
            throw new HomekitServerException("Failed to check pairing status", e);
        }
    }

    /**
     * Processes the pairing status response from the remote accessory.
     * Parses the response to determine if this controller is paired with the accessory
     * and updates the server state accordingly.
     * 
     * @param contentResult The response from the /pairings endpoint
     * @throws HomekitServerException if there's an error processing the response
     */
    private void processPairingStatusResponse(ContentResult contentResult) throws HomekitServerException {
        byte[] responseContent = contentResult.body.orElse(new byte[0]);
        if (responseContent == null || responseContent.length == 0) {
            logger.debug("{} [{}] : {} - Empty pairing status response", LOG_PREFIX, getUID(), LOG_STATE);
            setState(HomekitAccessoryServerState.UNPAIRED);
            return;
        }

        try {
            String pairingsList = new String(responseContent, StandardCharsets.UTF_8);
            String pairingIdStr = new String(getPairingId(), StandardCharsets.UTF_8);

            logger.debug("{} [{}] : {} - Received pairings list from accessory", LOG_PREFIX, getUID(), LOG_STATE);

            if (pairingsList.contains(pairingIdStr)) {
                logger.info("{} [{}] : {} - Accessory is paired with this controller", LOG_PREFIX, getUID(),
                        LOG_PAIRING);
                setState(HomekitAccessoryServerState.PAIRED);
            } else {
                logger.warn("{} [{}] : {} - Accessory is not paired with this controller but may be paired with others",
                        LOG_PREFIX, getUID(), LOG_PAIRING);
                setState(HomekitAccessoryServerState.UNPAIRED);
            }
        } catch (Exception e) {
            logger.error("{} [{}] : {} - Error parsing pairing status response - Error: {}", LOG_PREFIX, getUID(),
                    LOG_ERROR, e.getMessage());
            logger.debug("{} [{}] : {} - Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
            setState(HomekitAccessoryServerState.UNPAIRED);
        }
    }

    protected void handleVerificationFailure(int stage, StageResult result) throws HomekitServerException {
        logger.debug("{} [{}] : {} - Handling verification failure for stage {}", LOG_PREFIX, getUID(), LOG_STATE,
                stage);
        if (result.error.isPresent()) {
            logger.warn("{} [{}] : {} - Pair verification failed at stage {} with error: {}", LOG_PREFIX, getUID(),
                    LOG_WARN, stage, result.error.get());
            logger.debug("{} [{}] : {} : Stage {} : error details: {}", LOG_PREFIX, getUID(), LOG_STATE, stage,
                    result.error.get());
        } else {
            logger.warn("{} [{}] : {} - Pair verification failed at stage {} with message: {}", LOG_PREFIX, getUID(),
                    LOG_WARN, stage, result.message.orElse(""));
            logger.debug("{} [{}] : {} : Stage {} : failure message details: {}", LOG_PREFIX, getUID(), LOG_STATE,
                    stage, result.message.orElse(""));
        }
        setState(HomekitAccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("{} [{}] : {} - State set to PAIR_UNVERIFIED after failure", LOG_PREFIX, getUID(), LOG_STATE);
    }

    @Override
    public void advertise() {
        // no Op for HomekitRemoteAccessoryServer
    }

    // ========== Utility Methods ==========
    // Note: These utility methods use comprehensive @SuppressWarnings("null") due to Eclipse's
    // overly conservative null analysis in complex JSON parsing and reflection operations

    /**
     * Test method to debug HTTP connection issues.
     * This method helps identify why onComplete() might not be called.
     */
    public void testHttpConnection() {
        logger.info("{} [{}] : {} - Starting HTTP connection test", LOG_PREFIX, getUID(), LOG_STATE);
        logger.info("{} [{}] : {} - Server state: {}, httpClient null: {}", LOG_PREFIX, getUID(), LOG_STATE,
                currentState, httpClient == null);
        logger.info("{} [{}] : {} - Target address: {}:{}", LOG_PREFIX, getUID(), LOG_STATE, address.getHostAddress(),
                port);

        if (httpClient == null) {
            logger.error("{} [{}] : {} - HTTP client is null - cannot test connection", LOG_PREFIX, getUID(),
                    LOG_ERROR);
            return;
        }

        try {
            // Test simple GET request to root
            URI testUri = new URI("http", null, address.getHostAddress(), port, "/", null, null);
            logger.info("{} [{}] : {} - Testing connection to: {}", LOG_PREFIX, getUID(), LOG_STATE, testUri);

            httpClient.newRequest(testUri.toString()).method(HttpMethod.GET).timeout(10, TimeUnit.SECONDS)
                    .onRequestBegin(req -> {
                        logger.info("{} [{}] : {} - TEST: Request begin - {}", LOG_PREFIX, getUID(), LOG_STATE,
                                req.getURI());
                    }).onRequestSuccess(req -> {
                        logger.info("{} [{}] : {} - TEST: Request sent successfully", LOG_PREFIX, getUID(), LOG_STATE);
                    }).onRequestFailure((req, failure) -> {
                        logger.error("{} [{}] : {} - TEST: Request failed to send - {}", LOG_PREFIX, getUID(),
                                LOG_ERROR, failure.getMessage());
                    }).send(new BufferingResponseListener() {
                        @Override
                        public void onBegin(Response response) {
                            logger.info("{} [{}] : {} - TEST: Response begin - Status: {}", LOG_PREFIX, getUID(),
                                    LOG_STATE, response.getStatus());
                            super.onBegin(response);
                        }

                        @Override
                        public void onFailure(Response response, Throwable failure) {
                            logger.error("{} [{}] : {} - TEST: Response failure - {}", LOG_PREFIX, getUID(), LOG_ERROR,
                                    failure.getMessage());
                            super.onFailure(response, failure);
                        }

                        @Override
                        public void onComplete(@Nullable Result result) {
                            logger.info("{} [{}] : {} - TEST: onComplete called! - result null: {}, failed: {}",
                                    LOG_PREFIX, getUID(), LOG_STATE, result == null,
                                    result != null ? result.isFailed() : "N/A");
                            if (result != null && result.isFailed()) {
                                logger.error("{} [{}] : {} - TEST: Result failed - {}", LOG_PREFIX, getUID(), LOG_ERROR,
                                        result.getFailure() != null ? result.getFailure().getMessage() : "unknown");
                            }
                        }
                    });

        } catch (Exception e) {
            logger.error("{} [{}] : {} - TEST: Exception during connection test - {}", LOG_PREFIX, getUID(), LOG_ERROR,
                    e.getMessage());
            logger.debug("{} [{}] : {} - TEST: Exception details", LOG_PREFIX, getUID(), LOG_ERROR, e);
        }
    }
}
