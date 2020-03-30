package org.openhab.io.homekit.internal.http.jetty;

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

    private byte[] decryptionKey;
    private byte[] encryptionKey;

    public HomekitHttpConnectionOverHTTP(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise) {
        super(endPoint, destination, promise);
    }

    @Override
    protected HomekitHttpChannelOverHTTP newHttpChannel() {
        return new HomekitHttpChannelOverHTTP(this);
    }

    @Override
    public long getMessagesIn() {
        return ((HomekitHttpChannelOverHTTP) getHttpChannel()).getMessagesIn();
    }

    @Override
    public long getMessagesOut() {
        return ((HomekitHttpChannelOverHTTP) getHttpChannel()).getMessagesOut();
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
        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        logger.info("Setting Encryption Keys on {}", this);

        HttpChannelOverHTTP channel = this.getHttpChannel();

        if (channel instanceof HomekitHttpChannelOverHTTP) {
            ((HomekitHttpChannelOverHTTP) channel).setEncryptionKeys(decryptionKey, encryptionKey);
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
