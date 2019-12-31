package org.openhab.io.homekit.hap.impl.http.impl;

import java.util.Iterator;
import java.util.List;

import org.openhab.io.homekit.internal.http.netty.ServletBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpResponseEncoderAggregate extends HttpResponseEncoder {

    private static final Logger logger = LoggerFactory.getLogger(ServletBridgeHandler.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        super.encode(ctx, msg, out);

        logger.debug("ctx={} msg={}", ctx.toString(), msg.toString());

        if (out.size() > 0) {
            Iterator<Object> i = out.iterator();
            ByteBuf b = (ByteBuf) i.next();
            while (i.hasNext()) {
                logger.debug("writeBytes");
                b.writeBytes((ByteBuf) i.next());
                i.remove();
            }
        }
    }
}
