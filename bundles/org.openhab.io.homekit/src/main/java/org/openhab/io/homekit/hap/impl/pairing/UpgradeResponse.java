package org.openhab.io.homekit.hap.impl.pairing;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeResponse extends PairingResponse {

    protected static final Logger logger = LoggerFactory.getLogger(UpgradeResponse.class);

    private final byte[] readKey;
    private final byte[] writeKey;

    UpgradeResponse(byte[] body, byte[] readKey, byte[] writeKey) {
        super(body);
        this.readKey = readKey;
        logger.info("UpgradeResponse : Read Key is {}", byteToHexString(readKey));

        this.writeKey = writeKey;

        logger.info("UpgradeResponse : Write Key is {}", byteToHexString(readKey));
    }

    @Override
    public boolean doUpgrade() {
        return true;
    }

    public ByteBuffer getReadKey() {
        return ByteBuffer.wrap(readKey);
    }

    public ByteBuffer getWriteKey() {
        return ByteBuffer.wrap(writeKey);
    }

    protected static String byteToHexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
