package org.openhab.io.homekit.protocol.crypto;

import java.math.BigInteger;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6Util;
import org.openhab.io.homekit.util.HomekitByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SRP6 (Secure Remote Password) utility class implementing various calculation methods.
 * 
 * This class provides implementations of SRP6 cryptographic calculations following different
 * specifications and library behaviors. The SRP6 protocol is defined in RFC 2945 and RFC 5054.
 * 
 * Key SRP6 Formulas:
 * - x = H(salt || H(identity || ':' || password)) (client secret)
 * - v = g^x mod N (verifier)
 * - A = g^a mod N (client public key)
 * - B = (k * v + g^b) mod N (server public key)
 * - u = H(A || B) (scrambling parameter)
 * - S = (B - k * g^x)^(a + u * x) mod N (premaster secret)
 * - K = H(S) (session key)
 * - M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S)) (client evidence)
 * - M2 = H(A || M1 || H(S)) (server evidence)
 */
public class HomekitSRP6Util {

    private static final Logger logger = LoggerFactory.getLogger(HomekitSRP6Util.class);

    /**
     * Enumeration of available M1 and M2 calculation methods.
     */
    public enum CalculationMethod {
        /**
         * Use BouncyCastle implementation with HomekitByte.toByteArray() for g parameter (matches accessory behavior).
         */
        BOUNCYCASTLE,

        /**
         * Use BouncyCastle implementation with padding for all parameters (legacy behavior).
         */
        BOUNCYCASTLEPAD,

        /**
         * Use NimbusDS-style implementation with cumulative hashing.
         */
        NIMBUS,

        /**
         * Use single hashing with raw byte representation (matches fast-srp-hap).
         */
        FASTSRP,
        RFC
    }

    /**
     * Calculate M1 (client evidence message) using the specified calculation method.
     * 
     * Formula: M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S))
     * 
     * Where:
     * - H(N) = hash of the modulus N
     * - H(g) = hash of the generator g
     * - H(I) = hash of the identity
     * - s = salt
     * - A = client public key
     * - B = server public key
     * - H(S) = hash of the premaster secret S
     * 
     * This is the client evidence message that proves the client knows the password
     * without revealing it. The server can verify this by computing the same value.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use (for BouncyCastle method)
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @param g The generator
     * @param identity The identity
     * @param salt The salt
     * @return The client evidence message M1
     */
    public static BigInteger calculateM1(CalculationMethod method, Digest digest, BigInteger N, BigInteger A,
            BigInteger B, BigInteger S, BigInteger g, byte[] identity, byte[] salt) {

        switch (method) {
            case RFC:
            case BOUNCYCASTLE:
                return calculateM1BouncyCastle(digest, N, A, B, S, g, identity, salt);
            case BOUNCYCASTLEPAD:
                return calculateM1BouncyCastlePad(digest, N, A, B, S, g, identity, salt);
            case NIMBUS:
                return calculateM1Nimbus(N, A, B, S, g, identity, salt);
            case FASTSRP:
                return calculateM1FastSRP(digest, N, A, B, S, g, identity, salt);
            default:
                throw new IllegalArgumentException("Unsupported calculation method: " + method);
        }
    }

