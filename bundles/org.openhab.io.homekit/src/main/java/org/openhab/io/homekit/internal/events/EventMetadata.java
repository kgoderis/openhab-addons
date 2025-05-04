package org.openhab.io.homekit.internal.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Metadata for tracking event propagation and preventing loops.
 * <p>
 * This class maintains:
 * <ul>
 * <li>A unique event ID to track event instances</li>
 * <li>A hop count to limit event propagation</li>
 * <li>The original publisher UID to identify event origin</li>
 * <li>A history of event IDs to detect loops</li>
 * </ul>
 * </p>
 */
@NonNullByDefault
public class EventMetadata {
    private static final int MAX_HISTORY_SIZE = 100;
    private static final int MAX_HOPS = 10;
    private static final boolean DEBUG_MODE = false; // Set to true to enable detailed event history
    private static final long CORRELATION_ID_EXPIRY_MS = 300000; // 5 minutes
    
    private final String eventId;
    private final String originalPublisherUid;
    private int hopCount;
    private final long timestamp;
    private final Set<String> eventHistory = new LinkedHashSet<>();
    private final List<HomekitEvent> detailedEventHistory = DEBUG_MODE ? new ArrayList<>() : null;
    
    // Correlation tracking
    private final @Nullable String correlationId;
    private final Set<String> processedCorrelationIds = new HashSet<>();
    private final long correlationTimestamp;
    
    // Origin tracking
    private final @Nullable String immediateOrigin;
    private final Set<String> peerIdentifiers = new HashSet<>();

    /**
     * Creates new event metadata with a unique ID and initial hop count.
     *
     * @param publisherUid the UID of the original publisher
     * @param correlationId optional correlation ID to link related events
     * @param immediateOrigin optional identifier of the immediate event creator
     * @param peerIdentifiers optional set of identifiers for components that should be treated as peers
     */
    public EventMetadata(String publisherUid, @Nullable String correlationId, @Nullable String immediateOrigin,
            Set<String> peerIdentifiers) {
        this.eventId = UUID.randomUUID().toString();
        this.originalPublisherUid = publisherUid;
        this.hopCount = 0;
        this.timestamp = System.currentTimeMillis();
        this.eventHistory.add(this.eventId);
        this.correlationId = correlationId;
        this.correlationTimestamp = System.currentTimeMillis();
        if (correlationId != null) {
            this.processedCorrelationIds.add(correlationId);
        }
        this.immediateOrigin = immediateOrigin;
        this.peerIdentifiers.addAll(peerIdentifiers);
        if (DEBUG_MODE) {
            this.detailedEventHistory.add(null); // Placeholder for current event
        }
    }

    /**
     * Creates event metadata from existing metadata, incrementing the hop count
     * and preserving the event history and correlation tracking.
     *
     * @param original the original metadata to copy
     * @param immediateOrigin optional identifier of the immediate event creator
     */
    public EventMetadata(EventMetadata original, @Nullable String immediateOrigin) {
        this.eventId = original.eventId;
        this.originalPublisherUid = original.originalPublisherUid;
        this.hopCount = original.hopCount + 1;
        this.timestamp = original.timestamp;
        this.eventHistory.addAll(original.eventHistory);
        this.correlationId = original.correlationId;
        this.correlationTimestamp = original.correlationTimestamp;
        this.processedCorrelationIds.addAll(original.processedCorrelationIds);
        this.immediateOrigin = immediateOrigin;
        this.peerIdentifiers.addAll(original.peerIdentifiers);
        if (DEBUG_MODE) {
            this.detailedEventHistory.addAll(original.detailedEventHistory);
        }
    }

    /**
     * Returns the unique event ID.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the original publisher UID.
     *
     * @return the publisher UID
     */
    public String getOriginalPublisherUid() {
        return originalPublisherUid;
    }

    /**
     * Returns the current hop count.
     *
     * @return the hop count
     */
    public int getHopCount() {
        return hopCount;
    }

