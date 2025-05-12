package org.openhab.io.homekit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.openhab.io.homekit.protocol.error.HomekitErrorCode;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitTypeLengthValueEncoderDecoder {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitTypeLengthValueEncoderDecoder.class);

    private HomekitTypeLengthValueEncoderDecoder() {
    }

    public static DecodeResult decode(byte[] content) throws IOException {
        logger.trace("Decoding {}", HomekitByte.toHexString(content));
        DecodeResult ret = new DecodeResult();
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        while (bais.available() > 0) {
            byte type = (byte) (bais.read() & 0xFF);
            int length = bais.read();
            byte[] part = new byte[length];
            bais.read(part);
            ret.add(type, part);
            logger.trace("Decoded T {} L {} V {}", HomekitMessage.get(type).name(), length,
                    HomekitByte.toHexString(part));
        }
        return ret;
    }

    public static Encoder getEncoder() {
        return new Encoder();
    }

    public static final class Encoder {

        private final ByteArrayOutputStream baos;

        private Encoder() {
            baos = new ByteArrayOutputStream();
        }

        public void add(HomekitMessage type, BigInteger i) throws IOException {
            add(type, HomekitByte.toByteArray(i));
        }

        public void add(HomekitMessage type, short b) {
            baos.write(type.getKey());
            baos.write(1);
            baos.write(b);
        }

        public void add(HomekitMessage type, HomekitErrorCode e) {
            baos.write(type.getKey());
            baos.write(1);
            baos.write(e.getCode());
        }

        public void add(HomekitMessage type, byte[] bytes) throws IOException {
            InputStream bais = new ByteArrayInputStream(bytes);
            while (bais.available() > 0) {
                int toWrite = bais.available();
                toWrite = toWrite > 255 ? 255 : toWrite;
                baos.write(type.getKey());
                baos.write(toWrite);
                HomekitByte.copyStream(bais, baos, toWrite);
                logger.trace("Encoded T {} L {} V {}", type.name(), toWrite, HomekitByte.toHexString(bytes));
            }
        }

        public byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    public static final class DecodeResult {
        private final Map<Short, byte[]> result = new HashMap<>();

        private DecodeResult() {
        }

        public byte getByte(HomekitMessage type) {
            return result.get(type.getKey())[0];
        }

        public BigInteger getBigInt(HomekitMessage type) {
            return new BigInteger(1, result.get(type.getKey()));
        }

        public byte[] getBytes(HomekitMessage type) {
            return result.get(type.getKey());
        }

        public void getBytes(HomekitMessage type, byte[] dest, int srcOffset) {
            byte[] b = result.get(type.getKey());
            System.arraycopy(b, srcOffset, dest, 0, Math.min(dest.length, b.length));
        }

        public int getLength(HomekitMessage type) {
            return result.get(type.getKey()).length;
        }

        private void add(short type, byte[] bytes) {
            result.merge(type, bytes, HomekitByte::joinBytes);
        }
    }
}