    /**
     * Calculate M1 using BouncyCastle implementation with proper digest resets and padding.
     * 
     * Formula: M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(pad(N, padLength))
     * 2. Calculate H(g) = hash(pad(g, padLength))
     * 3. Calculate H(N) xor H(g)
     * 4. Calculate H(I) = hash(identity)
     * 5. Calculate H(S) = hash(pad(S, padLength))
     * 6. Concatenate: H(N) xor H(g) || H(I) || s || pad(A, padLength) || pad(B, padLength) || H(S)
     * 7. Calculate M1 = hash(concatenated_data)
     * 
     * This method uses padding for all parameters to ensure consistent byte lengths.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @param g The generator
     * @param identity The identity
     * @param salt The salt
     * @return The client evidence message M1
     */
    public static BigInteger calculateM1BouncyCastlePad(Digest digest, BigInteger N, BigInteger A, BigInteger B,
            BigInteger S, BigInteger g, byte[] identity, byte[] salt) {

        logger.debug("[SRP6][M1-BouncyCastlePad] Starting M1 calculation");
        logger.debug("[SRP6][M1-BouncyCastlePad] Input parameters:");
        logger.debug("[SRP6][M1-BouncyCastlePad]   N: {}", N.toString(16));
        logger.debug("[SRP6][M1-BouncyCastlePad]   g: {}", g.toString(16));
        logger.debug("[SRP6][M1-BouncyCastlePad]   A: {}", A.toString(16));
        logger.debug("[SRP6][M1-BouncyCastlePad]   B: {}", B.toString(16));
        logger.debug("[SRP6][M1-BouncyCastlePad]   S: {}", S.toString(16));
        logger.debug("[SRP6][M1-BouncyCastlePad]   identity: {}", HomekitByte.toHex(identity));
        logger.debug("[SRP6][M1-BouncyCastlePad]   salt: {}", HomekitByte.toHex(salt));

        // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))

        // Calculate H(N) - RFC-compliant: use N as-is
        digest.reset();
        byte[] nBytes = HomekitByte.Pad(N, N);
        logger.debug("[SRP6][M1-BouncyCastlePad] N bytes (padded): {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);
        byte[] hN = new byte[digest.getDigestSize()];
        digest.doFinal(hN, 0);
        logger.debug("[SRP6][M1-BouncyCastlePad] H(N): {}", HomekitByte.toHex(hN));

        // Calculate H(g) - RFC-compliant: use g as-is
        digest.reset();
        byte[] gBytes = HomekitByte.Pad(g, N);
        logger.debug("[SRP6][M1-BouncyCastlePad] g bytes (padded): {}", HomekitByte.toHex(gBytes));
        updateDigestInChunks(digest, gBytes);
        byte[] hg = new byte[digest.getDigestSize()];
        digest.doFinal(hg, 0);
        logger.debug("[SRP6][M1-BouncyCastlePad] H(g): {}", HomekitByte.toHex(hg));

        // H(N) xor H(g)
        byte[] hNxorHg = new byte[hN.length];
        for (int i = 0; i < hN.length; i++) {
            hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
        }
        logger.debug("[SRP6][M1-BouncyCastlePad] H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));

        // Calculate H(I)
        digest.reset();
        logger.debug("[SRP6][M1-BouncyCastlePad] identity bytes: {}", HomekitByte.toHex(identity));
        updateDigestInChunks(digest, identity);
        byte[] hu = new byte[digest.getDigestSize()];
        digest.doFinal(hu, 0);
        logger.debug("[SRP6][M1-BouncyCastlePad] H(I): {}", HomekitByte.toHex(hu));

        // Calculate H(S)
        digest.reset();
        byte[] sBytes = HomekitByte.Pad(S, N);
        logger.debug("[SRP6][M1-BouncyCastlePad] S bytes (padded): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);
        logger.debug("[SRP6][M1-BouncyCastlePad] H(S): {}", HomekitByte.toHex(hS));

        // Calculate final M1: H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        digest.reset();
        logger.debug("[SRP6][M1-BouncyCastlePad] Final M1 calculation - concatenating:");
        logger.debug("[SRP6][M1-BouncyCastlePad]   1. H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));
        logger.debug("[SRP6][M1-BouncyCastlePad]   2. H(I): {}", HomekitByte.toHex(hu));
        logger.debug("[SRP6][M1-BouncyCastlePad]   3. salt: {}", HomekitByte.toHex(salt));

        byte[] aBytes = HomekitByte.Pad(A, N);
        logger.debug("[SRP6][M1-BouncyCastlePad]   4. A bytes (padded): {}", HomekitByte.toHex(aBytes));

        byte[] bBytes = HomekitByte.Pad(B, N);
        logger.debug("[SRP6][M1-BouncyCastlePad]   5. B bytes (padded): {}", HomekitByte.toHex(bBytes));

        logger.debug("[SRP6][M1-BouncyCastlePad]   6. H(S): {}", HomekitByte.toHex(hS));

        updateDigestInChunks(digest, hNxorHg);
        updateDigestInChunks(digest, hu);
        updateDigestInChunks(digest, salt);
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);
        updateDigestInChunks(digest, hS);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m1 = new BigInteger(1, result);
        logger.debug("[SRP6][M1-BouncyCastlePad] Final M1: {}", m1.toString(16));
        return m1;
    }

    /**
     * Calculate M1 using BouncyCastle implementation with HomekitByte.toByteArray() for g parameter.
     * This matches the accessory behavior by using HomekitByte.toByteArray() instead of padding for g.
     * 
     * Formula: M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(pad(N, padLength))
     * 2. Calculate H(g) = hash(toByteArray(g)) // Uses raw bytes, not padded
     * 3. Calculate H(N) xor H(g)
     * 4. Calculate H(I) = hash(identity)
     * 5. Calculate H(S) = hash(pad(S, padLength))
     * 6. Concatenate: H(N) xor H(g) || H(I) || s || pad(A, padLength) || pad(B, padLength) || H(S)
     * 7. Calculate M1 = hash(concatenated_data)
     * 
     * This method uses HomekitByte.toByteArray() for g to match accessory behavior.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @param g The generator
     * @param identity The identity
     * @param salt The salt
     * @return The client evidence message M1
     */
    public static BigInteger calculateM1BouncyCastle(Digest digest, BigInteger N, BigInteger A, BigInteger B,
            BigInteger S, BigInteger g, byte[] identity, byte[] salt) {

        logger.debug("[SRP6][M1-BouncyCastle] Starting M1 calculation");
        logger.debug("[SRP6][M1-BouncyCastle] Input parameters:");
        logger.debug("[SRP6][M1-BouncyCastle]   N: {}", N.toString(16));
        logger.debug("[SRP6][M1-BouncyCastle]   g: {}", g.toString(16));
        logger.debug("[SRP6][M1-BouncyCastle]   A: {}", A.toString(16));
        logger.debug("[SRP6][M1-BouncyCastle]   B: {}", B.toString(16));
        logger.debug("[SRP6][M1-BouncyCastle]   S: {}", S.toString(16));
        logger.debug("[SRP6][M1-BouncyCastle]   identity: {}", HomekitByte.toHex(identity));
        logger.debug("[SRP6][M1-BouncyCastle]   salt: {}", HomekitByte.toHex(salt));

        // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][M1-BouncyCastle] padLength: {}", padLength);

        // Calculate H(N) - RFC-compliant: use N as-is
        digest.reset();
        byte[] nBytes = HomekitByte.Pad(N, N);
        logger.debug("[SRP6][M1-BouncyCastle] N bytes (padded): {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);
        byte[] hN = new byte[digest.getDigestSize()];
        digest.doFinal(hN, 0);
        logger.debug("[SRP6][M1-BouncyCastle] H(N): {}", HomekitByte.toHex(hN));

        // Calculate H(g) - Use HomekitByte.toByteArray() for g (matches accessory behavior)
        digest.reset();
        byte[] gBytes = HomekitByte.toByteArray(g);
        logger.debug("[SRP6][M1-BouncyCastle] g bytes (HomekitByte): {}", HomekitByte.toHex(gBytes));
        updateDigestInChunks(digest, gBytes);
        byte[] hg = new byte[digest.getDigestSize()];
        digest.doFinal(hg, 0);
        logger.debug("[SRP6][M1-BouncyCastle] H(g): {}", HomekitByte.toHex(hg));

        // H(N) xor H(g)
        byte[] hNxorHg = new byte[hN.length];
        for (int i = 0; i < hN.length; i++) {
            hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
        }
        logger.debug("[SRP6][M1-BouncyCastle] H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));

        // Calculate H(I)
        digest.reset();
        logger.debug("[SRP6][M1-BouncyCastle] identity bytes: {}", HomekitByte.toHex(identity));
        updateDigestInChunks(digest, identity);
        byte[] hu = new byte[digest.getDigestSize()];
        digest.doFinal(hu, 0);
        logger.debug("[SRP6][M1-BouncyCastle] H(I): {}", HomekitByte.toHex(hu));

        // Calculate H(S)
        digest.reset();
        byte[] sBytes = HomekitByte.Pad(S, N);
        logger.debug("[SRP6][M1-BouncyCastle] S bytes (padded): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);
        logger.debug("[SRP6][M1-BouncyCastle] H(S): {}", HomekitByte.toHex(hS));

        // Calculate final M1: H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        digest.reset();
        logger.debug("[SRP6][M1-BouncyCastle] Final M1 calculation - concatenating:");
        logger.debug("[SRP6][M1-BouncyCastle]   1. H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));
        logger.debug("[SRP6][M1-BouncyCastle]   2. H(I): {}", HomekitByte.toHex(hu));
        logger.debug("[SRP6][M1-BouncyCastle]   3. salt: {}", HomekitByte.toHex(salt));

        byte[] aBytes = HomekitByte.Pad(A, N);
        logger.debug("[SRP6][M1-BouncyCastle]   4. A bytes (padded): {}", HomekitByte.toHex(aBytes));

        byte[] bBytes = HomekitByte.Pad(B, N);
        logger.debug("[SRP6][M1-BouncyCastle]   5. B bytes (padded): {}", HomekitByte.toHex(bBytes));

        logger.debug("[SRP6][M1-BouncyCastle]   6. H(S): {}", HomekitByte.toHex(hS));

        updateDigestInChunks(digest, hNxorHg);
        updateDigestInChunks(digest, hu);
        updateDigestInChunks(digest, salt);
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);
        updateDigestInChunks(digest, hS);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m1 = new BigInteger(1, result);
        logger.debug("[SRP6][M1-BouncyCastle] Final M1: {}", m1.toString(16));
        return m1;
    }

    /**
     * Calculate M1 using Nimbus-style implementation with cumulative hashing.
     * 
     * Formula: M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(toByteArray(N))
     * 2. Calculate H(g) = hash(toByteArray(g))
     * 3. Calculate H(N) xor H(g)
     * 4. Calculate H(I) = hash(identity)
     * 5. Calculate H(S) = hash(toByteArray(S))
     * 6. Use cumulative hashing: digest.update() for each component
     * 7. Calculate M1 = digest.digest()
     * 
     * This method uses cumulative hashing (digest.update() calls) instead of
     * concatenating all data before hashing. It also uses toByteArray() for
     * all BigInteger parameters.
     * 
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @param g The generator
     * @param identity The identity
     * @param salt The salt
     * @return The client evidence message M1
     */
    public static BigInteger calculateM1Nimbus(BigInteger N, BigInteger A, BigInteger B, BigInteger S, BigInteger g,
            byte[] identity, byte[] salt) {
        logger.debug("[SRP6][M1-Nimbus] Starting M1 calculation");
        logger.debug("[SRP6][M1-Nimbus] Input parameters:");
        logger.debug("[SRP6][M1-Nimbus]   N: {}", N.toString(16));
        logger.debug("[SRP6][M1-Nimbus]   g: {}", g.toString(16));
        logger.debug("[SRP6][M1-Nimbus]   A: {}", A.toString(16));
        logger.debug("[SRP6][M1-Nimbus]   B: {}", B.toString(16));
        logger.debug("[SRP6][M1-Nimbus]   S: {}", S.toString(16));
        logger.debug("[SRP6][M1-Nimbus]   identity: {}", HomekitByte.toHex(identity));
        logger.debug("[SRP6][M1-Nimbus]   salt: {}", HomekitByte.toHex(salt));

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");

            // Calculate H(N) xor H(g)
            logger.debug("[SRP6][M1-Nimbus] Calculating H(N) xor H(g)");
            byte[] nBytes = HomekitByte.toByteArray(N);
            logger.debug("[SRP6][M1-Nimbus] N bytes (HomekitByte): {}", HomekitByte.toHex(nBytes));
            digest.update(nBytes);
            byte[] hN = digest.digest();
            logger.debug("[SRP6][M1-Nimbus] H(N): {}", HomekitByte.toHex(hN));

            byte[] gBytes = HomekitByte.toByteArray(g);
            logger.debug("[SRP6][M1-Nimbus] g bytes (HomekitByte): {}", HomekitByte.toHex(gBytes));
            digest.update(gBytes);
            byte[] hg = digest.digest();
            logger.debug("[SRP6][M1-Nimbus] H(g): {}", HomekitByte.toHex(hg));

            byte[] hNhg = HomekitByte.xor(hN, hg);
            logger.debug("[SRP6][M1-Nimbus] H(N) xor H(g): {}", HomekitByte.toHex(hNhg));

            // Calculate H(identity)
            logger.debug("[SRP6][M1-Nimbus] Calculating H(identity)");
            logger.debug("[SRP6][M1-Nimbus] identity bytes: {}", HomekitByte.toHex(identity));
            digest.update(identity); // In test: ctx.userID.getBytes(StandardCharsets.UTF_8)
            byte[] hu = digest.digest();
            logger.debug("[SRP6][M1-Nimbus] H(identity): {}", HomekitByte.toHex(hu));

            // Calculate H(S)
            logger.debug("[SRP6][M1-Nimbus] Calculating H(S)");
            byte[] sBytes = HomekitByte.toByteArray(S);
            logger.debug("[SRP6][M1-Nimbus] S bytes (HomekitByte): {}", HomekitByte.toHex(sBytes));
            digest.update(sBytes);
            byte[] hS = digest.digest();
            logger.debug("[SRP6][M1-Nimbus] H(S): {}", HomekitByte.toHex(hS));

            // Final M1 calculation: H(H(N) xor H(g) || H(identity) || s || A || B || H(S))
            logger.debug("[SRP6][M1-Nimbus] Final M1 calculation - concatenating:");
            logger.debug("[SRP6][M1-Nimbus]   1. H(N) xor H(g): {}", HomekitByte.toHex(hNhg));
            logger.debug("[SRP6][M1-Nimbus]   2. H(identity): {}", HomekitByte.toHex(hu));
            logger.debug("[SRP6][M1-Nimbus]   3. salt: {}", HomekitByte.toHex(salt));

            byte[] aBytes = HomekitByte.toByteArray(A);
            logger.debug("[SRP6][M1-Nimbus]   4. A bytes (HomekitByte): {}", HomekitByte.toHex(aBytes));

            byte[] bBytes = HomekitByte.toByteArray(B);
            logger.debug("[SRP6][M1-Nimbus]   5. B bytes (HomekitByte): {}", HomekitByte.toHex(bBytes));

            logger.debug("[SRP6][M1-Nimbus]   6. H(S): {}", HomekitByte.toHex(hS));

            digest.update(hNhg);
            digest.update(hu);
            digest.update(salt);
            digest.update(aBytes);
            digest.update(bBytes);
            digest.update(hS);

            byte[] result = digest.digest();
            BigInteger m1 = new BigInteger(1, result);
            logger.debug("[SRP6][M1-Nimbus] Final M1: {}", m1.toString(16));
            return m1;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 not available", e);
        }
    }

    /**
     * Calculate M1 using FastSRP-compliant implementation that matches the JavaScript fast-srp library.
     * 
     * Formula: M1 = H(H(N) xor H(g) || H(I) || s || A || B || H(S))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(pad(N, padLength))
     * 2. Calculate H(g) = hash(toByteArray(g)) // Uses raw bytes, not padded
     * 3. Calculate H(N) xor H(g)
     * 4. Calculate H(I) = hash(identity)
     * 5. Calculate H(S) = hash(toByteArray(S)) // Uses full byte representation
     * 6. Concatenate: H(N) xor H(g) || H(I) || s || pad(A, padLength) || pad(B, padLength) || H(S)
     * 7. Calculate M1 = hash(concatenated_data)
     * 
     * This method matches the fast-srp-hap JavaScript library behavior exactly.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @param S The session key
     * @param g The generator
     * @param identity The identity
     * @param salt The salt
     * @return The client evidence message M1
     */
    public static BigInteger calculateM1FastSRP(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
            BigInteger g, byte[] identity, byte[] salt) {

        logger.debug("[SRP6][M1-FastSRP] Starting M1 calculation");
        logger.debug("[SRP6][M1-FastSRP] Input parameters:");
        logger.debug("[SRP6][M1-FastSRP]   N: {}", N.toString(16));
        logger.debug("[SRP6][M1-FastSRP]   g: {}", g.toString(16));
        logger.debug("[SRP6][M1-FastSRP]   A: {}", A.toString(16));
        logger.debug("[SRP6][M1-FastSRP]   B: {}", B.toString(16));
        logger.debug("[SRP6][M1-FastSRP]   S: {}", S.toString(16));
        logger.debug("[SRP6][M1-FastSRP]   identity: {}", HomekitByte.toHex(identity));
        logger.debug("[SRP6][M1-FastSRP]   salt: {}", HomekitByte.toHex(salt));

        // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        // FastSRP uses raw byte representation without padding

        // Calculate H(N) - use padToN() to match fast-srp-hap behavior
        digest.reset();
        byte[] nBytes = HomekitByte.Pad(N, N);
        logger.debug("[SRP6][M1-FastSRP] N bytes (padded): {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);
        byte[] hN = new byte[digest.getDigestSize()];
        digest.doFinal(hN, 0);
        logger.debug("[SRP6][M1-FastSRP] H(N): {}", HomekitByte.toHex(hN));

        // Calculate H(g) - use HomekitByte.toByteArray() to match fast-srp-hap behavior
        digest.reset();
        byte[] gBytes = HomekitByte.toByteArray(g);
        logger.debug("[SRP6][M1-FastSRP] g bytes (raw): {}", HomekitByte.toHex(gBytes));
        updateDigestInChunks(digest, gBytes);
        byte[] hg = new byte[digest.getDigestSize()];
        digest.doFinal(hg, 0);
        logger.debug("[SRP6][M1-FastSRP] H(g): {}", HomekitByte.toHex(hg));

        // H(N) xor H(g)
        byte[] hNxorHg = new byte[hN.length];
        for (int i = 0; i < hN.length; i++) {
            hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
        }
        logger.debug("[SRP6][M1-FastSRP] H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));

        // Calculate H(I)
        digest.reset();
        logger.debug("[SRP6][M1-FastSRP] identity bytes: {}", HomekitByte.toHex(identity));
        updateDigestInChunks(digest, identity);
        byte[] hu = new byte[digest.getDigestSize()];
        digest.doFinal(hu, 0);
        logger.debug("[SRP6][M1-FastSRP] H(I): {}", HomekitByte.toHex(hu));

        // Calculate H(S) - use full byte representation (including leading zeros)
        digest.reset();
        byte[] sBytes = HomekitByte.toByteArray(S);
        logger.debug("[SRP6][M1-FastSRP] S bytes (full): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);
        logger.debug("[SRP6][M1-FastSRP] H(S): {}", HomekitByte.toHex(hS));

        // Calculate final M1: H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        digest.reset();
        logger.debug("[SRP6][M1-FastSRP] Final M1 calculation - concatenating:");
        logger.debug("[SRP6][M1-FastSRP]   1. H(N) xor H(g): {}", HomekitByte.toHex(hNxorHg));
        logger.debug("[SRP6][M1-FastSRP]   2. H(I): {}", HomekitByte.toHex(hu));
        logger.debug("[SRP6][M1-FastSRP]   3. salt: {}", HomekitByte.toHex(salt));

        byte[] aBytes = HomekitByte.Pad(A, N);
        logger.debug("[SRP6][M1-FastSRP]   4. A bytes (padded): {}", HomekitByte.toHex(aBytes));

        byte[] bBytes = HomekitByte.Pad(B, N);
        logger.debug("[SRP6][M1-FastSRP]   5. B bytes (padded): {}", HomekitByte.toHex(bBytes));

        logger.debug("[SRP6][M1-FastSRP]   6. H(S): {}", HomekitByte.toHex(hS));

        updateDigestInChunks(digest, hNxorHg);
        updateDigestInChunks(digest, hu);
        updateDigestInChunks(digest, salt);
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);
        updateDigestInChunks(digest, hS);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m1 = new BigInteger(1, result);
        logger.debug("[SRP6][M1-FastSRP] Final M1: {}", m1.toString(16));
        return m1;
    }

    /**
     * Calculate M2 (server evidence message) using the specified calculation method.
     * 
     * Formula: M2 = H(A || M1 || H(S))
     * 
     * Where:
     * - A = client public key
     * - M1 = client evidence message
     * - H(S) = hash of the premaster secret S
     * 
     * This is the server evidence message that proves the server knows the premaster secret
     * without revealing it. The client can verify this by computing the same value.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use (for BouncyCastle method)
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message M2
     */
    public static BigInteger calculateM2(CalculationMethod method, Digest digest, BigInteger N, BigInteger A,
            BigInteger M1, BigInteger S) {

        switch (method) {
            case RFC:
            case BOUNCYCASTLE:
                return calculateM2BouncyCastle(digest, N, A, M1, S);
            case BOUNCYCASTLEPAD:
                return calculateM2BouncyCastlePad(digest, N, A, M1, S);
            case NIMBUS:
                return calculateM2Nimbus(A, M1, S);
            case FASTSRP:
                return calculateM2FastSRP(digest, N, A, M1, S);
            default:
                throw new IllegalArgumentException("Unsupported calculation method: " + method);
        }
    }

    /**
     * Calculate M2 using BouncyCastle implementation with proper digest resets and padding.
     *
     * @param digest The message digest instance
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message 'M2'
     * @throws IllegalArgumentException if any parameter is null
     */
    public static BigInteger calculateM2BouncyCastlePad(Digest digest, BigInteger N, BigInteger A, BigInteger M1,
            BigInteger S) {

        logger.debug("[SRP6][M2-BouncyCastlePad] Starting M2 calculation");
        logger.debug("[SRP6][M2-BouncyCastlePad] Input parameters:");
        logger.debug("[SRP6][M2-BouncyCastlePad]   N: {}", N.toString(16));
        logger.debug("[SRP6][M2-BouncyCastlePad]   A: {}", A.toString(16));
        logger.debug("[SRP6][M2-BouncyCastlePad]   M1: {}", M1.toString(16));
        logger.debug("[SRP6][M2-BouncyCastlePad]   S: {}", S.toString(16));

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][M2-BouncyCastlePad] padLength: {}", padLength);

        // Validate all parameters
        StringBuilder missingParams = new StringBuilder();
        if (digest == null) {
            missingParams.append("digest, ");
        }
        if (N == null) {
            missingParams.append("N (modulus), ");
        }
        if (A == null) {
            missingParams.append("A (client public key), ");
        }
        if (M1 == null) {
            missingParams.append("M1 (client evidence message), ");
        }
        if (S == null) {
            missingParams.append("S (session key), ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new IllegalArgumentException(
                    "Missing required parameters for M2 calculation: " + missingParams.toString());
        }

        // M2 = H(A | M1 | H(S))
        logger.debug("[SRP6][M2-BouncyCastlePad] Calculating H(S)");
        digest.reset();
        byte[] sBytes = S.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastlePad] S bytes (padded): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);
        logger.debug("[SRP6][M2-BouncyCastlePad] H(S): {}", HomekitByte.toHex(hS));

        logger.debug("[SRP6][M2-BouncyCastlePad] Final M2 calculation - concatenating:");
        digest.reset();
        byte[] aBytes = A.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastlePad]   1. A bytes (padded): {}", HomekitByte.toHex(aBytes));
        updateDigestInChunks(digest, aBytes);

        byte[] m1Bytes = M1.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastlePad]   2. M1 bytes (padded): {}", HomekitByte.toHex(m1Bytes));
        updateDigestInChunks(digest, m1Bytes);

        logger.debug("[SRP6][M2-BouncyCastlePad]   3. H(S): {}", HomekitByte.toHex(hS));
        digest.update(hS, 0, hS.length);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m2 = new BigInteger(1, result);
        logger.debug("[SRP6][M2-BouncyCastlePad] Final M2: {}", m2.toString(16));
        return m2;
    }

    /**
     * Calculate M2 using BouncyCastle implementation with HomekitByte.toByteArray() for parameters.
     * This matches the accessory behavior by using HomekitByte.toByteArray() instead of padding.
     *
     * @param digest The message digest instance
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message 'M2'
     * @throws IllegalArgumentException if any parameter is null
     */
    public static BigInteger calculateM2BouncyCastle(Digest digest, BigInteger N, BigInteger A, BigInteger M1,
            BigInteger S) {

        logger.debug("[SRP6][M2-BouncyCastle] Starting M2 calculation");
        logger.debug("[SRP6][M2-BouncyCastle] Input parameters:");
        logger.debug("[SRP6][M2-BouncyCastle]   N: {}", N.toString(16));
        logger.debug("[SRP6][M2-BouncyCastle]   A: {}", A.toString(16));
        logger.debug("[SRP6][M2-BouncyCastle]   M1: {}", M1.toString(16));
        logger.debug("[SRP6][M2-BouncyCastle]   S: {}", S.toString(16));

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][M2-BouncyCastle] padLength: {}", padLength);

        // Validate all parameters
        StringBuilder missingParams = new StringBuilder();
        if (digest == null) {
            missingParams.append("digest, ");
        }
        if (N == null) {
            missingParams.append("N (modulus), ");
        }
        if (A == null) {
            missingParams.append("A (client public key), ");
        }
        if (M1 == null) {
            missingParams.append("M1 (client evidence message), ");
        }
        if (S == null) {
            missingParams.append("S (session key), ");
        }

        if (missingParams.length() > 0) {
            // Remove trailing comma and space
            missingParams.setLength(missingParams.length() - 2);
            throw new IllegalArgumentException(
                    "Missing required parameters for M2 calculation: " + missingParams.toString());
        }

        // M2 = H(A | M1 | H(S))
        logger.debug("[SRP6][M2-BouncyCastle] Calculating H(S)");
        digest.reset();
        byte[] sBytes = S.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastle] S bytes (HomekitByte): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);
        logger.debug("[SRP6][M2-BouncyCastle] H(S): {}", HomekitByte.toHex(hS));

