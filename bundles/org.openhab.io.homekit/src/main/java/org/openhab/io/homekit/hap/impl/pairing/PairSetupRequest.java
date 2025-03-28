package org.openhab.io.homekit.hap.impl.pairing;

import java.math.BigInteger;

import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.DecodeResult;

abstract class PairSetupRequest {

    private static final short VALUE_STAGE_1 = 1;
    private static final short VALUE_STAGE_2 = 3;
    private static final short VALUE_STAGE_3 = 5;

    public static PairSetupRequest of(byte[] content) throws Exception {
        org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d = TypeLengthValue.decode(content);
        short stage = d.getByte(Message.STATE);
        switch (stage) {
            case VALUE_STAGE_1:
                return new Stage1Request();

            case VALUE_STAGE_2:
                return new Stage2Request(d);

            case VALUE_STAGE_3:
                return new Stage3Request(d);

            default:
                throw new Exception("Unknown pair process stage: " + stage);
        }
    }

    public abstract Stage getStage();

    public static class Stage1Request extends PairSetupRequest {
        @Override
        public Stage getStage() {
            return Stage.ONE;
        }
    }

    public static class Stage2Request extends PairSetupRequest {

        private final BigInteger a;
        private final BigInteger m1;

        public Stage2Request(DecodeResult d) {
            a = d.getBigInt(Message.PUBLIC_KEY);
            m1 = d.getBigInt(Message.PROOF);
        }

        public BigInteger getA() {
            return a;
        }

        public BigInteger getM1() {
            return m1;
        }

        @Override
        public Stage getStage() {
            return Stage.TWO;
        }
    }

    static class Stage3Request extends PairSetupRequest {

        private final byte[] messageData;
        private final byte[] authTagData;

        public Stage3Request(DecodeResult d) {
            messageData = new byte[d.getLength(Message.ENCRYPTED_DATA) - 16];
            authTagData = new byte[16];
            d.getBytes(Message.ENCRYPTED_DATA, messageData, 0);
            d.getBytes(Message.ENCRYPTED_DATA, authTagData, messageData.length);
        }

        public byte[] getMessageData() {
            return messageData;
        }

        public byte[] getAuthTagData() {
            return authTagData;
        }

        @Override
        public Stage getStage() {
            return Stage.THREE;
        }
    }
}
