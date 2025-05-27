package org.openhab.io.homekit.protocol.crypto;

import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

/**
 * Provides Poly1305 Message Authentication Code (MAC) generation for HomeKit protocol.
 *
 * This class implements the Poly1305 MAC algorithm as specified in the HomeKit Accessory Protocol (HAP).
 * It is used to generate authentication tags for encrypted messages, ensuring message integrity
 * and authenticity during HomeKit communication.
 *
 * Key features:
 * - Uses Poly1305 for efficient MAC generation
 * - Supports additional authenticated data (AAD)
 * - Handles message padding according to protocol specifications
 * - Processes both ciphertext and additional data
 *
 * Security considerations:
 * - Implements proper MAC key handling
 * - Ensures correct padding of input data
 * - Processes data lengths for integrity verification
 * - Uses cryptographically secure MAC generation
 *
 * The MAC generation process:
 * 1. Initializes Poly1305 with the provided MAC key
 * 2. Processes additional authenticated data (if any)
 * 3. Processes ciphertext data
 * 4. Adds padding to ensure 16-byte alignment
 * 5. Includes data lengths for integrity verification
 * 6. Generates the final MAC value
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
class HomekitPolyKeyCreator {

    /**
     * Creates a Poly1305 MAC for the given data.
     *
     * This method generates a Message Authentication Code using the Poly1305 algorithm
     * for the provided ciphertext and additional authenticated data. The MAC is used
     * to verify the integrity and authenticity of encrypted messages in the HomeKit protocol.
     *
     * The MAC generation process:
     * 1. Processes additional authenticated data (if any)
     * 2. Processes ciphertext data
     * 3. Adds padding to ensure 16-byte alignment
     * 4. Includes data lengths for integrity verification
     * 5. Generates the final MAC value
     *
     * @param macKey The key to use for MAC generation
     * @param additionalData Additional authenticated data (can be null)
     * @param ciphertext The encrypted data to authenticate
     * @return The generated MAC value
     */
    public static byte[] create(KeyParameter macKey, byte[] additionalData, byte[] ciphertext) {
        Poly1305 poly = new Poly1305();
        poly.init(macKey);

        if (additionalData != null) {
            poly.update(additionalData, 0, additionalData.length);
            if (additionalData.length % 16 != 0) {
                int round = 16 - (additionalData.length % 16);
                poly.update(new byte[round], 0, round);
            }
        }

        poly.update(ciphertext, 0, ciphertext.length);
        if (ciphertext.length % 16 != 0) {
            int round = 16 - (ciphertext.length % 16);
            poly.update(new byte[round], 0, round);
        }

        // additional data length
        byte[] additionalDataLength;
        if (additionalData != null) {
            additionalDataLength = Pack.longToLittleEndian(additionalData.length);
        } else {
            additionalDataLength = new byte[8];
        }
        poly.update(additionalDataLength, 0, 8);
        byte[] ciphertextLength = Pack.longToLittleEndian(ciphertext.length);
        poly.update(ciphertextLength, 0, 8);

        byte[] calculatedMAC = new byte[poly.getMacSize()];
        poly.doFinal(calculatedMAC, 0);
        return calculatedMAC;
    }
}
