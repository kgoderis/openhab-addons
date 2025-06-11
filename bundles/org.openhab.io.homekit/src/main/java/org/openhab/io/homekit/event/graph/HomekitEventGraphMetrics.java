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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Utility class for tracking and managing HomeKit event graph metrics.
 * This class provides functionality to collect and maintain various metrics related to the event graph,
 * including node metrics, edge metrics, and cycle detection metrics.
 *
 * The class integrates with:
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphService} for graph management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphVisualizer} for graph visualization
 * - {@link java.util.concurrent.ConcurrentHashMap} for thread-safe metric storage
 * - {@link java.util.concurrent.atomic.AtomicLong} for atomic metric updates
 *
 * Key implementation details:
 * - Uses thread-safe collections for concurrent access
 * - Maintains separate metric maps for nodes, edges, and cycles
 * - Provides atomic increment operations for metrics
 * - Supports metric reset functionality
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEventGraphMetrics {
    private final Map<String, AtomicLong> nodeMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> edgeMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cycleMetrics = new ConcurrentHashMap<>();

    /**
     * Increments a metric for a specific node.
     * Uses atomic operations to ensure thread safety.
     *
     * Key implementation details:
     * - Creates metric key from node ID and metric name
     * - Uses computeIfAbsent for thread-safe initialization
     * - Performs atomic increment operation
     *
     * @param nodeId the identifier of the node
     * @param metric the name of the metric to increment
     */
    public void incrementNodeMetric(String nodeId, String metric) {
        String key = String.format("node.%s.%s", nodeId, metric);
        // Null Pointer Access Warning Checked
        // computeIfAbsent cannot return null since we're providing a non-null supplier function
        AtomicLong counter = nodeMetrics.computeIfAbsent(key, k -> new AtomicLong());
        counter.incrementAndGet();
    }

    /**
     * Increments a metric for a specific edge.
     * Uses atomic operations to ensure thread safety.
     *
     * Key implementation details:
     * - Creates metric key from source, target, and metric name
     * - Uses computeIfAbsent for thread-safe initialization
     * - Performs atomic increment operation
     *
     * @param sourceId the identifier of the source node
     * @param targetId the identifier of the target node
     * @param metric the name of the metric to increment
     */
    public void incrementEdgeMetric(String sourceId, String targetId, String metric) {
        String key = String.format("edge.%s->%s.%s", sourceId, targetId, metric);
        // Null Pointer Access Warning Checked
        // computeIfAbsent cannot return null since we're providing a non-null supplier function
        AtomicLong counter = edgeMetrics.computeIfAbsent(key, k -> new AtomicLong());
        counter.incrementAndGet();
    }

    /**
     * Increments a metric for a specific cycle.
     * Uses atomic operations to ensure thread safety.
     *
     * Key implementation details:
     * - Creates metric key from cycle path and metric name
     * - Uses computeIfAbsent for thread-safe initialization
     * - Performs atomic increment operation
     *
     * @param cyclePath the path of nodes that form the cycle
     * @param metric the name of the metric to increment
     */
    public void incrementCycleMetric(String cyclePath, String metric) {
        String key = String.format("cycle.%s.%s", cyclePath, metric);
        // Null Pointer Access Warning Checked
        // computeIfAbsent cannot return null since we're providing a non-null supplier function
        AtomicLong counter = cycleMetrics.computeIfAbsent(key, k -> new AtomicLong());
        counter.incrementAndGet();
    }

    /**
     * Retrieves the current value of a metric for a specific node.
     * Returns 0 if the metric doesn't exist.
     *
     * Key implementation details:
     * - Creates metric key from node ID and metric name
     * - Uses getOrDefault for safe access
     * - Returns atomic long value
     *
     * @param nodeId the identifier of the node
     * @param metric the name of the metric to retrieve
     * @return the current value of the metric
     */
    public long getNodeMetric(String nodeId, String metric) {
        String key = String.format("node.%s.%s", nodeId, metric);
        return nodeMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    /**
     * Retrieves the current value of a metric for a specific edge.
     * Returns 0 if the metric doesn't exist.
     *
     * Key implementation details:
     * - Creates metric key from source, target, and metric name
     * - Uses getOrDefault for safe access
     * - Returns atomic long value
     *
     * @param sourceId the identifier of the source node
     * @param targetId the identifier of the target node
     * @param metric the name of the metric to retrieve
     * @return the current value of the metric
     */
    public long getEdgeMetric(String sourceId, String targetId, String metric) {
        String key = String.format("edge.%s->%s.%s", sourceId, targetId, metric);
        return edgeMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    /**
     * Retrieves the current value of a metric for a specific cycle.
     * Returns 0 if the metric doesn't exist.
     *
     * Key implementation details:
     * - Creates metric key from cycle path and metric name
     * - Uses getOrDefault for safe access
     * - Returns atomic long value
     *
     * @param cyclePath the path of nodes that form the cycle
     * @param metric the name of the metric to retrieve
     * @return the current value of the metric
     */
    public long getCycleMetric(String cyclePath, String metric) {
        String key = String.format("cycle.%s.%s", cyclePath, metric);
        return cycleMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    /**
     * Returns all metrics for all nodes.
     * Creates a new map with current metric values.
     *
     * Key implementation details:
     * - Creates new concurrent map for thread safety
     * - Copies current values from atomic longs
     * - Returns immutable view of metrics
     *
     * @return a map of metric keys to their current values
     */
    public Map<String, Long> getAllNodeMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        nodeMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * Returns all metrics for all edges.
     * Creates a new map with current metric values.
     *
     * Key implementation details:
     * - Creates new concurrent map for thread safety
     * - Copies current values from atomic longs
     * - Returns immutable view of metrics
     *
     * @return a map of metric keys to their current values
     */
    public Map<String, Long> getAllEdgeMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        edgeMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * Returns all metrics for all cycles.
     * Creates a new map with current metric values.
     *
     * Key implementation details:
     * - Creates new concurrent map for thread safety
     * - Copies current values from atomic longs
     * - Returns immutable view of metrics
     *
     * @return a map of metric keys to their current values
     */
    public Map<String, Long> getAllCycleMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        cycleMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * Resets all metrics to their initial values.
     * This method clears all collected metrics for nodes, edges, and cycles.
     *
     * Key implementation details:
     * - Clears all metric maps
     * - Thread-safe operation
     * - Removes all collected data
     */
    public void resetMetrics() {
        nodeMetrics.clear();
        edgeMetrics.clear();
        cycleMetrics.clear();
    }
}
