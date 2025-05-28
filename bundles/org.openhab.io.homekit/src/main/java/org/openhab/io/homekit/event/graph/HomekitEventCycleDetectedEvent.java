package org.openhab.io.homekit.event.graph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.Event;

/**
 * Event class representing the detection of a cycle in the HomeKit event graph.
 * This event is used to notify subscribers when a cycle is detected in the event processing graph,
 * which could indicate potential issues in the event flow or processing logic.
 *
 * The class integrates with:
 * - {@link org.openhab.core.events.Event} for base event functionality
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphService} for graph management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphMetrics} for cycle tracking
 *
 * Key implementation details:
 * - Uses a string representation of the cycle path for easy serialization
 * - Provides JSON-formatted payload for event subscribers
 * - Integrates with OpenHAB's event system for notification
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEventCycleDetectedEvent implements Event {
    /** The type identifier for this event. */
    public static final String TYPE = "HomekitEventCycleDetectedEvent";

    private final String cyclePath;

    /**
     * Creates a new cycle detection event.
     * The event is initialized with the path of nodes that form the detected cycle.
     *
     * @param cyclePath the path of nodes that form the detected cycle, represented as a string
     */
    public HomekitEventCycleDetectedEvent(String cyclePath) {
        this.cyclePath = cyclePath;
    }

    /**
     * Returns the type identifier for this event.
     * This is used by the event system to identify and route the event.
     *
     * @return the event type identifier
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Returns the path of nodes that form the detected cycle.
     * The path is represented as a string, typically containing node identifiers
     * separated by a delimiter to show the sequence of nodes in the cycle.
     *
     * @return the cycle path as a string
     */
    public String getCyclePath() {
        return cyclePath;
    }

    /**
     * Returns the topic for this event.
     * The topic is used by the event system to route the event to interested subscribers.
     *
     * @return the event topic
     */
    @Override
    public String getTopic() {
        return "openhab/homekit/event-graph/cycle-detected";
    }

    /**
     * Returns the payload for this event.
     * The payload is formatted as a JSON string containing the cycle path.
     *
     * @return the event payload as a JSON string
     */
    @Override
    public String getPayload() {
        return String.format("{\"cyclePath\":\"%s\"}", cyclePath);
    }

    /**
     * Returns the source identifier for this event.
     * This identifies the component that generated the event.
     *
     * @return the event source identifier
     */
    @Override
    public String getSource() {
        return "homekit-event-graph";
    }
}
