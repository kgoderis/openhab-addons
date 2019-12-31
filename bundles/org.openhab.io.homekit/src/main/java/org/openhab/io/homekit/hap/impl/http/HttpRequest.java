package org.openhab.io.homekit.hap.impl.http;

public interface HttpRequest {

  String getUri();

  byte[] getBody();

  HttpMethod getMethod();
}
