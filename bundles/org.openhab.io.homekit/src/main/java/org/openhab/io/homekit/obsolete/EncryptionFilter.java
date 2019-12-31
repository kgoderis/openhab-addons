package org.openhab.io.homekit.obsolete;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.Pack;
import org.openhab.io.homekit.crypto.ChachaDecoder;
import org.openhab.io.homekit.crypto.ChachaEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionFilter implements Filter {

    protected static final Logger logger = LoggerFactory.getLogger(EncryptionFilter.class);

    private ServletContext context;
    private static int inboundBinaryMessageCount = 0;
    private static int outboundBinaryMessageCount = 0;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.context = filterConfig.getServletContext();
        this.context.log("EncryptionFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        HttpSession session = httpServletRequest.getSession();

        if (session != null) {
            byte[] readKey = (byte[]) session.getAttribute("Control-Read-Encryption-Key");
            byte[] writeKey = (byte[]) session.getAttribute("Control-Write-Encryption-Key");

            if (readKey != null && writeKey != null) {

                byte[] decryptedRequestBody = decryptRequest(IOUtils.toByteArray(request.getInputStream()), readKey);
                request.setAttribute("decryptedBody", decryptedRequestBody);

                ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) response);
                chain.doFilter(request, responseWrapper);

                byte[] decryptedResponseBody = responseWrapper.toByteArray();
                byte[] encryptedResponseBody = encryptResponse(decryptedResponseBody, writeKey);

                response.getOutputStream().write(encryptedResponseBody);
                httpServletResponse.setContentLength(encryptedResponseBody.length);
                response.getOutputStream().flush();

                // response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
                // response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

            } else {
                httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);

                // chain.doFilter(request, response);
            }
        }
    }

    @Override
    public void destroy() {
    }

    public byte[] decryptRequest(byte[] ciphertext, byte[] readKey) {

        LengthPrefixedByteArrayProcessor binaryProcessor = new LengthPrefixedByteArrayProcessor();

        Collection<byte[]> res = binaryProcessor.handle(ciphertext);
        if (res.isEmpty()) {
            return new byte[0];
        } else {
            try (ByteArrayOutputStream decrypted = new ByteArrayOutputStream()) {
                res.stream().map(msg -> decrypt(msg, readKey)).forEach(bytes -> {
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

    public byte[] encryptResponse(byte[] response, byte[] writeKey) throws IOException {
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

    private byte[] decrypt(byte[] msg, byte[] readKey) {
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) (msg.length - 16)).array();
        try {
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(readKey, nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class LengthPrefixedByteArrayProcessor {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private Byte firstLengthByteBuffer; // Only used if we've received a single byte at the start of a message
        private int targetLength = 0;

        public synchronized Collection<byte[]> handle(byte[] data) {
            Collection<byte[]> results = new LinkedList<>();
            int pos = 0;
            logger.trace("Received message of length {}. Existing buffer is {}", data.length, buffer.size());
            if (buffer.size() == 0) {
                while (data.length - pos > 18) {
                    int targetLength = (data[0] & 0xFF) + (data[1] & 0xFF) * 256 + 16 + 2;
                    logger.trace("Attempting to read message of length {}", targetLength);
                    if (data.length >= pos + targetLength) {
                        byte[] b = new byte[targetLength - 2];
                        System.arraycopy(data, pos + 2, b, 0, targetLength - 2);
                        results.add(b);
                        logger.trace("Read complete message");
                        pos = pos + targetLength;
                    } else {
                        logger.trace("Not enough data available");
                        break;
                    }
                }
            }
            if (data.length > pos) {
                logger.trace("Remaining data available");
                step(data, pos, results);
            }
            logger.trace("Returning {} results", results.size());
            return results;
        }

        private void step(byte[] data, int pos, Collection<byte[]> results) {
            logger.trace("Performing step operation on buffer of length {} with pos {}", data.length, pos);
            if (targetLength == 0 && data.length == 1 + pos) {
                firstLengthByteBuffer = data[pos];
                logger.trace("Received a single byte message, storing byte {} for later", firstLengthByteBuffer);
                return;
            }
            if (targetLength == 0) {
                if (firstLengthByteBuffer != null) {
                    targetLength = (firstLengthByteBuffer & 0xFF) + (data[pos] & 0xFF) * 256 + 16;
                    pos += 1;
                    logger.trace("Received the second byte after storing the first byte. New length is {}",
                            targetLength);
                } else {
                    targetLength = (data[pos] & 0xFF) + (data[pos + 1] & 0xFF) * 256 + 16;
                    pos += 2;
                    logger.trace("targetLength is {}", targetLength);
                }
            }
            int toWrite = targetLength - buffer.size();
            if (toWrite <= data.length - pos) {
                // We have a complete message
                logger.trace("Received a complete message");
                buffer.write(data, pos, toWrite);
                results.add(buffer.toByteArray());
                buffer.reset();
                targetLength = 0;
                if (pos + toWrite < data.length) {
                    step(data, pos + toWrite, results);
                }
            } else {
                logger.trace("Storing {} bytes in buffer until we receive the complete {}", data.length - pos,
                        targetLength);
                buffer.write(data, pos, data.length - pos);
            }
        }
    }

    private class ResponseWrapper extends HttpServletResponseWrapper {

        private ByteArrayOutputStream baos;

        public byte[] toByteArray() {
            return baos.toByteArray();
        }

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
            baos = new ByteArrayOutputStream();
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(baos, false);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ByteArrayServletStream(baos);
        }
    }

    private class ByteArrayServletStream extends ServletOutputStream {

        ByteArrayOutputStream baos;

        ByteArrayServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        @Override
        public void write(int param) throws IOException {
            baos.write(param);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }
}
