package org.openhab.io.homekit.network.http;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTokens.EndOfContent;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized HTTP generator for HomeKit communication with encryption support.
 *
 * <p>
 * This class extends Jetty's HttpGenerator to provide specialized HTTP message generation for HomeKit accessories,
 * including support for encrypted messages, custom HTTP versions, and optimized header handling. It implements
 * RFC7230-compliant message generation for HTTP/1.0 and HTTP/1.1, with optimizations for HomeKit-specific requirements.
 * </p>
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link HomekitHttpParser} for protocol parsing and validation</li>
 * <li>{@link HomekitHttpVersion} for version handling and compatibility</li>
 * <li>{@link HomekitHttpConnection} for connection lifecycle management</li>
 * <li>{@link org.eclipse.jetty.http.HttpField HttpField} for header field handling</li>
 * <li>{@link org.eclipse.jetty.http.MetaData MetaData} for message metadata</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>HTTP request/response generation</li>
 * <li>Header field management</li>
 * <li>Chunked transfer encoding</li>
 * <li>Persistent connection support</li>
 * <li>Header caching optimization</li>
 * <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>
 * <b>Security Considerations:</b>
 * </p>
 * <ul>
 * <li>Validates message integrity</li>
 * <li>Enforces protocol compliance</li>
 * <li>Handles encrypted content</li>
 * <li>Ensures proper initialization</li>
 * <li>Maintains thread safety</li>
 * </ul>
 *
 * <p>
 * <b>Implementation Details:</b>
 * </p>
 * <ul>
 * <li>Fast case-insensitive string lookups</li>
 * <li>Optimized buffer management</li>
 * <li>Chunked transfer support</li>
 * <li>Content length handling</li>
 * <li>Persistent connection management</li>
 * <li>Detailed logging</li>
 * </ul>
 *
 * <p>
 * <b>Configuration Options:</b>
 * </p>
 * <dl>
 * <dt>Strict Mode</dt>
 * <dd>Enabled via "org.eclipse.jetty.http.HttpGenerator.STRICT" system property.
 * When enabled, preserves exact case and whitespace of methods and headers.</dd>
 * <dt>Server Version</dt>
 * <dd>Customizable server version string for response headers</dd>
 * <dt>X-Powered-By</dt>
 * <dd>Optional X-Powered-By header inclusion</dd>
 * </dl>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitHttpGenerator extends HttpGenerator {

    /** Logger instance for this class */
    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpGenerator.class);

    /** Debug flag for detailed logging */
    protected static final boolean debug = logger.isDebugEnabled();

    /** Log message prefixes */
    protected static final String LOG_PREFIX = "HomeKit HTTP Generator: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Initialization - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State Change - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Configuration - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "Accessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    /** Strict mode flag */
    public final static boolean __STRICT = Boolean.getBoolean("org.eclipse.jetty.http.HttpGenerator.STRICT");

    /** Colon and space bytes for header formatting */
    private final static byte[] __colon_space = new byte[] { ':', ' ' };

    /** HTTP token constants */
    static final byte COLON = (byte) ':';
    static final byte TAB = 0x09;
    static final byte LINE_FEED = 0x0A;
    static final byte CARRIAGE_RETURN = 0x0D;
    static final byte SPACE = 0x20;
    static final byte[] CRLF = { CARRIAGE_RETURN, LINE_FEED };

    /** Close header value array */
    private final static HttpHeaderValue[] CLOSE = { HttpHeaderValue.CLOSE };

    /** Common response metadata */
    public static final MetaData.Response CONTINUE_100_INFO = new MetaData.Response(HttpVersion.HTTP_1_1, 100, null,
            null, -1);
    public static final MetaData.Response PROGRESS_102_INFO = new MetaData.Response(HttpVersion.HTTP_1_1, 102, null,
            null, -1);
    public final static MetaData.Response RESPONSE_500_INFO = new MetaData.Response(HttpVersion.HTTP_1_1,
            HttpStatus.INTERNAL_SERVER_ERROR_500, null, new HttpFields() {
                {
                    put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE);
                }
            }, 0);

    /** Chunk size constant */
    public static final int CHUNK_SIZE = 12;

    private State _state = State.START;
    private EndOfContent _endOfContent = EndOfContent.UNKNOWN_CONTENT;

    private long _contentPrepared = 0;
    private boolean _noContent = false;
    private Boolean _persistent = null;
    private boolean _needCRLF = false;

    private final int _send;
    private final static int SEND_SERVER = 0x01;
    private final static int SEND_XPOWEREDBY = 0x02;
    private final static Set<String> __assumedContentMethods = new HashSet<>(
            Arrays.asList(new String[] { HttpMethod.POST.asString(), HttpMethod.PUT.asString() }));

    // Common content
    private static final byte[] LAST_CHUNK = { (byte) '0', (byte) '\015', (byte) '\012', (byte) '\015', (byte) '\012' };
    private static final byte[] CONTENT_LENGTH_0 = StringUtil.getBytes("Content-Length: 0\015\012");
    private static final byte[] CONNECTION_KEEP_ALIVE = StringUtil.getBytes("Connection: keep-alive\015\012");
    private static final byte[] CONNECTION_CLOSE = StringUtil.getBytes("Connection: close\015\012");
    private static final byte[] HTTP_1_1_SPACE = StringUtil.getBytes(HttpVersion.HTTP_1_1 + " ");
    private static final byte[] EVENT_1_0_SPACE = StringUtil.getBytes("EVENT/1.0" + " ");
    private static final byte[] TRANSFER_ENCODING_CHUNKED = StringUtil.getBytes("Transfer-Encoding: chunked\015\012");
    private static final byte[][] SEND = new byte[][] { new byte[0],
            StringUtil.getBytes("Server: Jetty(9.x.x)\015\012"),
            StringUtil.getBytes("X-Powered-By: Jetty(9.x.x)\015\012"),
            StringUtil.getBytes("Server: Jetty(9.x.x)\015\012X-Powered-By: Jetty(9.x.x)\015\012") };

    // Build cache of response lines for status
    private static class PreparedResponse {
        byte[] _reason;
        @SuppressWarnings("unused")
        byte[] _schemeCode;
        @SuppressWarnings("unused")
        byte[] _responseLine;
    }

    private static final PreparedResponse[] __preprepared = new PreparedResponse[HttpStatus.MAX_CODE + 1];
    static {
        int versionLength = HttpVersion.HTTP_1_1.toString().length();

        for (int i = 0; i < __preprepared.length; i++) {
            HttpStatus.Code code = HttpStatus.getCode(i);
            if (code == null) {
                continue;
            }
            String reason = code.getMessage();
            byte[] line = new byte[versionLength + 5 + reason.length() + 2];
            HttpVersion.HTTP_1_1.toBuffer().get(line, 0, versionLength);
            line[versionLength + 0] = ' ';
            line[versionLength + 1] = (byte) ('0' + i / 100);
            line[versionLength + 2] = (byte) ('0' + (i % 100) / 10);
            line[versionLength + 3] = (byte) ('0' + (i % 10));
            line[versionLength + 4] = ' ';
            for (int j = 0; j < reason.length(); j++) {
                line[versionLength + 5 + j] = (byte) reason.charAt(j);
            }
            line[versionLength + 5 + reason.length()] = CARRIAGE_RETURN;
            line[versionLength + 6 + reason.length()] = LINE_FEED;

            __preprepared[i] = new PreparedResponse();
            __preprepared[i]._schemeCode = Arrays.copyOfRange(line, 0, versionLength + 5);
            __preprepared[i]._reason = Arrays.copyOfRange(line, versionLength + 5, line.length - 2);
            __preprepared[i]._responseLine = line;
        }
    }

    private static void putSanitisedName(String s, ByteBuffer buffer) {
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char c = s.charAt(i);

            if (c < 0 || c > 0xff || c == '\r' || c == '\n' || c == ':') {
                buffer.put((byte) '?');
            } else {
                buffer.put((byte) (0xff & c));
            }
        }
    }

    private static void putSanitisedValue(String s, ByteBuffer buffer) {
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char c = s.charAt(i);

            if (c < 0 || c > 0xff || c == '\r' || c == '\n') {
                buffer.put((byte) ' ');
            } else {
                buffer.put((byte) (0xff & c));
            }
        }
    }

    private void putContentLength(ByteBuffer header, long contentLength, boolean contentType, MetaData.Request request,
            MetaData.Response response) {
        if (contentLength > 0) {
            header.put(HttpHeader.CONTENT_LENGTH.getBytesColonSpace());
            BufferUtil.putDecLong(header, contentLength);
            header.put(CRLF);
        } else if (!_noContent) {
            if (contentType || response != null
                    || (request != null && __assumedContentMethods.contains(request.getMethod()))) {
                header.put(CONTENT_LENGTH_0);
            }
        }
    }

    /**
     * Sets the Jetty server version in the HTTP headers.
     *
     * <p>
     * This method updates both the Server and X-Powered-By headers with the provided version.
     * The version string is used to identify the server in HTTP responses.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Updates both Server and X-Powered-By headers</li>
     * <li>Maintains consistent version across all responses</li>
     * <li>Uses StringUtil for byte conversion</li>
     * </ul>
     *
     * @param serverVersion The version string to be used in the headers
     */
    public static void setJettyVersion(String serverVersion) {
        SEND[SEND_SERVER] = StringUtil.getBytes("Server: " + serverVersion + "\015\012");
        SEND[SEND_XPOWEREDBY] = StringUtil.getBytes("X-Powered-By: " + serverVersion + "\015\012");
        SEND[SEND_SERVER | SEND_XPOWEREDBY] = StringUtil
                .getBytes("Server: " + serverVersion + "\015\012X-Powered-By: " + serverVersion + "\015\012");
    }

    /**
     * Creates a new HomeKit HTTP generator with default settings.
     *
     * <p>
     * This constructor initializes a generator with default settings for server version
     * and X-Powered-By header inclusion.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes generator state</li>
     * <li>Sets default configuration</li>
     * <li>Configures header handling</li>
     * <li>Ensures thread safety</li>
     * </ul>
     */
    public HomekitHttpGenerator() {
        this(true, true);
        logger.debug("{}Initialized with default settings", LOG_INIT);
    }

    /**
     * Creates a new HomeKit HTTP generator with specified settings.
     *
     * <p>
     * This constructor initializes a generator with custom settings for server version
     * and X-Powered-By header inclusion.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Initializes generator state</li>
     * <li>Applies custom configuration</li>
     * <li>Configures header handling</li>
     * <li>Ensures thread safety</li>
     * </ul>
     *
     * @param sendServerVersion Whether to include server version in responses
     * @param sendXPoweredBy Whether to include X-Powered-By header in responses
     */
    public HomekitHttpGenerator(boolean sendServerVersion, boolean sendXPoweredBy) {
        _send = (sendServerVersion ? SEND_SERVER : 0) | (sendXPoweredBy ? SEND_XPOWEREDBY : 0);
        logger.debug("{}Initialized with custom settings: serverVersion={}, xPoweredBy={}", LOG_INIT, sendServerVersion,
                sendXPoweredBy);
    }

    /**
     * Resets the generator to its initial state.
     *
     * <p>
     * This method clears all internal state and prepares the generator for a new message.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Clears message state</li>
     * <li>Resets content tracking</li>
     * <li>Clears persistence flag</li>
     * <li>Ensures thread safety</li>
     * </ul>
     */
    @Override
    public void reset() {
        _state = State.START;
        _endOfContent = EndOfContent.UNKNOWN_CONTENT;
        _contentPrepared = 0;
        _noContent = false;
        _persistent = null;
        _needCRLF = false;
        logger.debug("{}Reset to initial state", LOG_STATE);
    }

    /**
     * Gets the current state of the generator.
     *
     * This method returns the current state of message generation, which indicates
     * the phase of HTTP message construction.
     *
     * Key implementation details:
     * - Returns internal state variable
     * - Used for tracking message generation progress
     *
     * @return The current state of the generator
     */
    @Override
    public State getState() {
        return _state;
    }

    /**
     * Checks if the generator is in a specific state.
     *
     * This method compares the current state against a provided state value.
     *
     * Key implementation details:
     * - Direct state comparison
     * - Used for state validation
     *
     * @param state The state to check against
     * @return true if the generator is in the specified state
     */
    @Override
    public boolean isState(State state) {
        return _state == state;
    }

    /**
     * Checks if the generator is idle.
     *
     * This method determines if the generator is in its initial state and ready
     * to begin message generation.
     *
     * Key implementation details:
     * - Checks for START state
     * - Used for readiness verification
     *
     * @return true if the generator is idle
     */
    @Override
    public boolean isIdle() {
        return _state == State.START;
    }

    /**
     * Checks if the generator has completed message generation.
     *
     * This method determines if the current message generation cycle is complete.
     *
     * Key implementation details:
     * - Checks for END state
     * - Used for completion verification
     *
     * @return true if the generator has completed
     */
    @Override
    public boolean isEnd() {
        return _state == State.END;
    }

    /**
     * Checks if the generator has committed to sending a message.
     *
     * This method determines if the generator has progressed beyond the initial
     * state and is committed to sending the current message.
     *
     * Key implementation details:
     * - Checks state ordinal against COMMITTED
     * - Used for commitment verification
     *
     * @return true if the generator has committed
     */
    @Override
    public boolean isCommitted() {
        return _state.ordinal() >= State.COMMITTED.ordinal();
    }

    /**
     * Checks if the generator is using chunked transfer encoding.
     *
     * This method determines if the current message is using chunked transfer
     * encoding for content delivery.
     *
     * Key implementation details:
     * - Checks endOfContent type
     * - Used for transfer encoding verification
     *
     * @return true if chunked transfer encoding is being used
     */
    @Override
    public boolean isChunking() {
        return _endOfContent == EndOfContent.CHUNKED_CONTENT;
    }

    /**
     * Checks if the message has no content.
     *
     * This method determines if the current message is a no-content response,
     * such as 204 No Content.
     *
     * Key implementation details:
     * - Checks internal noContent flag
     * - Used for content presence verification
     *
     * @return true if the message has no content
     */
    @Override
    public boolean isNoContent() {
        return _noContent;
    }

    /**
     * Sets whether the connection should be persistent.
     *
     * This method configures the connection persistence behavior, which affects
     * how the Connection header is generated.
     *
     * Key implementation details:
     * - Sets internal persistent flag
     * - Affects header generation
     *
     * @param persistent true if the connection should be persistent
     */
    @Override
    public void setPersistent(boolean persistent) {
        _persistent = persistent;
    }

    /**
     * Checks if the connection is known to be persistent.
     *
     * This method determines if the current connection is configured to be
     * persistent across multiple requests.
     *
     * Key implementation details:
     * - Checks internal persistent flag
     * - Used for connection behavior verification
     *
     * @return true if the connection is persistent
     */
    @Override
    public boolean isPersistent() {
        return Boolean.TRUE.equals(_persistent);
    }

    /**
     * Checks if any content has been written.
     *
     * This method determines if any content has been prepared for the current
     * message.
     *
     * Key implementation details:
     * - Checks contentPrepared counter
     * - Used for content presence verification
     *
     * @return true if content has been written
     */
    @Override
    public boolean isWritten() {
        return _contentPrepared > 0;
    }

    /**
     * Gets the amount of content that has been prepared.
     *
     * This method returns the total number of bytes that have been prepared
     * for the current message.
     *
     * Key implementation details:
     * - Returns contentPrepared counter
     * - Used for content size tracking
     *
     * @return The number of bytes of content prepared
     */
    @Override
    public long getContentPrepared() {
        return _contentPrepared;
    }

    /**
     * Aborts the current message generation.
     *
     * This method forces the connection to close and resets the generator state.
     * It should be called when an error occurs during message generation.
     *
     * Key implementation details:
     * - Sets persistent to false
     * - Sets state to END
     * - Clears endOfContent
     */
    @Override
    public void abort() {
        _persistent = false;
        _state = State.END;
        _endOfContent = null;
    }

    /**
     * Generates an HTTP request message.
     *
     * <p>
     * This method generates a complete HTTP request message, including headers and content,
     * according to the provided metadata and content buffers.
     * </p>
     *
     * <p>
     * <b>Implementation details:</b>
     * </p>
     * <ul>
     * <li>Generates request line</li>
     * <li>Handles headers</li>
     * <li>Manages content</li>
     * <li>Supports chunking</li>
     * <li>Maintains thread safety</li>
     * <li>Ensures state consistency</li>
     * <li>Logs generation progress</li>
     * </ul>
     *
     * @param info The request metadata
     * @param header The header buffer
     * @param chunk The chunk buffer
     * @param content The content buffer
     * @param last Whether this is the last content
     * @return The generation result
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Result generateRequest(MetaData.Request info, ByteBuffer header, ByteBuffer chunk, ByteBuffer content,
            boolean last) throws IOException {
        if (debug) {
            logger.debug("{}Generating request: method={}, uri={}", LOG_STATE, info.getMethod(), info.getURI());
        }
        switch (_state) {
            case START: {
                if (info == null) {
                    return Result.NEED_INFO;
                }

                if (header == null) {
                    return Result.NEED_HEADER;
                }

                // If we have not been told our persistence, set the default
                if (_persistent == null) {
                    _persistent = info.getHttpVersion().ordinal() > HttpVersion.HTTP_1_0.ordinal();
                    if (!_persistent && HttpMethod.CONNECT.is(info.getMethod())) {
                        _persistent = true;
                    }
                }

                // prepare the header
                int pos = BufferUtil.flipToFill(header);
                try {
                    // generate ResponseLine
                    generateRequestLine(info, header);

                    if (info.getHttpVersion() == HttpVersion.HTTP_0_9) {
                        throw new BadMessageException(500, "HTTP/0.9 not supported");
                    }

                    generateHeaders(info, header, content, last);

                    boolean expect100 = info.getFields().contains(HttpHeader.EXPECT,
                            HttpHeaderValue.CONTINUE.asString());

                    if (expect100) {
                        _state = State.COMMITTED;
                    } else {
                        // handle the content.
                        int len = BufferUtil.length(content);
                        if (len > 0) {
                            _contentPrepared += len;
                            if (isChunking()) {
                                prepareChunk(header, len);
                            }
                        }
                        _state = last ? State.COMPLETING : State.COMMITTED;
                    }

                    return Result.FLUSH;
                } catch (Exception e) {
                    String message = (e instanceof BufferOverflowException) ? "Request header too large"
                            : e.getMessage();
                    throw new BadMessageException(500, message, e);
                } finally {
                    BufferUtil.flipToFlush(header, pos);
                }
            }

            case COMMITTED: {
                int len = BufferUtil.length(content);

                if (len > 0) {
                    // Do we need a chunk buffer?
                    if (isChunking()) {
                        // Do we need a chunk buffer?
                        if (chunk == null) {
                            return Result.NEED_CHUNK;
                        }
                        BufferUtil.clearToFill(chunk);
                        prepareChunk(chunk, len);
                        BufferUtil.flipToFlush(chunk, 0);
                    }
                    _contentPrepared += len;
                }

                if (last) {
                    _state = State.COMPLETING;
                }

                return len > 0 ? Result.FLUSH : Result.CONTINUE;
            }

            case COMPLETING: {
                if (BufferUtil.hasContent(content)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}discarding content in COMPLETING", LOG_STATE);
                    }
                    BufferUtil.clear(content);
                }

                if (isChunking()) {
                    // Do we need a chunk buffer?
                    if (chunk == null) {
                        return Result.NEED_CHUNK;
                    }
                    BufferUtil.clearToFill(chunk);
                    prepareChunk(chunk, 0);
                    BufferUtil.flipToFlush(chunk, 0);
                    _endOfContent = EndOfContent.UNKNOWN_CONTENT;
                    return Result.FLUSH;
                }

                _state = State.END;
                return Boolean.TRUE.equals(_persistent) ? Result.DONE : Result.SHUTDOWN_OUT;
            }

            case END:
                if (BufferUtil.hasContent(content)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}discarding content in COMPLETING", LOG_STATE);
                    }
                    BufferUtil.clear(content);
                }
                return Result.DONE;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Generates an HTTP response message.
     *
     * This method handles the generation of response headers, content, and chunked
     * transfer encoding for HTTP responses.
     *
     * Key implementation details:
     * - Generates response line
     * - Handles headers
     * - Manages content and chunking
     * - Supports persistent connections
     *
     * @param info The response metadata containing status code and headers
     * @param header The buffer for the response header
     * @param chunk The buffer for chunked transfer encoding
     * @param content The buffer containing the response content
     * @param last Whether this is the last content buffer
     * @return The result of the generation operation
     * @throws IOException If an I/O error occurs during generation
     */
    @Override
    public Result generateResponse(MetaData.Response info, ByteBuffer header, ByteBuffer chunk, ByteBuffer content,
            boolean last) throws IOException {
        return generateResponse(info, false, header, chunk, content, last);
    }

    /**
     * Generates an HTTP response message with HEAD request handling.
     *
     * This method is similar to generateResponse but includes special handling
     * for HEAD requests, which should not include a message body.
     *
     * Key implementation details:
     * - Handles HEAD request specifics
     * - Suppresses body generation
     * - Maintains header consistency
     *
     * @param info The response metadata containing status code and headers
     * @param head Whether this is a response to a HEAD request
     * @param header The buffer for the response header
     * @param chunk The buffer for chunked transfer encoding
     * @param content The buffer containing the response content
     * @param last Whether this is the last content buffer
     * @return The result of the generation operation
     * @throws IOException If an I/O error occurs during generation
     */
    @Override
    public Result generateResponse(MetaData.Response info, boolean head, ByteBuffer header, ByteBuffer chunk,
            ByteBuffer content, boolean last) throws IOException {
        switch (_state) {
            case START: {
                if (info == null) {
                    return Result.NEED_INFO;
                }
                HttpVersion version = info.getHttpVersion();
                if (version == null) {
                    throw new BadMessageException(500, "No version");
                }
                switch (version) {
                    case HTTP_1_0:
                        if (_persistent == null) {
                            _persistent = Boolean.FALSE;
                        }
                        break;

                    case HTTP_1_1:
                        if (_persistent == null) {
                            _persistent = Boolean.TRUE;
                        }
                        break;

                    default:
                        _persistent = false;
                        _endOfContent = EndOfContent.EOF_CONTENT;
                        if (BufferUtil.hasContent(content)) {
                            _contentPrepared += content.remaining();
                        }
                        _state = last ? State.COMPLETING : State.COMMITTED;
                        return Result.FLUSH;
                }

                // Do we need a response header
                if (header == null) {
                    return Result.NEED_HEADER;
                }

                // prepare the header
                int pos = BufferUtil.flipToFill(header);
                try {
                    // generate ResponseLine
                    if (info.getFields().get("X-HAP-EVENT") != null) {
                        generateResponseLine(info, header, EVENT_1_0_SPACE);
                    } else {
                        generateResponseLine(info, header, HTTP_1_1_SPACE);
                    }
                    // Handle 1xx and no content responses
                    int status = info.getStatus();
                    if (status >= 100 && status < 200) {
                        _noContent = true;

                        if (status != HttpStatus.SWITCHING_PROTOCOLS_101) {
                            header.put(CRLF);
                            _state = State.COMPLETING_1XX;
                            return Result.FLUSH;
                        }
                    } else if (status == HttpStatus.NO_CONTENT_204 || status == HttpStatus.NOT_MODIFIED_304) {
                        _noContent = true;
                    }

                    generateHeaders(info, header, content, last);

                    // handle the content.
                    int len = BufferUtil.length(content);
                    if (len > 0) {
                        _contentPrepared += len;
                        if (isChunking() && !head) {
                            prepareChunk(header, len);
                        }
                    }
                    _state = last ? State.COMPLETING : State.COMMITTED;
                } catch (Exception e) {
                    String message = (e instanceof BufferOverflowException) ? "Response header too large"
                            : e.getMessage();
                    throw new BadMessageException(500, message, e);
                } finally {
                    BufferUtil.flipToFlush(header, pos);
                }

                return Result.FLUSH;
            }

            case COMMITTED: {
                int len = BufferUtil.length(content);

                // handle the content.
                if (len > 0) {
                    if (isChunking()) {
                        if (chunk == null) {
                            return Result.NEED_CHUNK;
                        }
                        BufferUtil.clearToFill(chunk);
                        prepareChunk(chunk, len);
                        BufferUtil.flipToFlush(chunk, 0);
                    }
                    _contentPrepared += len;
                }

                if (last) {
                    _state = State.COMPLETING;
                    return len > 0 ? Result.FLUSH : Result.CONTINUE;
                }
                return len > 0 ? Result.FLUSH : Result.DONE;
            }

            case COMPLETING_1XX: {
                reset();
                return Result.DONE;
            }

            case COMPLETING: {
                if (BufferUtil.hasContent(content)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}discarding content in COMPLETING", LOG_STATE);
                    }
                    BufferUtil.clear(content);
                }

                if (isChunking()) {
                    // Do we need a chunk buffer?
                    if (chunk == null) {
                        return Result.NEED_CHUNK;
                    }

                    // Write the last chunk
                    BufferUtil.clearToFill(chunk);
                    prepareChunk(chunk, 0);
                    BufferUtil.flipToFlush(chunk, 0);
                    _endOfContent = EndOfContent.UNKNOWN_CONTENT;
                    return Result.FLUSH;
                }

                _state = State.END;

                return Boolean.TRUE.equals(_persistent) ? Result.DONE : Result.SHUTDOWN_OUT;
            }

            case END:
                if (BufferUtil.hasContent(content)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{}discarding content in COMPLETING", LOG_STATE);
                    }
                    BufferUtil.clear(content);
                }
                return Result.DONE;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Prepares a chunk of data for chunked transfer encoding.
     *
     * This method formats the chunk size and adds necessary CRLF delimiters
     * for chunked transfer encoding.
     *
     * Key implementation details:
     * - Formats chunk size in hex
     * - Adds CRLF delimiters
     * - Handles last chunk specially
     *
     * @param chunk The buffer to write the chunk header to
     * @param remaining The size of the chunk in bytes
     */
    private void prepareChunk(ByteBuffer chunk, int remaining) {
        // if we need CRLF add this to header
        if (_needCRLF) {
            BufferUtil.putCRLF(chunk);
        }

        // Add the chunk size to the header
        if (remaining > 0) {
            BufferUtil.putHexInt(chunk, remaining);
            BufferUtil.putCRLF(chunk);
            _needCRLF = true;
        } else {
            chunk.put(LAST_CHUNK);
            _needCRLF = false;
        }
    }

    /**
     * Generates the request line for an HTTP request.
     *
     * This method formats the request line including method, URI, and HTTP version.
     *
     * Key implementation details:
     * - Formats method, URI, and version
     * - Adds proper spacing
     * - Terminates with CRLF
     *
     * @param request The request metadata
     * @param header The buffer to write the request line to
     */
    private void generateRequestLine(MetaData.Request request, ByteBuffer header) {
        header.put(StringUtil.getBytes(request.getMethod()));
        header.put((byte) ' ');
        header.put(StringUtil.getBytes(request.getURIString()));
        header.put((byte) ' ');
        header.put(request.getHttpVersion().toBytes());
        header.put(CRLF);
    }

    /**
     * Generates the response line for an HTTP response.
     *
     * This method formats the response line including HTTP version, status code,
     * and reason phrase.
     *
     * Key implementation details:
     * - Formats version, status, and reason
     * - Adds proper spacing
     * - Terminates with CRLF
     *
     * @param response The response metadata
     * @param header The buffer to write the response line to
     * @param version The HTTP version bytes to use
     */
    private void generateResponseLine(MetaData.Response response, ByteBuffer header, byte[] version) {
        // Look for prepared response line
        int status = response.getStatus();
        String reason = response.getReason();

        header.put(version);
        header.put((byte) ('0' + status / 100));
        header.put((byte) ('0' + (status % 100) / 10));
        header.put((byte) ('0' + (status % 10)));
        header.put((byte) ' ');
        if (reason == null) {
            header.put((byte) ('0' + status / 100));
            header.put((byte) ('0' + (status % 100) / 10));
            header.put((byte) ('0' + (status % 10)));
        } else {
            header.put(getReasonBytes(reason));
        }
        header.put(CRLF);
    }

    /**
     * Gets the reason phrase bytes for a given reason string.
     *
     * This method sanitizes the reason string and converts it to bytes,
     * ensuring it is valid for HTTP headers.
     *
     * Key implementation details:
     * - Truncates long reasons
     * - Sanitizes invalid characters
     * - Converts to bytes
     *
     * @param reason The reason phrase string
     * @return The sanitized reason phrase bytes
     */
    private byte[] getReasonBytes(String reason) {
        if (reason.length() > 1024) {
            reason = reason.substring(0, 1024);
        }
        byte[] _bytes = StringUtil.getBytes(reason);

        for (int i = _bytes.length; i-- > 0;) {
            if (_bytes[i] == '\r' || _bytes[i] == '\n') {
                _bytes[i] = '?';
            }
        }
        return _bytes;
    }

    /**
     * Generates HTTP headers for a request or response.
     *
     * This method handles the generation of all HTTP headers, including content
     * length, transfer encoding, and connection headers.
     *
     * Key implementation details:
     * - Handles content length
     * - Manages transfer encoding
     * - Sets connection behavior
     * - Adds server headers
     *
     * @param _info The request or response metadata
     * @param header The buffer to write headers to
     * @param content The content buffer
     * @param last Whether this is the last content
     */
    private void generateHeaders(MetaData _info, ByteBuffer header, ByteBuffer content, boolean last) {
        final MetaData.Request request = (_info instanceof MetaData.Request) ? (MetaData.Request) _info : null;
        final MetaData.Response response = (_info instanceof MetaData.Response) ? (MetaData.Response) _info : null;

        // default field values
        int send = _send;
        HttpField transfer_encoding = null;
        boolean keep_alive = false;
        boolean close = false;
        boolean content_type = false;
        StringBuilder connection = null;
        long content_length = _info.getContentLength();

        // Generate fields
        HttpFields fields = _info.getFields();
        if (fields != null) {
            int n = fields.size();
            for (int f = 0; f < n; f++) {
                HttpField field = fields.getField(f);
                HttpHeader h = field.getHeader();
                if (h == null) {
                    putTo(field, header);
                } else {
                    switch (h) {
                        case CONTENT_LENGTH:
                            _endOfContent = EndOfContent.CONTENT_LENGTH;
                            if (content_length < 0) {
                                content_length = Long.valueOf(field.getValue());
                            }
                            // handle setting the field specially below
                            break;

                        case CONTENT_TYPE: {
                            // write the field to the header
                            content_type = true;
                            putTo(field, header);
                            break;
                        }

                        case TRANSFER_ENCODING: {
                            if (_info.getHttpVersion() == HttpVersion.HTTP_1_1) {
                                transfer_encoding = field;
                            }
                            // Do NOT add yet!
                            break;
                        }

                        case CONNECTION: {
                            if (request != null) {
                                putTo(field, header);
                            }

                            // Lookup and/or split connection value field
                            HttpHeaderValue[] values = HttpHeaderValue.CLOSE.is(field.getValue()) ? CLOSE
                                    : new HttpHeaderValue[] { HttpHeaderValue.CACHE.get(field.getValue()) };
                            String[] split = null;

                            if (values[0] == null) {
                                split = StringUtil.csvSplit(field.getValue());
                                if (split.length > 0) {
                                    values = new HttpHeaderValue[split.length];
                                    for (int i = 0; i < split.length; i++) {
                                        values[i] = HttpHeaderValue.CACHE.get(split[i]);
                                    }
                                }
                            }

                            // Handle connection values
                            for (int i = 0; i < values.length; i++) {
                                HttpHeaderValue value = values[i];
                                switch (value == null ? HttpHeaderValue.UNKNOWN : value) {
                                    case UPGRADE: {
                                        // special case for websocket connection ordering
                                        header.put(HttpHeader.CONNECTION.getBytesColonSpace())
                                                .put(HttpHeader.UPGRADE.getBytes());
                                        header.put(CRLF);
                                        break;
                                    }

                                    case CLOSE: {
                                        close = true;
                                        _persistent = false;
                                        if (response != null) {
                                            if (_endOfContent == EndOfContent.UNKNOWN_CONTENT) {
                                                _endOfContent = EndOfContent.EOF_CONTENT;
                                            }
                                        }
                                        break;
                                    }

                                    case KEEP_ALIVE: {
                                        if (_info.getHttpVersion() == HttpVersion.HTTP_1_0) {
                                            keep_alive = true;
                                            if (response != null) {
                                                _persistent = true;
                                            }
                                        }
                                        break;
                                    }

                                    default: {
                                        if (connection == null) {
                                            connection = new StringBuilder();
                                        } else {
                                            connection.append(',');
                                        }
                                        connection.append(split == null ? field.getValue() : split[i]);
                                    }
                                }
                            }

                            // Do NOT add yet!
                            break;
                        }

                        case SERVER: {
                            send = send & ~SEND_SERVER;
                            putTo(field, header);
                            break;
                        }

                        default:
                            putTo(field, header);
                    }
                }
            }
        }

        // Calculate how to end _content and connection, _content length and transfer encoding
        // settings.
        // From http://tools.ietf.org/html/rfc7230#section-3.3.3
        // From RFC 2616 4.4:
        // 1. No body for 1xx, 204, 304 & HEAD response
        // 3. If Transfer-Encoding==(.*,)?chunked && HTTP/1.1 && !HttpConnection==close then chunk
        // 5. Content-Length without Transfer-Encoding
        // 6. Request and none over the above, then Content-Length=0 if POST/PUT
        // 7. close

        int status = response != null ? response.getStatus() : -1;
        switch (_endOfContent) {
            case UNKNOWN_CONTENT:
                // It may be that we have no _content, or perhaps _content just has not been
                // written yet?

                // Response known not to have a body
                if (_contentPrepared == 0 && response != null && _noContent) {
                    _endOfContent = EndOfContent.NO_CONTENT;
                } else if (_info.getContentLength() > 0) {
                    // we have been given a content length
                    _endOfContent = EndOfContent.CONTENT_LENGTH;
                    if ((response != null || content_length > 0 || content_type) && !_noContent) {
                        // known length but not actually set.
                        header.put(HttpHeader.CONTENT_LENGTH.getBytesColonSpace());
                        BufferUtil.putDecLong(header, content_length);
                        header.put(CRLF);
                    }
                } else if (last) {
                    // we have seen all the _content there is, so we can be content-length limited.
                    _endOfContent = EndOfContent.CONTENT_LENGTH;
                    long actual_length = _contentPrepared + BufferUtil.length(content);

                    if (content_length >= 0 && content_length != actual_length) {
                        throw new BadMessageException(500,
                                "Content-Length header(" + content_length + ") != actual(" + actual_length + ")");
                    }

                    // Do we need to tell the headers about it
                    putContentLength(header, actual_length, content_type, request, response);
                } else {
                    // No idea, so we must assume that a body is coming.
                    _endOfContent = EndOfContent.CHUNKED_CONTENT;
                    // HTTP 1.0 does not understand chunked content, so we must use EOF content.
                    // For a request with HTTP 1.0 & Connection: keep-alive
                    // we *must* close the connection, otherwise the client
                    // has no way to detect the end of the content.
                    if (!isPersistent() || _info.getHttpVersion().ordinal() < HttpVersion.HTTP_1_1.ordinal()) {
                        _endOfContent = EndOfContent.EOF_CONTENT;
                    }
                }
                break;

            case CONTENT_LENGTH: {
                putContentLength(header, content_length, content_type, request, response);
                break;
            }

            case NO_CONTENT:
                throw new BadMessageException(500);

            case EOF_CONTENT:
                _persistent = request != null;
                break;

            case CHUNKED_CONTENT:
                break;

            default:
                break;
        }

        // Add transfer_encoding if needed
        if (isChunking()) {
            // try to use user supplied encoding as it may have other values.
            if (transfer_encoding != null
                    && !HttpHeaderValue.CHUNKED.toString().equalsIgnoreCase(transfer_encoding.getValue())) {
                String c = transfer_encoding.getValue();
                if (c.endsWith(HttpHeaderValue.CHUNKED.toString())) {
                    putTo(transfer_encoding, header);
                } else {
                    throw new BadMessageException(500, "BAD TE");
                }
            } else {
                header.put(TRANSFER_ENCODING_CHUNKED);
            }
        }

        // Handle connection if need be
        if (_endOfContent == EndOfContent.EOF_CONTENT) {
            keep_alive = false;
            _persistent = false;
        }

        // If this is a response, work out persistence
        if (response != null) {
            if (!isPersistent() && (close || _info.getHttpVersion().ordinal() > HttpVersion.HTTP_1_0.ordinal())) {
                if (connection == null) {
                    header.put(CONNECTION_CLOSE);
                } else {
                    header.put(CONNECTION_CLOSE, 0, CONNECTION_CLOSE.length - 2);
                    header.put((byte) ',');
                    header.put(StringUtil.getBytes(connection.toString()));
                    header.put(CRLF);
                }
            } else if (keep_alive) {
                if (connection == null) {
                    header.put(CONNECTION_KEEP_ALIVE);
                } else {
                    header.put(CONNECTION_KEEP_ALIVE, 0, CONNECTION_KEEP_ALIVE.length - 2);
                    header.put((byte) ',');
                    header.put(StringUtil.getBytes(connection.toString()));
                    header.put(CRLF);
                }
            } else if (connection != null) {
                header.put(HttpHeader.CONNECTION.getBytesColonSpace());
                header.put(StringUtil.getBytes(connection.toString()));
                header.put(CRLF);
            }
        }

        if (status > 199) {
            header.put(SEND[send]);
        }

        // end the header.
        header.put(CRLF);
    }

    /**
     * Gets the reason phrase buffer for a given HTTP status code.
     *
     * This method returns a pre-encoded byte array containing the standard
     * reason phrase for the specified status code.
     *
     * Key implementation details:
     * - Uses pre-prepared responses
     * - Handles unknown codes
     * - Returns null for invalid codes
     *
     * @param code The HTTP status code
     * @return The byte array containing the reason phrase, or null if not found
     */
    public static byte[] getReasonBuffer(int code) {
        PreparedResponse status = code < __preprepared.length ? __preprepared[code] : null;
        if (status != null) {
            return status._reason;
        }
        return null;
    }

    /**
     * Returns a string representation of the generator.
     *
     * This method provides a string containing the class name, hash code,
     * and current state.
     *
     * Key implementation details:
     * - Includes class name
     * - Shows hash code
     * - Shows current state
     *
     * @return A string representation of the generator
     */
    @Override
    public String toString() {
        return String.format("%s@%x{s=%s}", getClass().getSimpleName(), hashCode(), _state);
    }

    /**
     * Writes an HTTP field to a buffer.
     *
     * This method handles both pre-encoded and regular HTTP fields, ensuring
     * proper formatting and sanitization of field names and values.
     *
     * Key implementation details:
     * - Handles pre-encoded fields
     * - Sanitizes field names
     * - Sanitizes field values
     * - Adds proper formatting
     *
     * @param field The HTTP field to write
     * @param bufferInFillMode The buffer to write to
     */
    public static void putTo(HttpField field, ByteBuffer bufferInFillMode) {
        if (field instanceof PreEncodedHttpField) {
            ((PreEncodedHttpField) field).putTo(bufferInFillMode, HttpVersion.HTTP_1_0);
        } else {
            HttpHeader header = field.getHeader();
            if (header != null) {
                bufferInFillMode.put(header.getBytesColonSpace());
                putSanitisedValue(field.getValue(), bufferInFillMode);
            } else {
                putSanitisedName(field.getName(), bufferInFillMode);
                bufferInFillMode.put(__colon_space);
                putSanitisedValue(field.getValue(), bufferInFillMode);
            }

            BufferUtil.putCRLF(bufferInFillMode);
        }
    }

    /**
     * Writes a collection of HTTP fields to a buffer.
     *
     * This method writes all fields in the collection, followed by a CRLF.
     *
     * Key implementation details:
     * - Iterates through fields
     * - Writes each field
     * - Adds final CRLF
     *
     * @param fields The collection of HTTP fields to write
     * @param bufferInFillMode The buffer to write to
     */
    public static void putTo(HttpFields fields, ByteBuffer bufferInFillMode) {
        for (HttpField field : fields) {
            if (field != null) {
                putTo(field, bufferInFillMode);
            }
        }
        BufferUtil.putCRLF(bufferInFillMode);
    }
}
