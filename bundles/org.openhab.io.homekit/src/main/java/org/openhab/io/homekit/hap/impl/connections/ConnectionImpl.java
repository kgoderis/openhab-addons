package org.openhab.io.homekit.hap.impl.connections;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.function.Consumer;

import org.bouncycastle.util.Pack;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.openhab.io.homekit.hap.HomekitAuthInfo;
import org.openhab.io.homekit.hap.impl.HomekitRegistry;
import org.openhab.io.homekit.hap.impl.http.HomekitClientConnection;
import org.openhab.io.homekit.hap.impl.http.HttpRequest;
import org.openhab.io.homekit.hap.impl.http.HttpResponse;
import org.openhab.io.homekit.hap.impl.jmdns.JmdnsHomekitAdvertiser;
import org.openhab.io.homekit.hap.impl.pairing.UpgradeResponse;
import org.openhab.io.homekit.util.Byte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectionImpl implements HomekitClientConnection {

    private final HttpSession httpSession;
    private LengthPrefixedByteArrayProcessor binaryProcessor;
    private int inboundBinaryMessageCount = 0;
    private int outboundBinaryMessageCount = 0;
    private byte[] readKey;
    private byte[] writeKey;
    private boolean isUpgraded = false;
    private final Consumer<HttpResponse> outOfBandMessageCallback;
    private final SubscriptionManager subscriptions;

    private static final Logger LOGGER = LoggerFactory.getLogger(HomekitClientConnection.class);

    public ConnectionImpl(HomekitAuthInfo authInfo, HomekitRegistry registry,
            Consumer<HttpResponse> outOfBandMessageCallback, SubscriptionManager subscriptions,
            JmdnsHomekitAdvertiser advertiser) {
        httpSession = new HttpSession(authInfo, registry, subscriptions, this, advertiser);
        this.outOfBandMessageCallback = outOfBandMessageCallback;
        this.subscriptions = subscriptions;
    }

    @Override
    public synchronized HttpResponse handleRequest(HttpRequest request) throws IOException {
        return doHandleRequest(request);
    }

    private HttpResponse doHandleRequest(HttpRequest request) throws IOException {
        HttpResponse response = isUpgraded ? httpSession.handleAuthenticatedRequest(request)
                : httpSession.handleRequest(request);
        if (response instanceof UpgradeResponse) {
            isUpgraded = true;
            readKey = ((UpgradeResponse) response).getReadKey().array();
            writeKey = ((UpgradeResponse) response).getWriteKey().array();
        }
        LOGGER.info(response.getStatusCode() + " " + request.getUri());
        return response;
    }

    @Override
    public byte[] decryptRequest(byte[] ciphertext) {

        // LOGGER.debug("OLD OLD OLD OLD OLD DECRYPT");
        //
        // byte[] oldResult = olddecryptRequest(ciphertext);
        //
        // LOGGER.debug("old result : {}", org.openhab.io.homekit.util.Byte.byteToHexString(oldResult));
        // LOGGER.debug("OLD OLD OLD OLD OLD DECRYPT");
        //
        // LOGGER.debug("NEW NEW NEW NEW DECRYPT");
        //
        // SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(ByteBuffer.allocateDirect(2048),
        // ByteBuffer.wrap(ciphertext), readKey, inboundBinaryMessageCount);
        // ByteBuffer plaintextBuffer = sBuffer.buffer;
        // inboundBinaryMessageCount = (int) sBuffer.sequenceNumber;
        //
        // // BufferUtil.flipToFlush(plaintextBuffer, 0);
        // byte[] result = new byte[plaintextBuffer.remaining()];
        // plaintextBuffer.get(result);
        //
        // LOGGER.debug("new result : {}", org.openhab.io.homekit.util.Byte.byteToHexString(result));
        // LOGGER.debug("NEW NEW NEW NEW DECRYPT");
        //
        // return oldResult;
        if (!isUpgraded) {
            throw new RuntimeException("Cannot handle binary before connection is upgraded");
        }
        if (binaryProcessor == null) {
            binaryProcessor = new LengthPrefixedByteArrayProcessor();
        }
        Collection<byte[]> res = binaryProcessor.handle(ciphertext);
        if (res.isEmpty()) {
            return new byte[0];
        } else {
            try (ByteArrayOutputStream decrypted = new ByteArrayOutputStream()) {
                res.stream().map(msg -> decrypt(msg)).forEach(bytes -> {
                    try {
                        decrypted.write(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                return decrypted.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public byte[] olddecryptRequest(byte[] ciphertext) {
        if (!isUpgraded) {
            throw new RuntimeException("Cannot handle binary before connection is upgraded");
        }
        if (binaryProcessor == null) {
            binaryProcessor = new LengthPrefixedByteArrayProcessor();
        }
        Collection<byte[]> res = binaryProcessor.handle(ciphertext);
        if (res.isEmpty()) {
            return new byte[0];
        } else {
            try (ByteArrayOutputStream decrypted = new ByteArrayOutputStream()) {
                res.stream().map(msg -> decrypt(msg)).forEach(bytes -> {
                    try {
                        decrypted.write(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                return decrypted.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private byte[] decrypt(byte[] msg) {

        LOGGER.debug("Decrypting Key {}", org.openhab.io.homekit.util.Byte.toHexString(readKey));
        LOGGER.debug("Decrypting Content {}", org.openhab.io.homekit.util.Byte.toHexString(msg));
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (msg.length - 16)).array();
        try {
            LOGGER.debug("Decrypting inboundBinaryMessageCount {}", inboundBinaryMessageCount);
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(readKey, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (IOException e) {
            LOGGER.error("Decrypt : Exception while decrypting {} with key {}", Byte.toHexString(msg),
                    Byte.toHexString(readKey));
            if (e instanceof org.bouncycastle.crypto.tls.TlsFatalAlert) {
                LOGGER.error("Description is {}",
                        ((org.bouncycastle.crypto.tls.TlsFatalAlert) e).getAlertDescription());
            }
            throw new RuntimeException(e);
        }
    }

    // @Override
    // public byte[] encryptResponse(byte[] response) throws IOException {
    //
    // SequenceBuffer sBuffer = HomekitEncryptionEngine.encryptBuffer(ByteBuffer.allocateDirect(2048),
    // ByteBuffer.wrap(response), writeKey, outboundBinaryMessageCount);
    // ByteBuffer encryptedBuffer = sBuffer.buffer;
    // outboundBinaryMessageCount = (int) sBuffer.sequenceNumber;
    //
    // // BufferUtil.flipToFlush(encryptedBuffer, 0);
    // byte[] result = new byte[encryptedBuffer.remaining()];
    // encryptedBuffer.get(result);
    //
    // return result;
    // }

    @Override
    public byte[] encryptResponse(byte[] response) throws IOException {
        int offset = 0;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            while (offset < response.length) {
                short length = (short) Math.min(response.length - offset, 0x400);
                byte[] lengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(length).array();
                baos.write(lengthBytes);

                byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
                byte[] plaintext;
                if (response.length == length) {
                    plaintext = response;
                } else {
                    plaintext = new byte[length];
                    System.arraycopy(response, offset, plaintext, 0, length);
                }
                offset += length;
                baos.write(new ChachaEncoder(writeKey, nonce).encodeCiphertext(plaintext, lengthBytes));
            }
            return baos.toByteArray();
        }
    }

    @Override
    public void close() {
        subscriptions.removeConnection(this);
    }

    @Override
    public void outOfBand(HttpResponse message) {
        outOfBandMessageCallback.accept(message);
    }
}
