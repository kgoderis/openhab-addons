package org.openhab.io.homekit.internal.events.graph;

import org.openhab.core.events.Event;

/**
 * Event indicating an update to the event processing graph.
 */
public class HomekitEventGraphUpdatedEvent implements Event {
    public static final String TYPE = "HomekitEventGraphUpdatedEvent";

    private final String nodeId;
    private final String edgeId;

    public HomekitEventGraphUpdatedEvent(String nodeId, String edgeId) {
        this.nodeId = nodeId;
        this.edgeId = edgeId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getEdgeId() {
        return edgeId;
    }

    @Override
    public String getTopic() {
        return "openhab/homekit/event-graph/updated";
    }

    @Override
    public String getPayload() {
        return String.format("{\"nodeId\":\"%s\",\"edgeId\":\"%s\"}", nodeId, edgeId);
    }

    @Override
    public String getSource() {
        return "homekit-event-graph";
    }
}
