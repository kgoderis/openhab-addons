package org.openhab.binding.atsadvanced.poc.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageResponse;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.Property;
import org.openhab.binding.atsadvanced.poc.prose.AtsProse;
import org.openhab.binding.atsadvanced.poc.session.PanelSession;
import org.openhab.binding.atsadvanced.poc.session.PanelSessionException;

/**
 * Minimal area status + full set/unset. No auto-inhibit: fails cleanly if session not ready.
 */
public final class AreaControl {

    public static final int MAX_AREAS = 8;

    private final PanelSession session;
    private final AtsProse prose;

    public AreaControl(PanelSession session, AtsProse prose) {
        this.session = session;
        this.prose = prose;
    }

    public AreaStatus getStatus(int areaNumber) throws IOException, InterruptedException, PanelSessionException {
        MessageResponse response = session.send(new MessageRequest("getSTAT.AREA").prop("objectID", areaNumber));
        Set<String> flags = new LinkedHashSet<>();
        for (Property p : response.properties) {
            if (p.id == null || "objectID".equals(p.id)) {
                continue;
            }
            if (Boolean.TRUE.equals(asBoolean(p.value))) {
                flags.add(p.id);
            }
        }
        boolean fullSet = flags.contains("AREV_FULLSET");
        boolean alarm = flags.contains("AREV_ALARM");
        List<String> proseFlags = new ArrayList<>();
        for (String f : flags) {
            proseFlags.add(prose.flag(f));
        }
        return new AreaStatus(areaNumber, fullSet, alarm, flags, proseFlags,
                prose.areaStatusSummary(fullSet, alarm));
    }

    public void setArea(int areaNumber) throws IOException, InterruptedException, PanelSessionException {
        int sessionId = createSession("createCC.A_SET", areaNumber);
        try {
            int state = getSessionState(sessionId);
            // CSMS_FC_Ready = 1280
            if (state != 1280) {
                throw new PanelSessionException(
                        prose.faultFromGatewayMessage("FAULT_CC_WRONG_STAT") + " (state=" + state + ")");
            }
            session.send(new MessageRequest("fnCC.A_SET_SETAREAS").prop("sessionID", sessionId));
            // Wait until Setting or Set (1284 / 1285) briefly
            long deadline = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < deadline) {
                state = getSessionState(sessionId);
                if (state == 1284 || state == 1285) {
                    break;
                }
                if (state == 1281 || state == 1282) {
                    throw new PanelSessionException(
                            "Cannot arm: faults or active zones present (auto-inhibit disabled in POC). state="
                                    + state);
                }
                Thread.sleep(200);
            }
        } finally {
            destroySession(sessionId);
        }
    }

    public void unsetArea(int areaNumber) throws IOException, InterruptedException, PanelSessionException {
        int sessionId = createSession("createCC.A_UNSET", areaNumber);
        try {
            int state = getSessionState(sessionId);
            // CSMS_UC_Ready = 768
            if (state != 768) {
                throw new PanelSessionException(
                        prose.faultFromGatewayMessage("FAULT_CC_WRONG_STAT") + " (state=" + state + ")");
            }
            session.send(new MessageRequest("fnCC.A_UNSET_UNSETAREAS").prop("sessionID", sessionId));
            long deadline = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < deadline) {
                state = getSessionState(sessionId);
                if (state == 772) { // Unset
                    break;
                }
                if (state == 770 || state == 771) {
                    session.send(new MessageRequest("fnCC.A_UNSET_SKIP").prop("sessionID", sessionId));
                }
                Thread.sleep(200);
            }
        } finally {
            destroySession(sessionId);
        }
    }

    private int createSession(String createMessage, int areaNumber)
            throws IOException, InterruptedException, PanelSessionException {
        MessageRequest req = new MessageRequest(createMessage);
        for (int i = 1; i <= MAX_AREAS; i++) {
            req.prop("area." + i, i == areaNumber);
        }
        // prop() always index 0 — need unique props; MessageRequest.prop appends — good
        // But we're calling prop in loop with same pattern - each adds a property. Good.

        // Fix: MessageRequest.prop always adds — but first call creates empty and we need
        // rebuild because createMessage already set name. OK.

        MessageResponse response = session.send(req);
        Object result = response.property("result");
        if (result instanceof Number n) {
            return n.intValue();
        }
        if (result instanceof String s) {
            return (int) Double.parseDouble(s);
        }
        throw new PanelSessionException("No control session id returned");
    }

    private int getSessionState(int sessionId) throws IOException, InterruptedException, PanelSessionException {
        MessageResponse response = session.send(new MessageRequest("statusCC.SESSION").prop("sessionID", sessionId));
        Object state = response.property("stateID");
        if (state instanceof Number n) {
            return n.intValue();
        }
        if (state instanceof String s) {
            return (int) Double.parseDouble(s);
        }
        return 0;
    }

    private void destroySession(int sessionId) {
        try {
            session.send(new MessageRequest("destroyCC.SESSION").prop("sessionID", sessionId));
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s) || "1".equals(s);
        }
        return false;
    }

    public record AreaStatus(int number, boolean fullSet, boolean alarm, Set<String> rawFlags,
            List<String> proseFlags, String summary) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "Area %d: %s | flags=%s", number, summary, proseFlags);
        }
    }
}
