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

package org.openhab.io.homekit.event.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEvent;
import org.openhab.io.homekit.util.HomekitUID;

/**
 * HomekitEventMetadata encapsulates all contextual and propagation information
 * for a Homekit event.
 * 
 * This class is central to the event loop prevention, correlation, and
 * diagnostics mechanisms
 * in the OpenHAB Homekit integration. It tracks the origin, propagation path,
 * correlation, and
 * peer relationships of an event as it traverses the system.
 * 
 * Key Responsibilities:
 * - Event Uniqueness: Assigns a globally unique event ID to each event instance
 * - Origin Tracking: Records the original publisher and immediate originator
 * - Hop Count: Maintains a hop count to limit event propagation
 * - Event History: Stores a history of event IDs to detect cycles
 * - Publisher History: Maintains an ordered list of publishers in the
 * propagation chain
 * - Correlation: Supports correlation IDs for grouping related events
 * - Peer Group Awareness: Tracks peer identifiers for trusted components
 * - Diagnostics: Optional detailed event history for debugging
 * 
 * Field Descriptions:
 * 
 * eventId:
 * A globally unique identifier for this event instance.
 * Format: homekit:event:{uuid}
 * Used to distinguish events and track their propagation.
 * 
 * originalPublisherUid:
 * The UID of the component that originally published the event.
 * Remains constant throughout the event's lifetime.
 * 
 * hopCount:
 * The number of times this event has been propagated ("hops").
 * Incremented with each forwarding to prevent infinite loops.
 * 
 * timestamp:
 * The creation time of the event metadata (milliseconds since epoch).
 * Used for time-based diagnostics and correlation expiry.
 * 
 * eventHistory:
 * An ordered set of event IDs representing the propagation path.
 * Used to detect cycles and provide traceability.
 * 
 * publisherHistory:
 * An ordered list of publisher UIDs representing the propagation chain.
 * Used to detect publisher-based loops and provide publisher traceability.
 * 
 * detailedEventHistory:
 * (Debug mode only) A list of HomekitEvent objects representing
 * the full propagation chain for in-depth diagnostics.
 * 
 * correlationId:
 * (Optional) An identifier used to correlate related events
 * (e.g., request/response pairs, state synchronizations).
 * 
 * processedCorrelationIds:
 * A set of correlation IDs that have already been processed,
 * to prevent duplicate handling.
 * 
 * correlationTimestamp:
 * The timestamp when the correlation ID was first set.
 * Used to expire old correlations and clean up state.
 * 
 * immediateOrigin:
 * (Optional) The UID of the component that most recently propagated
 * or created this event. Useful for hop-by-hop diagnostics.
 * 
 * peerIdentifiers:
 * A set of UIDs representing components considered "peers"
 * for loop prevention and trust relationships.
 * 
 * Usage Patterns:
 * - When an event is first created, a new HomekitEventMetadata is instantiated
 * with the original
 * publisher UID and (optionally) a correlation ID and peer group.
 * - Each time the event is propagated, a new HomekitEventMetadata is created
 * from the previous one,
 * incrementing the hop count and updating the immediate origin.
 * - Event consumers can use the hop count, event history, publisher history,
 * and peer group
 * to decide whether to process, forward, or drop the event.
 * - Correlation IDs allow grouping of related events for state synchronization,
 * request/response flows, or deduplication.
 * - Debug mode enables detailed tracing of event propagation for diagnostics.
 * 
 * Best Practices:
 * - Always use the provided methods to check for loops, hop limits, and peer
 * group membership
 * - Use correlation IDs for any multi-step or distributed workflows
 * - Enable debug mode only in development or troubleshooting scenarios
 * - Monitor publisher history to detect potential publisher-based loops
 * 
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
public class HomekitEventMetadata {
    // =============== Constants ===============

    /** Maximum number of event IDs to keep in history */
    private static final int MAX_HISTORY_SIZE = 100;

    /** Maximum number of hops an event can make before being dropped */
    private static final int MAX_HOPS = 10;

    /** Enable detailed event history tracking for debugging */
    private static final boolean DEBUG_MODE = false;

    /** Time after which correlation IDs expire (5 minutes) */
    private static final long CORRELATION_ID_EXPIRY_MS = 300000;

    // =============== Core Event Fields ===============

    /** Unique identifier for this event instance */
    private final HomekitUID eventId;

    /** Creation timestamp of this metadata */
    private final long timestamp;

    /** Number of times this event has been propagated */
    private int hopCount;

    // =============== Origin Tracking ===============

    /** UID of the component that originally published the event */
    private final UID originalPublisherUid;

    /** UID of the component that most recently propagated this event */
    private final Optional<UID> immediateOrigin;

    /** Set of UIDs representing peer components */
    private final Set<UID> peerIdentifiers = new HashSet<>();

    // =============== Correlation Tracking ===============

    /** Optional correlation ID for grouping related events */
    private final Optional<UID> correlationId;

    /** Set of correlation IDs that have been processed */
    private final Set<UID> processedCorrelationIds = new HashSet<>();

    /** Timestamp when correlation tracking began */
    private final long correlationTimestamp;

    // =============== History Management ===============

    /** Ordered set of event IDs in the propagation chain */
    private final Set<UID> eventHistory = new LinkedHashSet<>();

    /** Ordered list of publisher UIDs in the propagation chain */
    private final List<UID> publisherHistory = new ArrayList<>();

    /** Detailed event history for debugging (null if DEBUG_MODE is false) */
    private final List<HomekitEvent> detailedEventHistory = new ArrayList<>();

    // =============== Constructors ===============

    /**
     * Creates new event metadata with the specified parameters.
     * This constructor initializes all tracking mechanisms and validates parameters.
     *
     * @param publisherUid the UID of the original event publisher
     * @param correlationId optional correlation ID to link related events
     * @param immediateOrigin optional identifier of the immediate event creator
     * @param peerIdentifiers optional set of identifiers for components that should
     *            be treated as peers
     */
    public HomekitEventMetadata(UID publisherUid, @Nullable UID correlationId, @Nullable UID immediateOrigin,
            Set<HomekitUID> peerIdentifiers) {
        this.eventId = new HomekitUID("event");
        this.originalPublisherUid = publisherUid;
        this.hopCount = 0;
        this.timestamp = System.currentTimeMillis();
        this.eventHistory.add(this.eventId);
        this.publisherHistory.add(publisherUid); // Add initial publisher
        this.correlationId = correlationId != null ? Optional.of(correlationId) : Optional.empty();
        this.correlationTimestamp = System.currentTimeMillis();
        this.correlationId.ifPresent(this.processedCorrelationIds::add);
        this.immediateOrigin = immediateOrigin != null ? Optional.of(immediateOrigin) : Optional.empty();
        this.peerIdentifiers.addAll(peerIdentifiers);
    }

    /**
     * Creates event metadata from existing metadata, incrementing the hop count
     * and preserving the event history and correlation tracking.
     *
     * @param original the original metadata to copy
     * @param immediateOrigin optional identifier of the immediate event creator
     */
    public HomekitEventMetadata(HomekitEventMetadata original, @Nullable UID immediateOrigin) {
        this.eventId = original.eventId;
        this.originalPublisherUid = original.originalPublisherUid;
        this.hopCount = original.hopCount + 1;
        this.timestamp = original.timestamp;
        this.eventHistory.addAll(original.eventHistory);
        this.publisherHistory.addAll(original.publisherHistory); // Copy publisher history
        if (immediateOrigin != null) {
            this.publisherHistory.add(immediateOrigin); // Add new publisher if present
        }
        this.correlationId = original.correlationId;
        this.correlationTimestamp = original.correlationTimestamp;
        this.processedCorrelationIds.addAll(original.processedCorrelationIds);
        this.immediateOrigin = immediateOrigin != null ? Optional.of(immediateOrigin) : Optional.empty();
        this.peerIdentifiers.addAll(original.peerIdentifiers);
        if (DEBUG_MODE) {
            this.detailedEventHistory.addAll(original.detailedEventHistory);
        }
    }

    // =============== Core Event Methods ===============

    /**
     * Returns the unique event ID.
     *
     * @return the event ID
     */
    public UID getEventId() {
        return eventId;
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
     * Checks if the event has exceeded the maximum hop count.
     *
     * @return true if the hop count exceeds the maximum
     */
    public boolean hasExceededMaxHops() {
        return hopCount >= MAX_HOPS;
    }

    // =============== Origin Tracking Methods ===============

    /**
     * Returns the original publisher UID.
     *
     * @return the publisher UID
     */
    public UID getOriginalPublisherUID() {
        return originalPublisherUid;
    }

    /**
     * Returns the immediate origin if present.
     *
     * @return Optional containing the immediate origin
     */
    public Optional<UID> getImmediateOrigin() {
        return immediateOrigin;
    }

    /**
     * Returns the set of peer identifiers.
     *
     * @return the peer identifiers
     */
    public Set<UID> getPeerIdentifiers() {
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
    public boolean isCreatedBy(UID componentId) {
        return immediateOrigin.map(origin -> componentId.equals(origin)).orElse(false);
    }

    /**
     * Checks if the event is from a peer group.
     *
     * @param peerGroup the set of peer identifiers to check against
     * @return true if the event is from a peer in the group
     */
    public boolean isFromPeerGroup(Set<HomekitUID> peerGroup) {
        return immediateOrigin.map(peerGroup::contains).orElse(false);
    }

    // =============== Correlation Methods ===============

    /**
     * Returns the correlation ID if present.
     *
     * @return Optional containing the correlation ID
     */
    public Optional<UID> getCorrelationId() {
        return correlationId;
    }

    /**
     * Checks if a correlation ID has been processed.
     *
     * @param correlationId the correlation ID to check
     * @return true if the correlation ID has been processed
     */
    public boolean hasProcessedCorrelationId(UID correlationId) {
        return processedCorrelationIds.contains(correlationId);
    }

    /**
     * Adds a correlation ID to the processed set.
     *
     * @param correlationId the correlation ID to add
     */
    public void addProcessedCorrelationId(UID correlationId) {
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

    // =============== History Management Methods ===============

    /**
     * Returns an unmodifiable view of the event history.
     *
     * @return the event history
     */
    public Set<UID> getEventHistory() {
        return Collections.unmodifiableSet(eventHistory);
    }

    /**
     * Checks if an event ID is in the history.
     *
     * @param eventId the event ID to check
     * @return true if the event ID is in the history
     */
    public boolean isInEventHistory(UID eventId) {
        return eventHistory.contains(eventId);
    }

    /**
     * Adds an event to the history.
     *
     * @param event the event to add
     */
    public void addToEventHistory(HomekitEvent event) {
        if (eventHistory.size() >= MAX_HISTORY_SIZE) {
            @SuppressWarnings("null") // iterator().next() is safe when size >= MAX_HISTORY_SIZE
            UID oldestId = eventHistory.iterator().next();
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

    /**
     * Returns the detailed event history if debug mode is enabled.
     *
     * @return the detailed event history or null if debug mode is disabled
     */
    public List<HomekitEvent> getDetailedEventHistory() {
        return Collections.unmodifiableList(detailedEventHistory);
    }

    /**
     * Returns a string representation of the event history.
     *
     * @return the event history as a string
     */
    public String getEventHistoryAsString() {
        if (DEBUG_MODE) {
            return detailedEventHistory.stream()
                    .map(event -> event != null ? String.format("%s[%s] -> %s", event.getType(),
                            event.getPublisherUID(), event.getMetadata().getEventId()) : "INITIAL")
                    .collect(Collectors.joining(" -> "));
        } else {
            return String.join(" -> ", eventHistory.stream().map(UID::toString).collect(Collectors.toList()));
        }
    }

    // =============== Publisher History Methods ===============

    /**
     * Returns an unmodifiable view of the publisher history.
     *
     * @return the publisher history
     */
    public List<UID> getPublisherHistory() {
        return Collections.unmodifiableList(publisherHistory);
    }

    /**
     * Checks if there is a loop in the publisher history.
     * A loop is detected if any publisher appears more than once in the history.
     *
     * @return true if a loop is detected
     */
    public boolean hasLoop() {
        return publisherHistory.size() != new HashSet<>(publisherHistory).size();
    }

    public boolean isInPublisherHistory(UID publisherUid) {
        return publisherHistory.contains(publisherUid);
    }

    /**
     * Returns a string representation of the publisher history.
     *
     * @return the publisher history as a string
     */
    public String getPublisherHistoryAsString() {
        String result = publisherHistory.stream().map(UID::toString).collect(Collectors.joining(" -> "));
        return result;
    }

    /**
     * Adds a publisher to the history.
     *
     * @param publisherUid the publisher UID to add
     */
    public void addToPublisherHistory(UID publisherUid) {
        if (publisherHistory.size() >= MAX_HISTORY_SIZE) {
            publisherHistory.remove(0);
        }
        publisherHistory.add(publisherUid);
    }

    // =============== Static Methods ===============

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
}
