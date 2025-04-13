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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
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
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Error;
import org.openhab.io.homekit.api.hap.Message;
import org.openhab.io.homekit.api.hap.Method;
import org.openhab.io.homekit.api.hap.Pairing;
import org.openhab.io.homekit.api.hap.Service;
import org.openhab.io.homekit.api.listener.AccessoryServerChangeListener;
import org.openhab.io.homekit.api.registry.AccessoryRegistry;
import org.openhab.io.homekit.api.registry.PairingRegistry;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.crypto.EdsaSigner;
import org.openhab.io.homekit.crypto.EdsaVerifier;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.internal.accessory.AccessoryServerState;
import org.openhab.io.homekit.internal.accessory.GenericAccessory;
import org.openhab.io.homekit.internal.client.HomekitClientSRP6Session;
import org.openhab.io.homekit.internal.client.HomekitException;
import org.openhab.io.homekit.internal.events.AccessoryServerEvent;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpClientTransportOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitHttpDestinationOverHTTP;
import org.openhab.io.homekit.internal.http.jetty.HomekitProtocolHandler;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder.Encoder;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.XRoutineWithUserIdentity;

import djb.Curve25519;

public abstract class AbstractRemoteAccessoryServer extends AbstractAccessoryServer {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteAccessoryServer.class);

    private static final String HTTP_SCHEME = "http";

    private HomekitClientSRP6Session SRP6Session;
    private byte[] sessionKey;
    private byte[] sharedSecret;
    private byte[] clientPublicKey;
    private byte[] clientPrivateKey;

    private final ScheduledExecutorService scheduler;
    private @Nullable ScheduledFuture<?> connectionMonitorJob;
    private @Nullable HttpClient httpClient;
    private boolean isPairVerified;

    public AbstractRemoteAccessoryServer(InetAddress address, int port, byte[] pairingIdentifier, byte[] secretKey,
            AccessoryRegistry accessoryRegistry, PairingRegistry pairingRegistry) {
        super(address, port, pairingIdentifier, secretKey, accessoryRegistry, pairingRegistry);
        this.setupCode = "";
        this.isPairVerified = false;
        this.httpClient = new HttpClient(new HomekitHttpClientTransportOverHTTP(), null);
        this.scheduler = org.openhab.core.common.ThreadPoolManager.getScheduledPool("homekit-remote");

        // TODO : Detect when the remote end closes the connection -> Thing should go offline
        start();
    }

    @Deactivate
    public void dispose() {
        stop();
    }

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

    public AbstractRemoteAccessoryServer(InetAddress address, int port, AccessoryRegistry accessoryRegistry,
            PairingRegistry pairingRegistry) {
        this(address, port, generatePairingId(), generateSecretKey(), accessoryRegistry, pairingRegistry);
    }

    protected void startConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
        }

        // Schedule periodic connection monitoring
        connectionMonitorJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                monitorConnection();
            } catch (Exception e) {
                logger.warn("'{}' : Error during connection monitoring: {}", new String(getPairingId()), e.getMessage());
                logger.debug("'{}' : Exception details", new String(getPairingId()), e);
                setState(AccessoryServerState.DISCONNECTED);
            }
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void monitorConnection() throws HomekitException, IOException {
        logger.debug("'{}' : Monitoring connection state", new String(getPairingId()));
        
        // Check if we were previously disconnected
        if (currentState == AccessoryServerState.DISCONNECTED) {
            logger.info("'{}' : Connection was lost, attempting to re-establish", new String(getPairingId()));
            
            // If we're not paired at all, proceed with pairing
            if (!isPaired()) {
                logger.info("'{}' : Setting up new pairing", new String(getPairingId()));
                try {
                    pairSetup();
                } catch (IOException e) {
                    logger.warn("'{}' : Error during pair setup: {}", new String(getPairingId()), e.getMessage());
                    logger.debug("'{}' : Exception details", new String(getPairingId()), e);
                    setState(AccessoryServerState.MISSING_SETUP_CODE);
                    return;
                }
            }
        }

        // If connected but not paired, attempt to pair
        if (currentState == AccessoryServerState.CONNECTED && !isPaired()) {
            logger.info("'{}' : Connected but not paired, attempting to pair", new String(getPairingId()));
            try {
                pairSetup();
                if (isPaired()) {
                    pairVerify();
                }
            } catch (IOException e) {
                logger.warn("'{}' : Error during pair setup: {}", new String(getPairingId()), e.getMessage());
                logger.debug("'{}' : Exception details", new String(getPairingId()), e);
                setState(AccessoryServerState.MISSING_SETUP_CODE);
                return;
            }
        }

        // If paired, verify the connection
        if (isPaired()) {
            logger.debug("'{}' : Verifying connection", new String(getPairingId()));
            if (!isPairVerified()) {
                logger.info("'{}' : Connection not verified, attempting verification", new String(getPairingId()));
                if (!pairVerify()) {
                    logger.warn("'{}' : Connection verification failed", new String(getPairingId()));
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
                logger.warn("'{}' : Connection is no longer secure", new String(getPairingId()));
                setState(AccessoryServerState.DISCONNECTED);
                return;
            }

            setState(AccessoryServerState.CONNECTED);
            logger.debug("'{}' : Connection verified and secure", new String(getPairingId()));
        }
    }

    private void checkPairingStatus() {
        try {
            Future<ContentResult> contentFuture = getContent("/pairings");
            ContentResult contentResult = contentFuture.get();
            if (contentResult.result.getResponse().getStatus() == 200) {
                // Parse the pairings list to check if we're already paired
                byte[] responseContent = contentResult.body;
                if (responseContent != null && responseContent.length > 0) {
                    String pairingsList = new String(responseContent, StandardCharsets.UTF_8);
                    String pairingIdStr = new String(getPairingId(), StandardCharsets.UTF_8);
                    
                    // Check if our controller is in the list
                    if (!pairingsList.contains(pairingIdStr)) {
                        logger.info("'{}' : Accessory is paired with other controllers, but not with us", 
                                new String(getPairingId()));
                        setState(AccessoryServerState.PAIRED_TO_OTHER_CONTROLLER);
                        return;
                    } else {
                        logger.info("'{}' : Accessory is already paired with us and other controllers", 
                                new String(getPairingId()));
                        setState(AccessoryServerState.PAIRED);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.debug("'{}' : Error checking pairing status: {}", new String(getPairingId()), e.getMessage());
        }
    }

    protected void stopConnectionMonitor() {
        if (connectionMonitorJob != null) {
            connectionMonitorJob.cancel(true);
            connectionMonitorJob = null;
        }
    }

    @Override
    public void start() {
        try {
            if (httpClient != null) {
                httpClient.start();
                ProtocolHandlers handlers = httpClient.getProtocolHandlers();
                handlers.clear();
                handlers.put(new HomekitProtocolHandler(this));
                setState(AccessoryServerState.CONNECTED);
                
                // Create a test request to check connection using the address member
                String url = String.format("http://%s:%d", address.getHostAddress(), port);
                Request request = httpClient.newRequest(url);
                request.onRequestFailure((req, failure) -> {
                    logger.warn("'{}' : HTTP client connection failed", new String(getPairingId()));
                    logger.debug("'{}' : Failure details: {}", new String(getPairingId()), failure);
                    setState(AccessoryServerState.DISCONNECTED);
                });
                request.send();
            }
        } catch (Exception e) {
            logger.error("'{}' : Error starting HTTP client: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.DISCONNECTED);
        }

        startConnectionMonitor();
    }

    @Override
    public void stop() {
        stopConnectionMonitor();

        logger.info("'{}' : Stopping server", new String(getPairingId()));
        try {
            if (isPaired()) {
                pairRemove();
            }
        } catch (HomekitException | IOException e) {
            logger.warn("'{}' : Error removing pairing during stop: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
        }

        try {
            if (httpClient != null) {
                httpClient.stop();
            }
            setState(AccessoryServerState.STOPPED);
        } catch (Exception e) {
            logger.error("'{}' : Error stopping HTTP client: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
        }
    }

    public void advertise() {
        // No Operation
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

    public void pairSetup() throws IOException {
        logger.info("'{}' : Starting pair setup process", new String(getPairingId()));
        logger.debug("'{}' : Current state: {}", new String(getPairingId()), currentState);

        // Validate setup code
        if (setupCode == null || setupCode.isEmpty()) {
            logger.warn("'{}' : Unable to pair with {}:{} because no setup code is set", 
                new String(getPairingId()), address.getHostAddress(), port);
            setState(AccessoryServerState.MISSING_SETUP_CODE);
            return;
        }

        // Initialize pairing state
        resetPairingState();
        setState(AccessoryServerState.PAIR_SETUP_INITIAL);
        logger.debug("'{}' : Pairing state reset, starting authentication", new String(getPairingId()));

        try {
            // Stage 0: Initial Setup
            logger.debug("'{}' : Starting Stage 0 - Initial Setup", new String(getPairingId()));
            StageResult stage0Result = executePairingStage(0, () -> doPairSetupStage0());
            if (stage0Result.isFailure()) {
                handlePairingFailure(0, stage0Result);
                return;
            }
            logger.debug("'{}' : Stage 0 completed successfully", new String(getPairingId()));

            // Stage 1: SRP Protocol Exchange
            logger.debug("'{}' : Starting Stage 1 - SRP Protocol Exchange", new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_SRP);
            StageResult stage1Result = executePairingStage(1, () -> doPairSetupStage1(stage0Result));
            if (stage1Result.isFailure()) {
                handlePairingFailure(1, stage1Result);
                return;
            }
            logger.debug("'{}' : Stage 1 completed successfully", new String(getPairingId()));

            // Stage 2: Verify Proof
            logger.debug("'{}' : Starting Stage 2 - Verify Proof", new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_VERIFY);
            StageResult stage2Result = executePairingStage(2, () -> doPairSetupStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handlePairingFailure(2, stage2Result);
                return;
            }
            logger.debug("'{}' : Stage 2 completed successfully", new String(getPairingId()));

            // Stage 3: Exchange Keys
            logger.debug("'{}' : Starting Stage 3 - Exchange Keys", new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage3Result = executePairingStage(3, () -> doPairSetupStage3(stage2Result));
            if (stage3Result.isFailure()) {
                handlePairingFailure(3, stage3Result);
                return;
            }
            logger.debug("'{}' : Stage 3 completed successfully", new String(getPairingId()));
            
            // Pairing completed successfully
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            logger.info("'{}' : Pair setup completed successfully", new String(getPairingId()));
            logger.debug("'{}' : Final state: {}", new String(getPairingId()), currentState);

        } catch (HomekitException e) {
            logger.error("'{}' : Pair setup failed with error: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Homekit exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.UNPAIRED);
        } catch (Exception e) {
            logger.error("'{}' : Unexpected error during pair setup: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.UNPAIRED);
        }
    }

    private void resetPairingState() {
        logger.debug("'{}' : Resetting pairing state", new String(getPairingId()));
        sessionKey = null;
        sharedSecret = null;
        clientPublicKey = null;
        clientPrivateKey = null;
        isPairVerified = false;
        logger.debug("'{}' : Pairing state reset completed", new String(getPairingId()));
    }

    private StageResult executePairingStage(int stage, PairingStageExecutor executor) 
            throws IOException, HomekitException, InterruptedException, ExecutionException {
        logger.debug("'{}' : Executing pair setup stage {} - preparing payload", new String(getPairingId()), stage);
        byte[] payload = executor.execute();
        
        logger.debug("'{}' : Stage {} - sending payload", new String(getPairingId()), stage);
        Future<StageResult> stageFuture = sendPairSetupStage(payload);
        
        StageResult result = stageFuture.get();
        logger.debug("'{}' : Stage {} - received response, success: {}", 
            new String(getPairingId()), stage, !result.isFailure());
        
        return result;
    }

    private void handlePairingFailure(int stage, StageResult result) {
        logger.debug("'{}' : Handling failure for stage {}", new String(getPairingId()), stage);
        if (result.error != null) {
            if (result.error == Error.UNAVAILABLE) {
                logger.warn("'{}' : Pair setup failed - accessory is not available for pairing (already paired)", 
                    new String(getPairingId()));
                logger.debug("'{}' : Accessory reported UNAVAILABLE error", new String(getPairingId()));
            } else {
                logger.warn("'{}' : Pair setup failed at stage {} with error: {}", 
                    new String(getPairingId()), stage, result.error);
                logger.debug("'{}' : Stage {} error details: {}", new String(getPairingId()), stage, result.error);
            }
        } else {
            logger.warn("'{}' : Pair setup failed at stage {} with message: {}", 
                new String(getPairingId()), stage, result.message);
            logger.debug("'{}' : Stage {} failure message details: {}", 
                new String(getPairingId()), stage, result.message);
        }
        setState(AccessoryServerState.UNPAIRED);
        logger.debug("'{}' : State set to UNPAIRED after failure", new String(getPairingId()));
    }

    @FunctionalInterface
    private interface PairingStageExecutor {
        byte[] execute() throws IOException, HomekitException;
    }

    @Override
    public boolean pairVerify() {
        logger.info("'{}' : Starting pair verify process", new String(getPairingId()));
        logger.debug("'{}' : Current state: {}", new String(getPairingId()), currentState);

        // Reset verification state
        resetVerificationState();
        setState(AccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("'{}' : Verification state reset", new String(getPairingId()));

        try {
            // Stage 0: Initial Verification
            logger.debug("'{}' : Starting Stage 0 - Initial Verification", new String(getPairingId()));
            StageResult stage0Result = executePairingStage(0, () -> doPairVerifyStage0());
            if (stage0Result.isFailure()) {
                handleVerificationFailure(0, stage0Result);
                return false;
            }
            logger.debug("'{}' : Stage 0 completed successfully", new String(getPairingId()));

            // Stage 1: Exchange Keys
            logger.debug("'{}' : Starting Stage 1 - Exchange Keys", new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_VERIFY);
            
            // Handle stage 1 with authentication error handling
            final StageResult stage1Result = handleStage1Verification(stage0Result);
            if (stage1Result.isFailure()) {
                handleVerificationFailure(1, stage1Result);
                return false;
            }
            logger.debug("'{}' : Stage 1 completed successfully", new String(getPairingId()));

            // Stage 2: Final Verification
            logger.debug("'{}' : Starting Stage 2 - Final Verification", new String(getPairingId()));
            setState(AccessoryServerState.PAIR_SETUP_EXCHANGE);
            StageResult stage2Result = executePairingStage(2, () -> doPairVerifyStage2(stage1Result));
            if (stage2Result.isFailure()) {
                handleVerificationFailure(2, stage2Result);
                return false;
            }
            logger.debug("'{}' : Stage 2 completed successfully", new String(getPairingId()));

            // Verification completed successfully
            isPairVerified = true;
            setState(AccessoryServerState.PAIR_VERIFIED);
            logger.info("'{}' : Pair verification completed successfully", new String(getPairingId()));
            logger.debug("'{}' : Final state: {}", new String(getPairingId()), currentState);
            return true;

        } catch (HomekitException e) {
            logger.error("'{}' : Pair verification failed with error: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Homekit exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            return false;
        } catch (Exception e) {
            logger.error("'{}' : Unexpected error during pair verification: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            return false;
        }
    }

    private StageResult handleStage1Verification(StageResult stage0Result) 
            throws HomekitException, InterruptedException, ExecutionException, IOException {
        try {
            return executePairingStage(1, () -> doPairVerifyStage1(stage0Result));
        } catch (HomekitException e) {
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

    private void resetVerificationState() {
        logger.debug("'{}' : Resetting verification state", new String(getPairingId()));
        sessionKey = null;
        sharedSecret = null;
        clientPublicKey = null;
        clientPrivateKey = null;
        isPairVerified = false;
        logger.debug("'{}' : Verification state reset completed", new String(getPairingId()));
    }

    private void handleVerificationFailure(int stage, StageResult result) {
        logger.debug("'{}' : Handling verification failure for stage {}", new String(getPairingId()), stage);
        if (result.error != null) {
            if (result.error == Error.UNAVAILABLE) {
                logger.warn("'{}' : Pair verification failed - accessory is not available", new String(getPairingId()));
                logger.debug("'{}' : Accessory reported UNAVAILABLE error", new String(getPairingId()));
            } else {
                logger.warn("'{}' : Pair verification failed at stage {} with error: {}", 
                    new String(getPairingId()), stage, result.error);
                logger.debug("'{}' : Stage {} error details: {}", new String(getPairingId()), stage, result.error);
            }
        } else {
            logger.warn("'{}' : Pair verification failed at stage {} with message: {}", 
                new String(getPairingId()), stage, result.message);
            logger.debug("'{}' : Stage {} failure message details: {}", 
                new String(getPairingId()), stage, result.message);
        }
        setState(AccessoryServerState.PAIR_UNVERIFIED);
        logger.debug("'{}' : State set to PAIR_UNVERIFIED after failure", new String(getPairingId()));
    }

    @Override
    public void pairRemove() throws HomekitException, IOException {
        logger.info("'{}' : Starting pair remove process", new String(getPairingId()));
        logger.debug("'{}' : Current state: {}", new String(getPairingId()), currentState);

        if (!isPaired()) {
            logger.warn("'{}' : Cannot remove pairing - accessory is not paired", new String(getPairingId()));
            setState(AccessoryServerState.UNPAIRED);
            return;
        }

        if (!isPairVerified || !isSecure()) {
            logger.warn("'{}' : Cannot remove pairing - accessory is {} verified and connection is {} secured",
                    new String(getPairingId()), isPairVerified ? "already" : "not", isSecure() ? "" : "not ");
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            return;
        }

        try {
            // Prepare remove pairing request
            logger.debug("'{}' : Preparing remove pairing request", new String(getPairingId()));
            Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
            encoder.add(Message.STATE, (short) 0x01);
            encoder.add(Message.METHOD, Method.REMOVE_PAIRING.getKey());
            encoder.add(Message.IDENTIFIER, getPairingId());

            // Send remove pairing request
            logger.debug("'{}' : Sending remove pairing request", new String(getPairingId()));
            Future<StageResult> stageFuture = sendPairing(encoder.toByteArray());
            StageResult stageResult = stageFuture.get();

            // Verify response state
            short state = stageResult.decodeResult.getByte(Message.STATE);
            if (state != 2) {
                logger.error("'{}' : Invalid state in remove pairing response: {}", new String(getPairingId()), state);
                throw new HomekitException("Wrong STATE");
            }

            // Check for errors in response
            if (stageResult.decodeResult.getBytes(Message.ERROR) != null) {
                Error error = Error.get(stageResult.decodeResult.getByte(Message.ERROR));
                logger.warn("'{}' : Accessory failed to remove pairing: {}", new String(getPairingId()), error);
                setState(AccessoryServerState.PAIR_UNVERIFIED);
                return;
            }

            // Remove all pairings
            logger.debug("'{}' : Removing all pairings", new String(getPairingId()));
            for (Pairing pairing : getPairings()) {
                removePairing(pairing.getDestinationId());
            }

            // Update state
            isPairVerified = false;
            setState(AccessoryServerState.UNPAIRED);
            logger.info("'{}' : Successfully removed pairing", new String(getPairingId()));
            logger.debug("'{}' : Final state: {}", new String(getPairingId()), currentState);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("'{}' : Error during pair remove: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
            setState(AccessoryServerState.PAIR_UNVERIFIED);
            throw new HomekitException("Failed to remove pairing", e);
        }
    }

    protected byte[] doPairSetupStage0() throws IOException, HomekitException {
        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.METHOD, Method.PAIR_SETUP_WITH_AUTH.getKey());

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 1 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        BigInteger publicKey = stageResult.decodeResult.getBigInt(Message.PUBLIC_KEY);
        logger.info("'{}' : Setup Stage 1 : Public Key is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(publicKey)));

        BigInteger salt = stageResult.decodeResult.getBigInt(Message.SALT);
        logger.info("'{}' : Setup Stage 1 : Salt is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(salt)));

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BigInteger clientPublicKey = clientCredentials.A;
        logger.info("'{}' : Setup Stage 1 : Client Public Key is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(clientPublicKey)));

        BigInteger clientProof = clientCredentials.M1;
        logger.info("'{}' : Setup Stage 1 : Client Proof is {}", new String(getPairingId()),
                Byte.toHexString(Byte.toByteArray(clientProof)));

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);
        encoder.add(Message.PROOF, clientProof);
        logger.info("'{}' : Setup Stage 1 : End");

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 2 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitException("Wrong STATE");
        }

        BigInteger proof = stageResult.decodeResult.getBigInt(Message.PROOF);

        try {
            SRP6Session.step3(proof);
        } catch (SRP6Exception e) {
            // Verification failed
        }

        MessageDigest digest = SRP6Session.getCryptoParams().getMessageDigestInstance();
        BigInteger S = SRP6Session.getSessionKey(false);
        byte[] sBytes = Byte.toByteArray(S);
        logger.info("'{}' : Setup Stage 2 : SRP Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sBytes));
        sharedSecret = digest.digest(sBytes);
        logger.info("'{}' : Setup Stage 2 : Shared Secret is {}", new String(getPairingId()),
                Byte.toHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("'{}' : Setup Stage 2 : Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sessionKey));

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] clientDeviceX = new byte[32];
        hkdf.generateBytes(clientDeviceX, 0, 32);
        logger.info("'{}' : Setup Stage 2 : Client Device X is {}", new String(getPairingId()),
                Byte.toHexString(clientDeviceX));

        EdsaSigner signer = new EdsaSigner(secretKey);
        byte[] clientLongtermPublicKey = signer.getPublicKey();
        byte[] clientDeviceInfo = Byte.joinBytes(clientDeviceX, getPairingId(), clientLongtermPublicKey);
        byte[] clientSignature = null;
        try {
            clientSignature = signer.sign(clientDeviceInfo);
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.info("'{}' : Setup Stage 2 : Client Long Term Pubic Key is {}", new String(getPairingId()),
                Byte.toHexString(clientLongtermPublicKey));
        logger.info("'{}' : Setup Stage 2 : Client Signature X is {}", new String(getPairingId()),
                Byte.toHexString(clientSignature));

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.IDENTIFIER, getPairingId());
        encoder.add(Message.PUBLIC_KEY, clientLongtermPublicKey);
        encoder.add(Message.SIGNATURE, clientSignature);

        ChachaEncoder chachaEncoder = new ChachaEncoder(sessionKey, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEncoder.encodeCiphertext(encoder.toByteArray());

        encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x05);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);
        logger.info("'{}' : Setup Stage 2 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairSetupStage3(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Setup Stage 3 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 6) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
        logger.info("'{}' : Setup Stage 3 : Plaintext is {}", new String(getPairingId()), Byte.toHexString(plaintext));

        DecodeResult d = TypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        logger.info("'{}' : Setup Stage 3 : Accessory Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(destinationPairingIdentifier));
        byte[] destinationPublicKey = d.getBytes(Message.PUBLIC_KEY);
        logger.info("'{}' : Setup Stage 3 : Accessory Long Term Public Key is {}", new String(getPairingId()),
                Byte.toHexString(destinationPublicKey));
        byte[] accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("'{}' : Setup Stage 3 : Accessory Signature is {}", new String(getPairingId()),
                Byte.toHexString(accessorySignature));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] accessoryDeviceX = new byte[32];
        hkdf.generateBytes(accessoryDeviceX, 0, 32);

        byte[] accessoryDeviceInfo = Byte.joinBytes(accessoryDeviceX, destinationPairingIdentifier,
                destinationPublicKey);
        logger.info("'{}' : Setup Stage 3 : Accessory Device Info is {}", new String(getPairingId()),
                Byte.toHexString(accessoryDeviceInfo));

        try {
            if (!new EdsaVerifier(destinationPublicKey).verify(accessoryDeviceInfo, accessorySignature)) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e1) {
            throw new HomekitException("Signature verification failed");
        }

        addPairing(destinationPairingIdentifier, destinationPublicKey);
        SRP6Session = null;

        logger.info("'{}' : Setup Stage 3 : End", new String(getPairingId()));

        return null;
    }

    protected byte[] doPairVerifyStage0() throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 0 : Start", new String(getPairingId()));

        clientPublicKey = new byte[32];
        clientPrivateKey = new byte[32];
        HomekitEncryptionEngine.getSecureRandom().nextBytes(clientPrivateKey);
        Curve25519.keygen(clientPublicKey, null, clientPrivateKey);

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x01);
        encoder.add(Message.PUBLIC_KEY, clientPublicKey);

        logger.info("'{}' : Verify Stage 0 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage1(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 1 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 2) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] destinationPublicKey = stageResult.decodeResult.getBytes(Message.PUBLIC_KEY);
        logger.info("'{}' : Verify Stage 1 : Accessory Public Key is {}", new String(getPairingId()),
                Byte.toHexString(destinationPublicKey));

        byte[] messageData = new byte[stageResult.decodeResult.getLength(Message.ENCRYPTED_DATA) - 16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        byte[] authTagData = new byte[16];
        stageResult.decodeResult.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, clientPrivateKey, destinationPublicKey);
        logger.info("'{}' : Verify Stage 1 : Shared Secret is {}", new String(getPairingId()),
                Byte.toHexString(sharedSecret));

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32);
        logger.info("'{}' : Verify Stage 1 : Session Key is {}", new String(getPairingId()),
                Byte.toHexString(sessionKey));

        byte[] plaintext = null;
        ChachaDecoder chachaDecoder = new ChachaDecoder(sessionKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        try {
            plaintext = chachaDecoder.decodeCiphertext(authTagData, messageData);
            logger.info("'{}' : Verify Stage 1 : Plaintext is {}", new String(getPairingId()),
                    Byte.toHexString(plaintext));
        } catch (Exception e) {
            logger.warn("'{}' : Verify Stage 1 : Unable to decode the ciphertext");
            e.printStackTrace();
            throw new HomekitException("Ciphertext decoding failed");
        }

        Pairing accessoryPairing = null;
        byte[] accessorySignature = null;

        DecodeResult d = TypeLengthValueEncoderDecoder.decode(plaintext);
        byte[] destinationPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        // byte[] remoteAccessoryPairingIdentifier = d.getBytes(Message.IDENTIFIER);
        // if (isPaired()) {
        // if (!Arrays.equals(remoteAccessoryPairingIdentifier, destinationPairingIdentifier)) {
        // logger.warn(
        // "'{}' : Verify Stage 1 : The Accessory reports an pairing identifier that differs from what we have",
        // new String(getPairingId()));
        // throw new HomekitException("Invalid pairing identifier");
        // }
        // } else {
        // destinationPairingIdentifier = remoteAccessoryPairingIdentifier;
        // }

        logger.info("'{}' : Verify Stage 1 : Accessory Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(destinationPairingIdentifier));
        accessorySignature = d.getBytes(Message.SIGNATURE);
        logger.info("'{}' : Verify Stage 1 : Accessory Signature is {}", new String(getPairingId()),
                Byte.toHexString(accessorySignature));

        // accessoryPairing = pairingRegistry.get(new PairingUID(getPairingId(), destinationPairingIdentifier));
        accessoryPairing = getPairing(destinationPairingIdentifier);

        if (accessoryPairing == null) {
            throw new HomekitException("Accessory is not paired");
        } else {
            logger.info("'{}' : Fetched the Pairing {} : {}", new String(getPairingId()), accessoryPairing.getUID(),
                    Byte.toHexString(accessoryPairing.getPublicKey()));
        }

        byte[] accessoryDeviceInfo = Byte.joinBytes(destinationPublicKey, destinationPairingIdentifier,
                clientPublicKey);

        try {
            boolean signatureVerification = new EdsaVerifier(accessoryPairing.getPublicKey())
                    .verify(accessoryDeviceInfo, accessorySignature);
            if (!signatureVerification) {
                throw new HomekitException("Signature verification failed");
            }
        } catch (Exception e) {
            throw new HomekitException("Signature verification failed");
        }

        byte[] clientDeviceInfo = Byte.joinBytes(clientPublicKey, getPairingId(), destinationPublicKey);

        byte[] clientSignature = null;
        try {
            logger.info("'{}' : Verify Stage 1 : Client Long Term Secret Key is {}", new String(getPairingId()),
                    Byte.toHexString(secretKey));
            clientSignature = new EdsaSigner(secretKey).sign(clientDeviceInfo);
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Encoder encoder = TypeLengthValueEncoderDecoder.getEncoder();
        logger.info("'{}' : Verify Stage 1 : Client Pairing Id is {}", new String(getPairingId()),
                Byte.toHexString(getPairingId()));
        encoder.add(Message.IDENTIFIER, getPairingId());
        logger.info("'{}' : Verify Stage 1 : Client Signature is {}", new String(getPairingId()),
                Byte.toHexString(clientSignature));
        encoder.add(Message.SIGNATURE, clientSignature);
        plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = TypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(Message.STATE, (short) 0x03);
        encoder.add(Message.ENCRYPTED_DATA, ciphertext);

        logger.info("'{}' : Verify Stage 1 : End", new String(getPairingId()));

        return encoder.toByteArray();
    }

    protected byte[] doPairVerifyStage2(StageResult stageResult) throws IOException, HomekitException {
        logger.info("'{}' : Verify Stage 2 : Start", new String(getPairingId()));

        short state = stageResult.decodeResult.getByte(Message.STATE);
        if (state != 4) {
            throw new HomekitException("Wrong STATE");
        }

        byte[] writeKey = HomekitEncryptionEngine.createKey("Control-Write-Encryption-Key", sharedSecret);
        logger.info("'{}' : Verify Stage 2 : Write Key is {}", new String(getPairingId()), Byte.toHexString(writeKey));

        byte[] readKey = HomekitEncryptionEngine.createKey("Control-Read-Encryption-Key", sharedSecret);
        logger.info("'{}' : Verify Stage 2 : Read Key is {}", new String(getPairingId()), Byte.toHexString(readKey));

        HomekitHttpDestinationOverHTTP destination = (HomekitHttpDestinationOverHTTP) httpClient.getDestination(
                stageResult.result.getRequest().getScheme(), stageResult.result.getRequest().getHost(),
                stageResult.result.getRequest().getPort());
        logger.info("'{}' : Verify Stage 2 : Setting the keys on destination ", new String(getPairingId()),
                destination.toString());
        destination.setEncryptionKeys(readKey, writeKey);
        // destination.secure();

        logger.info("'{}' : Verify Stage 2 : End", new String(getPairingId()));

        return null;
    }

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
            e1.printStackTrace();
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
                                logger.info("'{}' : Received State {}", new String(getPairingId()), state);

                                StageResult stageResult = new StageResult(d, result);
                                completableFuture.complete(stageResult);
                            } catch (IOException e) {
                                SRP6Session = null;
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
            e1.printStackTrace();
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

    public void handleEvent(byte[] body) {

        try {
            Byte.logBuffer(logger, "handleEvent", Byte.toHexString(getPairingId()), ByteBuffer.wrap(body));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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

    public Collection<Accessory> getRemoteAccessories() {
        Collection<Accessory> result = new HashSet<Accessory>();

        if (isPaired() && isPairVerified() && isSecure()) {

            Future<ContentResult> contentFuture;
            ContentResult contentResult = null;
            try {
                contentFuture = getContent("/accessories");
                contentResult = contentFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logger.info("'{}' : Received {}", new String(getPairingId()), new String(contentResult.body));

            if (contentResult.result.getResponse().getStatus() == 200) {
                JsonArray accessories = Json.createReader(new ByteArrayInputStream(contentResult.body)).readObject()
                        .getJsonArray("accessories");
                for (JsonValue value : accessories) {
                    result.add(new GenericAccessory(value));
                }
            }
        }

        return result;
    }

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

    @Override
    public long getNextAvailableAccessoryId() {
        return 0;
    }

    @Override
    public void updateAccessories() throws IOException {
        if (!isPairVerified()) {
            logger.debug("'{}' : Cannot update accessories - not paired", new String(getPairingId()));
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
                    logger.info("'{}' : Adding new accessory {}", new String(getPairingId()), remoteAccessory);
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
                    logger.info("'{}' : Removing accessory {}", new String(getPairingId()), currentAccessory);
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
                                logger.info("'{}' : Adding new service {} to accessory {}", new String(getPairingId()), remoteService, currentAccessory);
                                //add the service to the accessory
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
                                logger.info("'{}' : Removing service {} from accessory {}", new String(getPairingId()), currentService, currentAccessory);
                                //remove the service from the accessory
                                currentAccessory.removeService(currentService);
                                notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.SERVICE_REMOVED);
                            }
                        }

                        // Compare characteristics for each service
                        for (Service currentService : currentAccessory.getServices()) {
                            for (Service remoteService : remoteServices) {
                                if (currentService.getInstanceId() == remoteService.getInstanceId()) {
                                    List<Characteristic<?>> currentCharacteristics = currentService.getCharacteristics();
                                    List<Characteristic<?>> remoteCharacteristics = remoteService.getCharacteristics();
                                    
                                    // Find new characteristics to add
                                    for (Characteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                        boolean found = false;
                                        for (Characteristic<?> currentCharacteristic : currentCharacteristics) {
                                            if (currentCharacteristic.getInstanceId() == remoteCharacteristic.getInstanceId()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            logger.info("'{}' : Adding new characteristic {} to service {} of accessory {}", 
                                                new String(getPairingId()), remoteCharacteristic, currentService, currentAccessory);
                                            //add the characteristic to the service
                                            currentService.addCharacteristic(remoteCharacteristic);
                                            notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_ADDED);
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
                                                    "'{}' : Removing characteristic {} from service {} of accessory {}",
                                                    new String(getPairingId()), currentCharacteristic, currentService,
                                                    currentAccessory);

                                                    //remove the characteristic from the service
                                            currentService.removeCharacteristic(currentCharacteristic);
                                            notifyChangeListeners(
                                                    AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_REMOVED);
                                        }
                                    }
                                    
                                    // Find characteristics that are the same, and check if they are equal()    
                                    for (Characteristic<?> currentCharacteristic : currentCharacteristics) {    
                                        for (Characteristic<?> remoteCharacteristic : remoteCharacteristics) {
                                            if (currentCharacteristic.getInstanceId() == remoteCharacteristic.getInstanceId()) {
                                                if (!currentCharacteristic.equals(remoteCharacteristic)) {
                                                    logger.info("'{}' : Characteristic {} is different from {}", 
                                                            new String(getPairingId()), currentCharacteristic, remoteCharacteristic);

                                                            //update the charactistic with the other one
                                                            currentCharacteristic.updateWith(remoteCharacteristic);

                                                              //notify the changelisteners that a characteristic had changed  
                                                            notifyChangeListeners(AccessoryServerEvent.AccessoryServerEventType.CHARACTERISTIC_UPDATED);
                                                }
                                            }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("'{}' : Error updating accessories: {}", new String(getPairingId()), e.getMessage());
            logger.debug("'{}' : Exception details", new String(getPairingId()), e);
            throw new IOException("Failed to update accessories", e);
        }
    }
}
