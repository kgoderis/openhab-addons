package org.openhab.io.homekit.internal.server;

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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryCategory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Error;
import org.openhab.io.homekit.api.hap.Message;
import org.openhab.io.homekit.api.hap.Method;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.CharacteristicChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.exception.HomekitConfigurationException;
import org.openhab.io.homekit.exception.HomekitException;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.client.HomekitClientSRP6Session;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent;
import org.openhab.io.homekit.internal.events.CharacteristicEvent.CharacteristicEventType;
import org.openhab.io.homekit.internal.http.HomekitHttpClientTransportOverHTTP;
import org.openhab.io.homekit.internal.http.HomekitHttpDestinationOverHTTP;
import org.openhab.io.homekit.internal.http.HomekitProtocolHandler;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

import djb.Curve25519;

// A bridge is a special type of HAP accessory server that bridges HomeKit Accessory Protocol and different RF/transport protocols, such as ZigBee or Z-Wave. A bridge must expose all the user-addressable functionality supported by its connected devices as HAP accessory objects to the HAP controller(s). A bridge must ensure that the instance ID assigned to the HAP accessory objects exposed on behalf of its connected devices do not change for the lifetime of the server/client pairing.
// For example, a bridge that bridges three lights would expose four HAP accessory objects: one HAP accessory object that represents the bridge itself that may include a "firmware update" service, and three additional HAP accessory objects that each contain a "lightbulb" service.
// A bridge must not expose more than 150 HAP accessory objects. The HAP accessory object with an instance ID of 1 is considered the primary HAP accessory object. For bridges, this must be the bridge itself.

public class RemoteAccessoryServer extends AbstractAccessoryServer implements CharacteristicChangeListener {

    // ========== Constants ==========
    protected static final Logger logger = LoggerFactory.getLogger(RemoteAccessoryServer.class);
    private static final String HTTP_SCHEME = "http";
    protected static final String LOG_PREFIX = "HomeKit RemoteAccessoryServer: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    // ========== Core Dependencies ==========
    private final ScheduledExecutorService scheduler;

    // ========== Component References and Locks ==========
    private HomekitClientSRP6Session SRP6Session;
    private HttpClient httpClient;

    // ========== State Management ==========
    private byte[] sessionKey;
    private byte[] sharedSecret;
    private byte[] clientPublicKey;
    private byte[] clientPrivateKey;
    private boolean isPairVerified;
    private @Nullable ScheduledFuture<?> connectionMonitorJob;

