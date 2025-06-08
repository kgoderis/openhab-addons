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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing the HomeKit event processing graph and its components.
 * This service provides functionality to build, maintain, and monitor the event graph
 * that represents relationships between HomeKit events, accessories, services, and characteristics.
 *
 * The service integrates with:
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphVisualizer} for graph visualization
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphIntegrations} for graph integrations
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphMetrics} for graph metrics
 * - {@link org.osgi.service.component.annotations.Component} for OSGi service registration
 *
 * @author Karel Goderis - Initial contribution
 */
@Component(service = HomekitEventGraphService.class)
@NonNullByDefault
public class HomekitEventGraphService {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventGraphService.class);

    private final HomekitEventGraphProcessor eventGraph;
    private final HomekitEventGraphVisualizer eventGraphVisualizer;
    private final HomekitEventGraphIntegrations eventGraphIntegrations;
    private final HomekitEventGraphMetrics eventGraphMetrics;

    /**
     * Creates a new instance of the HomeKit event graph service.
     * Initializes the graph processor, visualizer, integrations, and metrics components.
     * 
     * Key implementation details:
     * - Creates a new event graph processor
     * - Initializes the graph visualizer with the processor
     * - Sets up graph integrations with custom metrics handling
     * - Creates a new metrics instance
     * - Logs service startup
     */
    @Activate
    public HomekitEventGraphService(@Reference EventPublisher eventPublisher) {
        this.eventGraph = new HomekitEventGraphProcessor();
        this.eventGraphVisualizer = new HomekitEventGraphVisualizer(eventGraph);
        this.eventGraphIntegrations = new HomekitEventGraphIntegrations(eventGraph, eventGraphVisualizer,
                eventPublisher) {
            @Override
            public void registerMetrics() {
                // Override to use our custom metrics
                eventGraphMetrics.getAllNodeMetrics()
                        .forEach((key, value) -> logger.info("Node metric {}: {}", key, value));
                eventGraphMetrics.getAllEdgeMetrics()
                        .forEach((key, value) -> logger.info("Edge metric {}: {}", key, value));
                eventGraphMetrics.getAllCycleMetrics()
                        .forEach((key, value) -> logger.info("Cycle metric {}: {}", key, value));
            }
        };
        this.eventGraphMetrics = new HomekitEventGraphMetrics();

        logger.info("Event Graph HomekitService started");
    }

    /**
     * Deactivates the HomeKit event graph service.
     * Logs the service shutdown.
     * 
     * Key implementation details:
     * - Logs service shutdown message
     * - Called by OSGi framework during service deactivation
     */
    @Deactivate
    public void deactivate() {
        logger.info("Event Graph HomekitService stopped");
    }

    /**
     * Returns the event graph processor instance.
     * The processor is responsible for managing the graph structure and performing
     * graph analysis operations.
     *
     * @return the event graph processor
     */
    public HomekitEventGraphProcessor getEventGraph() {
        return eventGraph;
    }

    /**
     * Returns the event graph visualizer instance.
     * The visualizer provides functionality to generate visual representations
     * of the event graph for debugging and monitoring purposes.
     *
     * @return the event graph visualizer
     */
    public HomekitEventGraphVisualizer getEventGraphVisualizer() {
        return eventGraphVisualizer;
    }

    /**
     * Returns the event graph integrations instance.
     * The integrations component handles the interaction between the event graph
     * and other OpenHAB components.
     *
     * @return the event graph integrations
     */
    public HomekitEventGraphIntegrations getEventGraphIntegrations() {
        return eventGraphIntegrations;
    }

    /**
     * Returns the event graph metrics instance.
     * The metrics component collects and maintains various statistics about
     * the event graph's structure and performance.
     *
     * @return the event graph metrics
     */
    public HomekitEventGraphMetrics getEventGraphMetrics() {
        return eventGraphMetrics;
    }

    /**
     * Updates the event graph with a new node and edge.
     * This method adds the specified node and edge to the graph, updates the metrics,
     * and checks for cycles in the graph structure.
     *
     * Key implementation details:
     * - Adds the node and edge to the graph
     * - Updates node and edge metrics
     * - Checks for cycles in the graph
     * - Logs warnings if cycles are detected
     *
     * @param nodeId the identifier of the node to add
     * @param edgeId the identifier of the edge to add
     */
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

    /**
     * Logs the current state of the event graph.
     * This method uses the graph integrations to output the current graph state
     * for debugging or monitoring purposes.
     *
     * Key implementation details:
     * - Uses the graph integrations component to log state
     * - Includes information about nodes, edges, and metrics
     */
    public void logGraphState() {
        eventGraphIntegrations.logGraphState();
    }
}
