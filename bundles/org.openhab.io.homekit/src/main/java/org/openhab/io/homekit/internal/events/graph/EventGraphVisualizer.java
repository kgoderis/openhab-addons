package org.openhab.io.homekit.internal.events.graph;

import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Utility class for visualizing the event processing graph in various formats.
 */
public class EventGraphVisualizer {
    private final EventGraphProcessor graph;

    public EventGraphVisualizer(EventGraphProcessor graph) {
        this.graph = graph;
    }

    /**
     * Generate DOT format for Graphviz visualization
     */
    public String generateDotGraph() {
        StringBuilder dot = new StringBuilder("digraph EventFlow {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=filled, fillcolor=lightblue];\n");
        
        // Add nodes with metadata
        for (EventGraphProcessor.EventProcessingNode node : graph.getNodes().values()) {
            String label = String.format("%s\\nType: %s\\nEvents: %s", 
                node.id,
                node.metadata.get("type"),
                node.metadata.get("eventTypes"));
            dot.append(String.format("  \"%s\" [label=\"%s\"];\n", node.id, label));
        }
        
        // Add edges with metadata
        for (Map.Entry<String, Set<String>> entry : graph.getGraph().entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                dot.append(String.format("  \"%s\" -> \"%s\";\n", source, target));
            }
        }
        
        dot.append("}\n");
        return dot.toString();
    }
    
    /**
     * Generate JSON for web-based visualization
     */
    public String generateJsonGraph() {
        JsonObject json = new JsonObject();
        JsonArray nodes = new JsonArray();
        JsonArray edges = new JsonArray();
        
        // Add nodes
        for (EventGraphProcessor.EventProcessingNode node : graph.getNodes().values()) {
            JsonObject nodeJson = new JsonObject();
            nodeJson.addProperty("id", node.id);
            nodeJson.addProperty("type", (String) node.metadata.get("type"));
            nodeJson.addProperty("eventTypes", (String) node.metadata.get("eventTypes"));
            nodes.add(nodeJson);
        }
        
        // Add edges
        for (Map.Entry<String, Set<String>> entry : graph.getGraph().entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                JsonObject edgeJson = new JsonObject();
                edgeJson.addProperty("source", source);
                edgeJson.addProperty("target", target);
                edges.add(edgeJson);
            }
        }
        
        json.add("nodes", nodes);
        json.add("edges", edges);
        return json.toString();
    }
    
    /**
     * Generate metrics visualization
     */
    public String generateMetricsGraph() {
        JsonObject metrics = new JsonObject();
        
        // Node metrics
        JsonObject nodeMetrics = new JsonObject();
        for (EventGraphProcessor.EventProcessingNode node : graph.getNodes().values()) {
            JsonObject nodeStats = new JsonObject();
            nodeStats.addProperty("incomingEdges", node.incomingEdges.size());
            nodeStats.addProperty("outgoingEdges", node.outgoingEdges.size());
            nodeStats.addProperty("eventCount", (Integer) node.metadata.getOrDefault("eventCount", 0));
            nodeMetrics.add(node.id, nodeStats);
        }
        
        // Edge metrics
        JsonObject edgeMetrics = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : graph.getGraph().entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                String edgeId = source + "->" + target;
                JsonObject edgeStats = new JsonObject();
                edgeStats.addProperty("eventCount", (Integer) graph.getEdgeMetadata(edgeId).getOrDefault("eventCount", 0));
                edgeStats.addProperty("averageLatency", (Double) graph.getEdgeMetadata(edgeId).getOrDefault("averageLatency", 0.0));
                edgeMetrics.add(edgeId, edgeStats);
            }
        }
        
        metrics.add("nodes", nodeMetrics);
        metrics.add("edges", edgeMetrics);
        return metrics.toString();
    }
} 