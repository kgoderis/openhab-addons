package org.openhab.io.homekit.hap.impl.json;

class HapJsonNoContentResponse extends HapJsonResponse {

  public HapJsonNoContentResponse() {
    super(new byte[0]);
  }

  @Override
  public int getStatusCode() {
    return 204;
  }
}
