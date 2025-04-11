package org.openhab.io.homekit.internal.servlet;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.AccessoryServer;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.util.Debouncer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(asyncSupported = true)
public class CharacteristicServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(CharacteristicServlet.class);
    private static final int SC_MULTI_STATUS = 207;

    // Map characteristic ID to set of async contexts interested in it
    private final Map<Characteristic<?>, Set<AsyncContext>> characteristicSubscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<AsyncContext, Debouncer> debouncers = new ConcurrentHashMap<>();
    private final Map<AsyncContext, List<JsonObject>> pendingUpdates = new ConcurrentHashMap<>();
    private static final Duration DEBOUNCE_DELAY = Duration.ofSeconds(1);

    public CharacteristicServlet(AccessoryServer server) {
        super(server);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] ids = request.getParameterValues("id")[0].split(",");
        boolean includeMeta = Boolean.parseBoolean(request.getParameter("meta"));
        boolean includePermissions = Boolean.parseBoolean(request.getParameter("perms"));
        boolean includeType = Boolean.parseBoolean(request.getParameter("type"));
        boolean includeEvent = Boolean.parseBoolean(request.getParameter("ev"));

        // Handle event subscription requests
        if (includeEvent) {
            AsyncContext asyncContext = request.startAsync(request, response);
            asyncContext.setTimeout(0); // No timeout

            // Set response headers for long-lived connection
            response.setContentType("application/hap+json");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Transfer-Encoding", "chunked");

            // Subscribe to all requested characteristics
            for (String id : ids) {
                String[] parts = id.split("\\.");
                int aid = Integer.parseInt(parts[0]);
                int iid = Integer.parseInt(parts[1]);

                Accessory accessory = server.getAccessory(aid);
                if (accessory != null) {
                    accessory.getServices().stream().map(service -> (Characteristic<?>) service.getCharacteristic(iid))
                            .filter(characteristic -> characteristic != null).findFirst()
                            .ifPresent(characteristic -> characteristicSubscriptions
                                    .computeIfAbsent(characteristic, c -> ConcurrentHashMap.newKeySet())
                                    .add(asyncContext));
                }
            }

            // Add cleanup on client disconnect
            asyncContext.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    removeSubscription(asyncContext);
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    removeSubscription(asyncContext);
                }

                @Override
                public void onError(AsyncEvent event) {
                    removeSubscription(asyncContext);
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                }
            });

            return;
        }

        // Handle regular characteristic value requests
        JsonArrayBuilder characteristics = Json.createArrayBuilder();
        for (String id : ids) {
            String[] parts = id.split("\\.");
            if (parts.length != 2) {
                logger.error("Unexpected characteristics request: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int aid = Integer.parseInt(parts[0]);
            int iid = Integer.parseInt(parts[1]);

            Accessory accessory = server.getAccessory(aid);
            if (accessory != null) {
                accessory.getServices().stream().map(service -> (Characteristic<?>) service.getCharacteristic(iid))
                        .filter(characteristic -> characteristic != null).forEach(characteristic -> characteristics.add(
                                characteristic.toJson(includeMeta, includePermissions, includeType, includeEvent)));
            }
        }

        sendJsonResponse(response, Json.createObjectBuilder().add("characteristics", characteristics).build());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            JsonArray characteristics = Json.createReader(request.getInputStream()).readObject()
                    .getJsonArray("characteristics");

            for (JsonValue value : characteristics) {
                JsonObject characteristicWrite = (JsonObject) value;
                int aid = characteristicWrite.getInt("aid");
                int iid = characteristicWrite.getInt("iid");

                Accessory accessory = server.getAccessory(aid);
                if (accessory == null) {
                    continue;
                }

                accessory.getServices().stream().map(service -> (Characteristic<?>) service.getCharacteristic(iid))
                        .filter(characteristic -> characteristic != null).forEach(characteristic -> {
                            if (characteristicWrite.containsKey("value")) {
                                try {
                                    characteristic.setValue(characteristicWrite.get("value"));
                                } catch (Exception e) {
                                    logger.error("Error setting characteristic value", e);
                                }
                            }
                            if (characteristicWrite.containsKey("ev")) {
                                handleEventSubscription(characteristic, characteristicWrite.getBoolean("ev"));
                            }
                        });
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            // TODO : Handle TAble 6-11 with the HAP Specification Status Codes

        } catch (Exception e) {
            logger.error("Error processing characteristic update", e);
            response.setStatus(SC_MULTI_STATUS);
            sendJsonResponse(response,
                    Json.createObjectBuilder()
                            .add("characteristics",
                                    Json.createArrayBuilder().add(Json.createObjectBuilder().add("status", -70402) // HAP
                                                                                                                   // Specification
                                                                                                                   // error
                                                                                                                   // code
                                            .build()).build())
                            .build());
        }
    }

    /**
     * Publishes a characteristic update to all subscribed clients.
     * Updates are batched per connection and sent every second.
     */
    public void publishCharacteristicUpdate(Characteristic<?> characteristic) {
        Set<AsyncContext> subscribers = characteristicSubscriptions.get(characteristic);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        JsonObject update = Json.createObjectBuilder()
                .add("characteristics", Json.createArrayBuilder().add(characteristic.toEventJson())).build();

        subscribers.forEach(context -> {
            // Add update to pending list
            pendingUpdates.computeIfAbsent(context, k -> new ArrayList<>()).add(update);

            // Create or get debouncer for this context
            Debouncer debouncer = debouncers.computeIfAbsent(context,
                    k -> new Debouncer("HomeKit-Updates-" + context.hashCode(), scheduler, DEBOUNCE_DELAY,
                            Clock.systemUTC(), () -> sendBatchedUpdates(context)));

            // Trigger debounced send
            debouncer.call();
        });
    }

    /**
     * Sends all pending updates for a given context in a single batch
     */
    private void sendBatchedUpdates(AsyncContext context) {
        List<JsonObject> updates = pendingUpdates.get(context);
        if (updates == null || updates.isEmpty()) {
            return;
        }

        try {
            // Combine all characteristic updates into a single array
            JsonArrayBuilder characteristicsBuilder = Json.createArrayBuilder();
            updates.forEach(update -> update.getJsonArray("characteristics").forEach(characteristicsBuilder::add));

            // Create combined response
            JsonObject batchedUpdate = Json.createObjectBuilder().add("characteristics", characteristicsBuilder)
                    .build();

            // Send the batched update
            HttpServletResponse response = (HttpServletResponse) context.getResponse();
            response.setHeader("X-HAP-Event", "True");
            sendJsonResponse(response, batchedUpdate);

            // Clear pending updates after successful send
            updates.clear();
        } catch (IOException e) {
            logger.debug("Failed to send batched updates to subscriber, removing subscription", e);
            removeSubscription(context);
        }
    }

    private void handleEventSubscription(Characteristic<?> characteristic, boolean subscribe) {
        if (subscribe) {
            // Subscription handled in doGet
            characteristic.setHasEvents(true);
        } else {
            Set<AsyncContext> subscribers = characteristicSubscriptions.get(characteristic);
            if (subscribers != null) {
                subscribers.clear();
            }
            characteristic.setHasEvents(false);
        }
    }

    private void removeSubscription(AsyncContext context) {
        characteristicSubscriptions.values().forEach(subscribers -> subscribers.remove(context));
        debouncers.remove(context);
        pendingUpdates.remove(context);
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/hap+json");
        response.setHeader("Connection", "keep-alive");
        response.setStatus(HttpServletResponse.SC_OK);

        try (JsonWriter writer = Json.createWriter(response.getOutputStream())) {
            writer.write(json);
        }
        response.getOutputStream().flush();
    }

    @Override
    public void destroy() {
        super.destroy();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
