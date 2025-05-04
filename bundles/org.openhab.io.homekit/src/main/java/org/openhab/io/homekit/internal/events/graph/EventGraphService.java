package org.openhab.io.homekit.internal.events.graph;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing the event processing graph and its components.
 */
@Component(service = EventGraphService.class)
public class EventGraphService {
    private static final Logger logger = LoggerFactory.getLogger(EventGraphService.class);
    
    private final EventGraphProcessor eventGraph;
    private final EventGraphVisualizer eventGraphVisualizer;
    private final EventGraphIntegrations eventGraphIntegrations;
    private final EventGraphMetrics eventGraphMetrics;
    
    @Activate
    public EventGraphService() {
        this.eventGraph = new EventGraphProcessor();
        this.eventGraphVisualizer = new EventGraphVisualizer(eventGraph);
        this.eventGraphIntegrations = new EventGraphIntegrations(eventGraph, eventGraphVisualizer, null) {
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
        this.eventGraphMetrics = new EventGraphMetrics();
        
        logger.info("Event Graph Service started");
    }
    
    @Deactivate
    public void deactivate() {
        logger.info("Event Graph Service stopped");
    }
    
    public EventGraphProcessor getEventGraph() {
        return eventGraph;
    }
    
    public EventGraphVisualizer getEventGraphVisualizer() {
        return eventGraphVisualizer;
    }
    
    public EventGraphIntegrations getEventGraphIntegrations() {
        return eventGraphIntegrations;
    }
    
    public EventGraphMetrics getEventGraphMetrics() {
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