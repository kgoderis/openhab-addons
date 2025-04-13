package org.openhab.io.homekit.internal.server.servlet;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Message;
import org.openhab.io.homekit.internal.http.HomekitServletConfig;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.TypeLengthValueEncoderDecoder.DecodeResult;

@SuppressWarnings("serial")
public abstract class BaseServlet extends HttpServlet {

    AccessoryServer server;

    public BaseServlet() {
    }

    public BaseServlet(AccessoryServer server) {
        this.server = server;
    }

    @Override
    public void init(ServletConfig config) {
        if (config instanceof HomekitServletConfig) {
            this.server = (AccessoryServer) ((HomekitServletConfig) config).getAccessoryServer();
        }
    }

    protected short getState(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValueEncoderDecoder.decode(content);
        return d.getByte(Message.STATE);
    }

    protected byte[] getMessageData(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValueEncoderDecoder.decode(content);
        byte[] messageData = new byte[d.getLength(Message.ENCRYPTED_DATA) - 16];
        d.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
        return messageData;
    }

    protected byte[] getAuthTagData(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValueEncoderDecoder.decode(content);
        byte[] messageData = getMessageData(content);
        byte[] authTagData = new byte[16];
        d.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);
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
