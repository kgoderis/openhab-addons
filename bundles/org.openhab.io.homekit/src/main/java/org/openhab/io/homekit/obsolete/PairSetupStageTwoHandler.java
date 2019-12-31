package org.openhab.io.homekit.obsolete;

import java.io.IOException;
import java.math.BigInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.internal.servlet.HomekitSRP6ServerSession;
import org.openhab.io.homekit.internal.servlet.HomekitSRP6ServerSession.State;
import org.openhab.io.homekit.util.Message;
import org.openhab.io.homekit.util.TypeLengthValue;
import org.openhab.io.homekit.util.TypeLengthValue.Encoder;

import com.nimbusds.srp6.SRP6Exception;

public class PairSetupStageTwoHandler extends PairSetupHandler {

    public PairSetupStageTwoHandler(AccessoryServer server) {
        super(server);
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        byte[] body = IOUtils.toByteArray(request.getInputStream());

        if (getStage(body) == 2) {
            HttpSession session = request.getSession();
            HomekitSRP6ServerSession SRP6Session = (HomekitSRP6ServerSession) session.getAttribute("SRP6Session");

            if (SRP6Session == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                if (SRP6Session.getState() != State.STEP_1) {
                    logger.error("Session is not in state Step 1 when receiving step2");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                } else {
                    BigInteger proof = null;
                    Encoder encoder = TypeLengthValue.getEncoder();
                    try {
                        proof = SRP6Session.step2(getA(body), getM1(body));
                        encoder.add(Message.STATE, (short) 0x04);
                        encoder.add(Message.PROOF, proof);

                        response.setContentType("application/pairing+tlv8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getOutputStream().write(encoder.toByteArray());
                        response.getOutputStream().flush();
                    } catch (SRP6Exception e) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                }
            }

            baseRequest.setHandled(true);
        }
    }
}
