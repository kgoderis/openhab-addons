package org.openhab.io.homekit.hap.impl.json;

public class EventResponse extends HapJsonResponse {

  public EventResponse(byte[] body) {
    super(body);
  }

  @Override
  public HttpVersion getVersion() {
    return HttpVersion.EVENT_1_0;
  }
}
