package org.openhab.binding.atsadvanced.poc.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.openhab.binding.atsadvanced.poc.client.GatewayClient;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.ConfigurePanelRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.ConfigurePanelResponse;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageResponse;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.Property;
import org.openhab.binding.atsadvanced.poc.prose.AtsProse;

/**
 * Owns panel session lifecycle against the dumb gateway.
 */
public final class PanelSession implements AutoCloseable {

    public enum State {
        DISCONNECTED,
        CONFIGURED,
        LOGGED_IN
    }

    private final GatewayClient client;
    private final AtsProse prose;
    private final List<Consumer<MessageResponse>> eventListeners = new CopyOnWriteArrayList<>();
    private volatile State state = State.DISCONNECTED;
    private volatile String lastErrorProse;

    public PanelSession(GatewayClient client, AtsProse prose) {
        this.client = Objects.requireNonNull(client);
        this.prose = Objects.requireNonNull(prose);
    }

    public State getState() {
        return state;
    }

    public String getLastErrorProse() {
        return lastErrorProse;
    }

    public void addEventListener(Consumer<MessageResponse> listener) {
        eventListeners.add(listener);
    }

    public void connect(String panelHost, int panelPort, String pin)
            throws IOException, InterruptedException, PanelSessionException {
        client.startEventStream(msg -> {
            for (Consumer<MessageResponse> l : eventListeners) {
                l.accept(msg);
            }
            if ("msg.error".equals(msg.name)) {
                Object raw = msg.property("result");
                lastErrorProse = prose.faultFromGatewayMessage(raw == null ? null : String.valueOf(raw));
            }
        });

        ConfigurePanelRequest cfg = new ConfigurePanelRequest();
        cfg.hostaddress = panelHost;
        cfg.port = panelPort;
        cfg.password = "0".repeat(52);
        cfg.hearbeat = 10000;
        cfg.timeout = 5000;
        cfg.retries = 2;

        ConfigurePanelResponse cfgRes = client.configurePanel(cfg);
        if (cfgRes.result == null || !"1".equals(cfgRes.result.trim())) {
            lastErrorProse = prose.faultFromGatewayMessage(cfgRes.result);
            state = State.DISCONNECTED;
            throw new PanelSessionException(lastErrorProse);
        }
        state = State.CONFIGURED;

        MessageResponse login = client.message(new MessageRequest("device.getConnect")
                .prop("userPIN", pin)
                .prop("userAction_LOGREAD", true)
                .prop("userAction_CTRL", true)
                .prop("userAction_MONITOR", true)
                .prop("userAction_DIAG", true)
                .prop("userAction_UPLOAD", false)
                .prop("userAction_DOWNLOAD", false));

        if ("return.error".equals(login.name)) {
            lastErrorProse = prose.faultFromGatewayMessage(String.valueOf(login.property("result")));
            state = State.CONFIGURED;
            throw new PanelSessionException(lastErrorProse);
        }

        // Skip start.MONITOR in the POC: it fans out async panel events that race NAce on macOS
        // and is not required for area name/status reads.

        state = State.LOGGED_IN;
    }

    public MessageResponse send(MessageRequest request) throws IOException, InterruptedException, PanelSessionException {
        MessageResponse response = client.message(request);
        if ("return.error".equals(response.name)) {
            lastErrorProse = prose.faultFromGatewayMessage(String.valueOf(response.property("result")));
            throw new PanelSessionException(lastErrorProse);
        }
        return response;
    }

    public boolean isAlive() throws IOException, InterruptedException {
        MessageResponse r = client.message(new MessageRequest("is.Alive"));
        return !"return.error".equals(r.name);
    }

    public List<NamedObject> listAreas() throws IOException, InterruptedException, PanelSessionException {
        List<NamedObject> areas = new ArrayList<>();
        MessageResponse chunk = send(new MessageRequest("select.AreaNames").prop("index", 1));
        int index = 1;
        for (Property p : chunk.properties) {
            if ("index".equals(p.id) && p.value instanceof Number n) {
                index = n.intValue();
            }
        }
        for (Property p : chunk.properties) {
            if (!"name".equals(p.id)) {
                continue;
            }
            String name = p.value == null ? "" : String.valueOf(p.value).trim();
            if (!name.isEmpty() && !"[Empty]".equals(name)) {
                areas.add(new NamedObject(index, name));
            }
            index++;
        }
        return areas;
    }

    public void disconnect() {
        try {
            client.message(new MessageRequest("device.disconnect"));
        } catch (Exception ignored) {
            // best effort — panel/gateway may already be tearing down
        }
        state = State.DISCONNECTED;
        try {
            client.stopEventStream();
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Override
    public void close() {
        disconnect();
        client.close();
    }

    public record NamedObject(int number, String name) {
    }
}
