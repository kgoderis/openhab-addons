package org.openhab.binding.atsadvanced.poc.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import org.openhab.binding.atsadvanced.poc.client.MessageDtos.ConfigurePanelRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.ConfigurePanelResponse;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.HealthResponse;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Plain HttpClient + Jackson client for the .NET 8 dumb gateway. No ServiceStack.
 */
public final class GatewayClient implements AutoCloseable {

    private final URI base;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private volatile SseSubscriber sse;

    public GatewayClient(String gatewayUrl) {
        this.base = URI.create(gatewayUrl.endsWith("/") ? gatewayUrl.substring(0, gatewayUrl.length() - 1) : gatewayUrl);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public HealthResponse health() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(base.resolve("/health")).GET().timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        requireOk(res);
        return mapper.readValue(res.body(), HealthResponse.class);
    }

    public ConfigurePanelResponse configurePanel(ConfigurePanelRequest request) throws IOException, InterruptedException {
        return postJson("/configurepanel", request, ConfigurePanelResponse.class);
    }

    public MessageResponse message(MessageRequest request) throws IOException, InterruptedException {
        return postJson("/message", request, MessageResponse.class);
    }

    public synchronized void startEventStream(Consumer<MessageResponse> onEvent) {
        stopEventStream();
        sse = new SseSubscriber(base.resolve("/events"), mapper, onEvent);
        sse.start();
    }

    public synchronized void stopEventStream() {
        if (sse != null) {
            sse.close();
            sse = null;
        }
    }

    @Override
    public void close() {
        stopEventStream();
    }

    private <T> T postJson(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        requireOk(res);
        return mapper.readValue(res.body(), type);
    }

    private static void requireOk(HttpResponse<String> res) throws IOException {
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        }
        Objects.requireNonNull(res.body(), "empty body");
    }
}
