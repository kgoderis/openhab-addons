package org.openhab.io.homekit.event.graph;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Integration points for the event processing graph with OpenHAB components.
 */
public class HomekitEventGraphIntegrations {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventGraphIntegrations.class);

    private final HomekitEventGraphProcessor eventGraph;
    private final HomekitEventGraphVisualizer eventGraphVisualizer;
    private final EventPublisher eventPublisher;

    public HomekitEventGraphIntegrations(HomekitEventGraphProcessor eventGraph,
            HomekitEventGraphVisualizer eventGraphVisualizer, EventPublisher eventPublisher) {
        this.eventGraph = eventGraph;
        this.eventGraphVisualizer = eventGraphVisualizer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Register metrics with OpenHAB metrics registry
     */
    public void registerMetrics() {
        logger.info("Event Graph Metrics:");
        logger.info("  Nodes: {}", eventGraph.getNodes().size());
        logger.info("  Edges: {}", eventGraph.getGraph().values().stream().mapToInt(Set::size).sum());
        logger.info("  Cycles: {}", eventGraph.hasCycle() ? 1 : 0);
        logger.info("  Density: {}", eventGraph.calculateDensity());
    }

    /**
     * Log current graph state
     */
    public void logGraphState() {
        logger.info("Event Graph State:");
        logger.info("  Nodes: {}", eventGraph.getNodes().size());
        logger.info("  Edges: {}", eventGraph.getGraph().values().stream().mapToInt(Set::size).sum());
        logger.info("  Cycles: {}", eventGraph.hasCycle());
        if (eventGraph.hasCycle()) {
            logger.warn("  Cycle Path: {}", String.join(" -> ", eventGraph.getCyclePath()));
        }

        // Log performance metrics
        Map<String, Double> performance = eventGraph.calculateNodePerformance();
        performance
                .forEach((nodeId, avgTime) -> logger.info("  Node {} average processing time: {} ms", nodeId, avgTime));

        // Log bottlenecks
        Set<String> bottlenecks = eventGraph.findBottlenecks();
        if (!bottlenecks.isEmpty()) {
            logger.warn("  Potential bottlenecks: {}", bottlenecks);
        }
    }

    /**
     * Register event listeners for graph updates
     */
    public void registerEventListeners() {
        if (eventPublisher != null) {
            EventSubscriber subscriber = new EventSubscriber() {
                @Override
                public void receive(Event event) {
                    if (event instanceof HomekitEventGraphUpdatedEvent) {
                        HomekitEventGraphUpdatedEvent graphEvent = (HomekitEventGraphUpdatedEvent) event;
                        eventGraph.addNode(graphEvent.getNodeId());
                        eventGraph.addEdge(graphEvent.getNodeId(), graphEvent.getEdgeId());

                        if (eventGraph.hasCycle()) {
                            eventPublisher.post(new HomekitEventCycleDetectedEvent(eventGraph.getCyclePath()));
                        }
                    }
                }

                @Override
                public Set<String> getSubscribedEventTypes() {
                    return Set.of(HomekitEventGraphUpdatedEvent.TYPE);
                }
            };

            // Register the subscriber using the appropriate method
            try {
                eventPublisher.getClass().getMethod("addEventSubscriber", EventSubscriber.class).invoke(eventPublisher,
                        subscriber);
            } catch (Exception e) {
                logger.warn("Failed to register event subscriber: {}", e.getMessage());
            }
        }
    }

    /**
     * REST endpoint for graph visualization
     */
    @GET
    @Path("/event-graph")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventGraph() {
        return eventGraphVisualizer.generateJsonGraph();
    }

    /**
     * REST endpoint for graph metrics
     */
    @GET
    @Path("/event-graph/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventGraphMetrics() {
        return eventGraphVisualizer.generateMetricsGraph();
    }

    /**
     * REST endpoint for graph analysis
     */
    @GET
    @Path("/event-graph/analysis")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventGraphAnalysis() {
        JsonObject analysis = new JsonObject();

        // Add flow analysis
        Map<String, Integer> flows = eventGraph.analyzeEventFlows();
        JsonObject flowJson = new JsonObject();
        flows.forEach(flowJson::addProperty);
        analysis.add("flows", flowJson);

        // Add performance analysis
        Map<String, Double> performance = eventGraph.calculateNodePerformance();
        JsonObject performanceJson = new JsonObject();
        performance.forEach(performanceJson::addProperty);
        analysis.add("performance", performanceJson);

        // Add centrality analysis
        Map<String, Double> centrality = eventGraph.calculateNodeCentrality();
        JsonObject centralityJson = new JsonObject();
        centrality.forEach(centralityJson::addProperty);
        analysis.add("centrality", centralityJson);

        // Add critical paths
        Set<String> criticalPaths = eventGraph.findCriticalPaths();
        JsonArray criticalPathsJson = new JsonArray();
        criticalPaths.forEach(criticalPathsJson::add);
        analysis.add("criticalPaths", criticalPathsJson);

        // Add bottlenecks
        Set<String> bottlenecks = eventGraph.findBottlenecks();
        JsonArray bottlenecksJson = new JsonArray();
        bottlenecks.forEach(bottlenecksJson::add);
        analysis.add("bottlenecks", bottlenecksJson);

        return analysis.toString();
    }
}
