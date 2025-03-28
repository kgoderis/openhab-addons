package org.openhab.io.homekit.hap.impl.responses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.openhab.io.homekit.hap.impl.http.HttpResponse;

public class InternalServerErrorResponse implements HttpResponse {

    private final Exception e;

    public InternalServerErrorResponse(Exception e) {
        this.e = e;
    }

    @Override
    public int getStatusCode() {
        return 500;
    }

    @Override
    public ByteBuffer getBody() {
        return ByteBuffer.wrap(e.getClass().getName().getBytes(StandardCharsets.UTF_8));
    }

    public Exception getException() {
        return e;
    }
}
