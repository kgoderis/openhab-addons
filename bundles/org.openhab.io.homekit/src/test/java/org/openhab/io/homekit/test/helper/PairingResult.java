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
package org.openhab.io.homekit.test.helper;

import java.math.BigInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Result class for HomeKit pairing operations.
 * Combines functionality for both simple success/failure results and detailed stage-by-stage results.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class PairingResult {

    private final boolean successful;
    private final @Nullable String errorMessage;

    // Optional detailed stage results
    private final @Nullable Stage1Result stage1Result;
    private final @Nullable Stage2Result stage2Result;
    private final @Nullable Stage3Result stage3Result;

    /**
     * Constructor for simple success/failure result.
     * 
     * @param successful whether the pairing was successful
     * @param errorMessage error message if unsuccessful, null if successful
     */
    public PairingResult(boolean successful, @Nullable String errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.stage1Result = null;
        this.stage2Result = null;
        this.stage3Result = null;
    }

    /**
     * Constructor for detailed stage-by-stage result.
     * 
     * @param stage1Result result from stage 1
     * @param stage2Result result from stage 2
     * @param stage3Result result from stage 3
     */
    public PairingResult(Stage1Result stage1Result, Stage2Result stage2Result, Stage3Result stage3Result) {
        this.successful = stage1Result.isSuccessful() && stage2Result.isSuccessful() && stage3Result.isSuccessful();
        this.errorMessage = successful ? null : "One or more stages failed";
        this.stage1Result = stage1Result;
        this.stage2Result = stage2Result;
        this.stage3Result = stage3Result;
    }

    /**
     * Constructor for failure at a specific stage.
     * 
     * @param failedStage the stage number that failed (1, 2, or 3)
     * @param errorMessage the error message
     */
    public PairingResult(int failedStage, String errorMessage) {
        this.successful = false;
        this.errorMessage = "Stage " + failedStage + " failed: " + errorMessage;
        this.stage1Result = null;
        this.stage2Result = null;
        this.stage3Result = null;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public @Nullable Stage1Result getStage1Result() {
        return stage1Result;
    }

    public @Nullable Stage2Result getStage2Result() {
        return stage2Result;
    }

    public @Nullable Stage3Result getStage3Result() {
        return stage3Result;
    }

    /**
     * Gets the session key if available from the pairing stages.
     * 
     * @return session key from stage 2 or null if not available
     */
    public @Nullable String getSessionKey() {
        if (stage2Result != null && stage2Result.getSessionKey() != null) {
            return stage2Result.getSessionKey();
        }
        return null;
    }

    /**
     * Result from Stage 1 of HomeKit pairing.
     */
    public static class Stage1Result {
        private final boolean successful;
        private final @Nullable String errorMessage;
        private final @Nullable BigInteger salt;
        private final @Nullable BigInteger serverPublicKey;
        private final @Nullable BigInteger verifier;

        public Stage1Result(boolean successful, @Nullable String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.salt = null;
            this.serverPublicKey = null;
            this.verifier = null;
        }

        public Stage1Result(BigInteger salt, BigInteger serverPublicKey) {
            this.successful = true;
            this.errorMessage = null;
            this.salt = salt;
            this.serverPublicKey = serverPublicKey;
            this.verifier = null;
        }

        public Stage1Result(BigInteger salt, BigInteger serverPublicKey, BigInteger verifier) {
            this.successful = true;
            this.errorMessage = null;
            this.salt = salt;
            this.serverPublicKey = serverPublicKey;
            this.verifier = verifier;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public @Nullable String getErrorMessage() {
            return errorMessage;
        }

        public @Nullable BigInteger getSalt() {
            return salt;
        }

        public @Nullable BigInteger getServerPublicKey() {
            return serverPublicKey;
        }

        public @Nullable BigInteger getVerifier() {
            return verifier;
        }
    }

    /**
     * Result from Stage 2 of HomeKit pairing.
     */
    public static class Stage2Result {
        private final boolean successful;
        private final @Nullable String errorMessage;
        private final @Nullable BigInteger clientPublicKey;
        private final @Nullable String clientProof;
        private final @Nullable String serverProof;
        private final @Nullable String sessionKey;
        private final @Nullable String sharedSecret;

        public Stage2Result(boolean successful, @Nullable String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.clientPublicKey = null;
            this.clientProof = null;
            this.serverProof = null;
            this.sessionKey = null;
            this.sharedSecret = null;
        }

        public Stage2Result(BigInteger clientPublicKey, String clientProof, String serverProof, String sessionKey,
                String sharedSecret) {
            this.successful = true;
            this.errorMessage = null;
            this.clientPublicKey = clientPublicKey;
            this.clientProof = clientProof;
            this.serverProof = serverProof;
            this.sessionKey = sessionKey;
            this.sharedSecret = sharedSecret;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public @Nullable String getErrorMessage() {
            return errorMessage;
        }

        public @Nullable BigInteger getClientPublicKey() {
            return clientPublicKey;
        }

        public @Nullable String getClientProof() {
            return clientProof;
        }

        public @Nullable String getServerProof() {
            return serverProof;
        }

        public @Nullable String getSessionKey() {
            return sessionKey;
        }

        public @Nullable String getSharedSecret() {
            return sharedSecret;
        }
    }

    /**
     * Result from Stage 3 of HomeKit pairing.
     */
    public static class Stage3Result {
        private final boolean successful;
        private final @Nullable String errorMessage;
        private final @Nullable String clientDeviceX;
        private final @Nullable String serverDeviceX;
        private final @Nullable String clientPairingIdentifier;
        private final @Nullable String serverPairingIdentifier;
        private final @Nullable String clientLongtermPublicKey;
        private final @Nullable String serverLongtermPublicKey;
        private final @Nullable String clientSignature;
        private final @Nullable String serverSignature;
        private final @Nullable String clientDeviceInfo;
        private final @Nullable String serverDeviceInfo;

        public Stage3Result(boolean successful, @Nullable String errorMessage) {
            this.successful = successful;
            this.errorMessage = errorMessage;
            this.clientDeviceX = null;
            this.serverDeviceX = null;
            this.clientPairingIdentifier = null;
            this.serverPairingIdentifier = null;
            this.clientLongtermPublicKey = null;
            this.serverLongtermPublicKey = null;
            this.clientSignature = null;
            this.serverSignature = null;
            this.clientDeviceInfo = null;
            this.serverDeviceInfo = null;
        }

        public Stage3Result(String clientDeviceX, String serverDeviceX, String clientPairingIdentifier,
                String serverPairingIdentifier, String clientLongtermPublicKey, String serverLongtermPublicKey,
                String clientSignature, String serverSignature, String clientDeviceInfo, String serverDeviceInfo) {
            this.successful = true;
            this.errorMessage = null;
            this.clientDeviceX = clientDeviceX;
            this.serverDeviceX = serverDeviceX;
            this.clientPairingIdentifier = clientPairingIdentifier;
            this.serverPairingIdentifier = serverPairingIdentifier;
            this.clientLongtermPublicKey = clientLongtermPublicKey;
            this.serverLongtermPublicKey = serverLongtermPublicKey;
            this.clientSignature = clientSignature;
            this.serverSignature = serverSignature;
            this.clientDeviceInfo = clientDeviceInfo;
            this.serverDeviceInfo = serverDeviceInfo;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public @Nullable String getErrorMessage() {
            return errorMessage;
        }

        public @Nullable String getClientDeviceX() {
            return clientDeviceX;
        }

        public @Nullable String getServerDeviceX() {
            return serverDeviceX;
        }

        public @Nullable String getClientPairingIdentifier() {
            return clientPairingIdentifier;
        }

        public @Nullable String getServerPairingIdentifier() {
            return serverPairingIdentifier;
        }

        public @Nullable String getClientLongtermPublicKey() {
            return clientLongtermPublicKey;
        }

        public @Nullable String getServerLongtermPublicKey() {
            return serverLongtermPublicKey;
        }

        public @Nullable String getClientSignature() {
            return clientSignature;
        }

        public @Nullable String getServerSignature() {
            return serverSignature;
        }

        public @Nullable String getClientDeviceInfo() {
            return clientDeviceInfo;
        }

        public @Nullable String getServerDeviceInfo() {
            return serverDeviceInfo;
        }
    }
}
