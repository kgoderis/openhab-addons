package org.openhab.io.homekit.hap.impl.connections;

import  org.openhab.io.homekit.hap.HomekitAuthInfo;
import  org.openhab.io.homekit.hap.impl.HomekitRegistry;
import  org.openhab.io.homekit.hap.impl.http.HomekitClientConnection;
import  org.openhab.io.homekit.hap.impl.http.HomekitClientConnectionFactory;
import  org.openhab.io.homekit.hap.impl.http.HttpResponse;
import  org.openhab.io.homekit.hap.impl.jmdns.JmdnsHomekitAdvertiser;
import java.util.function.Consumer;

public class HomekitClientConnectionFactoryImpl implements HomekitClientConnectionFactory {

  private final HomekitAuthInfo authInfo;
  private final HomekitRegistry registry;
  private final SubscriptionManager subscriptions;
  private final JmdnsHomekitAdvertiser advertiser;

  public HomekitClientConnectionFactoryImpl(
      HomekitAuthInfo authInfo,
      HomekitRegistry registry,
      SubscriptionManager subscriptions,
      JmdnsHomekitAdvertiser advertiser) {
    this.registry = registry;
    this.authInfo = authInfo;
    this.subscriptions = subscriptions;
    this.advertiser = advertiser;
  }

  @Override
  public HomekitClientConnection createConnection(Consumer<HttpResponse> outOfBandMessageCallback) {
    return new ConnectionImpl(
        authInfo, registry, outOfBandMessageCallback, subscriptions, advertiser);
  }
}
