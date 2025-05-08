package org.openhab.io.homekit.internal.events.graph;

import org.openhab.core.events.Event;

/**
 * Event indicating a cycle was detected in the event processing graph.
 */
public class HomekitEventCycleDetectedEvent implements Event {
    public static final String TYPE = "HomekitEventCycleDetectedEvent";

    private final String cyclePath;

    public HomekitEventCycleDetectedEvent(String cyclePath) {
        this.cyclePath = cyclePath;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getCyclePath() {
        return cyclePath;
    }

    @Override
    public String getTopic() {
        return "openhab/homekit/event-graph/cycle-detected";
    }

    @Override
    public String getPayload() {
        return String.format("{\"cyclePath\":\"%s\"}", cyclePath);
    }

    @Override
    public String getSource() {
        return "homekit-event-graph";
    }
}
