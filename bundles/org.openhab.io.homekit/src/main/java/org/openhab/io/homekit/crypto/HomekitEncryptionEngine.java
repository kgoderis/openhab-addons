package org.openhab.io.homekit.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

import org.bouncycastle.util.Pack;
import org.eclipse.jetty.util.BufferUtil;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitEncryptionEngine {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitEncryptionEngine.class);

    private static volatile SecureRandom secureRandom;

    public static class SequenceBuffer {
        public ByteBuffer buffer;
        public long sequenceNumber;

        public SequenceBuffer(ByteBuffer buffer, long sequenceNumber) {
            this.buffer = buffer;
            this.sequenceNumber = sequenceNumber;
        }
    }

    public static SequenceBuffer decryptBuffer(ByteBuffer decryptedBuffer, ByteBuffer cipherTextBuffer, byte[] writeKey,
            long sequenceNumber) {

        logger.trace("DecryptBuffer : cipherTextBuffer = {}", BufferUtil.toDetailString(cipherTextBuffer));
        logger.trace("DecryptBuffer : decryptedBuffer = {}", BufferUtil.toDetailString(decryptedBuffer));
        logger.trace("DecryptBuffer : key = {}", Byte.byteToHexString(writeKey));
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
            try (ByteBufferOutputStream decrypted = new ByteBufferOutputStream(decryptedBuffer, true)) {
                for (byte[] msg : results) {
                    try {
                        decrypted.write(decrypt(msg, writeKey, currentSequenceNumber++));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                resultingBuffer = decrypted.toByteBuffer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new SequenceBuffer(resultingBuffer, currentSequenceNumber);
    }

    public static SequenceBuffer encryptBuffer(ByteBuffer encryptedBuffer, ByteBuffer plainTextBuffer, byte[] readKey,
            long sequenceNumber) throws IOException {

        logger.trace("EncryptBuffer : Input = {}", BufferUtil.toDetailString(plainTextBuffer));
        logger.trace("EncryptBuffer : Output = {}", BufferUtil.toDetailString(encryptedBuffer));
        logger.trace("EncryptBuffer : Key = {}", Byte.byteToHexString(readKey));
        logger.trace("EncryptBuffer : sequenceNumber = {}", sequenceNumber);

        long currentSequenceNumber = sequenceNumber;
        ByteBuffer resultingBuffer = encryptedBuffer;

        ByteBuffer dummy = plainTextBuffer.duplicate();
        logger.trace("EncryptBuffer : Encrypting '{}'", BufferUtil.toUTF8String(dummy));

        try (ByteBufferOutputStream encrypted = new ByteBufferOutputStream(encryptedBuffer, true)) {
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

                byte[] ciphertext = new ChachaEncoder(readKey, nonce).encodeCiphertext(plaintext, lengthBytes);

                encrypted.write(ciphertext);
                logger.trace("EncryptBuffer : Wrote Sequence {} ({} bytes) (Output={})", currentSequenceNumber,
                        ciphertext.length, BufferUtil.toSummaryString(encryptedBuffer));

            }

            resultingBuffer = encrypted.toByteBuffer();

        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.trace("EncryptBuffer : Output = {}", BufferUtil.toSummaryString(resultingBuffer));
        return new SequenceBuffer(resultingBuffer, currentSequenceNumber);
    }

    private static byte[] decrypt(byte[] msg, byte[] key, long sequenceNumber) {
        logger.trace("Decrypt : key {}", org.openhab.io.homekit.util.Byte.byteToHexString(key));
        logger.trace("Decrypt : content {}", org.openhab.io.homekit.util.Byte.byteToHexString(msg));
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
            return new ChachaDecoder(key, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (Exception e) {
            if (e instanceof org.bouncycastle.crypto.tls.TlsFatalAlert) {
                logger.error("Decrypt : Exception while decrypting : Description = {}",
                        ((org.bouncycastle.crypto.tls.TlsFatalAlert) e).getAlertDescription());
            }
            throw new RuntimeException(e);
        }
    }
}