    // ========== Constructor ==========
    public RemoteAccessoryServer(AccessoryCategory category, InetAddress address, int port, byte[] pairingIdentifier,
            byte[] secretKey, AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws HomekitConfigurationException {
        super(category, address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry);
        this.setupCode = "";
        this.isPairVerified = false;
        this.scheduler = org.openhab.core.common.ThreadPoolManager.getScheduledPool("homekit-remote");
    }

    public RemoteAccessoryServer(AccessoryCategory category, InetAddress address, int port,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry)
            throws HomekitConfigurationException, HomekitServerException {
        this(category, address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry);
    }

    // ========== Core Lifecycle Methods ==========
    @Override
    protected void initializeResources() throws HomekitServerException {
        super.initializeResources();
        try {
            httpClient = new HttpClient(new HomekitHttpClientTransportOverHTTP(), null);

            if (httpClient != null) {
                logger.debug("{}Starting HTTP client initialization", LOG_INIT);
                try {
                    httpClient.start();
                    ProtocolHandlers handlers = httpClient.getProtocolHandlers();
                    handlers.clear();
                    handlers.put(new HomekitProtocolHandler(this));
                    setState(AccessoryServerState.CONNECTED);
                    logger.debug("{}HTTP client initialized successfully", LOG_INIT);
                } catch (Exception e) {
                    logger.error("{}Failed to start HTTP client - Error: {}", LOG_ERROR, e.getMessage());
                    logger.debug("{}Exception details", LOG_ERROR, e);
                    setState(AccessoryServerState.DISCONNECTED);
                    throw new HomekitServerException("Failed to start HTTP client", e);
                }
            }
        } catch (HomekitServerException e) {
            logger.error("{}Failed to start HTTP client - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            try {
                setState(AccessoryServerState.DISCONNECTED);
            } catch (HomekitServerException ex) {
                logger.error("{}Failed to set state to DISCONNECTED: {}", LOG_ERROR, ex.getMessage());
            }
        }

        startConnectionMonitor();
    }

    @Override
    protected void cleanupResources() throws HomekitServerException {
        super.cleanupResources();
    }

    @Override
    public void start() throws HomekitServerException {
        super.start();
        try {
            // Create a test request to check connection using the address member
            String url = String.format("http://%s:%d", address.getHostAddress(), port);
            logger.debug("{}Testing connection to {}", LOG_INIT, url);
            Request request = httpClient.newRequest(url);
            request.onRequestFailure((req, failure) -> {
                logger.warn("{}Connection failed - Server: {}", LOG_STATE, new String(getPairingId()));
                logger.debug("{}Failure details: {}", LOG_STATE, failure);
                try {
                    setState(AccessoryServerState.DISCONNECTED);
                } catch (HomekitServerException e) {
                    logger.error("{}Failed to set state to DISCONNECTED: {}", LOG_ERROR, e.getMessage());
                }
            });
            request.send();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("{}Failed to start HTTP client - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            try {
                setState(AccessoryServerState.DISCONNECTED);
            } catch (HomekitServerException ex) {
                logger.error("{}Failed to set state to DISCONNECTED: {}", LOG_ERROR, ex.getMessage());
            }
        }

        startConnectionMonitor();
    }

    @Override
    public void stop() throws HomekitServerException {
        stopConnectionMonitor();

        logger.info("{}Stopping server - Server: {}", LOG_STATE, new String(getPairingId()));
        try {
            if (isPaired()) {
                pairRemove();
            }
        } catch (HomekitServerException e) {
            logger.warn("{}Error removing pairing during stop - Error: {}", LOG_WARN, e.getMessage());
            logger.debug("{}Exception details", LOG_WARN, e);
            throw e;
        }

        try {
            if (httpClient != null) {
                httpClient.stop();
            }
        } catch (Exception e) {
            logger.error("{}Error stopping HTTP client - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
        }

        super.stop();
    }

    @Override
    public void close() throws Exception {
        logger.info("{}Closing server - Server: {}", LOG_STATE, new String(getPairingId()));

        try {
            // First stop the server to clean up active connections
            stop();

            // Clean up HTTP client resources
            if (httpClient != null) {
                logger.debug("{}Destroying HTTP client - Server: {}", LOG_CONFIG, new String(getPairingId()));
                httpClient.destroy();
                httpClient = null;
            }

            // Reset pairing state
            resetPairingState();

            // Call super.close() last to ensure proper cleanup of base class resources
            super.close();

            logger.debug("{}Server closed successfully - Server: {}", LOG_STATE, new String(getPairingId()));
        } catch (Exception e) {
            logger.error("{}Error during server close - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw e;
        }
    }

    // ========== State Management Methods ==========
    protected AccessoryServerState getState() {
        return currentState;
    }

    public boolean isPairVerified() {
        return currentState == AccessoryServerState.PAIR_VERIFIED;
    }

    @Override
    public boolean isPaired() {
        return currentState == AccessoryServerState.PAIRED || currentState == AccessoryServerState.PAIR_VERIFIED;
    }

    public boolean isConnected() {
        return currentState != AccessoryServerState.DISCONNECTED;
    }

    @Override
    public boolean isSecure() {
        if (httpClient != null && address != null && port != 0) {
            Destination destination = httpClient.getDestination(HTTP_SCHEME, address.getHostAddress(), port);

            if (destination instanceof HomekitHttpDestinationOverHTTP) {
                return ((HomekitHttpDestinationOverHTTP) destination).hasEncryptionKeys();
            }
        }

        return false;
    }

    // ========== Connection Management Methods ==========
    protected void startConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
            logger.debug("{}Cancelled existing connection monitor", LOG_STATE);
        }

        // Schedule periodic connection monitoring
        connectionMonitorJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                logger.debug("{}Starting connection monitoring cycle", LOG_STATE);
                monitorConnection();
            } catch (Exception e) {
                logger.warn("{}Connection monitoring failed - Error: {}", LOG_STATE, e.getMessage());
                logger.debug("{}Exception details", LOG_STATE, e);
                try {
                    setState(AccessoryServerState.DISCONNECTED);
                } catch (HomekitServerException ex) {
                    logger.error("{}Failed to set disconnected state: {}", LOG_ERROR, ex.getMessage());
                }
            }
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS);
        logger.debug("{}Connection monitor scheduled", LOG_STATE);
    }

    protected void stopConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
            connectionMonitorJob = null;
            logger.debug("{}Connection monitor stopped", LOG_STATE);
        }
    }

    private void monitorConnection() throws HomekitServerException, IOException {
        logger.debug("{}Monitoring connection state - Server: {}", LOG_STATE, new String(getPairingId()));

        // Check if we were previously disconnected
        if (currentState == AccessoryServerState.DISCONNECTED) {
            logger.info("{}Connection was lost, attempting to re-establish - Server: {}", LOG_STATE,
                    new String(getPairingId()));

            // If we're not paired at all, proceed with pairing
            if (!isPaired()) {
                logger.info("{}Setting up new pairing - Server: {}", LOG_STATE, new String(getPairingId()));
                try {
                    pairSetup();
                } catch (HomekitServerException e) {
                    logger.warn("{}Pairing setup failed - Error: {}", LOG_STATE, e.getMessage());
                    logger.debug("{}Exception details", LOG_STATE, e);
                    setState(AccessoryServerState.MISSING_SETUP_CODE);
                    return;
                }
            }
        }

        // If connected but not paired, attempt to pair
        if (currentState == AccessoryServerState.CONNECTED && !isPaired()) {
            logger.info("{}Connected but not paired, attempting to pair - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            try {
                pairSetup();
                if (isPaired()) {
                    pairVerify();
                }
            } catch (HomekitServerException e) {
                logger.warn("{}Pairing setup failed - Error: {}", LOG_STATE, e.getMessage());
                logger.debug("{}Exception details", LOG_STATE, e);
                setState(AccessoryServerState.MISSING_SETUP_CODE);
                return;
            }
        }

        // If paired, verify the connection
        if (isPaired()) {
            logger.debug("{}Verifying connection - Server: {}", LOG_STATE, new String(getPairingId()));
            if (!isPairVerified()) {
                logger.info("{}Connection not verified, attempting verification - Server: {}", LOG_STATE,
                        new String(getPairingId()));
                if (!pairVerify()) {
                    logger.warn("{}Connection verification failed - Server: {}", LOG_STATE, new String(getPairingId()));
                    setState(AccessoryServerState.DISCONNECTED);
                    return;
                }
            }

            // After successful verification, check pairing status with accessory
            if (isPairVerified()) {
                checkPairingStatus();
            }

            // Check if connection is still secure
            if (!isSecure()) {
                logger.warn("{}Connection is no longer secure - Server: {}", LOG_STATE, new String(getPairingId()));
                setState(AccessoryServerState.DISCONNECTED);
                return;
            }

            setState(AccessoryServerState.CONNECTED);
            logger.debug("{}Connection verified and secure - Server: {}", LOG_STATE, new String(getPairingId()));
        }
    }

    // ========== Pairing Methods ==========
    @Override
    public void pairSetup() throws HomekitServerException {
        logger.info("{}Starting pair setup process - Server: {}", LOG_STATE, new String(getPairingId()));
        logger.debug("{}Current state: {}", LOG_STATE, currentState);

        // Validate setup code
        if (setupCode == null || setupCode.isEmpty()) {
            logger.warn("{}Unable to pair with {}:{} because no setup code is set - Server: {}", LOG_STATE,
                    address.getHostAddress(), port, new String(getPairingId()));
            setState(AccessoryServerState.MISSING_SETUP_CODE);
            return;
        }

        // Initialize pairing state
        resetPairingState();
        setState(AccessoryServerState.PAIR_SETUP_INITIAL);
        logger.debug("{}Pairing state reset, starting authentication - Server: {}", LOG_STATE,
                new String(getPairingId()));

        try {
            // Stage 0: Initial Setup
            logger.debug("{}Starting Stage 0 - Initial Setup - Server: {}", LOG_STATE, new String(getPairingId()));
            StageResult stage0Result = executePairingStage(0, () -> doPairSetupStage0());
            if (stage0Result.isFailure()) {
                handlePairingFailure(0, stage0Result);
                return;
            }
            logger.debug("{}Stage 0 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Stage 1: SRP Protocol Exchange
            logger.debug("{}Starting Stage 1 - SRP Protocol Exchange - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_SRP);
            StageResult stage1Result = executePairingStage(1, () -> doPairSetupStage1(stage0Result));
            if (stage1Result.isFailure()) {
                handlePairingFailure(1, stage1Result);
                return;
            }
            logger.debug("{}Stage 1 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Stage 2: Verify Proof
            logger.debug("{}Starting Stage 2 - Verify Proof - Server: {}", LOG_STATE, new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_VERIFY);
            StageResult stage2Result = executePairingStage(2, () -> doPairSetupStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handlePairingFailure(2, stage2Result);
                return;
            }
            logger.debug("{}Stage 2 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Stage 3: Exchange Keys
            logger.debug("{}Starting Stage 3 - Exchange Keys - Server: {}", LOG_STATE, new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage3Result = executePairingStage(3, () -> doPairSetupStage3(stage2Result));
            if (stage3Result.isFailure()) {
                handlePairingFailure(3, stage3Result);
                return;
            }
            logger.debug("{}Stage 3 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Pairing completed successfully
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            logger.info("{}Pair setup completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));
            logger.debug("{}Final state: {}", LOG_STATE, currentState);

        } catch (HomekitServerException e) {
            logger.error("{}Pair setup failed with error: {} - Server: {}", LOG_ERROR, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Homekit exception details", LOG_ERROR, e);
            setState(AccessoryServerState.UNPAIRED);
        } catch (Exception e) {
            logger.error("{}Unexpected error during pair setup: {} - Server: {}", LOG_ERROR, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Exception details", LOG_ERROR, e);
            setState(AccessoryServerState.UNPAIRED);
        }
    }

    @Override
    public boolean pairVerify() throws HomekitServerException {
        logger.info("{}Starting pair verify process - Server: {}", LOG_STATE, new String(getPairingId()));
        logger.debug("{}Current state: {}", LOG_STATE, currentState);

        // Reset verification state
        resetVerificationState();
        setState(AccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("{}Verification state reset - Server: {}", LOG_STATE, new String(getPairingId()));

        try {
            // Stage 0: Initial Verification
            logger.debug("{}Starting Stage 0 - Initial Verification - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            StageResult stage0Result = executePairingStage(0, () -> doPairVerifyStage0());
            if (stage0Result.isFailure()) {
                handleVerificationFailure(0, stage0Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{}Stage 0 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Stage 1: Exchange Keys
            logger.debug("{}Starting Stage 1 - Exchange Keys - Server: {}", LOG_STATE, new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_VERIFY);

            // Handle stage 1 with authentication error handling
            final StageResult stage1Result = handleStage1Verification(stage0Result);
            if (stage1Result.isFailure()) {
                handleVerificationFailure(1, stage1Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{}Stage 1 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Stage 2: Final Verification
            logger.debug("{}Starting Stage 2 - Final Verification - Server: {}", LOG_STATE, new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage2Result = executePairingStage(2, () -> doPairVerifyStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handleVerificationFailure(2, stage2Result);
                handlePairingVerification(false);
                return false;
            }
            logger.debug("{}Stage 2 completed successfully - Server: {}", LOG_STATE, new String(getPairingId()));

            // Verification completed successfully
            isPairVerified = true;
            handlePairingVerification(true);
            logger.info("{}Pair verification completed successfully - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            logger.debug("{}Final state: {}", LOG_STATE, currentState);
            return true;

        } catch (HomekitServerException e) {
            logger.error("{}Pair verification failed with error: {} - Server: {}", LOG_ERROR, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Homekit exception details", LOG_ERROR, e);
            handlePairingVerification(false);
            return false;
        } catch (Exception e) {
            logger.error("{}Unexpected error during pair verification: {} - Server: {}", LOG_ERROR, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Exception details", LOG_ERROR, e);
            handlePairingVerification(false);
            return false;
        }
    }

    @Override
    public void pairRemove() throws HomekitServerException {
        logger.info("{}Starting pair remove process - Server: {}", LOG_STATE, new String(getPairingId()));
        logger.debug("{}Current state: {}", LOG_STATE, currentState);

        if (!isPaired()) {
            logger.warn("{}Cannot remove pairing - accessory is not paired - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            setState(AccessoryServerState.UNPAIRED);
            return;
        }

        if (!isPairVerified || !isSecure()) {
            logger.warn("{}Cannot remove pairing - accessory is {} verified and connection is {} secured - Server: {}",
                    LOG_STATE, isPairVerified ? "already" : "not", isSecure() ? "" : "not ",
                    new String(getPairingId()));
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            return;
        }

        try {
            // Prepare remove pairing request
            logger.debug("{}Preparing remove pairing request - Server: {}", LOG_STATE, new String(getPairingId()));
            Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
            try {
                encoder.add(Message.STATE, (short) 0x01);
                encoder.add(Message.METHOD, Method.REMOVE_PAIRING.getKey());
                encoder.add(Message.IDENTIFIER, getPairingId());
            } catch (IOException e) {
                logger.error("{}Failed to prepare remove pairing request - Error: {}", LOG_ERROR, e.getMessage());
                throw new HomekitServerException("Failed to prepare remove pairing request", e);
            }

            // Send remove pairing request
            logger.debug("{}Sending remove pairing request - Server: {}", LOG_STATE, new String(getPairingId()));
            Future<StageResult> stageFuture = sendPairing(encoder.toByteArray());
            StageResult stageResult = stageFuture.get();

            // Verify response state
            short state = stageResult.decodeResult.getByte(Message.STATE);
            if (state != 2) {
                logger.error("{}Invalid state in remove pairing response: {} - Server: {}", LOG_ERROR, state,
                        new String(getPairingId()));
                throw new HomekitServerException("Wrong STATE");
            }

            // Check for errors in response
            if (stageResult.decodeResult.getBytes(Message.ERROR) != null) {
                Error error = Error.get(stageResult.decodeResult.getByte(Message.ERROR));
                logger.warn("{}Accessory failed to remove pairing: {} - Server: {}", LOG_STATE, error,
                        new String(getPairingId()));
                setState(AccessoryServerState.PAIR_UNVERIFIED);
                return;
            }

            // Remove all pairings
            logger.debug("{}Removing all pairings - Server: {}", LOG_STATE, new String(getPairingId()));
            for (Pairing pairing : getPairings()) {
                removePairing(pairing.getDestinationId());
            }

            // Update state
            isPairVerified = false;
            setState(AccessoryServerState.UNPAIRED);
            logger.info("{}Successfully removed pairing - Server: {}", LOG_STATE, new String(getPairingId()));
            logger.debug("{}Final state: {}", LOG_STATE, currentState);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("{}Error during pair remove: {} - Server: {}", LOG_ERROR, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Exception details", LOG_ERROR, e);
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            throw new HomekitServerException("Failed to remove pairing", e);
        }
    }

    // ========== Pairing Stage Methods ==========
    private void resetPairingState() {
        logger.debug("{}Resetting pairing state - Server: {}", LOG_STATE, new String(getPairingId()));
        sessionKey = null;
        sharedSecret = null;
        clientPublicKey = null;
        clientPrivateKey = null;
        isPairVerified = false;
        logger.debug("{}Pairing state reset completed - Server: {}", LOG_STATE, new String(getPairingId()));
    }

    private void resetVerificationState() {
        logger.debug("'{}' : Resetting verification state", new String(getPairingId()));
        sessionKey = null;
        sharedSecret = null;
        clientPublicKey = null;
        clientPrivateKey = null;
        isPairVerified = false;
        logger.debug("'{}' : Verification state reset completed", new String(getPairingId()));
    }

    private StageResult executePairingStage(int stage, PairingStageExecutor executor)
            throws HomekitServerException, InterruptedException, ExecutionException, IOException {
        logger.debug("{}Executing pair setup stage {} - preparing payload - Server: {}", LOG_STATE, stage,
                new String(getPairingId()));
        byte[] payload = executor.execute();

        logger.debug("{}Stage {} - sending payload - Server: {}", LOG_STATE, stage, new String(getPairingId()));
        Future<StageResult> stageFuture = sendPairSetupStage(payload);

        StageResult result = stageFuture.get();
        logger.debug("{}Stage {} - received response, success: {} - Server: {}", LOG_STATE, stage, !result.isFailure(),
                new String(getPairingId()));

        return result;
    }

    private void handlePairingFailure(int stage, StageResult result) throws HomekitServerException {
        logger.debug("{}Handling failure for stage {} - Server: {}", LOG_STATE, stage, new String(getPairingId()));
        if (result.error != null) {
            if (result.error == Error.UNAVAILABLE) {
                logger.warn(
                        "{}Pair setup failed - accessory is not available for pairing (already paired) - Server: {}",
                        LOG_STATE, new String(getPairingId()));
                logger.debug("{}Accessory reported UNAVAILABLE error - Server: {}", LOG_STATE,
                        new String(getPairingId()));
            } else {
                logger.warn("{}Pair setup failed at stage {} with error: {} - Server: {}", LOG_STATE, stage,
                        result.error, new String(getPairingId()));
                logger.debug("{}Stage {} error details: {} - Server: {}", LOG_STATE, stage, result.error,
                        new String(getPairingId()));
            }
        } else {
            logger.warn("{}Pair setup failed at stage {} with message: {} - Server: {}", LOG_STATE, stage,
                    result.message, new String(getPairingId()));
            logger.debug("{}Stage {} failure message details: {} - Server: {}", LOG_STATE, stage, result.message,
                    new String(getPairingId()));
        }
        setState(AccessoryServerState.UNPAIRED);
        logger.debug("{}State set to UNPAIRED after failure - Server: {}", LOG_STATE, new String(getPairingId()));
    }

    @FunctionalInterface
    private interface PairingStageExecutor {
        byte[] execute() throws IOException, HomekitServerException;
    }

    protected byte[] doPairSetupStage0() throws IOException, HomekitServerException {
        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.METHOD, Method.PAIR_SETUP_WITH_AUTH.getKey());

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage1(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{}Starting pair setup stage 1 - Server: {}", LOG_STATE, new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitServerException("Wrong STATE");
        }

        BigInteger publicKey = stageResult.decodeResult.getBigInt(Message.PUBLIC_KEY);
        logger.debug("{}Public key received - Server: {}", LOG_STATE, new String(getPairingId()));

        BigInteger salt = stageResult.decodeResult.getBigInt(Message.SALT);
        logger.debug("{}Salt received - Server: {}", LOG_STATE, new String(getPairingId()));

        if (SRP6Session == null) {
            SRP6Session = new HomekitClientSRP6Session();
            SRP6Session.setClientEvidenceRoutine(new HomekitEncryptionEngine.ClientEvidenceRoutineImpl());
            SRP6Session.setServerEvidenceRoutine(new HomekitEncryptionEngine.ServerEvidenceRoutineImpl());
            SRP6Session.setXRoutine(new XRoutineWithUserIdentity());
        }

        SRP6Session.step1("Pair-Setup", setupCode);

        SRP6ClientCredentials clientCredentials = null;
        try {
            clientCredentials = SRP6Session.step2(HomekitEncryptionEngine.SRP6Params, salt, publicKey);
        } catch (SRP6Exception e) {
            logger.error("{}SRP6 step 2 failed - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("SRP6 step 2 failed", e);
        }

        BigInteger clientPublicKey = clientCredentials.A;
        logger.debug("{}Client public key generated - Server: {}", LOG_STATE, new String(getPairingId()));

        BigInteger clientProof = clientCredentials.M1;
        logger.debug("{}Client proof generated - Server: {}", LOG_STATE, new String(getPairingId()));

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);
        encoder.add(Message.PROOF, clientProof);

        return encoder.toByteArray();
    }

    private StageResult handleStage1Verification(StageResult stage0Result)
            throws HomekitServerException, InterruptedException, ExecutionException, IOException {
        try {
            return executePairingStage(1, () -> doPairVerifyStage1(stage0Result));
        } catch (HomekitServerException e) {
            logger.error("'{}' : Authentication error in stage 1: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Sending authentication error to accessory", new String(getPairingId()));

            // Send authentication error to accessory
            Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(Message.STATE, (short) 0x03);
            encoder.add(Message.ERROR, Error.AUTHENTICATION);

            Future<StageResult> errorFuture = sendPairVerifyStage(encoder.toByteArray());
            return errorFuture.get();
        }
    }

    protected byte[] doPairSetupStage2(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{}Starting pair setup stage 2 - Server: {}", LOG_STATE, new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitServerException("Wrong STATE");
        }

        BigInteger proof = stageResult.decodeResult.getBigInt(Message.PROOF);

        try {
            SRP6Session.step3(proof);
        } catch (SRP6Exception e) {
            logger.error("{}SRP6 step 3 failed - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("SRP6 step 3 failed", e);
        }

        MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
        BigInteger S = SRP6Session.getSessionKey(false);
        byte[] sBytes = Byte.toByteArray(S);
        logger.debug("{}SRP session key generated - Server: {}", LOG_STATE, new String(getPairingId()));
        sharedSecret = digest.digest(sBytes);
        logger.debug("{}Shared secret generated - Server: {}", LOG_STATE, new String(getPairingId()));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.debug("{}Session key generated - Server: {}", LOG_STATE, new String(getPairingId()));

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.debug("{}Client device X generated - Server: {}", LOG_STATE, new String(getPairingId()));

        EdsaSigner signer = new EdsaSigner(secretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = Byte.joinBytes(clientDeviceX, getPairingId(), clientLongtermPublicKey);
        byte[] clientSignature = null;
        try {
            clientSignature = signer.sign(clientDeviceInfo);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{}Failed to sign client device info - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Failed to sign client device info", e);
        }
        logger.debug("{}Client signature generated - Server: {}", LOG_STATE, new String(getPairingId()));

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.IDENTIFIER, getPairingId());
        encoder.add(Message.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(Message.SIGNATURE, clientSignature);

        ChachaEncoder chachaEncoder = new ChachaEncoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x05);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage3(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{}Starting pair setup stage 3 - Server: {}", LOG_STATE, new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 6) {
            throw new HomekitServerException("Wrong STATE");
        }

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
        logger.debug("{}Plaintext decoded - Server: {}", LOG_STATE, new String(getPairingId()));

        DecodeResult d = TypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.debug("{}Destination pairing identifier received - Server: {}", LOG_STATE, new String(getPairingId()));
        byte[] destinationPublicKey = d.getBytes(Message.PUBLIC_KEY);
        logger.debug("{}Destination public key received - Server: {}", LOG_STATE, new String(getPairingId()));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.debug("{}Accessory signature received - Server: {}", LOG_STATE, new String(getPairingId()));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] accessoryDeviceX = new byte[32];
        hkdf.generateBytes(accessoryDeviceX, 0, 32);

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryDeviceX, destinationPairingIdentifier,
                destinationPublicKey);
        logger.debug("{}Accessory device info generated - Server: {}", LOG_STATE, new String(getPairingId()));

        try {
            if (!new EdsaVerifier(destinationPublicKey).verify(accessoryDeviceInfo, accessorySignature)) {
                logger.error("{}Signature verification failed - Server: {}", LOG_ERROR, new String(getPairingId()));
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e) {
            logger.error("{}Signature verification failed - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Signature verification failed", e);
        }

        addPairing(destinationPairingIdentifier, destinationPublicKey);
        SRP6Session = null;

        return null;
    }

    protected byte[] doPairVerifyStage0() throws IOException, HomekitServerException {
        logger.debug("{}Starting pair verify stage 0 - Server: {}", LOG_STATE, new String(getPairingId()));

        clientPublicKey = new byte[32];
        clientPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(clientPrivateKey);
        Curve25519.keygen(clientPublicKey, null, clientPrivateKey);

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage1(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{}Starting pair verify stage 1 - Server: {}", LOG_STATE, new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitServerException("Wrong STATE");
        }

        byte[] destinationPublicKey = stageResult.decodeResult.getBytes(Message.PUBLIC_KEY);
        logger.debug("{}Destination public key received - Server: {}", LOG_STATE, new String(getPairingId()));

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, clientPrivateKey, destinationPublicKey);
        logger.debug("{}Shared secret generated - Server: {}", LOG_STATE, new String(getPairingId()));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.debug("{}Session key generated - Server: {}", LOG_STATE, new String(getPairingId()));

        byte[] plaintext = null;
        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        try {
            plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
            logger.debug("{}Plaintext decoded - Server: {}", LOG_STATE, new String(getPairingId()));
        } catch (Exception e) {
            logger.error("{}Failed to decode ciphertext - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Ciphertext decoding failed", e);
        }

        DecodeResult d = TypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.debug("{}Destination pairing identifier received - Server: {}", LOG_STATE, new String(getPairingId()));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.debug("{}Accessory signature received - Server: {}", LOG_STATE, new String(getPairingId()));

        Pairing accessoryPairing = getPairing(destinationPairingIdentifier);

        if (accessoryPairing == null) {
            logger.error("{}Accessory is not paired - Server: {}", LOG_ERROR, new String(getPairingId()));
            throw new HomekitServerException("Accessory is not paired");
        } else {
            logger.debug("{}Accessory pairing found - Server: {}", LOG_STATE, new String(getPairingId()));
        }

        byte[] accessoryDeviceInfo = Byte.joinBytes(destinationPublicKey, destinationPairingIdentifier,
                clientPublicKey);

        try {
            boolean signatureVerification = new EdsaVerifier(accessoryPairing.getPublicKey())
                    .verify(accessoryDeviceInfo, accessorySignature);
            if (!signatureVerification) {
                logger.error("{}Signature verification failed - Server: {}", LOG_ERROR, new String(getPairingId()));
                throw new HomekitServerException("Signature verification failed");
            }
        } catch (Exception e) {
            logger.error("{}Signature verification failed - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Signature verification failed", e);
        }

        byte[] clientDeviceInfo = Byte.joinBytes(clientPublicKey, getPairingId(), destinationPublicKey);

        byte[] clientSignature = null;
        try {
            logger.debug("{}Signing client device info - Server: {}", LOG_STATE, new String(getPairingId()));
            clientSignature = new EdsaSigner(secretKey).sign(clientDeviceInfo);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("{}Failed to sign client device info - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
            throw new HomekitServerException("Failed to sign client device info", e);
        }

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.IDENTIFIER, getPairingId());
        encoder.add(Message.SIGNATURE, clientSignature);
        plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage2(StageResult stageResult) throws IOException, HomekitServerException {
        logger.debug("{}Starting pair verify stage 2 - Server: {}", LOG_STATE, new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitServerException("Wrong STATE");
        }

        byte[] writeKey = HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret);
        logger.debug("{}Write key generated - Server: {}", LOG_STATE, new String(getPairingId()));

        byte[] readKey = HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret);
        logger.debug("{}Read key generated - Server: {}", LOG_STATE, new String(getPairingId()));

        HomekitHttpDestinationOverHTTP destination = (HomekitHttpDestinationOverHTTP) httpClient.getDestination(
                stageResult.result.getRequest().getScheme(), stageResult.result.getRequest().getHost(),
                stageResult.result.getRequest().getPort());
        logger.debug("{}Setting encryption keys on destination - Server: {}", LOG_STATE, new String(getPairingId()));
        destination.setEncryptionKeys(readKey, writeKey);

        return null;
    }

    // ========== Communication Methods ==========
    protected Future<StageResult> sendPairSetupStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-setup");
    }

    protected Future<StageResult> sendPairVerifyStage(byte[] request) throws InterruptedException {
        return sendStage(request, "/pair-verify");
    }

    protected Future<StageResult> sendPairing(byte[] request) throws InterruptedException {
        return sendStage(request, "/pairings");
    }

    protected Future<StageResult> sendStage(byte[] request, String url) throws InterruptedException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{}Failed to create URI - Error: {}", LOG_ERROR, e1.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e1);
        }

        CompletableFuture<StageResult> completableFuture = new CompletableFuture<>();

        httpClient.newRequest(uri.toString()).method(HttpMethod.POST)
                .content(new BytesContentProvider(request), "application/pairing+tlv8")
                .header(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString())
                .send(new BufferingResponseListener(8 * 1024 * 1024) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            try {
                                byte[] body = getContent();

                                DecodeResult d = TypeLengthValueEncoderDecoder.decode(body);

                                if (d.getBytes(Message.ERROR) != null) {
                                    SRP6Session = null;
                                    StageResult stageResult = new StageResult(Error.get(d.getByte(Message.ERROR)));
                                    completableFuture.complete(stageResult);
                                    return;
                                }

                                short state = d.getByte(Message.STATE);
                                logger.info("{}Received State {} - Server: {}", LOG_STATE, state,
                                        new String(getPairingId()));

                                StageResult stageResult = new StageResult(d, result);
                                completableFuture.complete(stageResult);
                            } catch (IOException e) {
                                SRP6Session = null;
                                logger.error("{}Failed to decode response - Error: {}", LOG_ERROR, e.getMessage());
                                logger.debug("{}Exception details", LOG_ERROR, e);
                            }
                        } else {
                            StageResult stageResult = new StageResult(result.getResponseFailure().getMessage());
                            completableFuture.complete(stageResult);
                        }
                    }
                });

        return completableFuture;
    }

    protected Future<ContentResult> getContent(String url) throws InterruptedException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{}Failed to create URI - Error: {}", LOG_ERROR, e1.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e1);
        }

        CompletableFuture<ContentResult> completableFuture = new CompletableFuture<>();

        httpClient.newRequest(uri.toString()).method(HttpMethod.GET)
                .send(new BufferingResponseListener(8 * 1024 * 1024) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            byte[] body = getContent();
                            ContentResult stageResult = new ContentResult(body, result);
                            completableFuture.complete(stageResult);
                        } else {
                            ContentResult stageResult = new ContentResult(
                                    result.getResponseFailure().getMessage().getBytes(), result);
                            completableFuture.complete(stageResult);
                        }
                    }
                });

        return completableFuture;
    }

    protected Future<ContentResult> putContent(String url, byte[] body) throws InterruptedException {
        URI uri = null;
        try {
            uri = new URI("http", null, address.getHostAddress(), port, url, null, null);
        } catch (URISyntaxException e1) {
            logger.error("{}Failed to create URI - Error: {}", LOG_ERROR, e1.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e1);
        }

        CompletableFuture<ContentResult> completableFuture = new CompletableFuture<>();

        httpClient.newRequest(uri.toString()).method(HttpMethod.PUT)
                .content(new BytesContentProvider(body), "application/pairing+json")
                .header(HttpHeader.CONNECTION.asString(), HttpHeader.KEEP_ALIVE.asString())
                .send(new BufferingResponseListener(8 * 1024 * 1024) {
                    @Override
                    public void onComplete(Result result) {
                        if (!result.isFailed()) {
                            byte[] body = getContent();
                            ContentResult stageResult = new ContentResult(body, result);
                            completableFuture.complete(stageResult);
                        } else {
                            ContentResult stageResult = new ContentResult(
                                    result.getResponseFailure().getMessage().getBytes(), result);
                            completableFuture.complete(stageResult);
                        }
                    }
                });

        return completableFuture;
    }

    // ========== Event Handling Methods ==========
    public void handleEvent(byte[] body) {
        try {
            logger.debug("{}Processing event - Server: {}", LOG_STATE, new String(getPairingId()));
            Byte.logBuffer(logger, "handleEvent", Byte.toHexString(getPairingId()), ByteBuffer.wrap(body));
        } catch (IOException e) {
            logger.error("{}Failed to process event - Error: {}", LOG_ERROR, e.getMessage());
            logger.debug("{}Exception details", LOG_ERROR, e);
        }
    }

    @Override
    public void onCharacteristicEvent(CharacteristicEvent event) {
        if (event.getEventType() == CharacteristicEventType.CHARACTERISTIC_START_EVENTS) {
            logger.debug("{}Starting events for characteristic - Server: {}", LOG_STATE, new String(getPairingId()));
            subscriveEvents(event.getCharacteristic(), true);
        } else if (event.getEventType() == CharacteristicEventType.CHARACTERISTIC_STOP_EVENTS) {
            logger.debug("{}Stopping events for characteristic - Server: {}", LOG_STATE, new String(getPairingId()));
            subscriveEvents(event.getCharacteristic(), false);
        }
    }

    // ========== Accessory Management Methods ==========
    @Override
    public long getNextAvailableAccessoryId() {
        return 0;
    }

    public Collection<Accessory> getRemoteAccessories() {
        Collection<Accessory> result = new HashSet<Accessory>();

        if (isPaired() && isPairVerified() && isSecure()) {
            Future<ContentResult> contentFuture;
            ContentResult contentResult = null;
            try {
                contentFuture = getContent("/accessories");
                contentResult = contentFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("{}Error getting remote accessories - Error: {}", LOG_ERROR, e.getMessage());
                logger.debug("{}Exception details", LOG_ERROR, e);
            }

            logger.info("{}Received accessories data - Server: {}", LOG_STATE, new String(getPairingId()));

            if (contentResult != null && contentResult.result.getResponse().getStatus() == 200) {
                JsonArray accessories = Json.createReader(new ByteArrayInputStream(contentResult.body)).readObject()
                        .getJsonArray("accessories");
                for (JsonValue value : accessories) {
                    result.add(new GenericAccessory(value));
                }
            }
        }

        return result;
    }

    public boolean subscriveEvents(Characteristic<?> characteristic, boolean subscribe) {
        if (!isPairVerified()) {
            logger.debug("{}Cannot subscribe to events - not paired - Server: {}", LOG_STATE,
                    new String(getPairingId()));
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
            ContentResult contentResult = contentFuture.get();

            if (contentResult.result.getResponse().getStatus() == 204) {
                logger.debug("{}Successfully subscribed to events for characteristic {} - Server: {}", LOG_STATE,
                        characteristic.getUID(), new String(getPairingId()));
                characteristic.setHasEvents(true);
                return true;
            } else {
                logger.warn("{}Failed to subscribe to events for characteristic {} - Status: {} - Server: {}",
                        LOG_STATE, characteristic.getUID(), contentResult.result.getResponse().getStatus(),
                        new String(getPairingId()));
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("{}Error subscribing to events for characteristic {}: {} - Server: {}", LOG_ERROR,
                    characteristic.getUID(), e.getMessage(), new String(getPairingId()));
            return false;
        }
    }

    @Override
    public void updateAccessories() throws HomekitAccessoryOperationException {
        if (!isPairVerified()) {
            logger.debug("{}Cannot update accessories - not paired - Server: {}", LOG_STATE,
                    new String(getPairingId()));
            return;
        }

        try {
            // Get current accessories
            Collection<Accessory> currentAccessories = new HashSet<>(getAccessories());

            // Get remote accessories
            Collection<Accessory> remoteAccessories = getRemoteAccessories();

            // Find new accessories to add
            for (Accessory remoteAccessory : remoteAccessories) {
                boolean found = false;
                for (Accessory currentAccessory : currentAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.info("{}Adding new accessory {} - Server: {}", LOG_STATE, remoteAccessory,
                            new String(getPairingId()));
                    addAccessory(remoteAccessory);
                    notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.ACCESSORY_ADDED);
                }
            }

            // Find accessories to remove
            for (Accessory currentAccessory : currentAccessories) {
                boolean found = false;
                for (Accessory remoteAccessory : remoteAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.info("{}Removing accessory {} - Server: {}", LOG_STATE, currentAccessory,
                            new String(getPairingId()));
                    removeAccessory(currentAccessory);
                    notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.ACCESSORY_REMOVED);
                }
            }

            // Compare services and characteristics for each accessory
            for (Accessory currentAccessory : getAccessories()) {
                for (Accessory remoteAccessory : remoteAccessories) {
                    if (currentAccessory.getAccessoryId() == remoteAccessory.getAccessoryId()) {
                        // Compare services
                        Collection<Service> currentServices = currentAccessory.getServices();
                        Collection<Service> remoteServices = remoteAccessory.getServices();

                        // Find new services to add
                        for (Service remoteService : remoteServices) {
                            boolean found = false;
                            for (Service currentService : currentServices) {
                                if (currentService.getInstanceId() == remoteService.getInstanceId()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                logger.info("{}Adding new service {} to accessory {} - Server: {}", LOG_STATE,
                                        remoteService, currentAccessory, new String(getPairingId()));
                                currentAccessory.addService(remoteService);
                                notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.SERVICE_ADDED);
                            }
                        }

                        // Find services to remove
                        for (Service currentService : currentServices) {
                            boolean found = false;
                            for (Service remoteService : remoteServices) {
                                if (currentService.getInstanceId() == remoteService.getInstanceId()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                logger.info("{}Removing service {} from accessory {} - Server: {}", LOG_STATE,
                                        currentService, currentAccessory, new String(getPairingId()));
                                currentAccessory.removeService(currentService);
                                notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.SERVICE_REMOVED);
                            }
                        }

                        // Compare characteristics for each service
                        for (Service currentService : currentAccessory.getServices()) {
                            for (Service remoteService : remoteServices) {
                                if (currentService.getInstanceId() == remoteService.getInstanceId()) {
                                    Set<Characteristic<?>> currentCharacteristics = currentService.getCharacteristics();
                                    Set<Characteristic<?>> remoteCharacteristics = remoteService.getCharacteristics();

                                    // Find new characteristics to add
                                    for (Characteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                        boolean found = false;
                                        for (Characteristic<?> currentCharacteristic : currentCharacteristics) {
                                            if (currentCharacteristic.getInstanceId() == remoteCharacteristic
                                                    .getInstanceId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            logger.info(
                                                    "{}Adding new characteristic {} to service {} of accessory {} - Server: {}",
                                                    LOG_STATE, remoteCharacteristic, currentService, currentAccessory,
                                                    new String(getPairingId()));
                                            currentService.addCharacteristic(remoteCharacteristic);
                                            notifyChangeListeners(
                                                    AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_ADDED);
                                        }
                                    }

                                    // Find characteristics to remove
                                    for (Characteristic<?> currentCharacteristic : currentCharacteristics) {
                                        boolean found = false;
                                        for (Characteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                            if (currentCharacteristic.getInstanceId() == remoteCharacteristic
                                                    .getInstanceId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            logger.info(
                                                    "{}Removing characteristic {} from service {} of accessory {} - Server: {}",
                                                    LOG_STATE, currentCharacteristic, currentService, currentAccessory,
                                                    new String(getPairingId()));
                                            currentService.removeCharacteristic(currentCharacteristic);
                                            notifyChangeListeners(
                                                    AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_REMOVED);
                                        }
                                    }

                                    // Find characteristics that are the same, and check if they are equal()
                                    for (Characteristic<?> currentCharacteristic : currentCharacteristics) {
                                        for (Characteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                            if (currentCharacteristic.getInstanceId() == remoteCharacteristic
                                                    .getInstanceId()) {
                                                if (!currentCharacteristic.equals(remoteCharacteristic)) {
                                                    logger.info("{}Characteristic {} is different from {} - Server: {}",
                                                            LOG_STATE, currentCharacteristic, remoteCharacteristic,
                                                            new String(getPairingId()));
                                                    currentCharacteristic.updateWith(remoteCharacteristic);
                                                    notifyChangeListeners(
                                                            AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_UPDATED);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("{}Error updating accessories: {} - Server: {}", LOG_STATE, e.getMessage(),
                    new String(getPairingId()));
            logger.debug("{}Exception details", LOG_STATE, e);
            throw new HomekitAccessoryOperationException("Failed to update accessories", e);
        }
    }

    @Override
    public void addAccessory(Accessory accessory) throws HomekitAccessoryOperationException {
        super.addAccessory(accessory);
        for (Service service : accessory.getServices()) {
            for (Characteristic<?> characteristic : service.getCharacteristics()) {
                characteristic.addChangeListener(this);
            }
        }
    }

    @Override
    public void removeAccessory(Accessory accessory) throws HomekitAccessoryOperationException {
        super.removeAccessory(accessory);
        for (Service service : accessory.getServices()) {
            for (Characteristic<?> characteristic : service.getCharacteristics()) {
                characteristic.removeChangeListener(this);
            }
        }
    }

    // ========== Inner Classes ==========
    protected class StageResult {
        public StageResult(DecodeResult decodeResult, Result result) {
            this.decodeResult = decodeResult;
            this.result = result;
        }

        public StageResult(String message) {
            this.message = message;
        }

        public StageResult(Error error) {
            this.error = error;
        }

        public boolean isFailure() {
            return message != null || error != null;
        }

        public DecodeResult decodeResult;
        public Result result;
        public String message;
        public Error error;
    }

    public static class ContentResult {
        public Result result;
        public byte[] body;
        public String message;

        public ContentResult(byte[] body, Result result) {
            this.body = body;
            this.result = result;
        }

        public ContentResult(String message) {
            this.message = message;
        }
    }

    // ========== Utility Methods ==========
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> beanClass) {
        JsonValue value = Json.createReader(new StringReader(json)).read();
        return (T) decode(value, beanClass);
    }

    private static Object decode(JsonValue jsonValue, Type targetType) {
        if (jsonValue.getValueType() == ValueType.NULL) {
            return null;
        } else if (jsonValue.getValueType() == ValueType.TRUE || jsonValue.getValueType() == ValueType.FALSE) {
            return decodeBoolean(jsonValue, targetType);
        } else if (jsonValue instanceof JsonNumber) {
            return decodeNumber((JsonNumber) jsonValue, targetType);
        } else if (jsonValue instanceof JsonString) {
            return decodeString((JsonString) jsonValue, targetType);
        } else if (jsonValue instanceof JsonArray) {
            return decodeArray((JsonArray) jsonValue, targetType);
        } else if (jsonValue instanceof JsonObject) {
            return decodeObject((JsonObject) jsonValue, targetType);
        } else {
            throw new UnsupportedOperationException("Unsupported json value: " + jsonValue);
        }
    }

    private static Object decodeBoolean(JsonValue jsonValue, Type targetType) {
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(jsonValue.toString());
        } else {
            throw new UnsupportedOperationException("Unsupported boolean type: " + targetType);
        }
    }

    private static Object decodeNumber(JsonNumber jsonNumber, Type targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return jsonNumber.intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return jsonNumber.longValue();
        } else {
            throw new UnsupportedOperationException("Unsupported number type: " + targetType);
        }
    }

    private static Object decodeString(JsonString jsonString, Type targetType) {
        if (targetType == String.class) {
            return jsonString.getString();
        } else if (targetType == Date.class) {
            try {
                return new SimpleDateFormat("MMM dd, yyyy H:mm:ss a", Locale.ENGLISH).parse(jsonString.getString());
            } catch (ParseException e) {
                throw new UnsupportedOperationException("Unsupported date format: " + jsonString.getString());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported string type: " + targetType);
        }
    }

    private static Object decodeArray(JsonArray jsonArray, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (List.class.isAssignableFrom(targetClass)) {
            Class<?> elementClass = (Class<?>) ((ParameterizedType) targetType).getActualTypeArguments()[0];
            List<Object> list = new ArrayList<>();

            for (JsonValue item : jsonArray) {
                list.add(decode(item, elementClass));
            }

            return list;
        } else if (targetClass.isArray()) {
            Class<?> elementClass = targetClass.getComponentType();
            Object array = Array.newInstance(elementClass, jsonArray.size());

            for (int i = 0; i < jsonArray.size(); i++) {
                Array.set(array, i, decode(jsonArray.get(i), elementClass));
            }

            return array;
        } else {
            throw new UnsupportedOperationException("Unsupported array type: " + targetClass);
        }
    }

    private static Object decodeObject(JsonObject object, Type targetType) {
        Class<?> targetClass = (Class<?>) ((targetType instanceof ParameterizedType)
                ? ((ParameterizedType) targetType).getRawType()
                : targetType);

        if (Map.class.isAssignableFrom(targetClass)) {
            Class<?> valueClass = (Class<?>) ((ParameterizedType) targetType).getActualTypeArguments()[1];
            Map<String, Object> map = new LinkedHashMap<>();

            for (Entry<String, JsonValue> entry : object.entrySet()) {
                map.put(entry.getKey(), decode(entry.getValue(), valueClass));
            }

            return map;
        } else {
            try {
                Constructor[] ctors = targetClass.getDeclaredConstructors();
                Constructor ctor = null;
                for (int i = 0; i < ctors.length; i++) {
                    ctor = ctors[i];
                    if (ctor.getGenericParameterTypes().length == 0) {
                        break;
                    }
                }

                // Object bean = targetClass.newInstance(); Constructor.newInstance(targetClass);
                Object bean = ctor.newInstance(targetClass);

                for (PropertyDescriptor property : Introspector.getBeanInfo(targetClass).getPropertyDescriptors()) {
                    if (property.getWriteMethod() != null && object.containsKey(property.getName())) {
                        property.getWriteMethod().invoke(bean, decode(object.get(property.getName()),
                                property.getWriteMethod().getGenericParameterTypes()[0]));
                    }
                }

                return bean;
            } catch (Exception e) {
                throw new UnsupportedOperationException("Unsupported object type: " + targetClass, e);
            }
        }
    }

    protected void checkPairingStatus() throws HomekitServerException, IOException {
        logger.debug("{}Checking pairing status - Server: {}", LOG_STATE, new String(getPairingId()));
        // ... existing code ...
    }

    protected void handleVerificationFailure(int stage, StageResult result) throws HomekitServerException {
        logger.debug("{}Handling verification failure for stage {} - Server: {}", LOG_STATE, stage,
                new String(getPairingId()));
        if (result.error != null) {
            logger.warn("{}Pair verification failed at stage {} with error: {} - Server: {}", LOG_STATE, stage,
                    result.error, new String(getPairingId()));
            logger.debug("{}Stage {} error details: {} - Server: {}", LOG_STATE, stage, result.error,
                    new String(getPairingId()));
        } else {
            logger.warn("{}Pair verification failed at stage {} with message: {} - Server: {}", LOG_STATE, stage,
                    result.message, new String(getPairingId()));
            logger.debug("{}Stage {} failure message details: {} - Server: {}", LOG_STATE, stage, result.message,
                    new String(getPairingId()));
        }
        setState(AccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("{}State set to PAIR_UNVERIFIED after failure - Server: {}", LOG_STATE,
                new String(getPairingId()));
    }

    @Override
    public void advertise() {
        // no Op for RemoteAccessoryServer
    }
}
