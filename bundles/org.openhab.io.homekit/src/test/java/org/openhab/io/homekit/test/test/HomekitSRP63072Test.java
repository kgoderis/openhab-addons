// package org.openhab.io.homekit.test.test;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.math.BigInteger;
// import java.security.MessageDigest;

// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.openhab.io.homekit.protocol.crypto.HomekitClientSRP6Session;
// import org.openhab.io.homekit.protocol.crypto.HomekitServerSRP6Session;
// import org.openhab.io.homekit.test.helper.SRP63072TestVectors;

// import com.nimbusds.srp6.SRP6ClientCredentials;
// import com.nimbusds.srp6.SRP6CryptoParams;

// /**
// * Test class for validating SRP6 implementation against 3072-bit test vectors
// *
// * This test uses 3072-bit test vectors to validate that our SRP6 implementation
// * correctly follows the SRP6 protocol specification with modern cryptographic parameters.
// * The 3072-bit parameters provide stronger security than the 1024-bit RFC5054 test vectors.
// */
// public class HomekitSRP63072Test {

// @BeforeAll
// static void setUp() {
// // Print test vector information for debugging
// SRP63072TestVectors.printTestVectorInfo();
// }

// @Test
// void test3072BitTestVectorValidation() {
// // Validate that all test vectors are valid hexadecimal strings
// assertTrue(SRP63072TestVectors.validateHexStrings(),
// "All 3072-bit test vectors should be valid hexadecimal strings");
// }

// @Test
// void testVerifierCalculation() throws Exception {
// // Test verifier calculation using 3072-bit parameters
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger s = SRP63072TestVectors.getS();
// String I = SRP63072TestVectors.USERNAME;
// String P = SRP63072TestVectors.PASSWORD;

// // Calculate x = H(s | H(I | ":" | P))
// MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

// // H(I | ":" | P)
// String userPassHash = I + ":" + P;
// byte[] userPassHashBytes = sha512.digest(userPassHash.getBytes());

// // H(s | H(I | ":" | P))
// byte[] sBytes = s.toByteArray();
// byte[] combined = new byte[sBytes.length + userPassHashBytes.length];
// System.arraycopy(sBytes, 0, combined, 0, sBytes.length);
// System.arraycopy(userPassHashBytes, 0, combined, sBytes.length, userPassHashBytes.length);
// byte[] xBytes = sha512.digest(combined);
// BigInteger x = new BigInteger(1, xBytes);

// // Calculate v = g^x mod N
// BigInteger v = g.modPow(x, N);

// // Compare with expected verifier
// BigInteger expectedV = SRP63072TestVectors.getV();
// assertEquals(expectedV, v, "Verifier calculation should match 3072-bit test vector");

// System.out.println("=== VERIFIER CALCULATION TEST ===");
// System.out.println("Calculated x: " + x.toString(16).toUpperCase());
// System.out.println("Expected x: " + SRP63072TestVectors.X_HEX);
// System.out.println("Calculated v: " + v.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Expected v: " + SRP63072TestVectors.EXPECTED_VERIFIER_HEX.substring(0, 40) + "...");
// System.out.println("Verifier match: " + (expectedV.equals(v) ? "PASS" : "FAIL"));
// }

// @Test
// void testServerSessionWith3072BitVectors() throws Exception {
// // Test server session using 3072-bit test vectors
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger v = SRP63072TestVectors.getV();
// BigInteger s = SRP63072TestVectors.getS();
// String I = SRP63072TestVectors.USERNAME;
// BigInteger expectedB = SRP63072TestVectors.getBPublic();

// // Create SRP6 crypto parameters
// SRP6CryptoParams config = new SRP6CryptoParams(N, g, "SHA-512");

// // Create server session
// HomekitServerSRP6Session serverSession = new HomekitServerSRP6Session(config);

// // Execute step1 to get server public value
// BigInteger bPrivate = SRP63072TestVectors.getBPrivate();
// BigInteger calculatedB = serverSession.step1(I, s, v, bPrivate);

// // Compare with expected value
// assertEquals(expectedB, calculatedB, "Server public value should match 3072-bit test vector");

