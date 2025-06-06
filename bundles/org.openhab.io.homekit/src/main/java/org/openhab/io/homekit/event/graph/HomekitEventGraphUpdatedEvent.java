package org.openhab.io.homekit.event.graph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.Event;

/**
 * Event indicating an update to the HomeKit event processing graph.
 * This event is used to notify subscribers when changes occur in the event graph structure,
 * such as the addition of new nodes or edges, or modifications to existing relationships.
 *
 * The class integrates with:
 * - {@link org.openhab.core.events.Event} for base event functionality
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphService} for graph management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphMetrics} for graph metrics
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEventGraphUpdatedEvent implements Event {
    /** The type identifier for this event. */
    public static final String TYPE = "HomekitEventGraphUpdatedEvent";

    private final String nodeId;
    private final String edgeId;

    /**
     * Creates a new event graph update event.
     * This event is published when changes occur in the event graph structure.
     *
     * Key implementation details:
     * - Stores the identifiers of the updated node and edge
     * - Used by the graph service to notify subscribers of changes
     *
     * @param nodeId the identifier of the updated node
     * @param edgeId the identifier of the updated edge
     */
    public HomekitEventGraphUpdatedEvent(String nodeId, String edgeId) {
        this.nodeId = nodeId;
        this.edgeId = edgeId;
    }

    /**
     * Returns the type identifier for this event.
     * This is used to identify the event type in the event system.
     *
     * @return the event type identifier
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Returns the identifier of the updated node.
     * This identifies which node in the graph was modified.
     *
     * @return the node identifier
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns the identifier of the updated edge.
     * This identifies which edge in the graph was modified.
     *
     * @return the edge identifier
     */
    public String getEdgeId() {
        return edgeId;
    }

    /**
     * Returns the topic for this event.
     * The topic is used for event routing in the OpenHAB event system.
     *
     * @return the event topic
     */
    @Override
    public String getTopic() {
        return "openhab/homekit/event-graph/updated";
    }

    /**
     * Returns the payload for this event.
     * The payload contains the node and edge identifiers in JSON format.
     *
     * @return the event payload as a JSON string
     */
    @Override
    public String getPayload() {
        return String.format("{\"nodeId\":\"%s\",\"edgeId\":\"%s\"}", nodeId, edgeId);
    }

    /**
     * Returns the source of this event.
     * The source identifies the component that generated the event.
     *
     * @return the event source
     */
    @Override
    public String getSource() {
        return "homekit-event-graph";
    }
}
