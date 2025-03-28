package org.openhab.io.homekit.hap.impl;

import  org.openhab.io.homekit.hap.impl.http.HomekitClientConnectionFactory;
import java.util.concurrent.CompletableFuture;

public interface HomekitWebHandler {

  CompletableFuture<Integer> start(HomekitClientConnectionFactory clientConnectionFactory);

  void stop();

  void resetConnections();
}
