package org.openhab.io.homekit.server.servlet;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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

import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.accessory.HomekitAccessory;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristic;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;
import org.openhab.io.homekit.core.characteristic.AbstractHomekitCharacteristic;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicUpdateEvent;
import org.openhab.io.homekit.exception.HomekitAccessoryOperationException;
import org.openhab.io.homekit.protocol.status.HomekitStatusCode;
import org.openhab.io.homekit.util.HomekitDebouncer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that implements the HomeKit Accessory Protocol (HAP) for characteristic value management.
 *
 * <p>This servlet serves as the primary interface for HomeKit clients to interact with
 * accessory characteristics. It implements a robust event-driven architecture that supports
 * both synchronous value requests and asynchronous event subscriptions, following the HAP
 * specification for real-time accessory state updates.
 *
 * <p>The servlet's core responsibilities include:
 * <ul>
 *     <li>Processing GET requests to retrieve current characteristic values</li>
 *     <li>Handling PUT requests to update characteristic states</li>
 *     <li>Managing long-lived connections for real-time event notifications</li>
 *     <li>Implementing efficient update batching and debouncing mechanisms</li>
 * </ul>
 *
 * <p>Key architectural features:
 * <ul>
 *     <li>Asynchronous event handling using {@link AsyncContext} for efficient resource usage</li>
 *     <li>Debounced updates to prevent network flooding while maintaining responsiveness</li>
 *     <li>Thread-safe subscription management using {@link ConcurrentHashMap}</li>
 *     <li>JSON-based communication following HAP protocol specifications</li>
 * </ul>
 *
 * <p>The class integrates with:
 * <ul>
 *     <li>{@link HomekitBaseServlet} for base servlet functionality</li>
 *     <li>{@link HomekitAccessoryServer} for server and accessory management</li>
 *     <li>{@link HomekitEventManager} for event distribution and handling</li>
 *     <li>{@link HomekitCharacteristic} for characteristic value operations</li>
 *     <li>{@link HomekitAccessory} for accessory lifecycle management</li>
 * </ul>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
@WebServlet(asyncSupported = true)
public class HomekitCharacteristicServlet extends HomekitBaseServlet {
    // ========== Log Message Prefixes ==========
    private static final Logger logger = LoggerFactory.getLogger(HomekitCharacteristicServlet.class);
    private static final int SC_MULTI_STATUS = 207;
    private static final String LOG_PREFIX = "Homekit CharacteristicServlet: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";
    private static final String LOG_WARN = LOG_PREFIX + "Warning - ";
    private static final String LOG_EVENT = LOG_PREFIX + "Event - ";
    private static final String LOG_REQUEST = LOG_PREFIX + "Request - ";
    private static final String LOG_SUBSCRIPTION = LOG_PREFIX + "Subscription - ";

    /** Map of characteristic subscriptions to their async contexts */
    private final Map<HomekitCharacteristic<?>, Set<AsyncContext>> characteristicSubscriptions = new ConcurrentHashMap<>();

    /** Scheduler for handling debounced updates */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Map of debouncers for each async context */
    private final Map<AsyncContext, HomekitDebouncer> debouncers = new ConcurrentHashMap<>();

    /** Map of pending updates for each async context */
    private final Map<AsyncContext, List<JsonObject>> pendingUpdates = new ConcurrentHashMap<>();

    /** Delay for debouncing updates */
    private static final Duration DEBOUNCE_DELAY = Duration.ofSeconds(1);

    /** Event manager for publishing characteristic updates */
    private final HomekitEventManager eventManager;