    /**
     * Returns the event timestamp.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns an unmodifiable view of the event history.
     *
     * @return the event history
     */
    public Set<String> getEventHistory() {
        return Collections.unmodifiableSet(eventHistory);
    }

    /**
     * Checks if the event has exceeded the maximum hop count.
     *
     * @return true if the hop count exceeds the maximum
     */
    public boolean hasExceededMaxHops() {
        return hopCount >= MAX_HOPS;
    }

    /**
     * Checks if an event ID is in the history.
     *
     * @param eventId the event ID to check
     * @return true if the event ID is in the history
     */
    public boolean isInHistory(String eventId) {
        return eventHistory.contains(eventId);
    }

    /**
     * Returns the maximum allowed hop count.
     *
     * @return the maximum hop count
     */
    public static int getMaxHops() {
        return MAX_HOPS;
    }

    /**
     * Returns the maximum event history size.
     *
     * @return the maximum history size
     */
    public static int getMaxHistory() {
        return MAX_HISTORY_SIZE;
    }

    public void addToHistory(HomekitEvent event) {
        if (eventHistory.size() >= MAX_HISTORY_SIZE) {
            String oldestId = eventHistory.iterator().next();
            eventHistory.remove(oldestId);
            if (DEBUG_MODE) {
                detailedEventHistory.remove(0);
            }
        }
        eventHistory.add(event.getMetadata().getEventId());
        if (DEBUG_MODE) {
            detailedEventHistory.add(event);
        }
    }

    public List<HomekitEvent> getDetailedEventHistory() {
        return DEBUG_MODE ? Collections.unmodifiableList(detailedEventHistory) : null;
    }

    public String getEventHistoryAsString() {
        if (DEBUG_MODE) {
            return detailedEventHistory.stream()
                .map(event -> event != null ? 
                    String.format("%s[%s] -> %s", 
                        event.getType(), 
                        event.getPublisherUID(), 
                        event.getMetadata().getEventId()) : 
                    "INITIAL")
                .collect(Collectors.joining(" -> "));
        } else {
            return String.join(" -> ", eventHistory);
        }
    }

    /**
     * Returns the correlation ID if present.
     *
     * @return the correlation ID or null
     */
    public @Nullable String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns the immediate origin if present.
     *
     * @return the immediate origin or null
     */
    public @Nullable String getImmediateOrigin() {
        return immediateOrigin;
    }

    /**
     * Returns the set of peer identifiers.
     *
     * @return the peer identifiers
     */
    public Set<String> getPeerIdentifiers() {
        return Collections.unmodifiableSet(peerIdentifiers);
    }

    /**
     * Checks if the event originated from a peer.
     *
     * @return true if the event originated from a peer
     */
    public boolean isFromPeer() {
        return peerIdentifiers.contains(originalPublisherUid);
    }

    /**
     * Checks if the event was created by the specified component.
     *
     * @param componentId the component ID to check
     * @return true if the event was created by the component
     */
    public boolean isCreatedBy(String componentId) {
        return componentId.equals(immediateOrigin);
    }

    /**
     * Checks if a correlation ID has been processed.
     *
     * @param correlationId the correlation ID to check
     * @return true if the correlation ID has been processed
     */
    public boolean hasProcessedCorrelationId(String correlationId) {
        return processedCorrelationIds.contains(correlationId);
    }

    /**
     * Adds a correlation ID to the processed set.
     *
     * @param correlationId the correlation ID to add
     */
    public void addProcessedCorrelationId(String correlationId) {
        processedCorrelationIds.add(correlationId);
    }

    /**
     * Checks if the correlation tracking has expired.
     *
     * @return true if the correlation tracking has expired
     */
    public boolean hasCorrelationExpired() {
        return System.currentTimeMillis() - correlationTimestamp > CORRELATION_ID_EXPIRY_MS;
    }

    /**
     * Cleans up expired correlation IDs.
     */
    public void cleanupExpiredCorrelationIds() {
        if (hasCorrelationExpired()) {
            processedCorrelationIds.clear();
        }
    }
} 