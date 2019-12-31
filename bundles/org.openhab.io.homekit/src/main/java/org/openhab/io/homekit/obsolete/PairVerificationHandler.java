package org.openhab.io.homekit.obsolete;

import java.io.IOException;

import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PairVerificationHandler extends BaseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(PairSetupHandler.class);

    public PairVerificationHandler(AccessoryServer server) {
        super(server);
    }

    protected short getStage(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getByte(Message.STATE);
    }

    public byte[] getClientPublicKey(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBytes(Message.PUBLIC_KEY);
    }

    protected byte[] getMessageData(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        byte[] messageData = new byte[d.getLength(Message.ENCRYPTED_DATA) - 16];
        d.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        return messageData;
    }

    protected byte[] getAuthTagData(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        byte[] messageData = getMessageData(content);
        byte[] authTagData = new byte[16];
        d.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);
        return authTagData;
    }

}
