package org.openhab.io.homekit.protocol.crypto;

import java.math.BigInteger;

import org.bouncycastle.crypto.Digest;
import org.openhab.io.homekit.util.HomekitByte;

public class HomekitSRP6Util {

    /**
     * Calculate M1 using RFC-compliant implementation that can handle large inputs
     * without buffer limitations.
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
    public static BigInteger calculateM1(Digest digest, BigInteger N, BigInteger A, BigInteger B, BigInteger S,
            BigInteger g, byte[] identity, byte[] salt) {

        // M1 = H(H(N) xor H(g) | H(I) | s | A | B | H(S))

        int padLength = (N.bitLength() + 7) / 8;

        // Calculate H(N) - RFC-compliant: use N as-is
        digest.reset();
        // byte[] nBytes = N.toByteArray(); // ✅ RFC-compliant: preserve full byte representation
        byte[] nBytes = getPadded(N, padLength);
        updateDigestInChunks(digest, nBytes);
        byte[] hN = new byte[digest.getDigestSize()];
        digest.doFinal(hN, 0);

        // Calculate H(g) - RFC-compliant: use g as-is
        digest.reset();
        // byte[] gBytes = g.toByteArray(); // ✅ RFC-compliant: preserve full byte representation
        byte[] gBytes = getPadded(g, padLength);
        updateDigestInChunks(digest, gBytes);
        byte[] hg = new byte[digest.getDigestSize()];
        digest.doFinal(hg, 0);

        // H(N) xor H(g)
        byte[] hNxorHg = new byte[hN.length];
        for (int i = 0; i < hN.length; i++) {
            hNxorHg[i] = (byte) (hN[i] ^ hg[i]);
        }

        // Calculate H(I)
        digest.reset();
        updateDigestInChunks(digest, identity);
        byte[] hu = new byte[digest.getDigestSize()];
        digest.doFinal(hu, 0);

        // Calculate H(S)
        digest.reset();
        // byte[] sBytes = S.toByteArray(); // ✅ RFC-compliant: preserve full byte representation
        byte[] sBytes = getPadded(S, padLength);
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);

        // Calculate final M1: H(H(N) xor H(g) | H(I) | s | A | B | H(S))
        digest.reset();
        updateDigestInChunks(digest, hNxorHg);
        updateDigestInChunks(digest, hu);
        updateDigestInChunks(digest, salt);
        // updateDigestInChunks(digest, A.toByteArray()); // ✅ RFC-compliant: preserve full byte representation
        byte[] aBytes = getPadded(A, padLength);
        updateDigestInChunks(digest, aBytes);
        // updateDigestInChunks(digest, B.toByteArray()); // ✅ RFC-compliant: preserve full byte representation
        byte[] bBytes = getPadded(B, padLength);
        updateDigestInChunks(digest, bBytes);
        updateDigestInChunks(digest, hS);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
    }

    /**
     * Calculate k parameter using RFC 2945/5054 compliant implementation.
     * 
     * RFC specification: k = H(N | PAD(g))
     * Where PAD(g) pads g to the same byte length as N
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
     * Computes the server evidence message 'M2' using HAP-specific formula.
     *
     * @param digest The message digest instance
     * @param N The modulus
     * @param A The client public key
     * @param M1 The client evidence message
     * @param S The session key
     * @return The server evidence message 'M2'
     * @throws IllegalArgumentException if any parameter is null
     */
    public static BigInteger calculateM2(Digest digest, BigInteger N, BigInteger A, BigInteger M1, BigInteger S) {

        int padLength = (N.bitLength() + 7) / 8;

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
        digest.reset();
        // digest.update(S.toByteArray(), 0, S.toByteArray().length);
        byte[] sBytes = getPadded(S, padLength);
        updateDigestInChunks(digest, sBytes);
        byte[] hS = new byte[digest.getDigestSize()];
        digest.doFinal(hS, 0);

        digest.reset();
        // digest.update(A.toByteArray(), 0, A.toByteArray().length);
        byte[] aBytes = getPadded(A, padLength);
        updateDigestInChunks(digest, aBytes);
        // digest.update(M1.toByteArray(), 0, M1.toByteArray().length);
        byte[] m1Bytes = getPadded(M1, padLength);
        updateDigestInChunks(digest, m1Bytes);
        digest.update(hS, 0, hS.length);

        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
    }

    /**
     * Calculate the scrambling parameter u using our own implementation
     * that can handle large 3072-bit public keys without buffer limitations.
     * 
     * @param digest The digest to use
     * @param A The client public key
     * @param B The server public key
     * @return The scrambling parameter u
     */
    public static BigInteger calculateU(Digest digest, BigInteger N, BigInteger A, BigInteger B) {
        digest.reset();

        int padLength = (N.bitLength() + 7) / 8;

        // Convert BigIntegers to byte arrays and hash them
        // byte[] aBytes = A.toByteArray();
        // byte[] bBytes = B.toByteArray();
        byte[] aBytes = getPadded(A, padLength);
        byte[] bBytes = getPadded(B, padLength);

        // Update digest with A || B
        digest.update(aBytes, 0, aBytes.length);
        digest.update(bBytes, 0, bBytes.length);

        // Get the hash result
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);

        return new BigInteger(1, result);
    }

    public static byte[] getPadded(BigInteger n, int length) {
        byte[] bs = HomekitByte.toByteArray(n);
        if (bs.length < length) {
            byte[] tmp = new byte[length];
            System.arraycopy(bs, 0, tmp, length - bs.length, bs.length);
            bs = tmp;
        }
        return bs;
    }

    /**
     * Update digest with large byte arrays in chunks to avoid buffer overflow.
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