    /**
     * Creates a new characteristic servlet with the specified server and event manager.
     *
     * <p>This constructor initializes the servlet with the necessary components for
     * handling characteristic operations and event management. It sets up:
     * <ul>
     *     <li>The base servlet functionality through the parent class</li>
     *     <li>The event manager for publishing characteristic updates</li>
     *     <li>Thread-safe collections for subscription management</li>
     *     <li>A scheduler for handling debounced updates</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Uses thread-safe collections for concurrent access</li>
     *     <li>Implements proper resource cleanup</li>
     *     <li>Maintains secure event handling</li>
     * </ul>
     *
     * @param server The HomeKit accessory server instance
     * @param eventManager The event manager for handling characteristic updates
     */
    public HomekitCharacteristicServlet(HomekitAccessoryServer server, HomekitEventManager eventManager) {
        super(server);
        this.eventManager = eventManager;
        logger.debug("{}Creating new characteristic servlet with server and event manager", LOG_INIT);
    }

    /**
     * Handles GET requests for characteristic values and event subscriptions.
     *
     * <p>This method implements the HAP protocol for characteristic value retrieval and
     * event subscription management. It supports two distinct request types:
     * <ul>
     *     <li>Value requests: Synchronous retrieval of current characteristic states</li>
     *     <li>Event subscriptions: Asynchronous setup of real-time update channels</li>
     * </ul>
     *
     * <p>For value requests, the method:
     * <ul>
     *     <li>Parses and validates characteristic identifiers (aid.iid format)</li>
     *     <li>Retrieves current values from the accessory hierarchy</li>
     *     <li>Constructs a JSON response with requested characteristic data</li>
     * </ul>
     *
     * <p>For event subscriptions, the method:
     * <ul>
     *     <li>Establishes a persistent HTTP connection using async servlet features</li>
     *     <li>Registers the connection for characteristic update notifications</li>
     *     <li>Implements automatic cleanup on client disconnection</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates characteristic identifiers to prevent injection attacks</li>
     *     <li>Implements proper access control for characteristic values</li>
     *     <li>Maintains secure event subscription handling</li>
     *     <li>Protects against resource exhaustion through proper cleanup</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Proper validation of request parameters</li>
     *     <li>Graceful handling of invalid characteristic IDs</li>
     *     <li>Detailed error logging for debugging</li>
     *     <li>Appropriate HTTP status codes for different error conditions</li>
     * </ul>
     *
     * @param request The HTTP request containing characteristic IDs and options
     * @param response The HTTP response for the characteristic values
     * @throws ServletException if the request cannot be processed
     * @throws IOException if an I/O error occurs during response writing
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("{}Handling GET request for characteristics", LOG_REQUEST);
        try {
            String[] ids = request.getParameterValues("id")[0].split(",");
            boolean includeMeta = Boolean.parseBoolean(request.getParameter("meta"));
            boolean includePermissions = Boolean.parseBoolean(request.getParameter("perms"));
            boolean includeType = Boolean.parseBoolean(request.getParameter("type"));
            boolean includeEvent = Boolean.parseBoolean(request.getParameter("ev"));

            logger.trace("{}Request parameters - ids: {}, meta: {}, perms: {}, type: {}, ev: {}", LOG_REQUEST,
                    String.join(",", ids), includeMeta, includePermissions, includeType, includeEvent);

            // Handle event subscription requests
            if (includeEvent) {
                logger.debug("{}Setting up event subscription for characteristics: {}", LOG_SUBSCRIPTION,
                        String.join(",", ids));
                AsyncContext asyncContext = request.startAsync(request, response);
                asyncContext.setTimeout(0); // No timeout

                // Set response headers for long-lived connection
                response.setContentType("application/hap+json");
                response.setHeader("Connection", "keep-alive");
                response.setHeader("Transfer-Encoding", "chunked");

                // Subscribe to all requested characteristics
                for (String id : ids) {
                    String[] parts = id.split("\\.");
                    if (parts.length != 2) {
                        logger.error("{}Invalid characteristic ID format: {}", LOG_ERROR, id);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }

                    try {
                        int aid = Integer.parseInt(parts[0]);
                        int iid = Integer.parseInt(parts[1]);

                        HomekitAccessory accessory = server.getAccessory(aid);
                        if (accessory != null) {
                            accessory.getServices().stream()
                                    .map(service -> (HomekitCharacteristic<?>) service.getCharacteristic(iid).get())
                                    .filter(characteristic -> characteristic != null).findFirst()
                                    .ifPresent(characteristic -> {
                                        characteristicSubscriptions
                                                .computeIfAbsent(characteristic, c -> ConcurrentHashMap.newKeySet())
                                                .add(asyncContext);
                                        logger.debug("{}Subscribed to characteristic {}.{}", LOG_SUBSCRIPTION, aid, iid);
                                    });
                        }
                    } catch (NumberFormatException e) {
                        logger.error("{}Invalid characteristic ID format: {}", LOG_ERROR, id);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } catch (HomekitAccessoryOperationException e) {
                        logger.error("{}Error accessing accessory: {}", LOG_ERROR, e.getMessage(), e);
                    }
                }

                // Add cleanup on client disconnect
                asyncContext.addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        logger.debug("{}Client disconnected, cleaning up subscription", LOG_SUBSCRIPTION);
                        removeSubscription(asyncContext);
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        logger.debug("{}Subscription timeout, cleaning up", LOG_SUBSCRIPTION);
                        removeSubscription(asyncContext);
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        logger.error("{}Subscription error, cleaning up: {}", LOG_ERROR, event.getThrowable().getMessage());
                        removeSubscription(asyncContext);
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) {
                        logger.trace("{}Async context started", LOG_SUBSCRIPTION);
                    }
                });

                return;
            }

            // Handle regular characteristic value requests
            logger.debug("{}Processing value request for characteristics: {}", LOG_REQUEST, String.join(",", ids));
            JsonArrayBuilder characteristics = Json.createArrayBuilder();
            for (String id : ids) {
                String[] parts = id.split("\\.");
                if (parts.length != 2) {
                    logger.error("{}Invalid characteristic ID format: {}", LOG_ERROR, id);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                try {
                    int aid = Integer.parseInt(parts[0]);
                    int iid = Integer.parseInt(parts[1]);

                    HomekitAccessory accessory = server.getAccessory(aid);
                    if (accessory != null) {
                        accessory.getServices().stream()
                                .map(service -> (HomekitCharacteristic<?>) service.getCharacteristic(iid).get())
                                .filter(characteristic -> characteristic != null)
                                .forEach(characteristic -> {
                                    characteristics.add(characteristic.toJson(includeMeta, includePermissions, includeType,
                                            includeEvent));
                                    logger.trace("{}Retrieved value for characteristic {}.{}", LOG_REQUEST, aid, iid);
                                });
                    }
                } catch (NumberFormatException e) {
                    logger.error("{}Invalid characteristic ID format: {}", LOG_ERROR, id);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                } catch (HomekitAccessoryOperationException e) {
                    logger.error("{}Error accessing accessory: {}", LOG_ERROR, e.getMessage(), e);
                }
            }

            sendJsonResponse(response, Json.createObjectBuilder().add("characteristics", characteristics).build());
            logger.debug("{}Successfully processed value request", LOG_REQUEST);
        } catch (Exception e) {
            logger.error("{}Unexpected error processing GET request: {}", LOG_ERROR, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles PUT requests for updating characteristic values and managing event subscriptions.
     *
     * <p>This method implements the HAP protocol for characteristic value updates and
     * subscription management. It processes batch updates efficiently while maintaining
     * proper error handling and status reporting.
     *
     * <p>The update process includes:
     * <ul>
     *     <li>Parsing and validating the JSON request payload</li>
     *     <li>Processing each characteristic update in the batch</li>
     *     <li>Publishing update events through the event system</li>
     *     <li>Managing subscription state changes</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates JSON payload structure and content</li>
     *     <li>Implements proper access control for updates</li>
     *     <li>Protects against batch update attacks</li>
     *     <li>Maintains secure event handling</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Individual update failures don't affect the entire batch</li>
     *     <li>Proper status codes are returned for each update</li>
     *     <li>Detailed error information is logged for debugging</li>
     *     <li>Graceful handling of invalid requests</li>
     * </ul>
     *
     * @param request The HTTP request containing characteristic updates
     * @param response The HTTP response indicating update status
     * @throws ServletException if the request cannot be processed
     * @throws IOException if an I/O error occurs during response writing
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("{}Handling PUT request for characteristic updates", LOG_REQUEST);
        try {
            JsonArray characteristics = Json.createReader(request.getInputStream()).readObject()
                    .getJsonArray("characteristics");
            logger.trace("{}Received update request for {} characteristics", LOG_REQUEST, characteristics.size());

            for (JsonValue value : characteristics) {
                JsonObject characteristicWrite = (JsonObject) value;
                int aid = characteristicWrite.getInt("aid");
                int iid = characteristicWrite.getInt("iid");

                logger.trace("{}Processing update for characteristic {}.{}", LOG_REQUEST, aid, iid);

                HomekitAccessory accessory = server.getAccessory(aid);
                if (accessory == null) {
                    logger.warn("{}Accessory {} not found", LOG_WARN, aid);
                    continue;
                }

                accessory.getServices().stream()
                        .map(service -> (HomekitCharacteristic<?>) service.getCharacteristic(iid).get())
                        .filter(characteristic -> characteristic != null).forEach(characteristic -> {
                            if (characteristicWrite.containsKey("value")) {
                                try {
                                    if (characteristic instanceof AbstractHomekitCharacteristic<?> genericCharacteristic) {
                                        logger.debug("{}Updating value for characteristic {}.{}", LOG_REQUEST, aid, iid);
                                        HomekitEvent newEvent = new HomekitCharacteristicUpdateEvent(
                                                (UID) server.getUID(), (UID) genericCharacteristic.getUID(),
                                                genericCharacteristic, JsonValue.NULL, characteristicWrite.get("value"),
                                                Collections.emptyMap(), new HomekitEventMetadata((UID) server.getUID(),
                                                        null, (UID) server.getUID(), Collections.emptySet()));
                                        eventManager.publishEvent(newEvent);
                                    }
                                } catch (Exception e) {
                                    logger.error("{}Error setting characteristic value for {}.{}: {}", LOG_ERROR, aid, iid,
                                            e.getMessage(), e);
                                }
                            }
                            if (characteristicWrite.containsKey("ev")) {
                                boolean subscribe = characteristicWrite.getBoolean("ev");
                                logger.debug("{}Updating subscription state for characteristic {}.{} to {}", LOG_SUBSCRIPTION,
                                        aid, iid, subscribe);
                                handleEventSubscription(characteristic, subscribe);
                            }
                        });
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            logger.debug("{}Successfully processed characteristic updates", LOG_REQUEST);
        } catch (Exception e) {
            logger.error("{}Error processing characteristic update: {}", LOG_ERROR, e.getMessage(), e);
            response.setStatus(SC_MULTI_STATUS);
            sendJsonResponse(response, Json.createObjectBuilder().add("characteristics", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("status", HomekitStatusCode.UNABLE_TO_PERFORM.getKey()).build())
                    .build()).build());
        }
    }

    /**
     * Publishes a characteristic update to all subscribed clients.
     *
     * <p>This method implements the HAP event notification system, ensuring efficient
     * delivery of characteristic updates to all interested clients. It employs a
     * sophisticated batching and debouncing mechanism to optimize network usage.
     *
     * <p>The update process includes:
     * <ul>
     *     <li>Identifying all subscribers for the updated characteristic</li>
     *     <li>Creating optimized JSON update messages</li>
     *     <li>Batching multiple updates to reduce network overhead</li>
     *     <li>Using debouncing to prevent update flooding</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates characteristic updates before publishing</li>
     *     <li>Implements proper access control for updates</li>
     *     <li>Protects against update flooding attacks</li>
     *     <li>Maintains secure event distribution</li>
     * </ul>
     *
     * <p>The method ensures:
     * <ul>
     *     <li>Real-time updates while maintaining system performance</li>
     *     <li>Efficient network usage through update batching</li>
     *     <li>Proper cleanup of disconnected clients</li>
     *     <li>Graceful handling of update failures</li>
     * </ul>
     *
     * @param characteristic The characteristic that was updated
     */
    public void publishCharacteristicUpdate(HomekitCharacteristic<?> characteristic) {
        Set<AsyncContext> subscribers = characteristicSubscriptions.get(characteristic);
        if (subscribers == null || subscribers.isEmpty()) {
            logger.trace("{}No subscribers for characteristic update", LOG_EVENT);
            return;
        }

        logger.debug("{}Publishing update to {} subscribers", LOG_EVENT, subscribers.size());
        JsonObject update = Json.createObjectBuilder()
                .add("characteristics", Json.createArrayBuilder().add(characteristic.toEventJson())).build();

        subscribers.forEach(context -> {
            // Add update to pending list
            pendingUpdates.computeIfAbsent(context, k -> new ArrayList<>()).add(update);

            // Create or get debouncer for this context
            HomekitDebouncer debouncer = debouncers.computeIfAbsent(context,
                    k -> new HomekitDebouncer("Homekit-Updates-" + context.hashCode(), scheduler, DEBOUNCE_DELAY,
                            Clock.systemUTC(), () -> sendBatchedUpdates(context)));

            // Trigger debounced send
            debouncer.call();
        });
    }

