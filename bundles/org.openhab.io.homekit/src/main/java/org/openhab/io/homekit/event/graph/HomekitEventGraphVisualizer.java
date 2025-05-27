package org.openhab.io.homekit.event.graph;

import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Visualizer for the HomeKit event processing graph.
 * This class provides functionality to generate visual representations of the event graph,
 * which can be used for debugging, monitoring, or documentation purposes.
 *
 * The class integrates with:
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphProcessor} for graph processing
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphService} for graph management
 * - {@link org.openhab.io.homekit.event.graph.HomekitEventGraphMetrics} for graph metrics
 * - {@link com.google.gson.JsonObject} for JSON serialization
 * - {@link org.slf4j.Logger} for logging
 *
 * Key implementation details:
 * - Supports multiple visualization formats (text, DOT, JSON)
 * - Includes metadata and metrics in visualizations
 * - Provides logging capabilities for debugging
 * - Handles graph cycles and metrics visualization
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEventGraphVisualizer {
    private static final Logger logger = LoggerFactory.getLogger(HomekitEventGraphVisualizer.class);
    private final HomekitEventGraphProcessor eventGraph;

    /**
     * Creates a new event graph visualizer.
     * Initializes the visualizer with the provided event graph processor.
     *
     * Key implementation details:
     * - Stores reference to event graph processor
     * - Used by all visualization methods
     *
     * @param eventGraph the event graph processor to visualize
     */
    public HomekitEventGraphVisualizer(HomekitEventGraphProcessor eventGraph) {
        this.eventGraph = eventGraph;
    }

    /**
     * Generates a visual representation of the event graph.
     * This method creates a string representation of the graph structure,
     * including nodes, edges, and their relationships.
     *
     * Key implementation details:
     * - Creates formatted text output
     * - Includes nodes, edges, and cycles
     * - Uses StringBuilder for efficient string construction
     *
     * @return a string containing the visual representation of the graph
     */
    public String visualizeGraph() {
        StringBuilder visualization = new StringBuilder();
        visualization.append("Event Graph Visualization:\n");
        visualization.append("========================\n\n");

        // Add nodes
        visualization.append("Nodes:\n");
        eventGraph.getNodes().values().forEach(node -> {
            visualization.append("- ").append(node.id).append("\n");
        });

        // Add edges
        visualization.append("\nEdges:\n");
        eventGraph.getGraph().forEach((source, targets) -> {
            targets.forEach(target -> {
                visualization.append("- ").append(source).append(" -> ").append(target).append("\n");
            });
        });

        // Add cycles if any
        if (eventGraph.hasCycle()) {
            visualization.append("\nCycles Detected:\n");
            visualization.append("- ").append(eventGraph.getCyclePath()).append("\n");
        }

        return visualization.toString();
    }

    /**
     * Logs the current state of the event graph.
     * This method outputs the graph structure to the logger for debugging purposes.
     *
     * Key implementation details:
     * - Uses SLF4J logger for output
     * - Calls visualizeGraph() for graph representation
     * - Logs at INFO level for visibility
     */
    public void logGraphState() {
        logger.info("Current Event Graph State:\n{}", visualizeGraph());
    }

    /**
     * Generates a DOT format representation of the graph for Graphviz visualization.
     * This format can be used to create visual diagrams of the event flow.
     *
     * Key implementation details:
     * - Creates DOT language output
     * - Includes node metadata and styling
     * - Uses left-to-right graph direction
     * - Adds node labels with type and event information
     *
     * @return a string containing the DOT format representation of the graph
     */
    public String generateDotGraph() {
        StringBuilder dot = new StringBuilder("digraph EventFlow {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box, style=filled, fillcolor=lightblue];\n");

        // Add nodes with metadata
        for (HomekitEventGraphProcessor.EventProcessingNode node : eventGraph.getNodes().values()) {
            String label = String.format("%s\\nType: %s\\nEvents: %s", node.id, node.metadata.get("type"),
                    node.metadata.get("eventTypes"));
            dot.append(String.format("  \"%s\" [label=\"%s\"];\n", node.id, label));
        }

        // Add edges with metadata
        for (Map.Entry<String, Set<String>> entry : eventGraph.getGraph().entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                dot.append(String.format("  \"%s\" -> \"%s\";\n", source, target));
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Generates a JSON representation of the graph for web-based visualization.
     * This format can be used to create interactive visualizations in web applications.
     *
     * Key implementation details:
     * - Uses Gson for JSON serialization
     * - Includes node and edge metadata
     * - Creates arrays for nodes and edges
     * - Supports web-based visualization tools
     *
     * @return a string containing the JSON representation of the graph
     */
    public String generateJsonGraph() {
        JsonObject json = new JsonObject();
        JsonArray nodes = new JsonArray();
        JsonArray edges = new JsonArray();

        // Add nodes
        for (HomekitEventGraphProcessor.EventProcessingNode node : eventGraph.getNodes().values()) {
            JsonObject nodeJson = new JsonObject();
            nodeJson.addProperty("id", node.id);
            nodeJson.addProperty("type", (String) node.metadata.get("type"));
            nodeJson.addProperty("eventTypes", (String) node.metadata.get("eventTypes"));
            nodes.add(nodeJson);
        }

        // Add edges
        for (Map.Entry<String, Set<String>> entry : eventGraph.getGraph().entrySet()) {
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
     * Generates a metrics visualization of the graph.
     * This includes statistics about nodes, edges, and their relationships.
     *
     * Key implementation details:
     * - Creates JSON structure for metrics
     * - Includes node and edge statistics
     * - Handles missing metrics with default values
     * - Provides performance and event count data
     *
     * @return a string containing the metrics visualization in JSON format
     */
    public String generateMetricsGraph() {
        JsonObject metrics = new JsonObject();

        // Node metrics
        JsonObject nodeMetrics = new JsonObject();
        for (HomekitEventGraphProcessor.EventProcessingNode node : eventGraph.getNodes().values()) {
            JsonObject nodeStats = new JsonObject();
            nodeStats.addProperty("incomingEdges", node.incomingEdges.size());
            nodeStats.addProperty("outgoingEdges", node.outgoingEdges.size());
            nodeStats.addProperty("eventCount", (Integer) node.metadata.getOrDefault("eventCount", 0));
            nodeMetrics.add(node.id, nodeStats);
        }

        // Edge metrics
        JsonObject edgeMetrics = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : eventGraph.getGraph().entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                String edgeId = source + "->" + target;
                JsonObject edgeStats = new JsonObject();
                edgeStats.addProperty("eventCount",
                        (Integer) eventGraph.getEdgeMetadata(edgeId).getOrDefault("eventCount", 0));
                edgeStats.addProperty("averageLatency",
                        (Double) eventGraph.getEdgeMetadata(edgeId).getOrDefault("averageLatency", 0.0));
                edgeMetrics.add(edgeId, edgeStats);
            }
        }

        metrics.add("nodes", nodeMetrics);
        metrics.add("edges", edgeMetrics);
        return metrics.toString();
    }
}