// System.out.println("=== SERVER SESSION TEST ===");
// System.out.println("Calculated B: " + calculatedB.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Expected B: " + expectedB.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Server public value match: " + (expectedB.equals(calculatedB) ? "PASS" : "FAIL"));
// }

// @Test
// void testClientSessionWith3072BitVectors() throws Exception {
// // Test client session using 3072-bit test vectors
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger s = SRP63072TestVectors.getS();
// BigInteger B = SRP63072TestVectors.getBPublic();
// String I = SRP63072TestVectors.USERNAME;
// String P = SRP63072TestVectors.PASSWORD;
// BigInteger expectedA = SRP63072TestVectors.getAPublic();

// // Create SRP6 crypto parameters
// SRP6CryptoParams config = new SRP6CryptoParams(N, g, "SHA-512");

// // Create client session
// HomekitClientSRP6Session clientSession = new HomekitClientSRP6Session();

// // Execute step1 to record user credentials
// clientSession.step1(I, P);

// // Execute step2 to get client public value and credentials
// BigInteger aPrivate = SRP63072TestVectors.getAPrivate();
// SRP6ClientCredentials credentials = clientSession.step2(config, s, B, aPrivate);
// BigInteger calculatedA = credentials.A;

// // Compare with expected value
// assertEquals(expectedA, calculatedA, "Client public value should match 3072-bit test vector");

// System.out.println("=== CLIENT SESSION TEST ===");
// System.out.println("Calculated A: " + calculatedA.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Expected A: " + expectedA.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Client public value match: " + (expectedA.equals(calculatedA) ? "PASS" : "FAIL"));
// }

// @Test
// void testScramblingParameterCalculation() throws Exception {
// // Test scrambling parameter calculation
// BigInteger A = SRP63072TestVectors.getAPublic();
// BigInteger B = SRP63072TestVectors.getBPublic();
// BigInteger N = SRP63072TestVectors.getN();

// // Calculate u = H(A | B)
// MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
// byte[] aBytes = A.toByteArray();
// byte[] bBytes = B.toByteArray();
// byte[] combined = new byte[aBytes.length + bBytes.length];
// System.arraycopy(aBytes, 0, combined, 0, aBytes.length);
// System.arraycopy(bBytes, 0, combined, aBytes.length, bBytes.length);
// byte[] uBytes = sha512.digest(combined);
// BigInteger u = new BigInteger(1, uBytes);

// // Compare with expected value
// BigInteger expectedU = SRP63072TestVectors.getU();
// assertEquals(expectedU, u, "Scrambling parameter should match 3072-bit test vector");

// System.out.println("=== SCRAMBLING PARAMETER TEST ===");
// System.out.println("Calculated u: " + u.toString(16).toUpperCase());
// System.out.println("Expected u: " + expectedU.toString(16).toUpperCase());
// System.out.println("Scrambling parameter match: " + (expectedU.equals(u) ? "PASS" : "FAIL"));
// }

// @Test
// void testSessionKeyCalculation() throws Exception {
// // Test session key calculation using 3072-bit test vectors
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger v = SRP63072TestVectors.getV();
// BigInteger A = SRP63072TestVectors.getAPublic();
// BigInteger B = SRP63072TestVectors.getBPublic();
// BigInteger a = SRP63072TestVectors.getAPrivate();
// BigInteger b = SRP63072TestVectors.getBPrivate();
// BigInteger u = SRP63072TestVectors.getU();

// // Calculate client session key: S = (B - k * g^x)^(a + u * x) mod N
// // where k = H(N | g)
// MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
// byte[] nBytes = N.toByteArray();
// byte[] gBytes = g.toByteArray();
// byte[] combined = new byte[nBytes.length + gBytes.length];
// System.arraycopy(nBytes, 0, combined, 0, nBytes.length);
// System.arraycopy(gBytes, 0, combined, nBytes.length, gBytes.length);
// byte[] kBytes = sha512.digest(combined);
// BigInteger k = new BigInteger(1, kBytes);

// // Calculate x from verifier: v = g^x mod N
// // For this test, we'll use the expected x value from test vectors
// BigInteger x = SRP63072TestVectors.getX();

// // Client session key calculation
// BigInteger kTimesGx = k.multiply(g.modPow(x, N)).mod(N);
// BigInteger base = B.subtract(kTimesGx).mod(N);
// BigInteger exponent = a.add(u.multiply(x));
// BigInteger clientSessionKey = base.modPow(exponent, N);

