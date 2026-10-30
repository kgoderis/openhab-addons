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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.atsadvanced.internal.ATSAdvancedException;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanel;
import org.openhab.binding.atsadvanced.internal.PanelClient.ConfigurePanelResponse;
import org.openhab.binding.atsadvanced.internal.PanelClient.Message;
import org.openhab.binding.atsadvanced.internal.PanelClient.MessageResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP + SSE client for the AtsAdvancedGateway process.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public final class AtsGatewayClient implements AutoCloseable {

    private final URI base;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private @Nullable SseSubscriber sse;

    public AtsGatewayClient(String gatewayUrl) {
        String trimmed = gatewayUrl.endsWith("/") ? gatewayUrl.substring(0, gatewayUrl.length() - 1) : gatewayUrl;
        this.base = URI.create(trimmed);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean isHealthy() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(base.resolve("/health")).GET().timeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        return res.statusCode() >= 200 && res.statusCode() < 300;
    }

    public ConfigurePanelResponse configure(ConfigurePanel request) {
        try {
            return postJson("/configurepanel", request, ConfigurePanelResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ATSAdvancedException("Interrupted talking to gateway: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ATSAdvancedException("Gateway configure failed: " + e.getMessage(), e);
        }
    }

    public MessageResponse message(Message request) {
        try {
            return postJson("/message", request, MessageResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ATSAdvancedException("Interrupted talking to gateway: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ATSAdvancedException("Gateway message failed: " + e.getMessage(), e);
        }
    }

    public synchronized void startEventStream(Consumer<MessageResponse> onEvent) {
        stopEventStream();
        SseSubscriber subscriber = new SseSubscriber(base.resolve("/events"), mapper, onEvent);
        sse = subscriber;
        subscriber.start();
    }

    public synchronized void stopEventStream() {
        SseSubscriber subscriber = sse;
        if (subscriber != null) {
            subscriber.close();
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
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        }
        return mapper.readValue(Objects.requireNonNull(res.body(), "empty body"), type);
    }
}
