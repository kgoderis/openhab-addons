package org.openhab.io.homekit.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.util.Pack;
import org.eclipse.jetty.util.BufferUtil;
import org.openhab.io.homekit.obsolete.PairVerificationStageOneHandler;
import org.openhab.io.homekit.util.Byte;
import org.openhab.io.homekit.util.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.ClientEvidenceRoutine;
import com.nimbusds.srp6.SRP6ClientEvidenceContext;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6ServerEvidenceContext;
import com.nimbusds.srp6.ServerEvidenceRoutine;

public class HomekitEncryptionEngine {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitEncryptionEngine.class);

    public static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    public static final BigInteger G = BigInteger.valueOf(5);
    public static final SRP6CryptoParams SRP6Params = new SRP6CryptoParams(N_3072, G, "SHA-512");
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
        logger.trace("DecryptBuffer : key = {}", Byte.toHexString(writeKey));
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
        logger.trace("EncryptBuffer : Key = {}", Byte.toHexString(readKey));
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
        logger.trace("Decrypt : key {}", org.openhab.io.homekit.util.Byte.toHexString(key));
        logger.trace("Decrypt : content {}", org.openhab.io.homekit.util.Byte.toHexString(msg));
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

    public static class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine {

        // public ClientEvidenceRoutineImpl() {
        // }

        /**
         * Calculates M1 according to the following formula:
         *
         * <p>
         * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
         */
        @Override
        public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx) {

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(cryptoParams.H);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not locate requested algorithm", e);
            }
            digest.update(Byte.toByteArray(cryptoParams.N));
            byte[] hN = digest.digest();

            digest.update(Byte.toByteArray(cryptoParams.g));
            byte[] hg = digest.digest();

            byte[] hNhg = xor(hN, hg);

            digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
            byte[] hu = digest.digest();

            digest.update(Byte.toByteArray(ctx.S));
            byte[] hS = digest.digest();

            digest.update(hNhg);
            digest.update(hu);
            digest.update(Byte.toByteArray(ctx.s));
            digest.update(Byte.toByteArray(ctx.A));
            digest.update(Byte.toByteArray(ctx.B));
            digest.update(hS);
            BigInteger ret = new BigInteger(1, digest.digest());
            return ret;
        }

        private byte[] xor(byte[] b1, byte[] b2) {
            byte[] result = new byte[b1.length];
            for (int i = 0; i < b1.length; i++) {
                result[i] = (byte) (b1[i] ^ b2[i]);
            }
            return result;
        }
    }

    public static class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine {

        @Override
        public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx) {

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(cryptoParams.H);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not locate requested algorithm", e);
            }

            byte[] hS = digest.digest(Byte.toByteArray(ctx.S));

            digest.update(Byte.toByteArray(ctx.A));
            digest.update(Byte.toByteArray(ctx.M1));
            digest.update(hS);

            return new BigInteger(1, digest.digest());
        }

    }

    public static byte[] createKey(String info, byte[] sharedSecret) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

    public static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized (PairVerificationStageOneHandler.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }
}
