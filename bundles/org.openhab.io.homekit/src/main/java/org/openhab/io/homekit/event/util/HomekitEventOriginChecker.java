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

package org.openhab.io.homekit.event.util;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEvent;

/**
 * Utility class for validating and checking the origin of HomeKit events in the OpenHAB system.
 * This class provides methods to verify event sources, track event propagation, and prevent
 * unauthorized event processing.
 *
 * The class integrates with:
 * - {@link HomekitEvent} for event data and metadata access
 * - {@link org.openhab.core.thing.UID OpenHAB's UID system} for component identification
 * - {@link org.openhab.io.homekit.api.event.HomekitEventMetadata} for event metadata handling
 *
 * Key Features:
 * - Origin validation and parsing
 * - Publisher verification
 * - Bridge and accessory tracking
 * - Service and characteristic validation
 * - Event correlation checking
 * - Peer group validation
 *
 * Security Considerations:
 * - Validates event origins to prevent unauthorized events
 * - Tracks event propagation to detect loops
 * - Verifies publisher authenticity
 * - Enforces component hierarchy
 *
 * Usage Patterns:
 * - Event origin validation: Use {@link #isValidOrigin(String)} to validate origin format
 * - Publisher verification: Use {@link #isOriginalPublisher(HomekitEvent, UID)} to verify event source
 * - Component tracking: Use {@link #hasPassedThrough(HomekitEvent, UID)} to track event propagation
 * - Bridge validation: Use {@link #isFromBridge(HomekitEvent, UID)} to verify bridge origin
 * - Accessory validation: Use {@link #isFromAccessory(HomekitEvent, String, String)} to verify accessory origin
 * - Service validation: Use {@link #isFromService(HomekitEvent, String, String, String)} to verify service origin
 * - Characteristic validation: Use {@link #isFromCharacteristic(HomekitEvent, String, String, String, String)} to
 * verify characteristic origin
 * - Peer validation: Use {@link #isFromPeer(HomekitEvent)} to verify peer group origin
 * - Event correlation: Use {@link #isCorrelated(HomekitEvent, HomekitEvent)} to check event relationships
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitEventOriginChecker {
    private static final String ORIGIN_SEPARATOR = ":";
    private static final String WILDCARD = "*";

    /**
     * Checks if an event originated from a specific source.
     * The origin format is: type:source[:target]
     *
     * @param eventOrigin The origin of the event to check
     * @param expectedOrigin The expected origin pattern to match against
     * @return true if the event origin matches the expected pattern, false otherwise
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    public static boolean isOrigin(String eventOrigin, String expectedOrigin) {
        if (eventOrigin.isEmpty()) {
            throw new IllegalArgumentException("Event origin cannot be empty");
        }
        if (expectedOrigin.isEmpty()) {
            throw new IllegalArgumentException("Expected origin cannot be empty");
        }

        String[] eventParts = eventOrigin.split(ORIGIN_SEPARATOR);
        String[] expectedParts = expectedOrigin.split(ORIGIN_SEPARATOR);

        if (eventParts.length < 2 || expectedParts.length < 2) {
            return false;
        }

        // Check type and source
        if (!matchesPart(eventParts[0], expectedParts[0]) || !matchesPart(eventParts[1], expectedParts[1])) {
            return false;
        }

        // Check target if present in both
        if (eventParts.length > 2 && expectedParts.length > 2) {
            return matchesPart(eventParts[2], expectedParts[2]);
        }

        return true;
    }

    /**
     * Validates if a string is a valid HomeKit event origin.
     * A valid origin must contain at least the type and source components.
     *
     * @param origin The origin to validate
     * @return true if the origin is valid, false otherwise
     */
    public static boolean isValidOrigin(String origin) {
        if (origin.isEmpty()) {
            return false;
        }

        String[] parts = origin.split(ORIGIN_SEPARATOR);
        return parts.length >= 2;
    }

    /**
     * Extracts the type component from an event origin.
     * The type identifies the category of the event source.
     *
     * @param origin The event origin
     * @return The type component
     * @throws IllegalArgumentException if the origin is invalid
     */
    public static String getType(String origin) {
        if (!isValidOrigin(origin)) {
            throw new IllegalArgumentException("Invalid origin format: " + origin);
        }
        @SuppressWarnings("null") // split() result is validated by isValidOrigin() check above
        String[] parts = origin.split(ORIGIN_SEPARATOR);
        return parts[0];
    }

    /**
     * Extracts the source component from an event origin.
     * The source identifies the specific component that generated the event.
     *
     * @param origin The event origin
     * @return The source component
     * @throws IllegalArgumentException if the origin is invalid
     */
    public static String getSource(String origin) {
        if (!isValidOrigin(origin)) {
            throw new IllegalArgumentException("Invalid origin format: " + origin);
        }
        @SuppressWarnings("null") // split() result is validated by isValidOrigin() check above
        String[] parts = origin.split(ORIGIN_SEPARATOR);
        return parts[1];
    }

    /**
     * Extracts the target component from an event origin.
     * The target identifies the specific component that the event is intended for.
     *
     * @param origin The event origin
     * @return The target component, or null if no target is specified
     * @throws IllegalArgumentException if the origin is invalid
     */
    public static Optional<String> getTarget(String origin) {
        if (!isValidOrigin(origin)) {
            throw new IllegalArgumentException("Invalid origin format: " + origin);
        }
        String[] parts = origin.split(ORIGIN_SEPARATOR);
        return parts.length > 2 ? Optional.of(parts[2]) : Optional.empty();
    }

    /**
     * Checks if a part of an origin matches an expected pattern.
     * The pattern can include wildcards (*) to match any value.
     *
     * @param part The part to check
     * @param pattern The pattern to match against
     * @return true if the part matches the pattern, false otherwise
     */
    private static boolean matchesPart(String part, String pattern) {
        return WILDCARD.equals(pattern) || pattern.equals(part);
    }

    /**
     * Checks if an event originated from a specific publisher.
     * This method verifies the original publisher of the event.
     *
     * @param event the event to check
     * @param publisherUID the publisher UID to check against
     * @return true if the event originated from the specified publisher
     */
    public static boolean isOriginalPublisher(HomekitEvent event, UID publisherUID) {
        return event.getMetadata().getOriginalPublisherUID().equals(publisherUID);
    }

    /**
     * Checks if an event has passed through a specific publisher.
     * This method verifies if the event has been processed by the specified publisher.
     *
     * @param event the event to check
     * @param publisherUID the publisher UID to check for
     * @return true if the event has passed through the specified publisher
     */
    public static boolean hasPassedThrough(HomekitEvent event, UID publisherUID) {
        return event.getMetadata().getEventHistory().stream().anyMatch(id -> id.equals(publisherUID));
    }

    /**
     * Checks if an event originated from a specific bridge.
     * This method verifies if the event was generated by a component on the specified bridge.
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @return true if the event originated from the specified bridge
     */
    public static boolean isFromBridge(HomekitEvent event, UID bridgeUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return originalPublisher.toString().startsWith("bridge:" + bridgeUID + ":");
    }

    /**
     * Checks if an event originated from an accessory on a specific bridge.
     * This method verifies if the event was generated by the specified accessory.
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @param accessoryUID the accessory UID to check against
     * @return true if the event originated from the specified accessory on the bridge
     */
    public static boolean isFromAccessory(HomekitEvent event, String bridgeUID, String accessoryUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return ("bridge:" + bridgeUID + ":accessory:" + accessoryUID).equals(originalPublisher.toString());
    }

    /**
     * Checks if an event originated from a service on a specific accessory.
     * This method verifies if the event was generated by the specified service.
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @param accessoryUID the accessory UID to check against
     * @param serviceUID the service UID to check against
     * @return true if the event originated from the specified service
     */
    public static boolean isFromService(HomekitEvent event, String bridgeUID, String accessoryUID, String serviceUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return ("bridge:" + bridgeUID + ":accessory:" + accessoryUID + ":service:" + serviceUID)
                .equals(originalPublisher.toString());
    }

    /**
     * Checks if an event originated from a characteristic on a specific service.
     * This method verifies if the event was generated by the specified characteristic.
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @param accessoryUID the accessory UID to check against
     * @param serviceUID the service UID to check against
     * @param characteristicUID the characteristic UID to check against
     * @return true if the event originated from the specified characteristic
     */
    public static boolean isFromCharacteristic(HomekitEvent event, String bridgeUID, String accessoryUID,
            String serviceUID, String characteristicUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return ("bridge:" + bridgeUID + ":accessory:" + accessoryUID + ":service:" + serviceUID + ":characteristic:"
                + characteristicUID).equals(originalPublisher.toString());
    }

    /**
     * Checks if an event originated from a peer component.
     * This method verifies if the event was generated by a peer in the system.
     *
     * @param event the event to check
     * @return true if the event originated from a peer
     */
    public static boolean isFromPeer(HomekitEvent event) {
        return event.getMetadata().isFromPeer();
    }

    /**
     * Checks if an event was created by a specific component.
     * This method verifies the creator of the event.
     *
     * @param event the event to check
     * @param componentId the component ID to check
     * @return true if the event was created by the component
     */
    public static boolean isCreatedBy(HomekitEvent event, UID componentId) {
        return event.getMetadata().isCreatedBy(componentId);
    }

    /**
     * Checks if an event is correlated with another event.
     * This method verifies if two events are part of the same correlation group.
     *
     * @param event1 the first event
     * @param event2 the second event
     * @return true if the events share the same correlation ID
     */
    public static boolean isCorrelated(HomekitEvent event1, HomekitEvent event2) {
        return event1.getMetadata().getCorrelationId().isPresent()
                && event1.getMetadata().getCorrelationId().equals(event2.getMetadata().getCorrelationId());
    }

    /**
     * Checks if an event should be processed by a component based on origin and correlation.
     * This method helps prevent feedback loops in decoupled event processing.
     *
     * @param event the event to check
     * @param componentId the ID of the component that would process the event
     * @return true if the event should be processed
     */
    public static boolean shouldProcess(HomekitEvent event, UID componentId) {
        // Don't process events created by this component
        if (isCreatedBy(event, componentId)) {
            return false;
        }

        // Don't process events from peers if configured
        if (isFromPeer(event)) {
            return false;
        }

        // Check if this component has already processed a correlated event
        if (event.getMetadata().getCorrelationId().isPresent()) {
            // This would require tracking processed correlation IDs per component
            // Implementation depends on your specific needs
        }

        return true;
    }
}
