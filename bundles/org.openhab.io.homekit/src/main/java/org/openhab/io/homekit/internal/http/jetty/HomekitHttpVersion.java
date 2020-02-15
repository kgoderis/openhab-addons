package org.openhab.io.homekit.internal.http.jetty;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Trie;

public enum HomekitHttpVersion {
    HTTP_0_9("HTTP/0.9", 9),
    HTTP_1_0("HTTP/1.0", 10),
    HTTP_1_1("HTTP/1.1", 11),
    HTTP_2("HTTP/2.0", 20),
    EVENT_1_0("EVENT/1.0", 11);

    public static final Trie<HomekitHttpVersion> CACHE = new ArrayTrie<HomekitHttpVersion>();

    static {
        for (HomekitHttpVersion version : HomekitHttpVersion.values()) {
            CACHE.put(version.toString(), version);
        }
    }

    public static HttpVersion get(String version) {
        HomekitHttpVersion httpversion = CACHE.get(version);
        return httpversion != null ? HttpVersion.fromVersion(httpversion.getVersion()) : null;
    }

    /**
     * Optimised lookup to find an Http Version and whitespace in a byte array.
     *
     * @param bytes Array containing ISO-8859-1 characters
     * @param position The first valid index
     * @param limit The first non valid index
     * @return An HttpMethod if a match or null if no easy match.
     */
    public static HttpVersion lookAheadGet(byte[] bytes, int position, int limit) {
        int length = limit - position;
        if (length < 9) {
            return null;
        }

        if (bytes[position + 4] == '/' && bytes[position + 6] == '.'
                && Character.isWhitespace((char) bytes[position + 8])
                && ((bytes[position] == 'H' && bytes[position + 1] == 'T' && bytes[position + 2] == 'T'
                        && bytes[position + 3] == 'P')
                        || (bytes[position] == 'h' && bytes[position + 1] == 't' && bytes[position + 2] == 't'
                                && bytes[position + 3] == 'p'))) {
            switch (bytes[position + 5]) {
                case '1':
                    switch (bytes[position + 7]) {
                        case '0':
                            return HttpVersion.HTTP_1_0;
                        case '1':
                            return HttpVersion.HTTP_1_1;
                        default:
                            return null;
                    }
                case '2':
                    switch (bytes[position + 7]) {
                        case '0':
                            return HttpVersion.HTTP_2;
                        default:
                            return null;
                    }
                default:
                    return null;
            }
        }

        if (bytes[position + 5] == '/' && bytes[position + 7] == '.'
                && Character.isWhitespace((char) bytes[position + 9])
                && ((bytes[position] == 'E' && bytes[position + 1] == 'V' && bytes[position + 2] == 'E'
                        && bytes[position + 3] == 'N' && bytes[position + 3] == 'T')
                        || (bytes[position] == 'e' && bytes[position + 1] == 'v' && bytes[position + 2] == 'e'
                                && bytes[position + 3] == 'n') && bytes[position + 4] == 't')) {
            switch (bytes[position + 6]) {
                case '1': {
                    return HttpVersion.HTTP_1_1;
                }

                default:
                    return null;
            }
        }

        return null;
    }

    /**
     * Optimised lookup to find an HTTP Version and trailing white space in a byte array.
     *
     * @param buffer buffer containing ISO-8859-1 characters
     * @return An HttpVersion if a match or null if no easy match.
     */
    public static HttpVersion lookAheadGet(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(),
                    buffer.arrayOffset() + buffer.limit());
        }
        return null;
    }

    private final String _string;
    private final byte[] _bytes;
    private final ByteBuffer _buffer;
    private final int _version;

    HomekitHttpVersion(String s, int version) {
        _string = s;
        _bytes = StringUtil.getBytes(s);
        _buffer = ByteBuffer.wrap(_bytes);
        _version = version;
    }

    public byte[] toBytes() {
        return _bytes;
    }

    public ByteBuffer toBuffer() {
        return _buffer.asReadOnlyBuffer();
    }

    public int getVersion() {
        return _version;
    }

    public boolean is(String s) {
        return _string.equalsIgnoreCase(s);
    }

    public String asString() {
        return _string;
    }

    @Override
    public String toString() {
        return _string;
    }

    /**
     * Case insensitive fromString() conversion
     *
     * @param version the String to convert to enum constant
     * @return the enum constant or null if version unknown
     */
    public static HttpVersion fromString(String version) {
        HomekitHttpVersion httpversion = CACHE.get(version);
        return httpversion != null ? HttpVersion.fromVersion(httpversion.getVersion()) : null;
    }

    public static HttpVersion fromVersion(int version) {
        return HttpVersion.fromVersion(version);
    }

    public static HttpVersion getBest(ByteBuffer buffer, int i, int remaining) {
        HomekitHttpVersion httpversion = CACHE.getBest(buffer, i, remaining);
        return httpversion != null ? HttpVersion.fromVersion(httpversion.getVersion()) : null;
    }
}
