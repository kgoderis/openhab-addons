package org.openhab.binding.atsadvanced.poc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.ConfigurePanelRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageRequest;
import org.openhab.binding.atsadvanced.poc.client.MessageDtos.MessageResponse;

import com.sun.net.httpserver.HttpServer;

/**
 * Contract test against an in-process mock gateway — no openHAB, no panel.
 */
class GatewayClientContractTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startMock() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", ex -> {
            byte[] body = "{\"status\":\"ok\",\"configured\":true,\"runtime\":\"mock\"}"
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/configurepanel", ex -> {
            byte[] body = "{\"result\":\"1\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/message", ex -> {
            String bodyJson = """
                    {"name":"returnSTAT.AREA","properties":[
                      {"id":"objectID","index":0,"value":1},
                      {"id":"AREV_FULLSET","index":0,"value":true},
                      {"id":"AREV_ALARM","index":0,"value":false}
                    ]}
                    """;
            byte[] body = bodyJson.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopMock() {
        server.stop(0);
    }

    @Test
    void healthConfigureAndMessage() throws Exception {
        try (GatewayClient client = new GatewayClient(baseUrl)) {
            assertEquals("ok", client.health().status);
            assertTrue(client.health().configured);

            ConfigurePanelRequest cfg = new ConfigurePanelRequest();
            cfg.hostaddress = "192.168.0.11";
            cfg.port = 32000;
            assertEquals("1", client.configurePanel(cfg).result);

            MessageResponse stat = client.message(new MessageRequest("getSTAT.AREA").prop("objectID", 1));
            assertEquals("returnSTAT.AREA", stat.name);
            assertEquals(Boolean.TRUE, stat.property("AREV_FULLSET"));
        }
    }
}
