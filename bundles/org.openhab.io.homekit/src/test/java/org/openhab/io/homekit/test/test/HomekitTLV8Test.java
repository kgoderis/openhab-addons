package org.openhab.io.homekit.test.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.openhab.io.homekit.protocol.message.HomekitMessage;
import org.openhab.io.homekit.protocol.method.HomekitMethod;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.DecodeResult;
import org.openhab.io.homekit.util.HomekitTypeLengthValueEncoderDecoder.Encoder;

public class HomekitTLV8Test {

    @Test
    void testPairingMessage() {
        // Test encoding
        Encoder encoder = HomekitTypeLengthValueEncoderDecoder.getEncoder();
        encoder.add(HomekitMessage.STATE, (short) 1);
        encoder.add(HomekitMessage.METHOD, HomekitMethod.PAIR_SETUP_WITH_AUTH.getKey());
        byte[] encoded = encoder.toByteArray();

        // Test decoding
        DecodeResult result;
        try {
            result = HomekitTypeLengthValueEncoderDecoder.decode(encoded);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode TLV8 data", e);
        }
        assertEquals(1, result.getByte(HomekitMessage.STATE));
        assertEquals(HomekitMethod.PAIR_SETUP_WITH_AUTH.getKey(), result.getByte(HomekitMessage.METHOD));
    }
}