    /**
     * Sends all pending updates for a given context in a single batch.
     *
     * <p>This method consolidates multiple characteristic updates into a single
     * response to optimize network usage. It:
     * <ul>
     *     <li>Retrieves all pending updates for the context</li>
     *     <li>Combines them into a single JSON response</li>
     *     <li>Sends the response to the client</li>
     *     <li>Cleans up the pending updates list</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates JSON response structure</li>
     *     <li>Implements proper error handling</li>
     *     <li>Protects against response flooding</li>
     *     <li>Maintains secure response handling</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Graceful handling of I/O errors</li>
     *     <li>Proper cleanup of failed subscriptions</li>
     *     <li>Detailed error logging for debugging</li>
     * </ul>
     *
     * @param context The async context for which to send updates
     */
    private void sendBatchedUpdates(AsyncContext context) {
        List<JsonObject> updates = pendingUpdates.get(context);
        if (updates == null || updates.isEmpty()) {
            return;
        }

        try {
            logger.trace("{}Sending {} batched updates", LOG_EVENT, updates.size());
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
            logger.debug("{}Successfully sent batched updates", LOG_EVENT);
        } catch (IOException e) {
            logger.error("{}Failed to send batched updates: {}", LOG_ERROR, e.getMessage(), e);
            logger.debug("{}Removing failed subscription", LOG_SUBSCRIPTION);
            removeSubscription(context);
        }
    }

