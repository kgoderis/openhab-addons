package org.openhab.binding.atsadvanced.poc.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

final class SseSubscriber implements AutoCloseable {

    private final URI eventsUri;
    private final ObjectMapper mapper;
    private final Consumer<MessageResponse> onEvent;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread thread;

    SseSubscriber(URI eventsUri, ObjectMapper mapper, Consumer<MessageResponse> onEvent) {
        this.eventsUri = eventsUri;
        this.mapper = mapper;
        this.onEvent = onEvent;
    }

    void start() {
        thread = new Thread(this::loop, "ats-sse");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        while (running.get()) {
            try {
                HttpRequest req = HttpRequest.newBuilder(eventsUri).header("Accept", "text/event-stream").GET()
                        .build();
                HttpResponse<java.io.InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    Thread.sleep(2000);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                    String line;
                    String event = null;
                    StringBuilder data = new StringBuilder();
                    while (running.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith("event:")) {
                            event = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            if (data.length() > 0) {
                                data.append('\n');
                            }
                            data.append(line.substring(5).trim());
                        } else if (line.isEmpty()) {
                            if ("panelevent".equals(event) && data.length() > 0) {
                                MessageResponse msg = mapper.readValue(data.toString(), MessageResponse.class);
                                onEvent.accept(msg);
                            }
                            event = null;
                            data.setLength(0);
                        }
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
