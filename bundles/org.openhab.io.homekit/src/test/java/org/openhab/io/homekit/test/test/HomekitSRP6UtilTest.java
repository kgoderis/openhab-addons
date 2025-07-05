package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.junit.jupiter.api.Test;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Util;
import org.openhab.io.homekit.protocol.crypto.HomekitSRP6Util.CalculationMethod;
import org.openhab.io.homekit.test.helper.HomekitSRP6TestVectors;
import org.openhab.io.homekit.util.HomekitByte;

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
    void testCalculateM1() {
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger m1 = HomekitSRP6Util.calculateM1(method, digest, N, A, B, S, g, identity, salt);
            System.out.printf("M1 (%s): %s\n", method, m1.toString(16));
            assertNotNull(m1, "M1 should not be null for method " + method);
        }
    }

    @Test
    void testCalculateM2() {
        BigInteger m1 = HomekitSRP6Util.calculateM1(CalculationMethod.BOUNCYCASTLE, digest, N, A, B, S, g, identity,
                salt);
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger m2 = HomekitSRP6Util.calculateM2(method, digest, N, A, m1, S);
            System.out.printf("M2 (%s): %s\n", method, m2.toString(16));
            assertNotNull(m2, "M2 should not be null for method " + method);
        }
    }

    @Test
    void testCalculateU() {
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger uVal = HomekitSRP6Util.calculateU(method, digest, N, A, B);
            System.out.printf("u (%s): %s\n", method, uVal.toString(16));
            assertNotNull(uVal, "u should not be null for method " + method);
        }
    }

    @Test
    void testCalculateSessionKey() throws Exception {
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger k = HomekitSRP6Util.calculateSessionKey(method, digest, S, N);
            System.out.printf("SessionKey (%s): %s\n", method, k.toString(16));
            assertNotNull(k, "SessionKey should not be null for method " + method);
        }
    }

    @Test
    void testCalculateS() {
        // For S, k is required. Use calculateK
        BigInteger kVal = HomekitSRP6Util.calculateK(digest, N, g);
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger sVal = HomekitSRP6Util.calculateS(method, digest, N, g, A, B, a, HomekitSRP6TestVectors.getX(),
                    u, kVal);
            System.out.printf("S (%s): %s\n", method, sVal.toString(16));
            assertNotNull(sVal, "S should not be null for method " + method);
        }
    }

    @Test
    void testCalculateX() {
        for (CalculationMethod method : CalculationMethod.values()) {
            BigInteger x = HomekitSRP6Util.calculateX(method, digest, N, salt, identity, pw);
            System.out.printf("x (%s): %s\n", method, x.toString(16));
            assertNotNull(x, "x should not be null for method " + method);
        }
    }

    @Test
    void testCalculateK() {
        BigInteger k = HomekitSRP6Util.calculateK(digest, N, g);
        System.out.printf("k: %s\n", k.toString(16));
        assertNotNull(k, "k should not be null");
    }

    @Test
    void testCalculateKRFC() {
        BigInteger k = HomekitSRP6Util.calculateKRFC(digest, N, g);
        System.out.printf("k (RFC): %s\n", k.toString(16));
        assertNotNull(k, "k (RFC) should not be null");
    }
}