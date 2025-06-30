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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Helper class for extracting and comparing crypto verification values from
 * HomeKit pairing setup logs.
 * 
 * This class provides utilities to:
 * - Parse standardized verification logs from both client and server
 * - Extract crypto values (keys, proofs, signatures, etc.)
 * - Compare values between client and server implementations
 * - Validate against known test vectors
 * 
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitCryptoVerificationHelper {

    // ========== Log Pattern Constants ==========

    // Base pattern for extracting log entries with stage information
    private static final String LOG_PATTERN_BASE = ".*\\[(.+?)\\] : Verify : Stage (\\d+) : (.+?) = ([a-fA-F0-9]+).*";

    // Specific crypto value patterns
    private static final String SALT_PATTERN = "Salt = ([a-fA-F0-9]+)";
    private static final String SERVER_PUBLIC_KEY_PATTERN = "Server public key = ([a-fA-F0-9]+)";
    private static final String CLIENT_PUBLIC_KEY_PATTERN = "Client public key = ([a-fA-F0-9]+)";
    private static final String CLIENT_PROOF_PATTERN = "Client proof = ([a-fA-F0-9]+)";
    private static final String SERVER_PROOF_PATTERN = "Server proof = ([a-fA-F0-9]+)";
    private static final String VERIFIER_PATTERN = "Verifier = ([a-fA-F0-9]+)";
    private static final String SRP_SESSION_KEY_PATTERN = "SRP session key = ([a-fA-F0-9]+)";
    private static final String SHARED_SECRET_PATTERN = "Shared secret = ([a-fA-F0-9]+)";
    private static final String SESSION_KEY_PATTERN = "Session key = ([a-fA-F0-9]+)";
    private static final String CLIENT_DEVICE_X_PATTERN = "Client device X = ([a-fA-F0-9]+)";
    private static final String SERVER_DEVICE_X_PATTERN = "Server device X = ([a-fA-F0-9]+)";
    private static final String CLIENT_PAIRING_ID_PATTERN = "Client pairing identifier = ([a-fA-F0-9]+)";
    private static final String SERVER_PAIRING_ID_PATTERN = "Server pairing identifier = ([a-fA-F0-9]+)";
    private static final String CLIENT_LONGTERM_KEY_PATTERN = "Client longterm public key = ([a-fA-F0-9]+)";
    private static final String SERVER_LONGTERM_KEY_PATTERN = "Server longterm public key = ([a-fA-F0-9]+)";
    private static final String CLIENT_SIGNATURE_PATTERN = "Client signature = ([a-fA-F0-9]+)";
    private static final String SERVER_SIGNATURE_PATTERN = "Server signature = ([a-fA-F0-9]+)";
    private static final String CLIENT_DEVICE_INFO_PATTERN = "Client device info = ([a-fA-F0-9]+)";
    private static final String SERVER_DEVICE_INFO_PATTERN = "Server device info = ([a-fA-F0-9]+)";

    // ========== Crypto Value Extraction Methods ==========

    /**
     * Extracts all crypto verification values from a log content string.
     * 
     * @param logContent The log content to parse
     * @param serverId The server ID to filter logs for (optional)
     * @return Map of crypto value names to their hex values
     */
    public static Map<String, String> extractAllValues(String logContent, @Nullable String serverId) {
        Map<String, String> values = new HashMap<>();

        // Extract values by stage
        values.putAll(extractStage1Values(logContent, serverId));
        values.putAll(extractStage2Values(logContent, serverId));
        values.putAll(extractStage3Values(logContent, serverId));

        return values;
    }

    /**
     * Extracts Stage 1 crypto values (Salt, Server Public Key, Verifier).
     * 
     * @param logContent The log content to parse
     * @param serverId The server ID to filter logs for (optional)
     * @return Map of Stage 1 crypto values
     */
    public static Map<String, String> extractStage1Values(String logContent, @Nullable String serverId) {
        Map<String, String> values = new HashMap<>();

        String content = filterByServerId(logContent, serverId);

        values.put("salt", extractHexValue(SALT_PATTERN, content));
        values.put("serverPublicKey", extractHexValue(SERVER_PUBLIC_KEY_PATTERN, content));
        values.put("verifier", extractHexValue(VERIFIER_PATTERN, content));

        return values;
    }

    /**
     * Extracts Stage 2 crypto values (Client keys, proofs, session data).
     * 
     * @param logContent The log content to parse
     * @param serverId The server ID to filter logs for (optional)
     * @return Map of Stage 2 crypto values
     */
    public static Map<String, String> extractStage2Values(String logContent, @Nullable String serverId) {
        Map<String, String> values = new HashMap<>();

        String content = filterByServerId(logContent, serverId);

        values.put("clientPublicKey", extractHexValue(CLIENT_PUBLIC_KEY_PATTERN, content));
        values.put("clientProof", extractHexValue(CLIENT_PROOF_PATTERN, content));
        values.put("serverProof", extractHexValue(SERVER_PROOF_PATTERN, content));
        values.put("srpSessionKey", extractHexValue(SRP_SESSION_KEY_PATTERN, content));
        values.put("sharedSecret", extractHexValue(SHARED_SECRET_PATTERN, content));
        values.put("sessionKey", extractHexValue(SESSION_KEY_PATTERN, content));
        values.put("clientDeviceX", extractHexValue(CLIENT_DEVICE_X_PATTERN, content));
        values.put("clientPairingId", extractHexValue(CLIENT_PAIRING_ID_PATTERN, content));
        values.put("clientLongtermKey", extractHexValue(CLIENT_LONGTERM_KEY_PATTERN, content));
        values.put("clientSignature", extractHexValue(CLIENT_SIGNATURE_PATTERN, content));
        values.put("clientDeviceInfo", extractHexValue(CLIENT_DEVICE_INFO_PATTERN, content));

        return values;
    }

    /**
     * Extracts Stage 3 crypto values (Server signatures and device info).
     * 
     * @param logContent The log content to parse
     * @param serverId The server ID to filter logs for (optional)
     * @return Map of Stage 3 crypto values
     */
    public static Map<String, String> extractStage3Values(String logContent, @Nullable String serverId) {
        Map<String, String> values = new HashMap<>();

        String content = filterByServerId(logContent, serverId);

        values.put("serverDeviceX", extractHexValue(SERVER_DEVICE_X_PATTERN, content));
        values.put("serverPairingId", extractHexValue(SERVER_PAIRING_ID_PATTERN, content));
        values.put("serverLongtermKey", extractHexValue(SERVER_LONGTERM_KEY_PATTERN, content));
        values.put("serverSignature", extractHexValue(SERVER_SIGNATURE_PATTERN, content));
        values.put("serverDeviceInfo", extractHexValue(SERVER_DEVICE_INFO_PATTERN, content));

        return values;
    }

    // ========== Comparison Methods ==========

    /**
     * Compares crypto values between client and server logs.
     * 
     * @param clientLogContent The client log content
     * @param serverLogContent The server log content
     * @param serverId The server ID to filter for (optional)
     * @return ComparisonResult with matched and mismatched values
     */
    public static ComparisonResult compareClientServer(String clientLogContent, String serverLogContent,
            @Nullable String serverId) {
        Map<String, String> clientValues = extractAllValues(clientLogContent, serverId);
        Map<String, String> serverValues = extractAllValues(serverLogContent, serverId);

        return compareValueMaps(clientValues, serverValues);
    }

    /**
     * Compares two maps of crypto values and identifies matches/mismatches.
     * 
     * @param clientValues Client crypto values
     * @param serverValues Server crypto values
     * @return ComparisonResult with analysis
     */
    public static ComparisonResult compareValueMaps(Map<String, String> clientValues,
            Map<String, String> serverValues) {
        ComparisonResult result = new ComparisonResult();

        // Find common keys and compare values
        for (String key : clientValues.keySet()) {
            String clientValue = clientValues.get(key);
            String serverValue = serverValues.get(key);

            if (serverValue != null && clientValue != null) {
                if (clientValue.equals(serverValue)) {
                    result.addMatch(key, clientValue);
                } else {
                    result.addMismatch(key, clientValue, serverValue);
                }
            } else if (clientValue != null) {
                result.addClientOnly(key, clientValue);
            }
        }

        // Find server-only values
        for (String key : serverValues.keySet()) {
            if (!clientValues.containsKey(key)) {
                String serverValue = serverValues.get(key);
                if (serverValue != null) {
                    result.addServerOnly(key, serverValue);
                }
            }
        }

        return result;
    }

    // ========== Validation Methods ==========

    /**
     * Validates crypto values against known test vectors.
     * 
     * @param values The crypto values to validate
     * @param testVectors Known good test vectors
     * @return ValidationResult with pass/fail status
     */
    public static ValidationResult validateAgainstTestVectors(Map<String, String> values,
            Map<String, String> testVectors) {
        ValidationResult result = new ValidationResult();

        for (String key : testVectors.keySet()) {
            String expectedValue = testVectors.get(key);
            String actualValue = values.get(key);

            if (actualValue != null && expectedValue != null) {
                if (expectedValue.equals(actualValue)) {
                    result.addPass(key, actualValue);
                } else {
                    result.addFail(key, expectedValue, actualValue);
                }
            } else if (expectedValue != null) {
                result.addMissing(key, expectedValue);
            }
        }

        return result;
    }

    // ========== Advanced Extraction Methods ==========

    /**
     * Extracts verification data from a list of log lines for a specific identifier.
     * 
     * @param logLines The list of log lines to parse
     * @param identifier The identifier to filter logs for
     * @return VerificationData containing extracted crypto values
     */
    public static VerificationData extractFromLogs(List<String> logLines, String identifier) {
        String logContent = String.join("\n", logLines);
        Map<String, String> values = extractAllValues(logContent, identifier);

        return new VerificationData(values.getOrDefault("salt", ""), values.getOrDefault("serverPublicKey", ""),
                values.getOrDefault("clientPublicKey", ""), values.getOrDefault("verifier", ""),
                values.getOrDefault("clientProof", ""), values.getOrDefault("serverProof", ""),
                values.getOrDefault("srpSessionKey", ""), values.getOrDefault("sharedSecret", ""),
                values.getOrDefault("sessionKey", ""), values.getOrDefault("clientDeviceX", ""),
                values.getOrDefault("serverDeviceX", ""), values.getOrDefault("clientPairingId", ""),
                values.getOrDefault("serverPairingId", ""), values.getOrDefault("clientLongtermKey", ""),
                values.getOrDefault("serverLongtermKey", ""), values.getOrDefault("clientSignature", ""),
                values.getOrDefault("serverSignature", ""), values.getOrDefault("clientDeviceInfo", ""),
                values.getOrDefault("serverDeviceInfo", ""));
    }

    /**
     * Verifies that matching crypto values between client and server are identical.
     * 
     * @param clientData Client verification data
     * @param serverData Server verification data
     * @throws AssertionError if matching values don't match
     */
    public static void verifyMatching(VerificationData clientData, VerificationData serverData) {
        // Values that should match between client and server
        verifyMatch("Salt", clientData.salt, serverData.salt);
        verifyMatch("SRP Session Key", clientData.srpSessionKey, serverData.srpSessionKey);
        verifyMatch("Shared Secret", clientData.sharedSecret, serverData.sharedSecret);
        verifyMatch("Session Key", clientData.sessionKey, serverData.sessionKey);
        verifyMatch("Server Public Key", clientData.serverPublicKey, serverData.serverPublicKey);
        verifyMatch("Client Public Key", clientData.clientPublicKey, serverData.clientPublicKey);
        verifyMatch("Client Proof", clientData.clientProof, serverData.clientProof);
        verifyMatch("Server Proof", clientData.serverProof, serverData.serverProof);
    }

    private static void verifyMatch(String valueName, String clientValue, String serverValue) {
        if (!clientValue.isEmpty() && !serverValue.isEmpty() && !clientValue.equals(serverValue)) {
            throw new AssertionError(
                    String.format("%s mismatch: client=%s, server=%s", valueName, clientValue, serverValue));
        }
    }

    // ========== Helper Methods ==========

    /**
     * Extracts a hex value using the given pattern.
     * 
     * @param pattern The regex pattern to match
     * @param content The content to search
     * @return The extracted hex value or empty string if not found
     */
    private static String extractHexValue(String pattern, String content) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Filters log content by server ID if provided.
     * 
     * @param logContent The log content to filter
     * @param serverId The server ID to filter for (optional)
     * @return Filtered log content
     */
    private static String filterByServerId(String logContent, @Nullable String serverId) {
        if (serverId == null) {
            return logContent;
        }

        String[] lines = logContent.split("\n");
        StringBuilder filtered = new StringBuilder();

        for (String line : lines) {
            if (line.contains("[" + serverId + "]")) {
                filtered.append(line).append("\n");
            }
        }

        return filtered.toString();
    }

    // ========== Result Classes ==========

    /**
     * Result of comparing client and server crypto values.
     */
    public static class ComparisonResult {
        private final Map<String, String> matches = new HashMap<>();
        private final Map<String, ValuePair> mismatches = new HashMap<>();
        private final Map<String, String> clientOnly = new HashMap<>();
        private final Map<String, String> serverOnly = new HashMap<>();

        public void addMatch(String key, String value) {
            matches.put(key, value);
        }

        public void addMismatch(String key, String clientValue, String serverValue) {
            mismatches.put(key, new ValuePair(clientValue, serverValue));
        }

        public void addClientOnly(String key, String value) {
            clientOnly.put(key, value);
        }

        public void addServerOnly(String key, String value) {
            serverOnly.put(key, value);
        }

        public Map<String, String> getMatches() {
            return matches;
        }

        public Map<String, ValuePair> getMismatches() {
            return mismatches;
        }

        public Map<String, String> getClientOnly() {
            return clientOnly;
        }

        public Map<String, String> getServerOnly() {
            return serverOnly;
        }

        public boolean hasMatches() {
            return !matches.isEmpty();
        }

        public boolean hasMismatches() {
            return !mismatches.isEmpty();
        }

        public int getMatchCount() {
            return matches.size();
        }

        public int getMismatchCount() {
            return mismatches.size();
        }

        /**
         * Prints the comparison results for manual verification.
         */
        public void printResults() {
            System.out.println("=== COMPARISON RESULTS ===");
            System.out.println("Matches: " + matches.size());
            for (Map.Entry<String, String> entry : matches.entrySet()) {
                System.out.println("  ✓ " + entry.getKey() + " = " + entry.getValue());
            }

            System.out.println("Mismatches: " + mismatches.size());
            for (Map.Entry<String, ValuePair> entry : mismatches.entrySet()) {
                ValuePair pair = entry.getValue();
                System.out.println(
                        "  ✗ " + entry.getKey() + " - Client: " + pair.getFirst() + " | Server: " + pair.getSecond());
            }

            System.out.println("Client Only: " + clientOnly.size());
            for (Map.Entry<String, String> entry : clientOnly.entrySet()) {
                System.out.println("  → " + entry.getKey() + " = " + entry.getValue());
            }

            System.out.println("Server Only: " + serverOnly.size());
            for (Map.Entry<String, String> entry : serverOnly.entrySet()) {
                System.out.println("  ← " + entry.getKey() + " = " + entry.getValue());
            }
            System.out.println("========================");
        }
    }

    /**
     * Result of validating crypto values against test vectors.
     */
    public static class ValidationResult {
        private final Map<String, String> passed = new HashMap<>();
        private final Map<String, ValuePair> failed = new HashMap<>();
        private final Map<String, String> missing = new HashMap<>();

        public void addPass(String key, String value) {
            passed.put(key, value);
        }

        public void addFail(String key, String expected, String actual) {
            failed.put(key, new ValuePair(expected, actual));
        }

        public void addMissing(String key, String expected) {
            missing.put(key, expected);
        }

        public Map<String, String> getPassed() {
            return passed;
        }

        public Map<String, ValuePair> getFailed() {
            return failed;
        }

        public Map<String, String> getMissing() {
            return missing;
        }

        public boolean allPassed() {
            return failed.isEmpty() && missing.isEmpty();
        }

        public int getPassCount() {
            return passed.size();
        }

        public int getFailCount() {
            return failed.size();
        }

        public int getMissingCount() {
            return missing.size();
        }

        /**
         * Prints the validation results for manual verification.
         */
        public void printResults() {
            System.out.println("=== VALIDATION RESULTS ===");
            System.out.println("Passed: " + passed.size());
            for (Map.Entry<String, String> entry : passed.entrySet()) {
                System.out.println("  ✓ " + entry.getKey() + " = " + entry.getValue());
            }

            System.out.println("Failed: " + failed.size());
            for (Map.Entry<String, ValuePair> entry : failed.entrySet()) {
                ValuePair pair = entry.getValue();
                System.out.println(
                        "  ✗ " + entry.getKey() + " - Expected: " + pair.getFirst() + " | Actual: " + pair.getSecond());
            }

            System.out.println("Missing: " + missing.size());
            for (Map.Entry<String, String> entry : missing.entrySet()) {
                System.out.println("  ? " + entry.getKey() + " = " + entry.getValue());
            }
            System.out.println("========================");
        }
    }

    /**
     * Represents a pair of values for comparison.
     */
    public static class ValuePair {
        private final String first;
        private final String second;

        public ValuePair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public String getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return String.format("(%s, %s)", first, second);
        }
    }

    // ========== Data Classes ==========

    /**
     * Container for extracted verification data from pairing logs.
     */
    public static class VerificationData {
        public final String salt;
        public final String serverPublicKey;
        public final String clientPublicKey;
        public final String verifier;
        public final String clientProof;
        public final String serverProof;
        public final String srpSessionKey;
        public final String sharedSecret;
        public final String sessionKey;
        public final String clientDeviceX;
        public final String serverDeviceX;
        public final String clientPairingId;
        public final String serverPairingId;
        public final String clientLongtermKey;
        public final String serverLongtermKey;
        public final String clientSignature;
        public final String serverSignature;
        public final String clientDeviceInfo;
        public final String serverDeviceInfo;

        // Add aliases for backward compatibility
        public final String deviceX;
        public final String signature;

        public VerificationData(String salt, String serverPublicKey, String clientPublicKey, String verifier,
                String clientProof, String serverProof, String srpSessionKey, String sharedSecret, String sessionKey,
                String clientDeviceX, String serverDeviceX, String clientPairingId, String serverPairingId,
                String clientLongtermKey, String serverLongtermKey, String clientSignature, String serverSignature,
                String clientDeviceInfo, String serverDeviceInfo) {
            this.salt = salt;
            this.serverPublicKey = serverPublicKey;
            this.clientPublicKey = clientPublicKey;
            this.verifier = verifier;
            this.clientProof = clientProof;
            this.serverProof = serverProof;
            this.srpSessionKey = srpSessionKey;
            this.sharedSecret = sharedSecret;
            this.sessionKey = sessionKey;
            this.clientDeviceX = clientDeviceX;
            this.serverDeviceX = serverDeviceX;
            this.clientPairingId = clientPairingId;
            this.serverPairingId = serverPairingId;
            this.clientLongtermKey = clientLongtermKey;
            this.serverLongtermKey = serverLongtermKey;
            this.clientSignature = clientSignature;
            this.serverSignature = serverSignature;
            this.clientDeviceInfo = clientDeviceInfo;
            this.serverDeviceInfo = serverDeviceInfo;

            // Set aliases - use client values if available, otherwise server values
            this.deviceX = !clientDeviceX.isEmpty() ? clientDeviceX : serverDeviceX;
            this.signature = !clientSignature.isEmpty() ? clientSignature : serverSignature;
        }
    }
}
