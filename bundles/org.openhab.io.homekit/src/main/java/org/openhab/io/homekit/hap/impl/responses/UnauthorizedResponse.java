package org.openhab.io.homekit.hap.impl.responses;

import  org.openhab.io.homekit.hap.impl.http.HttpResponse;

public class UnauthorizedResponse implements HttpResponse {

  @Override
  public int getStatusCode() {
    return 401;
  }
}
