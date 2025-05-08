package org.openhab.io.homekit.internal.events.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.io.homekit.internal.events.HomekitEventType;

/**
 * Event processing graph implementation with analysis capabilities.
 */
public class HomekitEventGraphProcessor {
    protected final Map<String, EventProcessingNode> nodes = new HashMap<>();
    protected final Map<String, Set<String>> graph = new HashMap<>();
    protected final Map<String, Map<String, Object>> edgeMetadata = new HashMap<>();

    public static class EventProcessingNode {
        public final String id;
        public final Set<String> incomingEdges = new HashSet<>();
        public final Set<String> outgoingEdges = new HashSet<>();
        public final Map<String, Object> metadata = new HashMap<>();

        public EventProcessingNode(String id) {
            this.id = id;
        }
    }

    public void addNode(String nodeId) {
        nodes.computeIfAbsent(nodeId, EventProcessingNode::new);
        graph.computeIfAbsent(nodeId, k -> new HashSet<>());
    }

    public void addEdge(String sourceId, String targetId) {
        addNode(sourceId);
        addNode(targetId);

        graph.get(sourceId).add(targetId);
        nodes.get(sourceId).outgoingEdges.add(targetId);
        nodes.get(targetId).incomingEdges.add(sourceId);

        String edgeId = sourceId + "->" + targetId;
        edgeMetadata.computeIfAbsent(edgeId, k -> new HashMap<>());
    }

    public Map<String, EventProcessingNode> getNodes() {
        return nodes;
    }

    public Map<String, Set<String>> getGraph() {
        return graph;
    }

    public Map<String, Object> getEdgeMetadata(String edgeId) {
        return edgeMetadata.getOrDefault(edgeId, new HashMap<>());
    }

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
     * Analyze event flows and return counts for each node
     */
    public Map<String, Integer> analyzeEventFlows() {
        Map<String, Integer> flowCounts = new HashMap<>();
        for (EventProcessingNode node : nodes.values()) {
            flowCounts.put(node.id, node.outgoingEdges.size());
        }
        return flowCounts;
    }

    /**
     * Calculate performance metrics for each node
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
     * Find all dependencies for a given node
     */
    public Set<String> findDependencies(String nodeId) {
        Set<String> dependencies = new HashSet<>();
        findDependenciesUtil(nodeId, dependencies);
        return dependencies;
    }

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
     * Analyze event types for each node
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
     * Calculate graph density
     */
    public double calculateDensity() {
        int totalNodes = nodes.size();
        int totalEdges = graph.values().stream().mapToInt(Set::size).sum();
        int maxPossibleEdges = totalNodes * (totalNodes - 1);
        return maxPossibleEdges > 0 ? (double) totalEdges / maxPossibleEdges : 0.0;
    }

    /**
     * Find critical paths in the graph
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
     * Calculate node centrality
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
     * Find potential bottlenecks
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
