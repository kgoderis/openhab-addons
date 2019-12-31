package org.openhab.io.homekit.hap.impl.pairing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.openhab.io.homekit.hap.HomekitAuthInfo;
import org.openhab.io.homekit.hap.impl.http.HttpRequest;
import org.openhab.io.homekit.hap.impl.http.HttpResponse;
import org.openhab.io.homekit.hap.impl.jmdns.JmdnsHomekitAdvertiser;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;

public class PairingUpdateController {

    private final HomekitAuthInfo authInfo;
    private final JmdnsHomekitAdvertiser advertiser;

    public PairingUpdateController(HomekitAuthInfo authInfo, JmdnsHomekitAdvertiser advertiser) {
        this.authInfo = authInfo;
        this.advertiser = advertiser;
    }

    public HttpResponse handle(HttpRequest request) throws IOException {
        org.openhab.io.homekit.util.TypeLengthValue.DecodeResult d = TypeLengthValue.decode(request.getBody());

        int method = d.getByte(Message.METHOD);
        if (method == 3) { // Add pairing
            byte[] username = d.getBytes(Message.IDENTIFIER);
            byte[] ltpk = d.getBytes(Message.PUBLIC_KEY);
            authInfo.createUser(authInfo.getMac() + new String(username, StandardCharsets.UTF_8), ltpk);
        } else if (method == 4) { // Remove pairing
            byte[] username = d.getBytes(Message.IDENTIFIER);
            authInfo.removeUser(authInfo.getMac() + new String(username, StandardCharsets.UTF_8));
            if (!authInfo.hasUser()) {
                advertiser.setDiscoverable(true);
            }
        } else {
            throw new RuntimeException("Unrecognized method: " + method);
        }
        return new PairingResponse(new byte[] { 0x06, 0x01, 0x02 });
    }
}
