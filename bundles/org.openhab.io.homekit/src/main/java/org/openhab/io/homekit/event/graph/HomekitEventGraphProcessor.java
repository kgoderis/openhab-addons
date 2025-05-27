package org.openhab.io.homekit.event.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.io.homekit.api.event.HomekitEventType;

/**
 * Core implementation of the HomeKit event processing graph with advanced analysis capabilities.
 * This class provides functionality to build, maintain, and analyze the event processing graph
 * that represents relationships between HomeKit events, accessories, services, and characteristics.
 *
 * The class integrates with:
 * - {@link org.openhab.io.homekit.api.event.HomekitEventType} for event type management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphService} for graph management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphVisualizer} for graph visualization
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphMetrics} for graph metrics
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphUpdatedEvent} for graph updates
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventCycleDetectedEvent} for cycle detection
 *
 * Key implementation details:
 * - Uses adjacency list representation for efficient graph operations
 * - Maintains metadata for nodes and edges
 * - Provides cycle detection and path analysis
 * - Supports performance metrics and dependency analysis
 *
 * @author Karel Goderis - Initial contribution
 */
public class HomekitEventGraphProcessor {
    protected final Map<String, EventProcessingNode> nodes = new HashMap<>();
    protected final Map<String, Set<String>> graph = new HashMap<>();
    protected final Map<String, Map<String, Object>> edgeMetadata = new HashMap<>();

    /**
     * Represents a node in the event processing graph.
     * Each node maintains information about its incoming and outgoing edges,
     * as well as metadata about the events it processes.
     *
     * Key implementation details:
     * - Tracks incoming and outgoing edges for dependency analysis
     * - Stores metadata for performance metrics and event processing
     * - Maintains unique identifier for node identification
     */
    public static class EventProcessingNode {
        public final String id;
        public final Set<String> incomingEdges = new HashSet<>();
        public final Set<String> outgoingEdges = new HashSet<>();
        public final Map<String, Object> metadata = new HashMap<>();

        /**
         * Creates a new event processing node.
         * Initializes the node with a unique identifier and empty edge sets.
         *
         * @param id the unique identifier for the node
         */
        public EventProcessingNode(String id) {
            this.id = id;
        }
    }

    /**
     * Adds a new node to the graph if it doesn't already exist.
     * Creates both the node and its corresponding entry in the adjacency list.
     *
     * Key implementation details:
     * - Uses computeIfAbsent for thread-safe node creation
     * - Initializes empty edge sets for the new node
     *
     * @param nodeId the identifier of the node to add
     */
    public void addNode(String nodeId) {
        nodes.computeIfAbsent(nodeId, EventProcessingNode::new);
        graph.computeIfAbsent(nodeId, k -> new HashSet<>());
    }

    /**
     * Adds a new edge to the graph between two nodes.
     * If either node doesn't exist, it will be created.
     *
     * Key implementation details:
     * - Creates nodes if they don't exist
     * - Updates both nodes' edge sets
     * - Initializes edge metadata
     *
     * @param sourceId the identifier of the source node
     * @param targetId the identifier of the target node
     */
    public void addEdge(String sourceId, String targetId) {
        addNode(sourceId);
        addNode(targetId);

        graph.get(sourceId).add(targetId);
        nodes.get(sourceId).outgoingEdges.add(targetId);
        nodes.get(targetId).incomingEdges.add(sourceId);

        String edgeId = sourceId + "->" + targetId;
        edgeMetadata.computeIfAbsent(edgeId, k -> new HashMap<>());
    }

    /**
     * Returns all nodes in the graph.
     * Provides access to the complete set of nodes and their metadata.
     *
     * @return a map of node identifiers to their corresponding nodes
     */
    public Map<String, EventProcessingNode> getNodes() {
        return nodes;
    }

    /**
     * Returns the adjacency list representation of the graph.
     * This representation shows all outgoing edges for each node.
     *
     * @return a map of node identifiers to their sets of outgoing edges
     */
    public Map<String, Set<String>> getGraph() {
        return graph;
    }

    /**
     * Returns the metadata associated with a specific edge.
     * Edge metadata can include performance metrics, event types, and other properties.
     *
     * @param edgeId the identifier of the edge
     * @return a map of metadata key-value pairs for the edge
     */
    public Map<String, Object> getEdgeMetadata(String edgeId) {
        return edgeMetadata.getOrDefault(edgeId, new HashMap<>());
    }

