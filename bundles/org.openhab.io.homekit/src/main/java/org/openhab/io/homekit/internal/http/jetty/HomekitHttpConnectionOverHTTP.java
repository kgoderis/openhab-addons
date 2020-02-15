package org.openhab.io.homekit.internal.http.jetty;

import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.http.HttpConnectionOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Promise;

public class HomekitHttpConnectionOverHTTP extends HttpConnectionOverHTTP {

    // private static final Logger LOG = Log.getLogger(HomekitHttpConnectionOverHTTP.class);

    // private final AtomicBoolean closed = new AtomicBoolean();
    // private final AtomicInteger sweeps = new AtomicInteger();
    // private final Promise<Connection> promise;
    // private final HomekitHttpChannelOverHTTP channel;
    // private long idleTimeout;

    public HomekitHttpConnectionOverHTTP(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise) {
        super(endPoint, destination, promise);
        // this.promise = promise;
        // this.channel = newHttpChannel();
    }

    @Override
    protected HomekitHttpChannelOverHTTP newHttpChannel() {
        return new HomekitHttpChannelOverHTTP(this);
    }

    // @Override
    // public HomekitHttpChannelOverHTTP getHttpChannel() {
    // return channel;
    // }

    @Override
    public long getMessagesIn() {
        return ((HomekitHttpChannelOverHTTP) getHttpChannel()).getMessagesIn();
    }

    @Override
    public long getMessagesOut() {
        return ((HomekitHttpChannelOverHTTP) getHttpChannel()).getMessagesOut();
    }

    @Override
    protected void addBytesIn(long bytesIn) {
        super.addBytesIn(bytesIn);
    }

    @Override
    public void close(Throwable failure) {
        super.close(failure);
    }

    // @Override
    // public void onOpen() {
    // super.onOpen();
    // fillInterested();
    // promise.succeeded(this);
    // }
    //
    // @Override
    // public boolean isClosed() {
    // return closed.get();
    // }

    // @Override
    // public void onFillable() {
    // HttpExchange exchange = channel.getHttpExchange();
    // if (exchange != null) {
    // channel.receive();
    // } else {
    // // If there is no exchange, then could be either a remote close,
    // // or garbage bytes; in both cases we close the connection
    // close();
    // }
    // }

    // @Override
    // public ByteBuffer onUpgradeFrom() {
    // HomekitHttpReceiverOverHTTP receiver = channel.getHttpReceiver();
    // return receiver.onUpgradeFrom();
    // }

    //
    // @Override
    // protected void close(Throwable failure) {
    // if (closed.compareAndSet(false, true)) {
    // getHttpDestination().close(this);
    // abort(failure);
    // channel.destroy();
    // getEndPoint().shutdownOutput();
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("Shutdown {}", this);
    // }
    // getEndPoint().close();
    // if (LOG.isDebugEnabled()) {
    // LOG.debug("Closed {}", this);
    // }
    // }
    // }
    //
    // @Override
    // protected boolean abort(Throwable failure) {
    // HttpExchange exchange = channel.getHttpExchange();
    // return exchange != null && exchange.getRequest().abort(failure);
    // }

    // @Override
    // public boolean sweep() {
    // if (!closed.get()) {
    // return false;
    // }
    // return sweeps.incrementAndGet() >= 4;
    // }

    //
    // private class Delegate extends HttpConnection {
    // private Delegate(HttpDestination destination) {
    // super(destination);
    // }
    //
    // @Override
    // protected SendFailure send(HttpExchange exchange) {
    // Request request = exchange.getRequest();
    // normalizeRequest(request);
    //
    // // Save the old idle timeout to restore it.
    // EndPoint endPoint = getEndPoint();
    // idleTimeout = endPoint.getIdleTimeout();
    // long requestIdleTimeout = request.getIdleTimeout();
    // if (requestIdleTimeout >= 0) {
    // endPoint.setIdleTimeout(requestIdleTimeout);
    // }
    //
    // // One channel per connection, just delegate the send.
    // return send(channel, exchange);
    // }
    //
    // @Override
    // public void close() {
    // HomekitHttpConnectionOverHTTP.this.close();
    // }
    //
    // @Override
    // public boolean isClosed() {
    // return HomekitHttpConnectionOverHTTP.this.isClosed();
    // }
    //
    // @Override
    // public String toString() {
    // return HomekitHttpConnectionOverHTTP.this.toString();
    // }
    // }

}
