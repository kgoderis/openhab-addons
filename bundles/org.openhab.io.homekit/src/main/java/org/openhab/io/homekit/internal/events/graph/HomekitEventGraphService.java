package org.openhab.io.homekit.internal.events.graph;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomekitService for managing the event processing graph and its components.
 */
@Component(service = HomekitEventGraphService.class)
public class HomekitEventGraphService {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventGraphService.class);

    private final HomekitEventGraphProcessor eventGraph;
    private final HomekitEventGraphVisualizer eventGraphVisualizer;
    private final HomekitEventGraphIntegrations eventGraphIntegrations;
    private final HomekitEventGraphMetrics eventGraphMetrics;

    @Activate
    public HomekitEventGraphService() {
        this.eventGraph = new HomekitEventGraphProcessor();
        this.eventGraphVisualizer = new HomekitEventGraphVisualizer(eventGraph);
        this.eventGraphIntegrations = new HomekitEventGraphIntegrations(eventGraph, eventGraphVisualizer, null) {
            @Override
            public void registerMetrics() {
                // Override to use our custom metrics
                eventGraphMetrics.getAllNodeMetrics().forEach((key, value) -> 
                    logger.info("Node metric {}: {}", key, value));
                eventGraphMetrics.getAllEdgeMetrics().forEach((key, value) -> 
                    logger.info("Edge metric {}: {}", key, value));
                eventGraphMetrics.getAllCycleMetrics().forEach((key, value) -> 
                    logger.info("Cycle metric {}: {}", key, value));
            }
        };
        this.eventGraphMetrics = new HomekitEventGraphMetrics();
        
        logger.info("Event Graph HomekitService started");
    }

    @Deactivate
    public void deactivate() {
        logger.info("Event Graph HomekitService stopped");
    }

    public HomekitEventGraphProcessor getEventGraph() {
        return eventGraph;
    }

    public HomekitEventGraphVisualizer getEventGraphVisualizer() {
        return eventGraphVisualizer;
    }

    public HomekitEventGraphIntegrations getEventGraphIntegrations() {
        return eventGraphIntegrations;
    }

    public HomekitEventGraphMetrics getEventGraphMetrics() {
        return eventGraphMetrics;
    }

    public void updateGraph(String nodeId, String edgeId) {
        eventGraph.addNode(nodeId);
        eventGraph.addEdge(nodeId, edgeId);

        // Update metrics
        eventGraphMetrics.incrementNodeMetric(nodeId, "updates");
        eventGraphMetrics.incrementEdgeMetric(nodeId, edgeId, "updates");

        // Check for cycles
        if (eventGraph.hasCycle()) {
            String cyclePath = eventGraph.getCyclePath();
            eventGraphMetrics.incrementCycleMetric(cyclePath, "detections");
            logger.warn("Cycle detected in event graph: {}", cyclePath);
        }
    }

    public void logGraphState() {
        eventGraphIntegrations.logGraphState();
    }
}