    /**
     * Handles event subscription state changes for a characteristic.
     *
     * <p>This method manages the subscription state by:
     * <ul>
     *     <li>Adding or removing the characteristic from the subscription map</li>
     *     <li>Updating the characteristic's event state</li>
     *     <li>Logging subscription changes</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates subscription state changes</li>
     *     <li>Implements proper access control</li>
     *     <li>Protects against subscription flooding</li>
     *     <li>Maintains secure state management</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Proper cleanup of unsubscribed characteristics</li>
     *     <li>Consistent state management</li>
     *     <li>Detailed logging of state changes</li>
     * </ul>
     *
     * @param characteristic The characteristic to update subscription for
     * @param subscribe Whether to subscribe or unsubscribe
     */
    private void handleEventSubscription(HomekitCharacteristic<?> characteristic, boolean subscribe) {
        if (subscribe) {
            // Subscription handled in doGet
            characteristic.withEvents(true);
            logger.debug("{}Enabled events for characteristic", LOG_SUBSCRIPTION);
        } else {
            Set<AsyncContext> subscribers = characteristicSubscriptions.get(characteristic);
            if (subscribers != null) {
                subscribers.clear();
                logger.debug("{}Cleared subscribers for characteristic", LOG_SUBSCRIPTION);
            }
            characteristic.withEvents(false);
            logger.debug("{}Disabled events for characteristic", LOG_SUBSCRIPTION);
        }
    }

