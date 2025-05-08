package org.openhab.io.homekit.internal.http;

import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpConnectionOverHTTP extends HttpConnectionOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpConnectionOverHTTP.class);
    protected static final String LOG_PREFIX = "Homekit HttpConnectionOverHTTP: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    private byte[] decryptionKey;
    private byte[] encryptionKey;

    public HomekitHttpConnectionOverHTTP(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise) {
        super(endPoint, destination, promise);
    }

    @Override
    protected HomekitHttpChannel newHttpChannel() {
        return new HomekitHttpChannel(this);
    }

    @Override
    public long getMessagesIn() {
        return ((HomekitHttpChannel) getHttpChannel()).getMessagesIn();
    }

    @Override
    public long getMessagesOut() {
        return ((HomekitHttpChannel) getHttpChannel()).getMessagesOut();
    }

    @Override
    protected void addBytesIn(long bytesIn) {
        super.addBytesIn(bytesIn);
    }

    @Override
    public long getBytesOut() {
        return super.getBytesOut();
    }

    @Override
    protected void addBytesOut(long bytesOut) {
        super.addBytesOut(bytesOut);
    }

    @Override
    public void close(Throwable failure) {
        super.close(failure);
    }

    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        logger.debug("{}setEncryptionKeys called for {}", LOG_CONFIG, this);
        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        logger.info("{}Setting Encryption Keys on {}", LOG_CONFIG, this);
        if (logger.isTraceEnabled()) {
            logger.trace("{}DecryptionKey: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(decryptionKey));
            logger.trace("{}EncryptionKey: {}", LOG_CONFIG,
                    javax.xml.bind.DatatypeConverter.printHexBinary(encryptionKey));
        }

        HttpChannelOverHTTP channel = this.getHttpChannel();

        if (channel instanceof HomekitHttpChannel) {
            ((HomekitHttpChannel) channel).setEncryptionKeys(decryptionKey, encryptionKey);
        }
    }

    public boolean hasEncryptionKeys() {
        return (decryptionKey != null && encryptionKey != null);
    }

    public byte[] getDecryptionKey() {
        return decryptionKey;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }
}
