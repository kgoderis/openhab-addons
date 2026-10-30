/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.atsadvanced.internal.gateway;

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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Background SSE subscriber for gateway {@code GET /events}.
 * <p>
 * Parses {@code event: panelevent} frames into {@link MessageResponse} JSON and invokes the consumer on a daemon
 * thread. Reconnects with a fixed delay on I/O errors until {@link #close()}. Gateway log lines (
 * {@code msg.info}, {@code msg.warn}, {@code msg.error}) use the same payload shape and are handled in
 * {@link org.openhab.binding.atsadvanced.internal.handler.PanelHandler#handleMessageResponse}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
final class SseSubscriber implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(SseSubscriber.class);
    private final URI eventsUri;
    private final ObjectMapper mapper;
    private final Consumer<MessageResponse> onEvent;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private @Nullable Thread thread;

    /**
     * @param eventsUri full {@code /events} URI
     * @param mapper Jackson mapper shared with {@link AtsGatewayClient}
     * @param onEvent callback for each {@code panelevent}
     */
    SseSubscriber(URI eventsUri, ObjectMapper mapper, Consumer<MessageResponse> onEvent) {
        this.eventsUri = eventsUri;
        this.mapper = mapper;
        this.onEvent = onEvent;
    }

    /** Starts the daemon reader thread. */
    void start() {
        Thread t = new Thread(this::loop, "atsadvanced-sse");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    private void loop() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        while (running.get()) {
            try {
                HttpRequest req = HttpRequest.newBuilder(eventsUri).header("Accept", "text/event-stream").GET().build();
                HttpResponse<java.io.InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    Thread.sleep(2000);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                    String line;
                    @Nullable
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
                                onEvent.accept(mapper.readValue(data.toString(), MessageResponse.class));
                            }
                            event = null;
                            data.setLength(0);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (running.get()) {
                    logger.debug("ATS gateway SSE reconnect after: {}", e.toString());
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
        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
    }
}
