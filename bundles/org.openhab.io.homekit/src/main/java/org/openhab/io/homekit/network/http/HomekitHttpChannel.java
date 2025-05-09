package org.openhab.io.homekit.network.http;

import org.eclipse.jetty.client.http.HttpChannelOverHTTP;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.client.http.HttpReceiverOverHTTP;
import org.eclipse.jetty.client.http.HttpSenderOverHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitHttpChannel extends HttpChannelOverHTTP {

    protected static final Logger logger = LoggerFactory.getLogger(HomekitHttpChannel.class);
    protected static final String LOG_PREFIX = "Homekit HttpChannelOverHTTP: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_STATE = LOG_PREFIX + "State - ";
    protected static final String LOG_CONFIG = LOG_PREFIX + "Config - ";
    protected static final String LOG_ACCESSORY = LOG_PREFIX + "HomekitAccessory - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    protected static final String LOG_WARN = LOG_PREFIX + "Warning - ";

    protected byte[] decryptionKey;
    protected byte[] encryptionKey;

    public HomekitHttpChannel(HttpConnectionOverHTTP connection) {
        super(connection);
    }

    @Override
    protected HttpSenderOverHTTP newHttpSender() {
        return new HomekitHttpSender(this);
    }

    @Override
    protected HttpReceiverOverHTTP newHttpReceiver() {
        return new HomekitHttpReceiver(this);
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
    protected HomekitHttpReceiver getHttpReceiver() {
        return (HomekitHttpReceiver) super.getHttpReceiver();
    }

    @Override
    protected HomekitHttpSender getHttpSender() {
        return (HomekitHttpSender) super.getHttpSender();
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

        HttpSenderOverHTTP sender = this.getHttpSender();

        if (sender instanceof HomekitHttpSender) {
            ((HomekitHttpSender) sender).setEncryptionKey(encryptionKey);
        }

        HttpReceiverOverHTTP receiver = this.getHttpReceiver();

        if (receiver instanceof HomekitHttpReceiver) {
            ((HomekitHttpReceiver) receiver).setDecryptionKey(decryptionKey);
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
