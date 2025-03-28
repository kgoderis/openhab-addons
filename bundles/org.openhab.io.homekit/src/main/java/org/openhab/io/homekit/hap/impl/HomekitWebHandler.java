package org.openhab.io.homekit.hap.impl;

import java.util.concurrent.CompletableFuture;

import org.openhab.io.homekit.hap.impl.http.HomekitClientConnectionFactory;

public interface HomekitWebHandler {

    CompletableFuture<Integer> start(HomekitClientConnectionFactory clientConnectionFactory);

    void stop();

    void resetConnections();
}
