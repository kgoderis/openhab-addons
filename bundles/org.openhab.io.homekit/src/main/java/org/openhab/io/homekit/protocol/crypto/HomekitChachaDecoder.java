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

import org.bouncycastle.crypto.engines.ChaChaEngine;
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.util.Arrays;

/**
 * Provides ChaCha20-Poly1305 decryption for HomeKit protocol messages.
 *
 * This class implements the ChaCha20 stream cipher with Poly1305 message authentication
 * as specified in the HomeKit Accessory Protocol (HAP). It is used to decrypt and verify
 * the authenticity of messages received during HomeKit communication.
 *
 * Key features:
 * - Uses ChaCha20 for efficient stream decryption
 * - Implements Poly1305 for message authentication
 * - Supports additional authenticated data (AAD)
 * - Handles nonce management
 * - Provides authenticated decryption
 *
 * Security considerations:
 * - Uses cryptographically secure ChaCha20 implementation
 * - Implements proper key and nonce handling
 * - Verifies message authentication before decryption
 * - Uses constant-time comparison for MAC verification
 * - Throws security alerts for authentication failures
 *
 * The decryption process:
 * 1. Initializes ChaCha20 with the provided key and nonce
 * 2. Generates a MAC key from the first cipher block
 * 3. Verifies the received MAC against the calculated MAC
 * 4. Decrypts the ciphertext using ChaCha20
 * 5. Returns the decrypted plaintext
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitChachaDecoder {

    /** The ChaCha20 cipher instance used for decryption */
    private final ChaChaEngine decryptCipher;

    /**
     * Creates a new ChaCha20-Poly1305 decoder with the specified key and nonce.
     *
     * This constructor initializes the ChaCha20 cipher with the provided key and nonce.
     * The cipher is configured for decryption mode and uses 20 rounds as specified in
     * the HomeKit protocol.
     *
     * @param key The decryption key
     * @param nonce The nonce (initialization vector)
     * @throws IOException if the cipher initialization fails
     */
    public HomekitChachaDecoder(byte[] key, byte[] nonce) throws IOException {
        this.decryptCipher = new ChaChaEngine(20);
        this.decryptCipher.init(false, new ParametersWithIV(new KeyParameter(key), nonce));
    }

    /**
     * Decrypts ciphertext data with authentication verification.
     *
     * This method performs authenticated decryption of the provided ciphertext,
     * verifying the message authentication code (MAC) before proceeding with
     * decryption. If authentication fails, a security alert is thrown.
     *
     * @param receivedMAC The MAC received with the ciphertext
     * @param additionalData Additional data used in authentication (can be null)
     * @param ciphertext The encrypted data to decrypt
     * @return The decrypted plaintext
     * @throws IOException if the decryption process fails
     * @throws TlsFatalAlert if message authentication fails
     */
    public byte[] decodeCiphertext(byte[] receivedMAC, byte[] additionalData, byte[] ciphertext) throws IOException {
        KeyParameter macKey = initRecordMAC(decryptCipher);

        byte[] calculatedMAC = HomekitPolyKeyCreator.create(macKey, additionalData, ciphertext);

        if (!Arrays.constantTimeAreEqual(calculatedMAC, receivedMAC)) {
            throw new TlsFatalAlert(AlertDescription.bad_record_mac);
        }

        byte[] output = new byte[ciphertext.length];
        decryptCipher.processBytes(ciphertext, 0, ciphertext.length, output, 0);

        return output;
    }

    /**
     * Decrypts ciphertext data without additional authenticated data.
     *
     * @param receivedMAC The MAC received with the ciphertext
     * @param ciphertext The encrypted data to decrypt
     * @return The decrypted plaintext
     * @throws IOException if the decryption process fails
     * @throws TlsFatalAlert if message authentication fails
     */
    public byte[] decodeCiphertext(byte[] receivedMAC, byte[] ciphertext) throws IOException {
        return decodeCiphertext(receivedMAC, null, ciphertext);
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
