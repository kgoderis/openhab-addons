package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.client.http.HttpReceiverOverHTTP;
import org.eclipse.jetty.client.http.HttpSenderOverHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpChannelOverHTTP extends HttpChannelOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpChannelOverHTTP.class);

    protected byte[] decryptionKey;
    protected byte[] encryptionKey;

    public HomekitHttpChannelOverHTTP(HttpConnectionOverHTTP connection) {
        super(connection);
    }

    @Override
    protected HttpSenderOverHTTP newHttpSender() {
        return new HomekitHttpSenderOverHTTP(this);
    }

    @Override
    protected HttpReceiverOverHTTP newHttpReceiver() {
        return new HomekitHttpReceiverOverHTTP(this);
    }

    @Override
    public long getMessagesIn() {
        return super.getMessagesIn();
    }

    @Override
    public long getMessagesOut() {
        return super.getMessagesOut();
    }

    @Override
    protected HomekitHttpReceiverOverHTTP getHttpReceiver() {
        return (HomekitHttpReceiverOverHTTP) super.getHttpReceiver();
    }

    @Override
    protected HomekitHttpSenderOverHTTP getHttpSender() {
        return (HomekitHttpSenderOverHTTP) super.getHttpSender();
    }

    public void setEncryptionKeys(byte[] decryptionKey, byte[] encryptionKey) {
        this.decryptionKey = decryptionKey;
        this.encryptionKey = encryptionKey;

        logger.info("Setting Encryption Keys on {}", this);

        HttpSenderOverHTTP sender = this.getHttpSender();

        if (sender instanceof HomekitHttpSenderOverHTTP) {
            ((HomekitHttpSenderOverHTTP) sender).setEncryptionKey(encryptionKey);
        }

        HttpReceiverOverHTTP receiver = this.getHttpReceiver();

        if (receiver instanceof HomekitHttpReceiverOverHTTP) {
            ((HomekitHttpReceiverOverHTTP) receiver).setDecryptionKey(decryptionKey);
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
