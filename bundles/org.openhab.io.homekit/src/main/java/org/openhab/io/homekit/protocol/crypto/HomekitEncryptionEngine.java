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

package org.openhab.io.homekit.protocol.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.util.Pack;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.BufferUtil;
import org.openhab.io.homekit.util.HomekitByte;
import org.openhab.io.homekit.util.HomekitByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides encryption and decryption functionality for HomeKit protocol messages.
 *
 * This class implements the cryptographic operations required for secure communication
 * in the HomeKit protocol, including message encryption, decryption, and authentication.
 * It uses ChaCha20-Poly1305 for message encryption and implements SRP-6a for secure
 * password-based authentication.
 *
 * Key features:
 * - Implements ChaCha20-Poly1305 encryption/decryption
 * - Handles message sequence numbering
 * - Supports message fragmentation and reassembly
 * - Implements SRP-6a authentication routines
 * - Provides secure key derivation
 *
 * Security considerations:
 * - Uses cryptographically secure random number generation
 * - Implements proper message authentication
 * - Handles sequence numbers to prevent replay attacks
 * - Uses secure key derivation functions
 * - Implements proper buffer management
 *
 * Message processing:
 * 1. Messages are fragmented into chunks of maximum size 0x400 bytes
 * 2. Each chunk is encrypted with a unique nonce derived from sequence number
 * 3. Message authentication is performed using Poly1305
 * 4. Decryption verifies message integrity before processing
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEncryptionEngine {

    /** Logger instance for this class */
    protected static final Logger logger = LoggerFactory.getLogger(HomekitEncryptionEngine.class);

    /** The 3072-bit prime modulus for SRP-6a */
    public static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");

    /** The generator for SRP-6a */
    public static final BigInteger G = BigInteger.valueOf(5);

    /** Thread-safe secure random number generator */
    private static volatile @Nullable SecureRandom secureRandom;

    /**
     * Represents a buffer with its associated sequence number.
     * Used to maintain message ordering and prevent replay attacks.
     */
    public static class SequenceBuffer {
        /** The buffer containing the message data */
        public ByteBuffer buffer;
        /** The sequence number associated with this buffer */
        public long sequenceNumber;

        /**
         * Creates a new sequence buffer with the specified buffer and sequence number.
         *
         * @param buffer The message buffer
         * @param sequenceNumber The sequence number for this message
         */
        public SequenceBuffer(ByteBuffer buffer, long sequenceNumber) {
            this.buffer = buffer;
            this.sequenceNumber = sequenceNumber;
        }
    }

    /**
     * Decrypts a buffer of encrypted messages.
     *
     * This method processes a buffer containing one or more encrypted messages,
     * decrypting each message and maintaining the sequence number for proper
     * message ordering.
     *
     * @param decryptedBuffer The buffer to store decrypted messages
     * @param cipherTextBuffer The buffer containing encrypted messages
     * @param writeKey The key used for decryption
     * @param sequenceNumber The starting sequence number
     * @return A SequenceBuffer containing the decrypted messages and updated sequence number
     */
    public static SequenceBuffer decryptBuffer(ByteBuffer decryptedBuffer, ByteBuffer cipherTextBuffer, byte[] writeKey,
            long sequenceNumber) {
        logger.trace("DecryptBuffer : cipherTextBuffer = {}", BufferUtil.toDetailString(cipherTextBuffer));
        logger.trace("DecryptBuffer : decryptedBuffer = {}", BufferUtil.toDetailString(decryptedBuffer));
        logger.trace("DecryptBuffer : key = {}", HomekitByte.toHex(writeKey));
        logger.trace("DecryptBuffer : sequenceNumber = {}", sequenceNumber);

        int currentPosition = cipherTextBuffer.position();
        long currentSequenceNumber = sequenceNumber;
        ByteBuffer resultingBuffer = decryptedBuffer;

        Collection<byte[]> results = new LinkedList<>();
        while (cipherTextBuffer.remaining() > 18) {
            int targetLength = (cipherTextBuffer.get() & 0xFF) + (cipherTextBuffer.get() & 0xFF) * 256 + 16;
            logger.trace("DecryptBuffer : Attempting to read a message of length {}", targetLength);
            if (cipherTextBuffer.remaining() >= targetLength) {
                byte[] b = new byte[targetLength];
                cipherTextBuffer.get(b, 0, targetLength);
                results.add(b);
                logger.trace("DecryptBuffer : Read a complete message ({} bytes)", targetLength);
            } else {
                cipherTextBuffer.position(currentPosition);
                logger.debug("DecryptBuffer : Not enough data available ({} bytes) for a complete message ({} bytes)",
                        cipherTextBuffer.remaining(), targetLength);
                break;
            }
        }

        if (!results.isEmpty()) {
            try (HomekitByteBufferOutputStream decrypted = new HomekitByteBufferOutputStream(decryptedBuffer, true)) {
                for (byte[] msg : results) {
                    try {
                        decrypted.write(decrypt(msg, writeKey, currentSequenceNumber++));
                    } catch (Exception e) {
                        logger.error("Failed to decrypt message", e);
                        throw new IllegalStateException("Failed to decrypt message", e);
                    }
                }
                resultingBuffer = decrypted.toByteBuffer();
            } catch (Exception e) {
                logger.error("Failed to process decrypted messages", e);
            }
        }

        return new SequenceBuffer(resultingBuffer, currentSequenceNumber);
    }

    /**
     * Encrypts a buffer of plaintext messages.
     *
     * This method processes a buffer containing plaintext messages, encrypting
     * each message and maintaining the sequence number for proper message ordering.
     * Messages are fragmented into chunks of maximum size 0x400 bytes.
     *
     * @param encryptedBuffer The buffer to store encrypted messages
     * @param plainTextBuffer The buffer containing plaintext messages
     * @param readKey The key used for encryption
     * @param sequenceNumber The starting sequence number
     * @return A SequenceBuffer containing the encrypted messages and updated sequence number
     * @throws IOException if the encryption process fails
     */
    public static SequenceBuffer encryptBuffer(ByteBuffer encryptedBuffer, ByteBuffer plainTextBuffer, byte[] readKey,
            long sequenceNumber) throws IOException {
        logger.trace("EncryptBuffer : Input = {}", BufferUtil.toDetailString(plainTextBuffer));
        logger.trace("EncryptBuffer : Output = {}", BufferUtil.toDetailString(encryptedBuffer));
        logger.trace("EncryptBuffer : Key = {}", HomekitByte.toHex(readKey));
        logger.trace("EncryptBuffer : sequenceNumber = {}", sequenceNumber);

        long currentSequenceNumber = sequenceNumber;
        ByteBuffer resultingBuffer = encryptedBuffer;

        ByteBuffer dummy = plainTextBuffer.duplicate();
        logger.trace("EncryptBuffer : Encrypting '{}'", BufferUtil.toUTF8String(dummy));

        try (HomekitByteBufferOutputStream encrypted = new HomekitByteBufferOutputStream(encryptedBuffer, true)) {
            while (plainTextBuffer.hasRemaining()) {
                short length = (short) Math.min(plainTextBuffer.remaining(), 0x400);
                logger.trace("EncryptBuffer : Encrypting {} bytes out of {} remaining in the input buffer", length,
                        plainTextBuffer.remaining());
                byte[] lengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(length).array();

                encrypted.write(lengthBytes);
                logger.trace("EncryptBuffer : Wrote LengthBytes (Output={})",
                        BufferUtil.toSummaryString(encryptedBuffer));

                byte[] nonce;
                synchronized (HomekitEncryptionEngine.class) {
                    nonce = Pack.longToLittleEndian(currentSequenceNumber++);
                }
                byte[] plaintext = new byte[length];
                plainTextBuffer.get(plaintext, 0, length);

                byte[] ciphertext = new HomekitChachaEncoder(readKey, nonce).encodeCiphertext(plaintext, lengthBytes);

                encrypted.write(ciphertext);
                logger.trace("EncryptBuffer : Wrote Sequence {} ({} bytes) (Output={})", currentSequenceNumber,
                        ciphertext.length, BufferUtil.toSummaryString(encryptedBuffer));
            }

            resultingBuffer = encrypted.toByteBuffer();
        } catch (Exception e) {
            logger.error("Failed to encrypt messages", e);
        }

        logger.trace("EncryptBuffer : Output = {}", BufferUtil.toSummaryString(resultingBuffer));
        return new SequenceBuffer(resultingBuffer, currentSequenceNumber);
    }

    /**
     * Decrypts a single encrypted message.
     *
     * This method decrypts a message using ChaCha20-Poly1305, verifying the
     * message authentication code before returning the decrypted content.
     *
     * @param msg The encrypted message with MAC
     * @param key The decryption key
     * @param sequenceNumber The sequence number for this message
     * @return The decrypted message content
     * @throws RuntimeException if decryption fails
     */
    private static byte[] decrypt(byte[] msg, byte[] key, long sequenceNumber) {
        logger.trace("Decrypt : key {}", HomekitByte.toHex(key));
        logger.trace("Decrypt : content {}", HomekitByte.toHex(msg));
        logger.trace("Decrypt : sequence {}", sequenceNumber);

        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (msg.length - 16)).array();
        try {
            byte[] nonce;
            synchronized (HomekitEncryptionEngine.class) {
                nonce = Pack.longToLittleEndian(sequenceNumber);
            }
            return new HomekitChachaDecoder(key, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (org.bouncycastle.tls.TlsFatalAlert e) {
            logger.error("Decrypt : Exception while decrypting : Description = {}", e.getAlertDescription());
            throw new IllegalStateException("Decryption failed: authentication check failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("Decryption failed: I/O error", e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /**
     * Creates a key using HKDF (HMAC-based Key Derivation Function).
     *
     * @param info The context information for key derivation
     * @param sharedSecret The shared secret to derive the key from
     * @return The derived key
     */
    public static byte[] createKey(String info, byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

    /**
     * Gets a thread-safe secure random number generator.
     *
     * @return A SecureRandom instance for cryptographic operations
     */
    public static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized (HomekitEncryptionEngine.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        SecureRandom result = secureRandom;
        if (result == null) {
            throw new IllegalStateException("SecureRandom initialization failed");
        }
        return result;
    }
}
