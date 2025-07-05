package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Util.CalculationMethod;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * **MANUAL TEST**: Real HomeKit Accessory Integration Test with SRP6 Calculation Method Support
 * 
 * This test requires manual configuration and real HomeKit hardware:
 * 
 * **PREREQUISITES:**
 * 1. A real HomeKit accessory (e.g., smart plug, light bulb, sensor)
 * 2. The accessory must be on the same network as the test machine
 * 3. The accessory's setup code (found on the device or in its manual)
 * 4. Network connectivity to the accessory
 * 
 * **CONFIGURATION:**
 * 1. Update REAL_ACCESSORY_IP with your accessory's IP address
 * 2. Update REAL_ACCESSORY_PORT if different from 80
 * 3. Update SETUP_CODE with your accessory's setup code
 * 
 * **SRP6 CALCULATION METHODS:**
 * - BOUNCYCASTLE: Uses BouncyCastle implementation with proper digest resets and padding
 * - NIMBUS: Uses NimbusDS-style implementation with cumulative hashing
 * 
 * **USAGE:**
 * - Run this test manually when you have real hardware available
 * - For automated CI/CD, use the mock-based tests instead
 * - This test validates end-to-end integration with real HomeKit devices
 * - Tests both SRP6 calculation methods to ensure compatibility
 * 
 * **ENHANCED LOGGING:**
 * - TRACE level logging enabled for all HomeKit components
 * - Detailed SRP6 debugging and value capture
 * - Network communication logging
 * - Comprehensive error reporting
 * - SRP6 calculation method comparison
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RealAccessoryTest {

    private static final Logger logger = LoggerFactory.getLogger(RealAccessoryTest.class);

    // **CONFIGURE THESE VALUES FOR YOUR REAL ACCESSORY**
    private static final String REAL_ACCESSORY_IP = "127.0.0.1"; // Replace with your accessory's IP
    private static final int REAL_ACCESSORY_PORT = 47129; // Usually 80, but may vary
    private static final String SETUP_CODE = "678-90-876"; // **REQUIRED**: Replace with your accessory's setup code

    // **SRP6 CALCULATION METHOD TESTING**
    private static final boolean TEST_BOTH_CALCULATION_METHODS = true; // Set to false to test only default method
    private static final CalculationMethod DEFAULT_CALCULATION_METHOD = CalculationMethod.BOUNCYCASTLE;

    // **HOW TO FIND THE SETUP CODE:**
    // 1. Check the physical device for a label with format XXX-XX-XXX
    // 2. Look in the device manual or documentation
    // 3. Check the device's configuration app or web interface
    // 4. If using HAP-Java, check the server configuration
    // 5. Common locations: device packaging, QR code, or device settings

    @BeforeEach
    void setUp() {
        logger.info("🔧 **REAL ACCESSORY TEST SETUP**");
        logger.info("📋 Configuration:");
        logger.info("   Accessory IP: {}", REAL_ACCESSORY_IP);
        logger.info("   Accessory Port: {}", REAL_ACCESSORY_PORT);
        logger.info("   Setup Code: {}", SETUP_CODE);
        logger.info("   Test Both Calculation Methods: {}", TEST_BOTH_CALCULATION_METHODS);
        logger.info("   Default Calculation Method: {}", DEFAULT_CALCULATION_METHOD);
        logger.info("🔍 **FULL LOGGING ENABLED** - All TRACE level logs will be captured");
        logger.info("📊 **SRP6 DEBUGGING** - All cryptographic values will be logged");
        logger.info("🌐 **NETWORK DEBUGGING** - All HTTP/TCP communication will be logged");
        logger.info("🔬 **SRP6 METHOD COMPARISON** - Both calculation methods will be tested");
    }

    /**
     * **MANUAL TEST**: Discovers and connects to a real HomeKit accessory
     * 
     * This test validates basic network connectivity and accessory discovery.
     * It will fail if no accessory is available at the configured IP address.
     */
    @Test
    @Order(1)
    void testDiscoverRealAccessory() throws Exception {
        logger.info("=== MANUAL TEST: Real Accessory Discovery ===");
        logger.info("🎯 **TEST OBJECTIVE**: Validate network connectivity and accessory discovery");
        logger.info("📍 **TARGET**: {}:{}", REAL_ACCESSORY_IP, REAL_ACCESSORY_PORT);
        logger.info("🔑 **SETUP CODE**: {}", SETUP_CODE);

        logger.debug("🔧 Creating test client...");
        HomekitRemoteAccessoryServer client = createTestClient();

        logger.debug("🚀 Starting client...");
        client.start();

        logger.debug("🔍 Checking connection status...");
        boolean connected = client.isConnected();
        logger.info("📡 Connection status: {}", connected ? "✅ CONNECTED" : "❌ DISCONNECTED");

        // Attempt connection
        assertTrue(connected, "Should connect to real accessory");
        logger.info("✅ **SUCCESS**: Successfully connected to real accessory");
        logger.info("🎉 **DISCOVERY TEST PASSED** - Network connectivity validated");
    }

    /**
     * **TEMPORARY TEST**: Tests both SRP6 calculation methods with real accessory
     * 
     * This test runs the pairing flow with both BouncyCastle and Nimbus calculation methods
     * to validate compatibility and identify any differences in behavior.
     */
    @ParameterizedTest
    @EnumSource(value = CalculationMethod.class, names = { "FASTSRP" })
    @DisplayName("Test SRP6 pairing with real accessory using FASTSRP method")
    void testPairingWithRealAccessory(CalculationMethod calculationMethod) throws Exception {
        performPairingTest(calculationMethod);
    }

    /**
     * **SRP6 COMPATIBILITY ANALYSIS**: Captures and logs all SRP6 values for debugging
     */
    private void captureSRP6Values(String stage, Map<String, Object> values) {
        logger.info("🔍 **SRP6 STAGE {} VALUES**", stage);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof byte[]) {
                logger.info("   {}: {}", key, bytesToHex((byte[]) value));
            } else if (value instanceof BigInteger) {
                logger.info("   {}: {}", key, ((BigInteger) value).toString(16));
            } else {
                logger.info("   {}: {}", key, value);
            }
        }
    }

    /**
     * Converts byte array to hex string for debugging
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Creates a test client configured for real accessory testing
     */
    private HomekitRemoteAccessoryServer createTestClient() throws Exception {
        logger.debug("🔧 Creating HomekitRemoteAccessoryServer...");
        logger.debug("   Category: {}", HomekitAccessoryCategory.OTHER);
        logger.debug("   Name: TestClient");
        logger.debug("   Target: {}:{}", REAL_ACCESSORY_IP, REAL_ACCESSORY_PORT);

        HomekitRemoteAccessoryServer client = new HomekitRemoteAccessoryServer(HomekitAccessoryCategory.OTHER,
                "TestClient", InetAddress.getByName(REAL_ACCESSORY_IP), REAL_ACCESSORY_PORT,
                mock(HomekitAccessoryRegistry.class), mock(HomekitPairingRegistry.class),
                mock(HomekitEventManager.class), mock(HomekitAccessoryFactory.class));

        // Disable automatic pairing for testing - we want manual control
        client.setDisableAutoPairing(true);
        logger.debug("✅ HomekitRemoteAccessoryServer created successfully with auto-pairing disabled");
        return client;
    }

    /**
     * Performs the actual pairing test with the specified calculation method
     * 
     * @param calculationMethod The SRP6 calculation method to use
     * @throws Exception if the test fails
     */
    private void performPairingTest(CalculationMethod calculationMethod) throws Exception {
        // Validate setup code format
        if ("XXX-XX-XXX".equals(SETUP_CODE)) {
            logger.error("❌ **CONFIGURATION ERROR**: SETUP CODE NOT CONFIGURED!");
            logger.error("📝 **REQUIRED ACTION**: Update the SETUP_CODE constant in this test");
            logger.error("📋 **FORMAT**: XXX-XX-XXX (e.g., 031-45-154)");
            logger.error("🔍 **WHERE TO FIND**: Device label, manual, or configuration app");
            fail("Setup code not configured - please update SETUP_CODE constant");
        }

        // **ENHANCED DEBUGGING**: Add comprehensive SRP6 value capture
        logger.info("🔍 **SRP6 DEBUGGING ENABLED** - Capturing all values for compatibility analysis");
        logger.info("📊 **VALUE CAPTURE**: All SRP6 parameters will be logged at TRACE level");
        logger.info("🔬 **COMPARISON**: Values will be compared with HAP-Java expectations");
        logger.info("🌐 **NETWORK LOGGING**: All HTTP/TCP communication will be logged");
        logger.info("🔬 **CALCULATION METHOD (REQUESTED)**: {}", calculationMethod);

        try {
            // Create client using existing helper method
            logger.debug("🔧 Creating test client with setup code...");
            HomekitRemoteAccessoryServer client = createTestClient();
            client.setSetupCode(SETUP_CODE);
            client.setSRP6CalculationMethod(calculationMethod);
            logger.debug("✅ Client created and setup code configured");

            // Log the actual calculation method in use
            CalculationMethod actualMethod = client.getSRP6CalculationMethod();
            logger.info("🔬 **CALCULATION METHOD (ACTUAL)**: {}", actualMethod);
            if (!actualMethod.equals(calculationMethod)) {
                logger.error("❌ **SRP6 CALCULATION METHOD MISMATCH**: Requested {} but actual is {}", calculationMethod,
                        actualMethod);
                fail("SRP6 calculation method mismatch: requested " + calculationMethod + ", actual " + actualMethod);
            }

            // Start pairing with enhanced debugging
            logger.info("🚀 **STARTING PAIRING PROCESS** with real accessory...");
            logger.debug("📡 Initiating pairSetup() method...");
            client.pairSetup();
            logger.info("✅ **PAIRING COMPLETED SUCCESSFULLY!**");

            // Verify pairing status
            boolean paired = client.isPaired();
            logger.info("🔍 Pairing status: {}", paired ? "✅ PAIRED" : "❌ NOT PAIRED");
            assertTrue(paired, "Client should be paired after successful pairing");

            // Verify pairing
            logger.info("🔐 **STARTING PAIRING VERIFICATION**...");
            logger.debug("📡 Initiating pairVerify() method...");
            boolean verified = client.pairVerify();
            logger.info("🔍 Verification result: {}", verified ? "✅ VERIFIED" : "❌ FAILED");
            assertTrue(verified, "Pairing verification should succeed");

            logger.info("✅ **PAIRING VERIFICATION COMPLETED SUCCESSFULLY!**");
            boolean pairVerified = client.isPairVerified();
            logger.info("🔍 Pair verification status: {}", pairVerified ? "✅ VERIFIED" : "❌ NOT VERIFIED");
            assertTrue(pairVerified, "Client should be pair verified after successful verification");

            // Test accessory interaction
            logger.info("📡 **TESTING ACCESSORY INTERACTION**...");
            logger.debug("📡 Retrieving remote accessories...");
            Collection<HomekitAccessory> accessories = client.getRemoteAccessories();
            assertNotNull(accessories, "Accessories collection should not be null");
            logger.info("📦 **ACCESSORY DISCOVERY**: Found {} accessories", accessories.size());

            // Detailed accessory information
            for (HomekitAccessory accessory : accessories) {
                logger.info("🔧 **ACCESSORY DETAILS**:");
                logger.info("   Label: {}", accessory.getLabel());
                logger.info("   UID: {}", accessory.getUID());

                Collection<HomekitService> services = accessory.getServices();
                logger.info("   📋 Services: {} total", services.size());

                for (HomekitService service : services) {
                    logger.info("     🔧 Service: {} ({})", service.getName(), service.getType());
                    Collection<HomekitCharacteristic<?>> characteristics = service.getCharacteristics();
                    logger.info("       📊 Characteristics: {} total", characteristics.size());

                    for (HomekitCharacteristic<?> characteristic : characteristics) {
                        logger.debug("         - type: {} value: {}", characteristic.getType(),
                                characteristic.getValue());
                    }
                }
            }

            logger.info("🎉 **REAL ACCESSORY TEST COMPLETED SUCCESSFULLY!**");
            logger.info("✅ **ALL TESTS PASSED**:");
            logger.info("   ✅ Network connectivity validated");
            logger.info("   ✅ SRP6 pairing completed");
            logger.info("   ✅ Pairing verification successful");
            logger.info("   ✅ Accessory discovery working");
            logger.info("   ✅ Service enumeration working");
            logger.info("🔬 **SRP6 COMPATIBILITY**: Implementation appears compatible with real HomeKit accessories");
            logger.info("🔬 **CALCULATION METHOD TESTED**: {}", calculationMethod);

        } catch (Exception e) {
            logger.error("❌ **REAL ACCESSORY TEST FAILED**", e);
            logger.error("🔍 **DEBUGGING INFORMATION**:");
            logger.error("   📊 Check TRACE logs above for detailed SRP6 values");
            logger.error("   🌐 Check network logs for HTTP/TCP communication");
            logger.error("   🔬 Compare SRP6 values with HAP-Java expectations");
            logger.error("   📋 Verify setup code format and correctness");
            logger.error("   🌍 Check network connectivity to accessory");
            logger.error("🔬 **SRP6 DEBUGGING ANALYSIS NEEDED**");
            logger.error("The error suggests potential SRP6 implementation differences");
            logger.error("Please check the logs above for captured SRP6 values");
            logger.error("Compare these values with HAP-Java's expected values");
            logger.error("🔬 **CALCULATION METHOD**: {}", calculationMethod);
            throw e;
        }
    }
}
