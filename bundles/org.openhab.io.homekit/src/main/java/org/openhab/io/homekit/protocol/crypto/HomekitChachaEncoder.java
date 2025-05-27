package org.openhab.io.homekit.protocol.crypto;

import java.io.IOException;

import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Provides ChaCha20-Poly1305 encryption for HomeKit protocol messages.
 *
 * This class implements the ChaCha20 stream cipher with Poly1305 message authentication
 * as specified in the HomeKit Accessory Protocol (HAP). It is used to encrypt and authenticate
 * messages exchanged during HomeKit communication.
 *
 * Key features:
 * - Uses ChaCha20 for efficient stream encryption
 * - Implements Poly1305 for message authentication
 * - Supports additional authenticated data (AAD)
 * - Handles nonce management
 * - Provides authenticated encryption
 *
 * Security considerations:
 * - Uses cryptographically secure ChaCha20 implementation
 * - Implements proper key and nonce handling
 * - Provides message authentication
 * - Ensures secure key generation for MAC
 *
 * The encryption process:
 * 1. Initializes ChaCha20 with the provided key and nonce
 * 2. Generates a MAC key from the first cipher block
 * 3. Encrypts the plaintext using ChaCha20
 * 4. Generates a MAC for the ciphertext and AAD
 * 5. Combines ciphertext and MAC for transmission
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitChachaEncoder {

    /** The ChaCha20 cipher instance used for encryption */
    private final ChaChaEngine encryptCipher;

    /**
     * Creates a new ChaCha20-Poly1305 encoder with the specified key and nonce.
     *
     * This constructor initializes the ChaCha20 cipher with the provided key and nonce.
     * The cipher is configured for encryption mode and uses 20 rounds as specified in
     * the HomeKit protocol.
     *
     * @param key The encryption key
     * @param nonce The nonce (initialization vector)
     * @throws IOException if the cipher initialization fails
     */
    public HomekitChachaEncoder(byte[] key, byte[] nonce) throws IOException {
        this.encryptCipher = new ChaChaEngine(20);
        this.encryptCipher.init(true, new ParametersWithIV(new KeyParameter(key), nonce));
    }

    /**
     * Encrypts plaintext data without additional authenticated data.
     *
     * @param plaintext The data to encrypt
     * @return The encrypted data with authentication tag
     * @throws IOException if the encryption process fails
     */
    public byte[] encodeCiphertext(byte[] plaintext) throws IOException {
        return encodeCiphertext(plaintext, null);
    }

    /**
     * Encrypts plaintext data with optional additional authenticated data.
     *
     * This method performs authenticated encryption of the provided plaintext,
     * optionally including additional data in the authentication process. The
     * result includes both the encrypted data and an authentication tag.
     *
     * @param plaintext The data to encrypt
     * @param additionalData Additional data to authenticate (can be null)
     * @return The encrypted data with authentication tag
     * @throws IOException if the encryption process fails
     */
    public byte[] encodeCiphertext(byte[] plaintext, byte[] additionalData) throws IOException {
        KeyParameter macKey = initRecordMAC(encryptCipher);

        byte[] ciphertext = new byte[plaintext.length];
        encryptCipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);

        byte[] calculatedMAC = HomekitPolyKeyCreator.create(macKey, additionalData, ciphertext);

        byte[] ret = new byte[ciphertext.length + 16];
        System.arraycopy(ciphertext, 0, ret, 0, ciphertext.length);
        System.arraycopy(calculatedMAC, 0, ret, ciphertext.length, 16);
        return ret;
    }

    /**
     * Initializes the MAC key for a new record.
     *
     * This method generates a MAC key from the first block of the ChaCha20 cipher
     * output. The key is properly clamped according to the Poly1305 specification.
     *
     * @param cipher The ChaCha20 cipher instance
     * @return The initialized MAC key
     */
    private KeyParameter initRecordMAC(ChaChaEngine cipher) {
        byte[] firstBlock = new byte[64];
        cipher.processBytes(firstBlock, 0, firstBlock.length, firstBlock, 0);

        // NOTE: The BC implementation puts 'r' after 'k'
        System.arraycopy(firstBlock, 0, firstBlock, 32, 16);
        KeyParameter macKey = new KeyParameter(firstBlock, 16, 32);
        Poly1305KeyGenerator.clamp(macKey.getKey());
        return macKey;
    }
}
