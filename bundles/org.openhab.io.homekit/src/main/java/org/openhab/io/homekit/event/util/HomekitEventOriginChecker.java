package org.openhab.io.homekit.event.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEvent;

/**
 * Utility class for checking event origins and preventing unauthorized event propagation.
 * <p>
 * This class provides methods to verify if an event originated from a specific source
 * or if it has passed through certain components in the system.
 * </p>
 */
@NonNullByDefault
public class HomekitEventOriginChecker {
    /**
     * Checks if an event originated from a specific publisher.
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
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @param accessoryUID the accessory UID to check against
     * @return true if the event originated from the specified accessory on the bridge
     */
    public static boolean isFromAccessory(HomekitEvent event, String bridgeUID, String accessoryUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return originalPublisher.toString().equals("bridge:" + bridgeUID + ":accessory:" + accessoryUID);
    }

    /**
     * Checks if an event originated from a service on a specific accessory.
     *
     * @param event the event to check
     * @param bridgeUID the bridge UID to check against
     * @param accessoryUID the accessory UID to check against
     * @param serviceUID the service UID to check against
     * @return true if the event originated from the specified service
     */
    public static boolean isFromService(HomekitEvent event, String bridgeUID, String accessoryUID, String serviceUID) {
        UID originalPublisher = event.getMetadata().getOriginalPublisherUID();
        return originalPublisher.toString()
                .equals("bridge:" + bridgeUID + ":accessory:" + accessoryUID + ":service:" + serviceUID);
    }

    /**
     * Checks if an event originated from a characteristic on a specific service.
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
        return originalPublisher.toString().equals("bridge:" + bridgeUID + ":accessory:" + accessoryUID + ":service:"
                + serviceUID + ":characteristic:" + characteristicUID);
    }

    /**
     * Checks if an event originated from a peer component.
     *
     * @param event the event to check
     * @return true if the event originated from a peer
     */
    public static boolean isFromPeer(HomekitEvent event) {
        return event.getMetadata().isFromPeer();
    }

    /**
     * Checks if an event was created by a specific component.
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
     *
     * @param event1 the first event
     * @param event2 the second event
     * @return true if the events share the same correlation ID
     */
    public static boolean isCorrelated(HomekitEvent event1, HomekitEvent event2) {
        UID correlationId1 = event1.getMetadata().getCorrelationId();
        UID correlationId2 = event2.getMetadata().getCorrelationId();
        return correlationId1 != null && correlationId1.equals(correlationId2);
    }

    /**
     * Checks if an event should be processed by a component based on origin and correlation.
     * This helps prevent feedback loops in decoupled event processing.
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
        if (event.getMetadata().getCorrelationId() != null) {
            // This would require tracking processed correlation IDs per component
            // Implementation depends on your specific needs
        }

        return true;
    }
}