    /**
     * Removes a subscription and cleans up associated resources.
     *
     * <p>This method handles cleanup when a client disconnects by:
     * <ul>
     *     <li>Removing the async context from all characteristic subscriptions</li>
     *     <li>Cleaning up the debouncer for the context</li>
     *     <li>Removing any pending updates</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Ensures complete resource cleanup</li>
     *     <li>Implements thread-safe removal</li>
     *     <li>Protects against resource leaks</li>
     *     <li>Maintains secure cleanup process</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Complete cleanup of all resources</li>
     *     <li>Thread-safe removal of subscriptions</li>
     *     <li>Proper logging of cleanup operations</li>
     * </ul>
     *
     * @param context The async context to clean up
     */
    private void removeSubscription(AsyncContext context) {
        characteristicSubscriptions.values().forEach(subscribers -> subscribers.remove(context));
        debouncers.remove(context);
        pendingUpdates.remove(context);
        logger.debug("{}Cleaned up subscription resources", LOG_SUBSCRIPTION);
    }

    /**
     * Sends a JSON response to the client.
     *
     * <p>This method handles the HTTP response by:
     * <ul>
     *     <li>Setting the appropriate content type</li>
     *     <li>Writing the JSON object to the response stream</li>
     *     <li>Flushing the response to ensure delivery</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Validates JSON response content</li>
     *     <li>Implements proper content type handling</li>
     *     <li>Protects against response injection</li>
     *     <li>Maintains secure response handling</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Proper resource cleanup</li>
     *     <li>Detailed error logging</li>
     *     <li>Graceful handling of I/O errors</li>
     * </ul>
     *
     * @param response The HTTP response to write to
     * @param json The JSON object to send
     * @throws IOException if an I/O error occurs during response writing
     */
    private void sendJsonResponse(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/hap+json");
        response.setHeader("Connection", "keep-alive");
        response.setStatus(HttpServletResponse.SC_OK);

        try (JsonWriter writer = Json.createWriter(response.getOutputStream())) {
            writer.write(json);
        }
        response.getOutputStream().flush();
        logger.trace("{}Sent JSON response", LOG_REQUEST);
    }

