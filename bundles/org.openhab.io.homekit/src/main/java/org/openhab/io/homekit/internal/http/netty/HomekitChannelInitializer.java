package org.openhab.io.homekit.internal.http.netty;

import org.openhab.io.homekit.hap.impl.http.impl.HttpResponseEncoderAggregate;
import org.openhab.io.homekit.hap.impl.http.impl.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;

public class HomekitChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger log = LoggerFactory.getLogger(HomekitChannelInitializer.class);

    private static final int MAX_POST = 1000000;

    private final DefaultEventExecutor eventExecutor = new DefaultEventExecutor();
    private final ChannelHandler idleStateHandler = new IdleStateHandler(60, 30, 0);
    private final ChannelGroup allChannels = new DefaultChannelGroup(eventExecutor);
    private final ServletBridgeHttpSessionStore sessionStore = new DefaultServletBridgeHttpSessionStore();
    private final Timer timer = new HashedWheelTimer();;
    private HttpSessionWatchdog watchdog;

    public HomekitChannelInitializer(WebappConfiguration config) {
        ServletBridgeWebapp webapp = ServletBridgeWebapp.get();

        webapp.init(config, allChannels);
        new Thread(this.watchdog = new HttpSessionWatchdog()).start();
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        log.debug("initChannel");

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = ch.pipeline();

        // pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast(new LoggingHandler());
        pipeline.addLast("responseAggregator", new HttpResponseEncoderAggregate());
        // pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(MAX_POST));

        // Remove the following line if you don't want automatic content
        // compression.
        // pipeline.addLast("deflater", new HttpContentCompressor());
        // pipeline.addLast("idle", new IdleStateHandler(60, 30, 0));

        ServletBridgeHandler bridge = new ServletBridgeHandler();
        bridge.addInterceptor(new ChannelInterceptor());
        bridge.addInterceptor(new HttpSessionInterceptor(sessionStore));

        pipeline.addLast("handler", bridge);
    }

    public void shutdown() {
        this.watchdog.stopWatching();
        ServletBridgeWebapp.get().destroy();
        this.timer.stop();
        this.allChannels.close().awaitUninterruptibly();
    }

    private class HttpSessionWatchdog implements Runnable {

        private boolean shouldStopWatching = false;

        @Override
        public void run() {

            while (!shouldStopWatching) {

                try {
                    if (sessionStore != null) {
                        sessionStore.destroyInactiveSessions();
                    }
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    return;
                }

            }

        }

        public void stopWatching() {
            this.shouldStopWatching = true;
        }

    }

}
