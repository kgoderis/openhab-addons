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
package org.openhab.io.homekit.test.demo;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.test.helper.HomekitCryptoVerificationHelper;
import org.openhab.io.homekit.test.helper.HomekitCryptoVerificationHelper.ComparisonResult;
import org.openhab.io.homekit.test.helper.HomekitCryptoVerificationHelper.VerificationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple demo class showing HomekitCryptoVerificationHelper working with captured logs.
 * This demonstrates the basic functionality without JUnit dependencies.
 * 
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitVerificationDemo {

    private static final Logger logger = LoggerFactory.getLogger(HomekitVerificationDemo.class);

    public static void main(String[] args) {
        logger.info("=== HomeKit Crypto Verification Helper Demo ===");

        // Simulate captured logs from our pairing process
        String clientLogs = """
                2025-01-15 10:30:15.123 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 1 : Salt = BEB25379D1A8581EB5A727673A2441EE
                2025-01-15 10:30:15.124 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 1 : Server public key = 9CAB7C8C9A2F49D2A0E6C1B8F3E4D5C6A7B8C9D0E1F2
                2025-01-15 10:30:15.125 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 1 : Client public key = 1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D7E8F9A0B1C2D
                2025-01-15 10:30:15.126 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 2 : Client proof = ABCDEF1234567890FEDCBA0987654321ABCDEF1234567890
                2025-01-15 10:30:15.127 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 2 : Server proof = 1234567890ABCDEF9876543210FEDCBA0123456789ABCDEF
                2025-01-15 10:30:15.128 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 2 : SRP session key = SHAREDSESSIONKEY123456789ABCDEF0123456789ABCDEF
                2025-01-15 10:30:15.129 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 2 : Shared secret = SHAREDSECRET123456789ABCDEF0123456789ABCDEF0123456789
                2025-01-15 10:30:15.130 [DEBUG] HomeKit [TEST_CLIENT_UID] : Verify : Stage 2 : Session key = SESSIONKEY123456789ABCDEF0123456789ABCDEF0123456789ABCDEF
                """;

        String serverLogs = """
                2025-01-15 10:30:15.120 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 1 : Salt = BEB25379D1A8581EB5A727673A2441EE
                2025-01-15 10:30:15.121 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 1 : Verifier = 7E273DE8696FFC4F4E337D05B4B375BEB0DDE1569E8FA00A9886D8129BADA1F1
                2025-01-15 10:30:15.122 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 1 : Server public key = 9CAB7C8C9A2F49D2A0E6C1B8F3E4D5C6A7B8C9D0E1F2
                2025-01-15 10:30:15.125 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 2 : Server proof = 1234567890ABCDEF9876543210FEDCBA0123456789ABCDEF
                2025-01-15 10:30:15.126 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 2 : SRP session key = SHAREDSESSIONKEY123456789ABCDEF0123456789ABCDEF
                2025-01-15 10:30:15.127 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 2 : Shared secret = SHAREDSECRET123456789ABCDEF0123456789ABCDEF0123456789
                2025-01-15 10:30:15.128 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 2 : Session key = SESSIONKEY123456789ABCDEF0123456789ABCDEF0123456789ABCDEF
                2025-01-15 10:30:15.129 [DEBUG] HomeKit [TEST_SERVER_UID] : Setup : Stage 3 : Server pairing identifier = SERVER_PAIRING_ID_123
                """;

        // Demonstrate value extraction
        logger.info("=== EXTRACTING VALUES FROM CAPTURED LOGS ===");
        Map<String, String> clientValues = HomekitCryptoVerificationHelper.extractAllValues(clientLogs,
                "TEST_CLIENT_UID");
        Map<String, String> serverValues = HomekitCryptoVerificationHelper.extractAllValues(serverLogs,
                "TEST_SERVER_UID");

        logger.info("Client extracted {} values:", clientValues.size());
        clientValues.forEach((key, value) -> logger.info("  {} = {}", key, value));

        logger.info("Server extracted {} values:", serverValues.size());
        serverValues.forEach((key, value) -> logger.info("  {} = {}", key, value));

        // Demonstrate comparison
        logger.info("=== COMPARING CLIENT AND SERVER VALUES ===");
        ComparisonResult comparison = HomekitCryptoVerificationHelper.compareValueMaps(clientValues, serverValues);
        comparison.printResults();

        // Demonstrate VerificationData extraction
        logger.info("=== EXTRACTING VERIFICATION DATA STRUCTURES ===");
        List<String> clientLogLines = List.of(clientLogs.split("\\n"));
        List<String> serverLogLines = List.of(serverLogs.split("\\n"));

        VerificationData clientData = HomekitCryptoVerificationHelper.extractFromLogs(clientLogLines,
                "TEST_CLIENT_UID");
        VerificationData serverData = HomekitCryptoVerificationHelper.extractFromLogs(serverLogLines,
                "TEST_SERVER_UID");

        logger.info("Client VerificationData:");
        logger.info("  Salt: {}", clientData.salt);
        logger.info("  Session Key: {}", clientData.sessionKey);
        logger.info("  Client Public Key: {}", clientData.clientPublicKey);

        logger.info("Server VerificationData:");
        logger.info("  Salt: {}", serverData.salt);
        logger.info("  Session Key: {}", serverData.sessionKey);
        logger.info("  Verifier: {}", serverData.verifier);

        // Demonstrate stage-specific extraction
        logger.info("=== STAGE-SPECIFIC EXTRACTION ===");
        Map<String, String> stage1 = HomekitCryptoVerificationHelper.extractStage1Values(serverLogs, "TEST_SERVER_UID");
        Map<String, String> stage2 = HomekitCryptoVerificationHelper.extractStage2Values(clientLogs, "TEST_CLIENT_UID");

        logger.info("Stage 1 values ({}): {}", stage1.size(), stage1.keySet());
        logger.info("Stage 2 values ({}): {}", stage2.size(), stage2.keySet());

        logger.info("=== DEMO COMPLETED SUCCESSFULLY ===");
    }
}
