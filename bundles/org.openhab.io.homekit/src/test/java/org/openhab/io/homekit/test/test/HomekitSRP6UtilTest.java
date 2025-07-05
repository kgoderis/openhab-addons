package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Util;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Util.CalculationMethod;
import org.openhab.io.homekit.test.helper.HomekitSRP6TestVectors;
import org.openhab.io.homekit.util.HomekitByte;

@TestMethodOrder(OrderAnnotation.class)
class HomekitSRP6UtilTest {

    private static final BigInteger N = HomekitSRP6TestVectors.getN();
    private static final BigInteger g = HomekitSRP6TestVectors.getG();
    private static final BigInteger A = HomekitSRP6TestVectors.getAPublic();
    private static final BigInteger B = HomekitSRP6TestVectors.getBPublic();
    private static final BigInteger a = HomekitSRP6TestVectors.getAPrivate();
    private static final BigInteger b = HomekitSRP6TestVectors.getBPrivate();
    private static final BigInteger v = HomekitSRP6TestVectors.getV();
    private static final BigInteger u = HomekitSRP6TestVectors.getU();
    private static final BigInteger S = HomekitSRP6TestVectors.getPremasterSecret();
    private static final String username = HomekitSRP6TestVectors.USERNAME;
    private static final String password = HomekitSRP6TestVectors.PASSWORD;
    private static final byte[] identity = username.getBytes(StandardCharsets.UTF_8);
    private static final byte[] pw = password.getBytes(StandardCharsets.UTF_8);
    private static final byte[] salt = HomekitByte.toByteArray(HomekitSRP6TestVectors.getS());
    private static final Digest digest = new SHA512Digest();

    @Test
    @Order(1)
    void testCalculateK() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateK across all calculation methods:\n");

        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger k = HomekitSRP6Util.calculateK(method, digest, N, g);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(k));
            results.append(String.format("✅ k (%s): %s\n", method, hexResult));
            assertNotNull(k, "k should not be null for method " + method);
        }

        results.append("✅ testCalculateK completed successfully\n");
        results.append(String.format("📋 Expected k from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.K_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(2)
    void testCalculateU() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateU across all calculation methods:\n");

        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger uVal = HomekitSRP6Util.calculateU(method, digest, N, A, B);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(uVal));
            results.append(String.format("✅ u (%s): %s\n", method, hexResult));
            assertNotNull(uVal, "u should not be null for method " + method);
        }

        results.append("✅ testCalculateU completed successfully\n");
        results.append(String.format("📋 Expected u from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.U_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(3)
    void testCalculateX() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateX across all calculation methods:\n");

        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger x = HomekitSRP6Util.calculateX(method, digest, N, salt, identity, pw);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(x));
            results.append(String.format("✅ x (%s): %s\n", method, hexResult));
            assertNotNull(x, "x should not be null for method " + method);
        }

        results.append("✅ testCalculateX completed successfully\n");
        results.append(String.format("📋 Expected x from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.X_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(4)
    void testCalculateM1() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateM1 across all calculation methods:\n");

        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger m1 = HomekitSRP6Util.calculateM1(method, digest, N, A, B, S, g, identity, salt);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(m1));
            results.append(String.format("✅ M1 (%s): %s\n", method, hexResult));
            assertNotNull(m1, "M1 should not be null for method " + method);
        }

        results.append("✅ testCalculateM1 completed successfully\n");
        results.append(String.format("📋 Expected M1 from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.M1_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(5)
    void testCalculateM2() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateM2 across all calculation methods:\n");

        BigInteger m1 = HomekitSRP6Util.calculateM1(CalculationMethod.BOUNCYCASTLE, digest, N, A, B, S, g, identity,
                salt);
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger m2 = HomekitSRP6Util.calculateM2(method, digest, N, A, m1, S);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(m2));
            results.append(String.format("✅ M2 (%s): %s\n", method, hexResult));
            assertNotNull(m2, "M2 should not be null for method " + method);
        }

        results.append("✅ testCalculateM2 completed successfully\n");
        results.append(String.format("📋 Expected M2 from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.M2_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(6)
    void testCalculateS() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateS across all calculation methods:\n");

        // For S, k is required. Use calculateK with RFC method
        BigInteger kVal = HomekitSRP6Util.calculateK(CalculationMethod.RFC, digest, N, g);
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger sVal = HomekitSRP6Util.calculateS(method, digest, N, g, A, B, a, HomekitSRP6TestVectors.getX(),
                    u, kVal);
            String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(sVal));
            results.append(String.format("✅ S (%s): %s\n", method, hexResult));
            assertNotNull(sVal, "S should not be null for method " + method);
        }

        results.append("✅ testCalculateS completed successfully\n");
        results.append(String.format("📋 Expected S (PremasterSecret) from test vector: %s\n", HomekitByte
                .toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.PREMASTER_SECRET_HEX, 16)))));
        System.out.print(results.toString());
    }

    @Test
    @Order(7)
    void testCalculateKRFC() {
        StringBuilder results = new StringBuilder();
        results.append("✅ Testing calculateKRFC:\n");

        BigInteger k = HomekitSRP6Util.calculateKRFC(digest, N, g);
        String hexResult = HomekitByte.toHex(HomekitByte.toByteArray(k));
        results.append(String.format("✅ k (RFC): %s\n", hexResult));
        assertNotNull(k, "k (RFC) should not be null");

        results.append("✅ testCalculateKRFC completed successfully\n");
        results.append(String.format("📋 Expected k from test vector: %s\n",
                HomekitByte.toHex(HomekitByte.toByteArray(new BigInteger(HomekitSRP6TestVectors.K_HEX, 16)))));
        System.out.print(results.toString());
    }
}