        logger.debug("[SRP6][M2-BouncyCastle] Final M2 calculation - concatenating:");
        digest.reset();
        byte[] aBytes = A.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastle]   1. A bytes (HomekitByte): {}", HomekitByte.toHex(aBytes));
        updateDigestInChunks(digest, aBytes);

        byte[] m1Bytes = M1.toByteArray();
        logger.debug("[SRP6][M2-BouncyCastle]   2. M1 bytes (HomekitByte): {}", HomekitByte.toHex(m1Bytes));
        updateDigestInChunks(digest, m1Bytes);

        logger.debug("[SRP6][M2-BouncyCastle]   3. H(S): {}", HomekitByte.toHex(hS));
        digest.update(hS, 0, hS.length);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m2 = new BigInteger(1, result);
        logger.debug("[SRP6][M2-BouncyCastle] Final M2: {}", m2.toString(16));
        return m2;
    }

    /**
     * Calculate M2 using Nimbus-style implementation with cumulative hashing.
     * 
     * Formula: M2 = H(A || M1 || H(S))
     * 
     * Algorithm:
     * 1. Calculate H(S) = hash(toByteArray(S))
     * 2. Final M2 calculation: H(A || M1 || H(S))
     * 
     * This method uses toByteArray() for S and cumulative hashing for A and M1.
     * 
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message M2
     */
    public static BigInteger calculateM2Nimbus(BigInteger A, BigInteger M1, BigInteger S) {
        logger.debug("[SRP6][M2-Nimbus] Starting M2 calculation");
        logger.debug("[SRP6][M2-Nimbus] Input parameters:");
        logger.debug("[SRP6][M2-Nimbus]   A: {}", A.toString(16));
        logger.debug("[SRP6][M2-Nimbus]   M1: {}", M1.toString(16));
        logger.debug("[SRP6][M2-Nimbus]   S: {}", S.toString(16));

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");

            // Calculate H(S)
            logger.debug("[SRP6][M2-Nimbus] Calculating H(S)");
            byte[] sBytes = S.toByteArray();
            logger.debug("[SRP6][M2-Nimbus] S bytes (HomekitByte): {}", HomekitByte.toHex(sBytes));
            digest.update(sBytes);
            byte[] hS = digest.digest();
            logger.debug("[SRP6][M2-Nimbus] H(S): {}", HomekitByte.toHex(hS));

            // Final M2 calculation: H(A || M1 || H(S))
            logger.debug("[SRP6][M2-Nimbus] Final M2 calculation - concatenating:");
            byte[] aBytes = A.toByteArray();
            logger.debug("[SRP6][M2-Nimbus]   1. A bytes (HomekitByte): {}", HomekitByte.toHex(aBytes));
            digest.update(aBytes);

            byte[] m1Bytes = M1.toByteArray();
            logger.debug("[SRP6][M2-Nimbus]   2. M1 bytes (HomekitByte): {}", HomekitByte.toHex(m1Bytes));
            digest.update(m1Bytes);

            logger.debug("[SRP6][M2-Nimbus]   3. H(S): {}", HomekitByte.toHex(hS));
            digest.update(hS);

            byte[] result = digest.digest();
            BigInteger m2 = new BigInteger(1, result);
            logger.debug("[SRP6][M2-Nimbus] Final M2: {}", m2.toString(16));
            return m2;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 not available", e);
        }
    }

    /**
     * Calculate M2 using FastSRP-compliant implementation that matches the JavaScript fast-srp library.
     * 
     * Formula: M2 = H(A || M1 || K)
     * 
     * Algorithm:
     * 1. Calculate K = hash(S) (session key)
     * 2. Final M2 calculation: H(A || M1 || K)
     * 
     * This method matches the fast-srp-hap JavaScript library's getM2() method exactly.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message M2
     */
    public static BigInteger calculateM2FastSRP(Digest digest, BigInteger N, BigInteger A, BigInteger M1,
            BigInteger S) {
        logger.debug("[SRP6][M2-FastSRP] Starting M2 calculation");
        logger.debug("[SRP6][M2-FastSRP] Input parameters:");
        logger.debug("[SRP6][M2-FastSRP]   A: {}", A.toString(16));
        logger.debug("[SRP6][M2-FastSRP]   M1: {}", M1.toString(16));
        logger.debug("[SRP6][M2-FastSRP]   S: {}", S.toString(16));

        // M2 = H(A || M1 || K)
        // FastSRP uses raw byte representation without padding

        // Calculate K = H(S) (session key)
        digest.reset();
        byte[] sBytes = HomekitByte.toByteArray(S);
        logger.debug("[SRP6][M2-FastSRP] S bytes (full): {}", HomekitByte.toHex(sBytes));
        updateDigestInChunks(digest, sBytes);
        byte[] kBytes = new byte[digest.getDigestSize()];
        digest.doFinal(kBytes, 0);
        logger.debug("[SRP6][M2-FastSRP] K = H(S): {}", HomekitByte.toHex(kBytes));

        // Final M2 calculation: H(A || M1 || K)
        digest.reset();
        logger.debug("[SRP6][M2-FastSRP] Final M2 calculation - concatenating:");
        byte[] aBytes = HomekitByte.toByteArray(A);
        logger.debug("[SRP6][M2-FastSRP]   1. A bytes (raw): {}", HomekitByte.toHex(aBytes));
        updateDigestInChunks(digest, aBytes);

        byte[] m1Bytes = HomekitByte.toByteArray(M1);
        logger.debug("[SRP6][M2-FastSRP]   2. M1 bytes (raw): {}", HomekitByte.toHex(m1Bytes));
        updateDigestInChunks(digest, m1Bytes);

        logger.debug("[SRP6][M2-FastSRP]   3. K bytes (raw): {}", HomekitByte.toHex(kBytes));
        updateDigestInChunks(digest, kBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger m2 = new BigInteger(1, result);
        logger.debug("[SRP6][M2-FastSRP] Final M2: {}", m2.toString(16));
        return m2;
    }

    /**
     * Calculate k parameter using the specified calculation method.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Where:
     * - N = modulus
     * - PAD(g) = generator g padded to the same byte length as N
     * 
     * The k parameter is used in the SRP6 protocol to prevent certain attacks.
     * RFC 2945/5054 specifies that g should be padded to match N's byte length.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateK(CalculationMethod method, Digest digest, BigInteger N, BigInteger g) {
        switch (method) {
            case BOUNCYCASTLE:
                return calculateKBouncyCastle(digest, N, g);
            case BOUNCYCASTLEPAD:
                return calculateKBouncyCastlePad(digest, N, g);
            case NIMBUS:
                return calculateKNimbus(digest, N, g);
            case FASTSRP:
                return calculateKFastSRP(digest, N, g);
            case RFC:
                return calculateKRFC(digest, N, g);
            default:
                throw new IllegalArgumentException("Unsupported calculation method: " + method);
        }
    }

    /**
     * Calculate k parameter using BouncyCastle implementation.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(N.toByteArray())
     * 2. Pad g to the same byte length as N by right-padding with zeros
     * 3. Calculate H(PAD(g)) = hash(padded_g_bytes)
     * 4. Concatenate: N || PAD(g)
     * 5. Calculate k = hash(concatenated_data)
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateKBouncyCastle(Digest digest, BigInteger N, BigInteger g) {
        logger.debug("[SRP6][K-BouncyCastle] Starting k calculation");
        logger.debug("[SRP6][K-BouncyCastle] Input parameters:");
        logger.debug("[SRP6][K-BouncyCastle]   N: {}", N.toString(16));
        logger.debug("[SRP6][K-BouncyCastle]   g: {}", g.toString(16));

        digest.reset();

        // RFC-compliant: Use N as-is (variable length)
        byte[] nBytes = N.toByteArray();
        logger.debug("[SRP6][K-BouncyCastle] N bytes: {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);

        // RFC-compliant: Pad g to the same byte length as N
        byte[] gBytes = g.toByteArray();
        byte[] paddedGBytes = new byte[nBytes.length];

        // Right-pad g with zeros to match N's length
        System.arraycopy(gBytes, 0, paddedGBytes, paddedGBytes.length - gBytes.length, gBytes.length);

        logger.debug("[SRP6][K-BouncyCastle] g bytes (raw): {}", HomekitByte.toHex(gBytes));
        logger.debug("[SRP6][K-BouncyCastle] g bytes (padded): {}", HomekitByte.toHex(paddedGBytes));
        updateDigestInChunks(digest, paddedGBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger k = new BigInteger(1, result);
        logger.debug("[SRP6][K-BouncyCastle] Final k: {}", k.toString(16));
        return k;
    }

    /**
     * Calculate k parameter using BouncyCastle implementation with padding.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(N.toByteArray())
     * 2. Pad g to the same byte length as N by right-padding with zeros
     * 3. Calculate H(PAD(g)) = hash(padded_g_bytes)
     * 4. Concatenate: N || PAD(g)
     * 5. Calculate k = hash(concatenated_data)
     * 
     * This method is identical to calculateKBouncyCastle but included for
     * consistency with the method naming pattern.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateKBouncyCastlePad(Digest digest, BigInteger N, BigInteger g) {
        logger.debug("[SRP6][K-BouncyCastlePad] Starting k calculation");
        logger.debug("[SRP6][K-BouncyCastlePad] Input parameters:");
        logger.debug("[SRP6][K-BouncyCastlePad]   N: {}", N.toString(16));
        logger.debug("[SRP6][K-BouncyCastlePad]   g: {}", g.toString(16));

        digest.reset();

        // RFC-compliant: Use N as-is (variable length)
        byte[] nBytes = N.toByteArray();
        logger.debug("[SRP6][K-BouncyCastlePad] N bytes: {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);

        // RFC-compliant: Pad g to the same byte length as N
        byte[] gBytes = g.toByteArray();
        byte[] paddedGBytes = new byte[nBytes.length];

        // Right-pad g with zeros to match N's length
        System.arraycopy(gBytes, 0, paddedGBytes, paddedGBytes.length - gBytes.length, gBytes.length);

        logger.debug("[SRP6][K-BouncyCastlePad] g bytes (raw): {}", HomekitByte.toHex(gBytes));
        logger.debug("[SRP6][K-BouncyCastlePad] g bytes (padded): {}", HomekitByte.toHex(paddedGBytes));
        updateDigestInChunks(digest, paddedGBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger k = new BigInteger(1, result);
        logger.debug("[SRP6][K-BouncyCastlePad] Final k: {}", k.toString(16));
        return k;
    }

    /**
     * Calculate k parameter using Nimbus implementation.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(N.toByteArray())
     * 2. Pad g to the same byte length as N by right-padding with zeros
     * 3. Calculate H(PAD(g)) = hash(padded_g_bytes)
     * 4. Concatenate: N || PAD(g)
     * 5. Calculate k = hash(concatenated_data)
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateKNimbus(Digest digest, BigInteger N, BigInteger g) {
        logger.debug("[SRP6][K-Nimbus] Starting k calculation");
        logger.debug("[SRP6][K-Nimbus] Input parameters:");
        logger.debug("[SRP6][K-Nimbus]   N: {}", N.toString(16));
        logger.debug("[SRP6][K-Nimbus]   g: {}", g.toString(16));

        digest.reset();

        // RFC-compliant: Use N as-is (variable length)
        byte[] nBytes = N.toByteArray();
        logger.debug("[SRP6][K-Nimbus] N bytes: {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);

        // RFC-compliant: Pad g to the same byte length as N
        byte[] gBytes = g.toByteArray();
        byte[] paddedGBytes = new byte[nBytes.length];

        // Right-pad g with zeros to match N's length
        System.arraycopy(gBytes, 0, paddedGBytes, paddedGBytes.length - gBytes.length, gBytes.length);

        logger.debug("[SRP6][K-Nimbus] g bytes (raw): {}", HomekitByte.toHex(gBytes));
        logger.debug("[SRP6][K-Nimbus] g bytes (padded): {}", HomekitByte.toHex(paddedGBytes));
        updateDigestInChunks(digest, paddedGBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger k = new BigInteger(1, result);
        logger.debug("[SRP6][K-Nimbus] Final k: {}", k.toString(16));
        return k;
    }

    /**
     * Calculate k parameter using FastSRP implementation.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(N.toByteArray())
     * 2. Pad g to the same byte length as N by right-padding with zeros
     * 3. Calculate H(PAD(g)) = hash(padded_g_bytes)
     * 4. Concatenate: N || PAD(g)
     * 5. Calculate k = hash(concatenated_data)
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateKFastSRP(Digest digest, BigInteger N, BigInteger g) {
        logger.debug("[SRP6][K-FastSRP] Starting k calculation");
        logger.debug("[SRP6][K-FastSRP] Input parameters:");
        logger.debug("[SRP6][K-FastSRP]   N: {}", N.toString(16));
        logger.debug("[SRP6][K-FastSRP]   g: {}", g.toString(16));

        digest.reset();

        // RFC-compliant: Use N as-is (variable length)
        byte[] nBytes = N.toByteArray();
        logger.debug("[SRP6][K-FastSRP] N bytes: {}", HomekitByte.toHex(nBytes));
        updateDigestInChunks(digest, nBytes);

        // RFC-compliant: Pad g to the same byte length as N
        byte[] gBytes = g.toByteArray();
        byte[] paddedGBytes = new byte[nBytes.length];

        // Right-pad g with zeros to match N's length
        System.arraycopy(gBytes, 0, paddedGBytes, paddedGBytes.length - gBytes.length, gBytes.length);

        logger.debug("[SRP6][K-FastSRP] g bytes (raw): {}", HomekitByte.toHex(gBytes));
        logger.debug("[SRP6][K-FastSRP] g bytes (padded): {}", HomekitByte.toHex(paddedGBytes));
        updateDigestInChunks(digest, paddedGBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        BigInteger k = new BigInteger(1, result);
        logger.debug("[SRP6][K-FastSRP] Final k: {}", k.toString(16));
        return k;
    }

    /**
     * Calculate k parameter using RFC 2945/5054 compliant implementation.
     * 
     * Formula: k = H(N || PAD(g))
     * 
     * Where:
     * - N = modulus
     * - PAD(g) = generator g padded to the same byte length as N
     * 
     * The k parameter is used in the SRP6 protocol to prevent certain attacks.
     * RFC 2945/5054 specifies that g should be padded to match N's byte length.
     * 
     * Algorithm:
     * 1. Calculate H(N) = hash(N.toByteArray())
     * 2. Pad g to the same byte length as N by right-padding with zeros
     * 3. Calculate H(PAD(g)) = hash(padded_g_bytes)
     * 4. Concatenate: N || PAD(g)
     * 5. Calculate k = hash(concatenated_data)
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @return The k parameter
     */
    public static BigInteger calculateKRFC(Digest digest, BigInteger N, BigInteger g) {
        digest.reset();

        // RFC-compliant: Use N as-is (variable length)
        byte[] nBytes = N.toByteArray();
        updateDigestInChunks(digest, nBytes);

        // RFC-compliant: Pad g to the same byte length as N
        byte[] gBytes = g.toByteArray();
        byte[] paddedGBytes = new byte[nBytes.length];

        // Right-pad g with zeros to match N's length
        System.arraycopy(gBytes, 0, paddedGBytes, paddedGBytes.length - gBytes.length, gBytes.length);

        updateDigestInChunks(digest, paddedGBytes);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
    }

    /**
     * Calculate the scrambling parameter u using the specified calculation method.
     * 
     * Formula: u = H(A || B)
     * 
     * Where:
     * - A = client public key
     * - B = server public key
     * 
     * The scrambling parameter u is used in the SRP6 protocol to prevent certain attacks.
     * It ensures that the client and server use different exponents in the key exchange.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @return The scrambling parameter u
     */
    public static BigInteger calculateU(CalculationMethod method, Digest digest, BigInteger N, BigInteger A,
            BigInteger B) {
        switch (method) {
            case RFC:
            case BOUNCYCASTLE:
            case BOUNCYCASTLEPAD:
                return calculateUBouncyCastle(digest, N, A, B);
            case NIMBUS:
                return calculateUNimbus(digest, N, A, B);
            case FASTSRP:
                return calculateUFastSRP(digest, N, A, B);
            default:
                throw new IllegalArgumentException("Unsupported calculation method: " + method);
        }
    }

    /**
     * Calculate the scrambling parameter u using BouncyCastle-style implementation.
     * 
     * Formula: u = H(PAD(A) || PAD(B))
     * 
     * Algorithm:
     * 1. Pad A to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 2. Pad B to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 3. Concatenate: PAD(A) || PAD(B)
     * 4. Calculate u = hash(concatenated_data)
     * 
     * This method uses padding for both A and B to ensure consistent byte lengths.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @return The scrambling parameter u
     */
    public static BigInteger calculateUBouncyCastle(Digest digest, BigInteger N, BigInteger A, BigInteger B) {
        logger.debug("[SRP6][U-BouncyCastle] Starting u calculation");
        logger.debug("[SRP6][U-BouncyCastle] Input parameters:");
        logger.debug("[SRP6][U-BouncyCastle]   N: {}", N.toString(16));
        logger.debug("[SRP6][U-BouncyCastle]   A: {}", A.toString(16));
        logger.debug("[SRP6][U-BouncyCastle]   B: {}", B.toString(16));

        digest.reset();

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][U-BouncyCastle] padLength: {}", padLength);

        // Convert BigIntegers to byte arrays and hash them
        byte[] aBytes = HomekitByte.Pad(A, N);
        byte[] bBytes = HomekitByte.Pad(B, N);

        logger.debug("[SRP6][U-BouncyCastle] A bytes (padded): {}", HomekitByte.toHex(aBytes));
        logger.debug("[SRP6][U-BouncyCastle] B bytes (padded): {}", HomekitByte.toHex(bBytes));

        // Update digest with A || B using chunked updates to avoid buffer overflow
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);

        // Get the hash result
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);

        BigInteger u = new BigInteger(1, result);
        logger.debug("[SRP6][U-BouncyCastle] Final u: {}", u.toString(16));
        return u;
    }

    /**
     * Calculate the scrambling parameter u using Nimbus-style implementation.
     * 
     * Formula: u = H(PAD(A) || PAD(B))
     * 
     * Algorithm:
     * 1. Pad A to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 2. Pad B to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 3. Concatenate: PAD(A) || PAD(B)
     * 4. Calculate u = hash(concatenated_data)
     * 
     * This method uses padding for both A and B to ensure consistent byte lengths.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @return The scrambling parameter u
     */
    public static BigInteger calculateUNimbus(Digest digest, BigInteger N, BigInteger A, BigInteger B) {
        logger.debug("[SRP6][U-Nimbus] Starting u calculation");
        logger.debug("[SRP6][U-Nimbus] Input parameters:");
        logger.debug("[SRP6][U-Nimbus]   N: {}", N.toString(16));
        logger.debug("[SRP6][U-Nimbus]   A: {}", A.toString(16));
        logger.debug("[SRP6][U-Nimbus]   B: {}", B.toString(16));

        digest.reset();

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][U-Nimbus] padLength: {}", padLength);

        // Convert BigIntegers to byte arrays and hash them
        byte[] aBytes = HomekitByte.Pad(A, N);
        byte[] bBytes = HomekitByte.Pad(B, N);

        logger.debug("[SRP6][U-Nimbus] A bytes (padded): {}", HomekitByte.toHex(aBytes));
        logger.debug("[SRP6][U-Nimbus] B bytes (padded): {}", HomekitByte.toHex(bBytes));

        // Update digest with A || B using chunked updates to avoid buffer overflow
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);

        // Get the hash result
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);

        BigInteger u = new BigInteger(1, result);
        logger.debug("[SRP6][U-Nimbus] Final u: {}", u.toString(16));
        return u;
    }

    /**
     * Calculate the scrambling parameter u using FastSRP-compliant implementation.
     * 
     * Formula: u = H(PAD(A) || PAD(B))
     * 
     * Algorithm:
     * 1. Pad A to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 2. Pad B to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 3. Concatenate: PAD(A) || PAD(B)
     * 4. Calculate u = hash(concatenated_data)
     * 
     * This method matches the fast-srp-hap JavaScript library's getu() method.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param A The client public key
     * @param B The server public key
     * @return The scrambling parameter u
     */
    public static BigInteger calculateUFastSRP(Digest digest, BigInteger N, BigInteger A, BigInteger B) {
        logger.debug("[SRP6][U-FastSRP] Starting u calculation");
        logger.debug("[SRP6][U-FastSRP] Input parameters:");
        logger.debug("[SRP6][U-FastSRP]   N: {}", N.toString(16));
        logger.debug("[SRP6][U-FastSRP]   A: {}", A.toString(16));
        logger.debug("[SRP6][U-FastSRP]   B: {}", B.toString(16));

        digest.reset();

        int padLength = (N.bitLength() + 7) / 8;
        logger.debug("[SRP6][U-FastSRP] padLength: {}", padLength);

        // Convert BigIntegers to byte arrays using padTo() approach
        // This matches the JavaScript: padTo(n, len) where len is the byte length of N
        byte[] aBytes = HomekitByte.Pad(A, N);
        byte[] bBytes = HomekitByte.Pad(B, N);

        logger.debug("[SRP6][U-FastSRP] A bytes (padded): {}", HomekitByte.toHex(aBytes));
        logger.debug("[SRP6][U-FastSRP] B bytes (padded): {}", HomekitByte.toHex(bBytes));

        // Update digest with A || B using chunked updates to avoid buffer overflow
        updateDigestInChunks(digest, aBytes);
        updateDigestInChunks(digest, bBytes);

        // Get the hash result
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);

        BigInteger u = new BigInteger(1, result);
        logger.debug("[SRP6][U-FastSRP] Final u: {}", u.toString(16));
        return u;
    }

    /**
     * Calculate the session key K using the specified calculation method.
     * 
     * Formula: K = H(S)
     * 
     * Where:
     * - S = premaster secret
     * 
     * The session key K is derived from the premaster secret S and is used
     * for subsequent encrypted communication between client and server.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use for hashing
     * @param S The premaster secret S
     * @param N The modulus N
     * @return The session key K
     * @throws CryptoException if parameters are invalid
     */
    public static BigInteger calculateSessionKey(CalculationMethod method, Digest digest, BigInteger S, BigInteger N)
            throws CryptoException {
        if (method == null) {
            throw new CryptoException("Calculation method cannot be null");
        }
        if (S == null) {
            throw new CryptoException("Premaster secret S cannot be null");
        }
        if (digest == null) {
            throw new CryptoException("Digest cannot be null");
        }
        if (N == null) {
            throw new CryptoException("Modulus N cannot be null");
        }

        switch (method) {
            case BOUNCYCASTLE:
            case BOUNCYCASTLEPAD:
                return calculateSessionKeyBouncyCastle(digest, S, N);
            case NIMBUS:
                return calculateSessionKeyNimbus(digest, S, N);
            case FASTSRP:
                return calculateSessionKeyFastSRP(digest, S, N);
            default:
                throw new CryptoException("Unsupported calculation method: " + method);
        }
    }

    /**
     * Calculate the session key K using BouncyCastle method (single hash with padding).
     * 
     * Formula: K = H(PAD(S))
     * 
     * Algorithm:
     * 1. Pad S to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 2. Calculate K = hash(pad(S, padLength))
     * 
     * @param digest The digest to use for hashing
     * @param S The premaster secret S
     * @param N The modulus N
     * @return The session key K
     */
    private static BigInteger calculateSessionKeyBouncyCastle(Digest digest, BigInteger S, BigInteger N) {
        byte[] _S = HomekitByte.Pad(S, N);
        digest.update(_S, 0, _S.length);

        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return new BigInteger(1, output);
    }

    /**
     * Calculate the session key K using Nimbus method (single hash with raw bytes).
     * 
     * Formula: K = H(PAD(S))
     * 
     * Algorithm:
     * 1. Pad S to the byte length of N: padLength = (N.bitLength() + 7) / 8
     * 2. Calculate K = hash(pad(S, padLength))
     * 
     * @param digest The digest to use for hashing
     * @param S The premaster secret S
     * @param N The modulus N
     * @return The session key K
     */
    private static BigInteger calculateSessionKeyNimbus(Digest digest, BigInteger S, BigInteger N) {
        int padLength = (N.bitLength() + 7) / 8;
        byte[] _S = HomekitByte.Pad(S, N);
        digest.update(_S, 0, _S.length);

        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return new BigInteger(1, output);
    }

    /**
     * Calculate the session key K using FastSRP method (single hash with raw bytes).
     * 
     * Formula: K = H(S)
     * 
     * Algorithm:
     * 1. Calculate K = hash(S.toByteArray())
     * 
     * This method uses the full byte representation of S without padding.
     * 
     * @param digest The digest to use for hashing
     * @param S The premaster secret S
     * @param N The modulus N (unused in this method)
     * @return The session key K
     */
    private static BigInteger calculateSessionKeyFastSRP(Digest digest, BigInteger S, BigInteger N) {
        byte[] sBytes = HomekitByte.toByteArray(S); // Use raw bytes, no padding
        digest.update(sBytes, 0, sBytes.length);
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return new BigInteger(1, output);
    }

    /**
     * Calculate the premaster secret S using the specified calculation method.
     * 
     * Formula: S = (B - k * g^x)^(a + u * x) mod N
     * 
     * Where:
     * - B = server public key
     * - k = multiplier parameter
     * - g = generator
     * - x = client secret
     * - a = client private key
     * - u = scrambling parameter
     * 
     * The premaster secret S is the shared secret that both client and server
     * derive independently. It's used to generate the session key K.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @param A The client public key
     * @param B The server public key
     * @param a The client private key
     * @param x The client secret
     * @param u The scrambling parameter
     * @param k The multiplier parameter
     * @return The premaster secret S
     */
    public static BigInteger calculateClientS(CalculationMethod method, Digest digest, BigInteger N, BigInteger g,
            BigInteger A, BigInteger B, BigInteger a, BigInteger x, BigInteger u, BigInteger k) {
        switch (method) {
            case RFC:
            case BOUNCYCASTLE:
                return calculateClientSBouncyCastle(digest, N, g, A, B, a, x, u, k);
            case BOUNCYCASTLEPAD:
                return calculateClientSBouncyCastlePad(digest, N, g, A, B, a, x, u, k);
            case NIMBUS:
                return calculateClientSNimbus(digest, N, g, A, B, a, x, u, k);
            case FASTSRP:
                return calculateClientSFastSRP(digest, N, g, A, B, a, x, u, k);
            default:
                throw new IllegalArgumentException("Unsupported calculation method: " + method);
        }
    }

    public static BigInteger calculateServerS(CalculationMethod method, Digest digest, BigInteger N, BigInteger A,
            BigInteger b, BigInteger u, BigInteger v) {

        // Calculate the premaster secret S using the standard SRP6 formula
        // For server: S = (A * v^u)^b mod N
        BigInteger base = A.multiply(v.modPow(u, N)).mod(N);
        return base.modPow(b, N);
    }

    /**
     * Calculate the premaster secret S using BouncyCastle method.
     * 
     * Formula: S = (B - k * g^x)^(a + u * x) mod N
     * 
     * Algorithm:
     * 1. Calculate g^x = g^x mod N
     * 2. Calculate k * g^x
     * 3. Calculate B - k * g^x
     * 4. Calculate u * x
     * 5. Calculate a + u * x
     * 6. Calculate S = (B - k * g^x)^(a + u * x) mod N
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @param A The client public key
     * @param B The server public key
     * @param a The client private key
     * @param x The client secret
     * @param u The scrambling parameter
     * @param k The multiplier parameter
     * @return The premaster secret S
     */
    private static BigInteger calculateClientSBouncyCastle(Digest digest, BigInteger N, BigInteger g, BigInteger A,
            BigInteger B, BigInteger a, BigInteger x, BigInteger u, BigInteger k) {
        logger.debug("[SRP6][S-BouncyCastle] Starting S calculation");
        BigInteger g_x = g.modPow(x, N);
        logger.debug("[SRP6][S-BouncyCastle] g^x: {}", g_x.toString(16));
        BigInteger k_g_x = k.multiply(g_x);
        logger.debug("[SRP6][S-BouncyCastle] k * g^x: {}", k_g_x.toString(16));
        BigInteger B_minus_k_g_x = B.subtract(k_g_x);
        logger.debug("[SRP6][S-BouncyCastle] B - k * g^x: {}", B_minus_k_g_x.toString(16));
        BigInteger u_x = u.multiply(x);
        logger.debug("[SRP6][S-BouncyCastle] u * x: {}", u_x.toString(16));
        BigInteger a_plus_u_x = a.add(u_x);
        logger.debug("[SRP6][S-BouncyCastle] a + u * x: {}", a_plus_u_x.toString(16));
        BigInteger S = B_minus_k_g_x.modPow(a_plus_u_x, N);
        logger.debug("[SRP6][S-BouncyCastle] S: {}", S.toString(16));
        return S;
    }

    /**
     * Calculate the premaster secret S using BouncyCastle method with padding.
     * 
     * Formula: S = (B - k * g^x)^(a + u * x) mod N
     * 
     * Algorithm:
     * 1. Calculate g^x = g^x mod N
     * 2. Calculate k * g^x
     * 3. Calculate B - k * g^x
     * 4. Calculate u * x
     * 5. Calculate a + u * x
     * 6. Calculate S = (B - k * g^x)^(a + u * x) mod N
     * 
     * This method is identical to calculateSBouncyCastle but included for
     * consistency with the method naming pattern.
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @param A The client public key
     * @param B The server public key
     * @param a The client private key
     * @param x The client secret
     * @param u The scrambling parameter
     * @param k The multiplier parameter
     * @return The premaster secret S
     */
    private static BigInteger calculateClientSBouncyCastlePad(Digest digest, BigInteger N, BigInteger g, BigInteger A,
            BigInteger B, BigInteger a, BigInteger x, BigInteger u, BigInteger k) {
        logger.debug("[SRP6][S-BouncyCastlePad] Starting S calculation");
        BigInteger g_x = g.modPow(x, N);
        logger.debug("[SRP6][S-BouncyCastlePad] g^x: {}", g_x.toString(16));
        BigInteger k_g_x = k.multiply(g_x);
        logger.debug("[SRP6][S-BouncyCastlePad] k * g^x: {}", k_g_x.toString(16));
        BigInteger B_minus_k_g_x = B.subtract(k_g_x);
        logger.debug("[SRP6][S-BouncyCastlePad] B - k * g^x: {}", B_minus_k_g_x.toString(16));
        BigInteger u_x = u.multiply(x);
        logger.debug("[SRP6][S-BouncyCastlePad] u * x: {}", u_x.toString(16));
        BigInteger a_plus_u_x = a.add(u_x);
        logger.debug("[SRP6][S-BouncyCastlePad] a + u * x: {}", a_plus_u_x.toString(16));
        BigInteger S = B_minus_k_g_x.modPow(a_plus_u_x, N);
        logger.debug("[SRP6][S-BouncyCastlePad] S: {}", S.toString(16));
        return S;
    }

    /**
     * Calculate the premaster secret S using Nimbus method.
     * 
     * Formula: S = (B - k * g^x)^(a + u * x) mod N
     * 
     * Algorithm:
     * 1. Calculate g^x = g^x mod N
     * 2. Calculate k * g^x
     * 3. Calculate B - k * g^x
     * 4. Calculate u * x
     * 5. Calculate a + u * x
     * 6. Calculate S = (B - k * g^x)^(a + u * x) mod N
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @param A The client public key
     * @param B The server public key
     * @param a The client private key
     * @param x The client secret
     * @param u The scrambling parameter
     * @param k The multiplier parameter
     * @return The premaster secret S
     */
    private static BigInteger calculateClientSNimbus(Digest digest, BigInteger N, BigInteger g, BigInteger A,
            BigInteger B, BigInteger a, BigInteger x, BigInteger u, BigInteger k) {
        logger.debug("[SRP6][S-Nimbus] Starting S calculation");
        BigInteger g_x = g.modPow(x, N);
        logger.debug("[SRP6][S-Nimbus] g^x: {}", g_x.toString(16));
        BigInteger k_g_x = k.multiply(g_x);
        logger.debug("[SRP6][S-Nimbus] k * g^x: {}", k_g_x.toString(16));
        BigInteger B_minus_k_g_x = B.subtract(k_g_x);
        logger.debug("[SRP6][S-Nimbus] B - k * g^x: {}", B_minus_k_g_x.toString(16));
        BigInteger u_x = u.multiply(x);
        logger.debug("[SRP6][S-Nimbus] u * x: {}", u_x.toString(16));
        BigInteger a_plus_u_x = a.add(u_x);
        logger.debug("[SRP6][S-Nimbus] a + u * x: {}", a_plus_u_x.toString(16));
        BigInteger S = B_minus_k_g_x.modPow(a_plus_u_x, N);
        logger.debug("[SRP6][S-Nimbus] S: {}", S.toString(16));
        return S;
    }

    /**
     * Calculate the premaster secret S using FastSRP method.
     * 
     * Formula: S = (B - k * g^x)^(a + u * x) mod N
     * 
     * Algorithm:
     * 1. Calculate g^x = g^x mod N
     * 2. Calculate k * g^x
     * 3. Calculate B - k * g^x
     * 4. Calculate u * x
     * 5. Calculate a + u * x
     * 6. Calculate S = (B - k * g^x)^(a + u * x) mod N
     * 
     * @param digest The digest to use
     * @param N The modulus
     * @param g The generator
     * @param A The client public key
     * @param B The server public key
     * @param a The client private key
     * @param x The client secret
     * @param u The scrambling parameter
     * @param k The multiplier parameter
     * @return The premaster secret S
     */
    private static BigInteger calculateClientSFastSRP(Digest digest, BigInteger N, BigInteger g, BigInteger A,
            BigInteger B, BigInteger a, BigInteger x, BigInteger u, BigInteger k) {
        logger.debug("[SRP6][S-FastSRP] Starting S calculation");
        BigInteger g_x = g.modPow(x, N);
        logger.debug("[SRP6][S-FastSRP] g^x: {}", g_x.toString(16));
        BigInteger k_g_x = k.multiply(g_x);
        logger.debug("[SRP6][S-FastSRP] k * g^x: {}", k_g_x.toString(16));
        BigInteger B_minus_k_g_x = B.subtract(k_g_x);
        logger.debug("[SRP6][S-FastSRP] B - k * g^x: {}", B_minus_k_g_x.toString(16));
        BigInteger u_x = u.multiply(x);
        logger.debug("[SRP6][S-FastSRP] u * x: {}", u_x.toString(16));
        BigInteger a_plus_u_x = a.add(u_x);
        logger.debug("[SRP6][S-FastSRP] a + u * x: {}", a_plus_u_x.toString(16));
        BigInteger S = B_minus_k_g_x.modPow(a_plus_u_x, N);
        logger.debug("[SRP6][S-FastSRP] S: {}", S.toString(16));
        return S;
    }

    /**
     * Calculate x = H(salt || H(identity || ':' || password)) using the specified calculation method.
     * 
     * Formula: x = H(salt || H(identity || ':' || password))
     * 
     * Where:
     * - salt = random salt value
     * - identity = user identity (e.g., username)
     * - password = user password
     * - ':' = literal colon character
     * 
     * The x value is the client secret that is derived from the password.
     * It's used to compute the verifier v = g^x mod N.
     * 
     * @param method The calculation method to use
     * @param digest The digest to use
     * @param N The modulus N
     * @param salt The salt
     * @param identity The identity
     * @param password The password
     * @return The calculated x value
     */
    public static BigInteger calculateX(CalculationMethod method, Digest digest, BigInteger N, byte[] salt,
            byte[] identity, byte[] password) {
        switch (method) {
            case RFC:
            case BOUNCYCASTLE:
            case BOUNCYCASTLEPAD:
                return calculateXBouncyCastle(digest, N, salt, identity, password);
            case NIMBUS:
                return calculateXNimbus(digest, N, salt, identity, password);
            case FASTSRP:
                return calculateXFastSRP(digest, N, salt, identity, password);
            default:
                throw new IllegalArgumentException("Unknown calculation method: " + method);
        }
    }

    /**
     * Calculate x using BouncyCastle's SRP6Util.calculateX method.
     * 
     * Formula: x = H(salt || H(identity || ':' || password))
     * 
     * This method delegates to BouncyCastle's implementation.
     * 
     * @param digest The digest to use
     * @param N The modulus N
     * @param salt The salt
     * @param identity The identity
     * @param password The password
     * @return The calculated x value
     */
    private static BigInteger calculateXBouncyCastle(Digest digest, BigInteger N, byte[] salt, byte[] identity,
            byte[] password) {
        return SRP6Util.calculateX(digest, N, salt, identity, password);
    }

    /**
     * Calculate x using Nimbus implementation (same as BouncyCastle).
     * 
     * Formula: x = H(salt || H(identity || ':' || password))
     * 
     * This method delegates to BouncyCastle's implementation.
     * 
     * @param digest The digest to use
     * @param N The modulus N
     * @param salt The salt
     * @param identity The identity
     * @param password The password
     * @return The calculated x value
     */
    private static BigInteger calculateXNimbus(Digest digest, BigInteger N, byte[] salt, byte[] identity,
            byte[] password) {
        return SRP6Util.calculateX(digest, N, salt, identity, password);
    }

    /**
     * Calculate x using FastSRP implementation (exact copy from original SRP6Client).
     * 
     * Formula: x = H(salt || H(identity || ':' || password))
     * 
     * Algorithm:
     * 1. Calculate H(identity || ':' || password)
     * - Concatenate: identity || ':' || password
     * - Calculate hash of concatenated data
     * 2. Calculate x = H(salt || H(identity || ':' || password))
     * - Concatenate: salt || H(identity || ':' || password)
     * - Calculate hash of concatenated data
     * 
     * This method implements the exact same algorithm as the original SRP6Client.
     * 
     * @param digest The digest to use
     * @param N The modulus N
     * @param salt The salt
     * @param identity The identity
     * @param password The password
     * @return The calculated x value
     */
    private static BigInteger calculateXFastSRP(Digest digest, BigInteger N, byte[] salt, byte[] identity,
            byte[] password) {
        // Step 1: Calculate H(identity || ':' || password)
        digest.reset();
        byte[] identityColonPassword = new byte[identity.length + 1 + password.length];
        System.arraycopy(identity, 0, identityColonPassword, 0, identity.length);
        identityColonPassword[identity.length] = (byte) ':'; // ASCII colon
        System.arraycopy(password, 0, identityColonPassword, identity.length + 1, password.length);

        updateDigestInChunks(digest, identityColonPassword);
        byte[] hashIP = new byte[digest.getDigestSize()];
        digest.doFinal(hashIP, 0);

        // Step 2: Calculate x = H(salt || H(identity || ':' || password))
        digest.reset();
        byte[] saltHashIP = new byte[salt.length + hashIP.length];
        System.arraycopy(salt, 0, saltHashIP, 0, salt.length);
        System.arraycopy(hashIP, 0, saltHashIP, salt.length, hashIP.length);

        updateDigestInChunks(digest, saltHashIP);
        byte[] xBytes = new byte[digest.getDigestSize()];
        digest.doFinal(xBytes, 0);

        return new BigInteger(1, xBytes);
    }

    /**
     * Update digest with large byte arrays in chunks to avoid buffer overflow.
     * 
     * This utility method processes large byte arrays in smaller chunks to prevent
     * buffer overflow issues that can occur with very large data (e.g., 3072-bit keys).
     * 
     * @param digest The digest to update
     * @param data The data to hash
     */
    public static void updateDigestInChunks(Digest digest, byte[] data) {
        // Process in chunks of 64 bytes (typical digest block size)
        int chunkSize = 64;
        int offset = 0;

        while (offset < data.length) {
            int length = Math.min(chunkSize, data.length - offset);
            digest.update(data, offset, length);
            offset += length;
        }
    }
}
