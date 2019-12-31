package org.openhab.io.homekit.hap.impl.pairing;

import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;

abstract class PairVerificationRequest {

    private static final short VALUE_STAGE_1 = 1;
    private static final short VALUE_STAGE_2 = 3;

    static PairVerificationRequest of(byte[] content) throws Exception {
        org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d = TypeLengthValue.decode(content);
        short stage = d.getByte(Message.STATE);
        switch (stage) {
            case VALUE_STAGE_1:
                return new Stage1Request(d);

            case VALUE_STAGE_2:
                return new Stage2Request(d);

            default:
                throw new Exception("Unknown pair process stage: " + stage);
        }
    }

    abstract Stage getStage();

    static class Stage1Request extends PairVerificationRequest {

        private final byte[] clientPublicKey;

        public Stage1Request(org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d) {
            clientPublicKey = d.getBytes(Message.PUBLIC_KEY);
        }

        public byte[] getClientPublicKey() {
            return clientPublicKey;
        }

        @Override
        Stage getStage() {
            return Stage.ONE;
        }
    }

    static class Stage2Request extends PairVerificationRequest {

        private final byte[] messageData;
        private final byte[] authTagData;

        public Stage2Request(org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d) {
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
            return Stage.TWO;
        }
    }
}
