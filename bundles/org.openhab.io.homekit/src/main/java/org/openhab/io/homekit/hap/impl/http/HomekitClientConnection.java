package org.openhab.io.homekit.hap.impl.http;

import java.io.IOException;

public interface HomekitClientConnection {

    HttpResponse handleRequest(HttpRequest request) throws IOException;

    byte[] decryptRequest(byte[] ciphertext);

    byte[] encryptResponse(byte[] plaintext) throws IOException;

    void close();

    void outOfBand(HttpResponse message);

    byte[] olddecryptRequest(byte[] ciphertext);
}
