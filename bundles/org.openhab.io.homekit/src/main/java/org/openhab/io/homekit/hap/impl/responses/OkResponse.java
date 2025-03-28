package org.openhab.io.homekit.hap.impl.responses;

import java.nio.ByteBuffer;

import org.openhab.io.homekit.hap.impl.http.HttpResponse;

public class OkResponse implements HttpResponse {

    private final ByteBuffer body;

    public OkResponse(byte[] body) {
        this.body = ByteBuffer.wrap(body);
    }

    @Override
    public ByteBuffer getBody() {
        return body;
    }

    @Override
    public int getStatusCode() {
        return 200;
    }
}