    /**
     * Checks if the graph contains any cycles.
     * A cycle is a path that starts and ends at the same node.
     *
     * Key implementation details:
     * - Uses depth-first search with recursion stack
     * - Tracks visited nodes to avoid redundant checks
     * - Detects cycles in directed graphs
     *
     * @return true if the graph contains at least one cycle, false otherwise
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : nodes.keySet()) {
            if (hasCycleUtil(nodeId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method for cycle detection using depth-first search.
     * Tracks visited nodes and recursion stack to detect cycles.
     *
     * @param nodeId the current node being visited
     * @param visited set of nodes that have been visited
     * @param recursionStack set of nodes in the current recursion path
     * @return true if a cycle is detected, false otherwise
     */
    private boolean hasCycleUtil(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (String neighbor : graph.getOrDefault(nodeId, new HashSet<>())) {
            if (hasCycleUtil(neighbor, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * Returns the path of nodes that form a cycle in the graph.
     * If multiple cycles exist, returns the first one found.
     *
     * Key implementation details:
     * - Uses depth-first search with parent tracking
     * - Builds cycle path from parent relationships
     * - Returns empty string if no cycle exists
     *
     * @return a string representation of the cycle path, or an empty string if no cycle exists
     */
    public String getCyclePath() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Map<String, String> parent = new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            if (getCyclePathUtil(nodeId, visited, recursionStack, parent)) {
                return buildCyclePath(nodeId, parent);
            }
        }
        return "";
    }

    /**
     * Helper method for finding cycle paths using depth-first search.
     * Tracks parent relationships to reconstruct the cycle path.
     *
     * @param nodeId the current node being visited
     * @param visited set of nodes that have been visited
     * @param recursionStack set of nodes in the current recursion path
     * @param parent map tracking parent relationships
     * @return true if a cycle is detected, false otherwise
     */
    private boolean getCyclePathUtil(String nodeId, Set<String> visited, Set<String> recursionStack,
            Map<String, String> parent) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (String neighbor : graph.getOrDefault(nodeId, new HashSet<>())) {
            parent.put(neighbor, nodeId);
            if (getCyclePathUtil(neighbor, visited, recursionStack, parent)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * Builds a string representation of a cycle path.
     * Uses parent relationships to reconstruct the path.
     *
     * @param nodeId the starting node of the cycle
     * @param parent map of parent relationships
     * @return string representation of the cycle path
     */
    private String buildCyclePath(String nodeId, Map<String, String> parent) {
        StringBuilder path = new StringBuilder();
        String current = nodeId;
        Set<String> seen = new HashSet<>();

        while (current != null && !seen.contains(current)) {
            seen.add(current);
            path.insert(0, current + " -> ");
            current = parent.get(current);
        }

        return path.substring(0, path.length() - 4); // Remove last " -> "
    }

    /**
     * Analyzes event flows and returns counts for each node.
     * The count represents the number of outgoing edges from each node.
     *
     * Key implementation details:
     * - Counts outgoing edges for each node
     * - Provides insight into event propagation
     *
     * @return a map of node identifiers to their event flow counts
     */
    public Map<String, Integer> analyzeEventFlows() {
        Map<String, Integer> flowCounts = new HashMap<>();
        for (EventProcessingNode node : nodes.values()) {
            flowCounts.put(node.id, node.outgoingEdges.size());
        }
        return flowCounts;
    }

    /**
     * Calculates performance metrics for each node.
     * The metrics include average processing time per event.
     *
     * Key implementation details:
     * - Uses metadata to track processing times
     * - Calculates averages based on event counts
     * - Handles edge cases with default values
     *
     * @return a map of node identifiers to their average processing times
     */
    public Map<String, Double> calculateNodePerformance() {
        Map<String, Double> performance = new HashMap<>();
        for (EventProcessingNode node : nodes.values()) {
            double avgProcessingTime = (double) node.metadata.getOrDefault("totalProcessingTime", 0L)
                    / (int) node.metadata.getOrDefault("eventCount", 1);
            performance.put(node.id, avgProcessingTime);
        }
        return performance;
    }

    /**
     * Finds all dependencies for a given node.
     * Dependencies are nodes that must be processed before the given node.
     *
     * Key implementation details:
     * - Uses depth-first search to find dependencies
     * - Tracks visited nodes to avoid cycles
     * - Includes both direct and indirect dependencies
     *
     * @param nodeId the identifier of the node to find dependencies for
     * @return a set of node identifiers that are dependencies
     */
    public Set<String> findDependencies(String nodeId) {
        Set<String> dependencies = new HashSet<>();
        findDependenciesUtil(nodeId, dependencies);
        return dependencies;
    }

    /**
     * Helper method for finding dependencies using depth-first search.
     * Recursively traverses incoming edges to find all dependencies.
     *
     * @param nodeId the current node being processed
     * @param dependencies set to store found dependencies
     */
    private void findDependenciesUtil(String nodeId, Set<String> dependencies) {
        EventProcessingNode node = nodes.get(nodeId);
        if (node == null)
            return;

        for (String incoming : node.incomingEdges) {
            if (dependencies.add(incoming)) {
                findDependenciesUtil(incoming, dependencies);
            }
        }
    }

    /**
     * Analyzes event types for each node.
     * Returns a map of node identifiers to their sets of event types.
     *
     * Key implementation details:
     * - Extracts event types from node metadata
     * - Handles type casting with suppression of unchecked warnings
     * - Skips nodes without event type information
     *
     * @return a map of node identifiers to their event type sets
     */
    public Map<String, Set<HomekitEventType>> analyzeEventTypes() {
        Map<String, Set<HomekitEventType>> eventTypes = new HashMap<>();
        for (EventProcessingNode node : nodes.values()) {
            @SuppressWarnings("unchecked")
            Set<HomekitEventType> types = (Set<HomekitEventType>) node.metadata.get("eventTypes");
            if (types != null) {
                eventTypes.put(node.id, types);
            }
        }
        return eventTypes;
    }

    /**
     * Calculates the density of the graph.
     * Density is the ratio of actual edges to possible edges.
     *
     * Key implementation details:
     * - Calculates total number of nodes and edges
     * - Computes maximum possible edges for a directed graph
     * - Handles edge case of empty graph
     *
     * @return the graph density as a value between 0 and 1
     */
    public double calculateDensity() {
        int totalNodes = nodes.size();
        int totalEdges = graph.values().stream().mapToInt(Set::size).sum();
        int maxPossibleEdges = totalNodes * (totalNodes - 1);
        return maxPossibleEdges > 0 ? (double) totalEdges / maxPossibleEdges : 0.0;
    }

    /**
     * Finds critical paths in the graph.
     * A critical path is a path that must be processed in a specific order.
     *
     * Key implementation details:
     * - Identifies nodes with no incoming or outgoing edges
     * - These nodes represent start or end points of critical paths
     * - Used for identifying potential bottlenecks in event processing
     *
     * @return a set of node identifiers that are part of critical paths
     */
    public Set<String> findCriticalPaths() {
        Set<String> criticalPaths = new HashSet<>();
        for (EventProcessingNode node : nodes.values()) {
            if (node.incomingEdges.isEmpty() || node.outgoingEdges.isEmpty()) {
                criticalPaths.add(node.id);
            }
        }
        return criticalPaths;
    }

    /**
     * Calculates node centrality for each node.
     * Centrality is based on the number of incoming and outgoing edges.
     *
     * Key implementation details:
     * - Uses degree centrality (sum of incoming and outgoing edges)
     * - Higher centrality indicates more important nodes
     * - Used for identifying key nodes in the event flow
     *
     * @return a map of node identifiers to their centrality values
     */
    public Map<String, Double> calculateNodeCentrality() {
        Map<String, Double> centrality = new HashMap<>();
        for (EventProcessingNode node : nodes.values()) {
            double degree = node.incomingEdges.size() + node.outgoingEdges.size();
            centrality.put(node.id, degree);
        }
        return centrality;
    }

    /**
     * Finds potential bottlenecks in the graph.
     * A bottleneck is a node that has multiple incoming edges but only one outgoing edge.
     *
     * Key implementation details:
     * - Identifies nodes with high incoming edge count
     * - Checks for single outgoing edge
     * - Used for performance optimization and load balancing
     *
     * @return a set of node identifiers that are potential bottlenecks
     */
    public Set<String> findBottlenecks() {
        Set<String> bottlenecks = new HashSet<>();
        for (EventProcessingNode node : nodes.values()) {
            if (node.incomingEdges.size() > 1 && node.outgoingEdges.size() == 1) {
                bottlenecks.add(node.id);
            }
        }
        return bottlenecks;
    }
}
