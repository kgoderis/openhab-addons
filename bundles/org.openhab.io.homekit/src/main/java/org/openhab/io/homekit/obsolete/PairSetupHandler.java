package org.openhab.io.homekit.obsolete;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.srp6.SRP6CryptoParams;

public abstract class PairSetupHandler extends BaseHandler {

    protected static final Logger logger = LoggerFactory.getLogger(PairSetupHandler.class);

    protected static final BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    protected static final BigInteger G = BigInteger.valueOf(5);
    protected static final String IDENTIFIER = "Pair-Setup";
    protected final SRP6CryptoParams config;

    public PairSetupHandler(AccessoryServer server) {
        super(server);

        config = new SRP6CryptoParams(N_3072, G, "SHA-512");
    }

    protected short getStage(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getByte(Message.STATE);
    }

    protected BigInteger getA(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBigInt(Message.PUBLIC_KEY);
    }

    protected BigInteger getM1(byte[] content) throws IOException {
        DecodeResult d = TypeLengthValue.decode(content);
        return d.getBigInt(Message.PROOF);
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

    protected static byte[] bigIntegerToUnsignedByteArray(BigInteger i) {
        byte[] array = i.toByteArray();
        if (array[0] == 0) {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }

    // TODO : Do this in handle() of this class, or add special handler at the end of the chain to set the headers?
    // response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
    // response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    // super.setProtocolVersion(version);
    // homekitResponse.getVersion() == HttpResponse.HttpVersion.EVENT_1_0
    // ? EVENT_VERSION
    // : HttpVersion.HTTP_1_1,

}
