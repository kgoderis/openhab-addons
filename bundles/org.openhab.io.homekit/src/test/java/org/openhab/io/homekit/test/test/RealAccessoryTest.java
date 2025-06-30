package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.net.InetAddress;
import java.util.Collection;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.accessory.HomekitAccessoryCategory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.factory.HomekitAccessoryFactory;
import org.openhab.io.homekit.api.registry.HomekitAccessoryRegistry;
import org.openhab.io.homekit.api.registry.HomekitPairingRegistry;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.exception.HomekitServerException;
import org.openhab.io.homekit.server.HomekitRemoteAccessoryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RealAccessoryTest {

    private static final Logger logger = LoggerFactory.getLogger(RealAccessoryTest.class);

    private static final String REAL_ACCESSORY_IP = "192.168.1.100"; // Configure for your accessory
    private static final int REAL_ACCESSORY_PORT = 80;
    private static final String SETUP_CODE = "123-45-678"; // Your accessory's setup code

    @Test
    @Order(1)
    void testDiscoverRealAccessory() throws Exception {
        HomekitRemoteAccessoryServer client = createTestClient();
        client.start();

        // Attempt connection
        assertTrue(client.isConnected());
    }

    @Test
    @Order(2)
    void testPairWithRealAccessory() throws Exception {
        HomekitRemoteAccessoryServer client = createTestClient();
        client.setSetupCode(SETUP_CODE);

        try {
            client.pairSetup();
            assertTrue(client.isPaired());

            // Test pair verification
            assertTrue(client.pairVerify());
            assertTrue(client.isPairVerified());

        } catch (HomekitServerException e) {
            // Log detailed error for analysis
            logger.error("Pairing failed: {}", e.getMessage(), e);
            fail("Pairing with real accessory failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void testAccessoryInteraction() throws Exception {
        HomekitRemoteAccessoryServer client = createTestClient();
        client.setSetupCode(SETUP_CODE);
        client.pairSetup();
        client.pairVerify();

        // Test getting accessories
        Collection<HomekitAccessory> accessories = client.getRemoteAccessories();
        assertFalse(accessories.isEmpty());

        // Test characteristic updates
        for (HomekitAccessory accessory : accessories) {
            logger.info("Found accessory: {}", accessory.getAccessoryId());
            for (HomekitService service : accessory.getServices()) {
                logger.info("  Service: {}", service.getClass().getSimpleName());
                for (HomekitCharacteristic<?> characteristic : service.getCharacteristics()) {
                    logger.info("    Characteristic: {}", characteristic.getClass().getSimpleName());
                }
            }
        }
    }

    private HomekitRemoteAccessoryServer createTestClient() throws Exception {
        return new HomekitRemoteAccessoryServer(HomekitAccessoryCategory.OTHER, "TestClient",
                InetAddress.getByName(REAL_ACCESSORY_IP), REAL_ACCESSORY_PORT, mock(HomekitAccessoryRegistry.class),
                mock(HomekitPairingRegistry.class), mock(HomekitEventManager.class),
                mock(HomekitAccessoryFactory.class));
    }
}
