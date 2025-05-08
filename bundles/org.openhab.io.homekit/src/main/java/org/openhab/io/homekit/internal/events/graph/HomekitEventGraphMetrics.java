package org.openhab.io.homekit.internal.events.graph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for tracking and managing event graph metrics.
 */
public class HomekitEventGraphMetrics {
    private final Map<String, AtomicLong> nodeMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> edgeMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> cycleMetrics = new ConcurrentHashMap<>();

    public void incrementNodeMetric(String nodeId, String metric) {
        String key = String.format("node.%s.%s", nodeId, metric);
        nodeMetrics.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public void incrementEdgeMetric(String sourceId, String targetId, String metric) {
        String key = String.format("edge.%s->%s.%s", sourceId, targetId, metric);
        edgeMetrics.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public void incrementCycleMetric(String cyclePath, String metric) {
        String key = String.format("cycle.%s.%s", cyclePath, metric);
        cycleMetrics.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public long getNodeMetric(String nodeId, String metric) {
        String key = String.format("node.%s.%s", nodeId, metric);
        return nodeMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    public long getEdgeMetric(String sourceId, String targetId, String metric) {
        String key = String.format("edge.%s->%s.%s", sourceId, targetId, metric);
        return edgeMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    public long getCycleMetric(String cyclePath, String metric) {
        String key = String.format("cycle.%s.%s", cyclePath, metric);
        return cycleMetrics.getOrDefault(key, new AtomicLong()).get();
    }

    public Map<String, Long> getAllNodeMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        nodeMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    public Map<String, Long> getAllEdgeMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        edgeMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    public Map<String, Long> getAllCycleMetrics() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        cycleMetrics.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    public void resetMetrics() {
        nodeMetrics.clear();
        edgeMetrics.clear();
        cycleMetrics.clear();
    }
}