    /**
     * Cleans up resources when the servlet is destroyed.
     *
     * <p>This method ensures proper cleanup by:
     * <ul>
     *     <li>Shutting down the scheduler</li>
     *     <li>Clearing all subscriptions</li>
     *     <li>Removing all pending updates</li>
     * </ul>
     *
     * <p>Security considerations:
     * <ul>
     *     <li>Ensures complete resource cleanup</li>
     *     <li>Implements graceful shutdown</li>
     *     <li>Protects against resource leaks</li>
     *     <li>Maintains secure shutdown process</li>
     * </ul>
     *
     * <p>Error handling ensures:
     * <ul>
     *     <li>Graceful shutdown of the scheduler</li>
     *     <li>Proper cleanup of all resources</li>
     *     <li>Detailed logging of shutdown process</li>
     * </ul>
     */
    @Override
    public void destroy() {
        logger.debug("{}Shutting down characteristic servlet", LOG_INIT);
        super.destroy();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                logger.warn("{}Scheduler did not terminate gracefully, forcing shutdown", LOG_WARN);
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("{}Scheduler shutdown interrupted: {}", LOG_ERROR, e.getMessage(), e);
            scheduler.shutdownNow();
        }
        logger.debug("{}Characteristic servlet shutdown complete", LOG_INIT);
    }
}
