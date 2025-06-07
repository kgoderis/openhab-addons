/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

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
 * Integration points for the HomeKit event processing graph with OpenHAB components.
 * This class provides functionality to integrate the event graph with other components
 * of the system, such as metrics collection, visualization, and event processing.
 *
 * The class integrates with:
 * - {@link org.openhab.core.events.Event} for event handling
 * - {@link org.openhab.core.events.EventPublisher} for event publishing
 * - {@link org.openhab.core.events.EventSubscriber} for event subscription
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphVisualizer} for graph visualization
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphUpdatedEvent} for graph updates
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventCycleDetectedEvent} for cycle detection
 * - {@link javax.ws.rs.GET} for REST endpoint definitions
 * - {@link com.google.gson.JsonObject} for JSON serialization
 *
 * Key implementation details:
 * - Provides REST endpoints for graph visualization and analysis
 * - Integrates with OpenHAB event system for graph updates
 * - Supports metrics collection and logging
 * - Handles cycle detection and bottleneck analysis
 *
 * @author Karel Goderis - Initial contribution
 */
public class HomekitEventGraphIntegrations {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventGraphIntegrations.class);

    private final HomekitEventGraphProcessor eventGraph;
    private final HomekitEventGraphVisualizer eventGraphVisualizer;
    private final EventPublisher eventPublisher;

    /**
     * Creates a new event graph integrations service.
     * Initializes the service with required components for graph processing,
     * visualization, and event publishing.
     *
     * Key implementation details:
     * - Stores references to core components
     * - Used by all integration methods
     * - Required for REST endpoints
     *
     * @param eventGraph the event graph processor
     * @param eventGraphVisualizer the event graph visualizer
     * @param eventPublisher the event publisher for posting events
     */
    public HomekitEventGraphIntegrations(HomekitEventGraphProcessor eventGraph,
            HomekitEventGraphVisualizer eventGraphVisualizer, EventPublisher eventPublisher) {
        this.eventGraph = eventGraph;
        this.eventGraphVisualizer = eventGraphVisualizer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers metrics with the OpenHAB metrics registry.
     * This method logs various graph metrics such as node count, edge count,
     * cycle detection, and graph density.
     *
     * Key implementation details:
     * - Uses SLF4J logger for output
     * - Calculates metrics from graph structure
     * - Logs at INFO level for visibility
     */
    public void registerMetrics() {
        logger.info("Event Graph Metrics:");
        logger.info("  Nodes: {}", eventGraph.getNodes().size());
        logger.info("  Edges: {}", eventGraph.getGraph().values().stream().mapToInt(Set::size).sum());
        logger.info("  Cycles: {}", eventGraph.hasCycle() ? 1 : 0);
        logger.info("  Density: {}", eventGraph.calculateDensity());
    }

    /**
     * Logs the current state of the event graph.
     * This method outputs detailed information about the graph structure,
     * including node and edge counts, cycle detection, performance metrics,
     * and potential bottlenecks.
     *
     * Key implementation details:
     * - Uses SLF4J logger for output
     * - Calculates performance metrics
     * - Identifies bottlenecks
     * - Logs at appropriate levels (INFO/WARN)
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
     * Registers event listeners for graph updates.
     * This method sets up a subscriber to handle graph update events and
     * cycle detection events.
     *
     * Key implementation details:
     * - Creates anonymous EventSubscriber implementation
     * - Handles HomekitEventGraphUpdatedEvent events
     * - Posts HomekitEventCycleDetectedEvent when cycles are found
     * - Uses reflection for subscriber registration
     *
     * @throws RuntimeException if event subscriber registration fails
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
     * REST endpoint for graph visualization.
     * Returns a JSON representation of the event graph structure.
     *
     * Key implementation details:
     * - Uses JAX-RS annotations for endpoint definition
     * - Returns JSON format for web visualization
     * - Includes node and edge information
     *
     * @return a JSON string containing the graph visualization
     */
    @GET
    @Path("/event-graph")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventGraph() {
        return eventGraphVisualizer.generateJsonGraph();
    }

    /**
     * REST endpoint for graph metrics.
     * Returns a JSON representation of the event graph metrics.
     *
     * Key implementation details:
     * - Uses JAX-RS annotations for endpoint definition
     * - Returns JSON format for metrics visualization
     * - Includes performance and event count data
     *
     * @return a JSON string containing the graph metrics
     */
    @GET
    @Path("/event-graph/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEventGraphMetrics() {
        return eventGraphVisualizer.generateMetricsGraph();
    }

    /**
     * REST endpoint for graph analysis.
     * Returns a comprehensive JSON analysis of the event graph, including:
     * - Event flow analysis
     * - Performance metrics
     * - Node centrality
     * - Critical paths
     * - Bottlenecks
     *
     * Key implementation details:
     * - Uses JAX-RS annotations for endpoint definition
     * - Returns JSON format for analysis visualization
     * - Combines multiple analysis types
     * - Uses Gson for JSON serialization
     *
     * @return a JSON string containing the graph analysis
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
