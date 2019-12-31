package org.openhab.io.homekit.internal.http.netty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.jetty.util.BufferUtil;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine;
import org.openhab.io.homekit.crypto.HomekitEncryptionEngine.SequenceBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

public class HomekitEncryptionHandler extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(HomekitEncryptionHandler.class);

    private boolean started = false;
    private final byte[] readKey;
    private final byte[] writeKey;
    private long inboundSequenceCount = 0;
    private long outboundSequenceCount = 0;

    private int bufferSize = 2048;

    public HomekitEncryptionHandler(byte[] readKey, byte[] writeKey) {
        this.readKey = readKey;
        this.writeKey = writeKey;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (started) {
            debugData("Encoding data", msg, ctx);

            byte[] b = new byte[msg.readableBytes()];
            msg.readBytes(b);

            SequenceBuffer sBuffer = HomekitEncryptionEngine.encryptBuffer(ByteBuffer.allocateDirect(bufferSize),
                    ByteBuffer.wrap(b), readKey, outboundSequenceCount);
            ByteBuffer encryptedBuffer = sBuffer.buffer;
            outboundSequenceCount = sBuffer.sequenceNumber;
            // BufferUtil.flipToFlush(encryptedBuffer, 0);

            out.writeBytes(encryptedBuffer);
        } else {
            out.writeBytes(msg);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] b = new byte[in.readableBytes()];
        in.readBytes(b);

        SequenceBuffer sBuffer = HomekitEncryptionEngine.decryptBuffer(ByteBuffer.allocateDirect(bufferSize),
                ByteBuffer.wrap(b), writeKey, inboundSequenceCount);
        ByteBuffer plaintextBuffer = sBuffer.buffer;
        inboundSequenceCount = sBuffer.sequenceNumber;

        logger.debug("{}", BufferUtil.toDetailString(plaintextBuffer));

        // BufferUtil.flipToFlush(plaintextBuffer, 0);
        ByteBuf outBuf = Unpooled.copiedBuffer(plaintextBuffer);

        debugData("Decoded data", outBuf, ctx);
        out.add(outBuf);
        started = true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        boolean errorLevel = !(cause instanceof IOException);
        if (errorLevel) {
            logger.error("Exception in HomekitEncryptionHandler : ", cause);
        } else {
            logger.debug("Exception in HomekitEncryptionHandler : ", cause);
        }
        super.exceptionCaught(ctx, cause);
    }

    private void debugData(String msg, ByteBuf b, ChannelHandlerContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s [%s]:%n%s", msg, ctx.channel().remoteAddress().toString(),
                    b.toString(StandardCharsets.UTF_8)));
        }
    }
}