// // Server session key calculation: S = (A * v^u)^b mod N
// BigInteger serverSessionKey = A.multiply(v.modPow(u, N)).modPow(b, N);

// // Both should be equal
// assertEquals(clientSessionKey, serverSessionKey, "Client and server session keys should be equal");

// // Compare with expected session key
// BigInteger expectedSessionKey = SRP63072TestVectors.getSessionKey();
// assertEquals(expectedSessionKey, clientSessionKey, "Session key should match 3072-bit test vector");

// System.out.println("=== SESSION KEY TEST ===");
// System.out
// .println("Client session key: " + clientSessionKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out
// .println("Server session key: " + serverSessionKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println(
// "Expected session key: " + expectedSessionKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Session keys equal: " + (clientSessionKey.equals(serverSessionKey) ? "PASS" : "FAIL"));
// System.out.println("Session key match: " + (expectedSessionKey.equals(clientSessionKey) ? "PASS" : "FAIL"));
// }

// @Test
// void testCompleteSRP6Exchange() throws Exception {
// // Test complete SRP6 exchange using 3072-bit test vectors
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger v = SRP63072TestVectors.getV();
// BigInteger s = SRP63072TestVectors.getS();
// String I = SRP63072TestVectors.USERNAME;
// String P = SRP63072TestVectors.PASSWORD;

// // Create SRP6 crypto parameters
// SRP6CryptoParams config = new SRP6CryptoParams(N, g, "SHA-512");

// // Create client and server sessions
// HomekitClientSRP6Session clientSession = new HomekitClientSRP6Session();
// HomekitServerSRP6Session serverSession = new HomekitServerSRP6Session(config);

// // Execute SRP6 protocol steps
// // Step 1: Client records credentials
// clientSession.step1(I, P);

// // Step 2: Server responds with salt and public value
// BigInteger bPrivate = SRP63072TestVectors.getBPrivate();
// BigInteger B = serverSession.step1(I, s, v, bPrivate);

// // Step 3: Client computes credentials
// BigInteger aPrivate = SRP63072TestVectors.getAPrivate();
// SRP6ClientCredentials clientCredentials = clientSession.step2(config, s, B, aPrivate);
// BigInteger A = clientCredentials.A;
// BigInteger M1 = clientCredentials.M1;

// // Verify public values match test vectors
// assertEquals(SRP63072TestVectors.getAPublic(), A, "Client public value should match test vector");
// assertEquals(SRP63072TestVectors.getBPublic(), B, "Server public value should match test vector");

// // Step 4: Server verifies client credentials and responds with evidence
// BigInteger M2 = serverSession.step2(A, M1);

// // Step 5: Client verifies server evidence
// clientSession.step3(M2);

// // Get session keys
// BigInteger clientKey = clientSession.getSessionKey(false);
// BigInteger serverKey = serverSession.getSessionKey(false);

// // Verify session keys are equal and match expected value
// assertEquals(clientKey, serverKey, "Client and server session keys should be equal");
// assertEquals(SRP63072TestVectors.getSessionKey(), clientKey, "Session key should match 3072-bit test vector");

// System.out.println("=== COMPLETE SRP6 EXCHANGE TEST ===");
// System.out.println("Client public value: " + A.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Server public value: " + B.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Client session key: " + clientKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Server session key: " + serverKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Expected session key: "
// + SRP63072TestVectors.getSessionKey().toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Exchange successful: "
// + (clientKey.equals(serverKey) && clientKey.equals(SRP63072TestVectors.getSessionKey()) ? "PASS"
// : "FAIL"));
// }

// @Test
// void testEvidenceMessageValidation() throws Exception {
// // Test evidence message validation using 3072-bit test vectors
// BigInteger expectedM1 = SRP63072TestVectors.getM1();
// BigInteger expectedM2 = SRP63072TestVectors.getM2();

// // Create SRP6 crypto parameters
// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// SRP6CryptoParams config = new SRP6CryptoParams(N, g, "SHA-512");

// // Create client and server sessions
// HomekitClientSRP6Session clientSession = new HomekitClientSRP6Session();
// HomekitServerSRP6Session serverSession = new HomekitServerSRP6Session(config);

