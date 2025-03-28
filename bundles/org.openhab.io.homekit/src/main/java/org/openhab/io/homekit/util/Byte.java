package org.openhab.io.homekit.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.HexDump;
import org.eclipse.jetty.util.BufferUtil;
import org.slf4j.Logger;

public class Byte {

    public static byte[] joinBytes(byte[]... piece) {
        int pos = 0;
        int length = 0;
        for (int i = 0; i < piece.length; i++) {
            length += piece[i].length;
        }
        byte[] ret = new byte[length];
        for (int i = 0; i < piece.length; i++) {
            System.arraycopy(piece[i], 0, ret, pos, piece[i].length);
            pos += piece[i].length;
        }
        return ret;
    }

    public static byte[] toByteArray(BigInteger i) {
        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }

    public static void copyStream(InputStream input, OutputStream output, int length) throws IOException {
        byte[] buffer = new byte[length];
        int remaining = length;
        int bytesRead;
        while ((bytesRead = input.read(buffer, 0, remaining)) != -1 && remaining > 0) {
            output.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
    }

    public static String toHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static void logBuffer(Logger logger, String label, String remote, ByteBuffer buf) throws IOException {
        if (buf.hasRemaining()) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                ByteBuffer buffer = buf.asReadOnlyBuffer();

                byte[] bytes = new byte[buf.remaining()];
                buffer.get(bytes, 0, buffer.remaining());

                HexDump.dump(bytes, 0, stream, 0);
                stream.flush();
                logger.trace(String.format("[%s] %s %s:%n%s%n", remote, label, BufferUtil.toDetailString(buf),
                        stream.toString(StandardCharsets.UTF_8.name())));
            }
        }
    }
}
