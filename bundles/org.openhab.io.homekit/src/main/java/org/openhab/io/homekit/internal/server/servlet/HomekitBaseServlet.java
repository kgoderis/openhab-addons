package org.openhab.io.homekit.internal.server.servlet;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import org.openhab.io.homekit.api.hap.HomekitAccessoryServer;
import org.openhab.io.homekit.api.hap.HomekitMessage;
import org.openhab.io.homekit.internal.http.HomekitServletConfig;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;

@SuppressWarnings("serial")
public abstract class HomekitBaseServlet extends HttpServlet {

    HomekitAccessoryServer server;

    public HomekitBaseServlet() {
    }

    public HomekitBaseServlet(HomekitAccessoryServer server) {
        this.server = server;
    }

    @Override
    public void init(ServletConfig config) {
        if (config instanceof HomekitServletConfig) {
            this.server = (HomekitAccessoryServer) ((HomekitServletConfig) config).getAccessoryServer();
        }
    }

    protected short getState(byte[] content) throws IOException {
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        return d.getByte(HomekitMessage.STATE);
    }

    protected byte[] getMessageData(byte[] content) throws IOException {
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        byte[] messageData = new byte[d.getLength(HomekitMessage.ENCRYPTED_DATA) - 16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, messageData, 0);
        return messageData;
    }

    protected byte[] getAuthTagData(byte[] content) throws IOException {
        DecodeResult d = HomekitTypeLengthValueEncoderDecoder.decode(content);
        byte[] messageData = getMessageData(content);
        byte[] authTagData = new byte[16];
        d.getBytes(HomekitMessage.ENCRYPTED_DATA, authTagData, messageData.length);
        return authTagData;
    }

    protected static byte[] bigIntegerToUnsignedByteArray(BigInteger i) {
        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }
}