// // Execute SRP6 protocol steps
// clientSession.step1(SRP63072TestVectors.USERNAME, SRP63072TestVectors.PASSWORD);
// BigInteger bPrivate = SRP63072TestVectors.getBPrivate();
// BigInteger B = serverSession.step1(SRP63072TestVectors.USERNAME, SRP63072TestVectors.getS(),
// SRP63072TestVectors.getV(), bPrivate);
// BigInteger aPrivate = SRP63072TestVectors.getAPrivate();
// SRP6ClientCredentials clientCredentials = clientSession.step2(config, SRP63072TestVectors.getS(), B, aPrivate);
// BigInteger M2 = serverSession.step2(clientCredentials.A, clientCredentials.M1);

// // Verify evidence messages match test vectors
// assertEquals(expectedM1, clientCredentials.M1, "Client evidence M1 should match 3072-bit test vector");
// assertEquals(expectedM2, M2, "Server evidence M2 should match 3072-bit test vector");

// System.out.println("=== EVIDENCE MESSAGE TEST ===");
// System.out.println("Client M1: " + clientCredentials.M1.toString(16).toUpperCase());
// System.out.println("Expected M1: " + expectedM1.toString(16).toUpperCase());
// System.out.println("Server M2: " + M2.toString(16).toUpperCase());
// System.out.println("Expected M2: " + expectedM2.toString(16).toUpperCase());
// System.out.println("Evidence messages match: "
// + (expectedM1.equals(clientCredentials.M1) && expectedM2.equals(M2) ? "PASS" : "FAIL"));
// }

// @Test
// void testHomeKitSpecificSRP6Implementation() throws Exception {
// // Test that our HomeKit SRP6 implementation works with 3072-bit test vectors
// // This test validates that our implementation is compatible with modern SRP6 protocol parameters

// BigInteger N = SRP63072TestVectors.getN();
// BigInteger g = SRP63072TestVectors.getG();
// BigInteger v = SRP63072TestVectors.getV();
// BigInteger s = SRP63072TestVectors.getS();
// String I = SRP63072TestVectors.USERNAME;
// String P = SRP63072TestVectors.PASSWORD;

// // Create SRP6 crypto parameters
// SRP6CryptoParams config = new SRP6CryptoParams(N, g, "SHA-512");

// // Create HomeKit SRP6 sessions
// HomekitClientSRP6Session clientSession = new HomekitClientSRP6Session();
// HomekitServerSRP6Session serverSession = new HomekitServerSRP6Session(config);

// // Execute SRP6 protocol steps
// // Step 1: Client records credentials
// clientSession.step1(I, P);

// // Step 2: Server responds with salt and public value
// BigInteger bPrivate = SRP63072TestVectors.getBPrivate();
// BigInteger B = serverSession.step1(I, s, v, bPrivate);

// // Step 3: Client computes credentials
// BigInteger aPrivate = SRP63072TestVectors.getAPrivate();
// SRP6ClientCredentials clientCredentials = clientSession.step2(config, s, B, aPrivate);
// BigInteger A = clientCredentials.A;
// BigInteger M1 = clientCredentials.M1;

// // Step 4: Server verifies client credentials and responds with evidence
// BigInteger M2 = serverSession.step2(A, M1);

// // Step 5: Client verifies server evidence
// clientSession.step3(M2);

// // Get session keys
// BigInteger clientKey = clientSession.getSessionKey(false);
// BigInteger serverKey = serverSession.getSessionKey(false);

// // Verify the exchange was successful
// assertNotNull(clientKey, "Client session key should not be null");
// assertNotNull(serverKey, "Server session key should not be null");
// assertEquals(clientKey, serverKey, "Client and server session keys should be equal");

// System.out.println("=== HOMEKIT SRP6 IMPLEMENTATION TEST ===");
// System.out.println("HomeKit implementation compatible with 3072-bit parameters: PASS");
// System.out.println("Client session key: " + clientKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Server session key: " + serverKey.toString(16).toUpperCase().substring(0, 40) + "...");
// System.out.println("Keys match: " + (clientKey.equals(serverKey) ? "PASS" : "FAIL"));
// }
// }
