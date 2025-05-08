# Homekit Event Mechanism

This document describes the event mechanism used in the Homekit integration, including event types, propagation, and processing.

## Overview

The event system in the Homekit integration provides a robust mechanism for:
- Event creation and propagation
- Event type management
- Event metadata handling
- Event correlation
- Event processing

## Event Types

The system supports several types of events:

1. **HomekitAccessory Events**
   - HomekitAccessory state changes
   - HomekitAccessory addition/removal
   - HomekitAccessory configuration updates

2. **HomekitCharacteristic Events**
   - HomekitCharacteristic value changes
   - HomekitCharacteristic metadata updates
   - HomekitCharacteristic configuration changes

3. **HomekitService Events**
   - HomekitService state changes
   - HomekitService addition/removal
   - HomekitService configuration updates

## Event Metadata

The `HomekitEventMetadata` class provides the foundation for event tracking:

```java
// Create new event metadata
HomekitEventMetadata metadata = new HomekitEventMetadata(
    "publisher:123",          // Original publisher UID
    "correlation:456",        // Optional correlation ID
    "bridge:789",             // Immediate origin (creator)
    Set.of("bridge:789")      // Peer identifiers
);

// Create event metadata from existing metadata
HomekitEventMetadata newMetadata = new HomekitEventMetadata(originalMetadata, "newCreator");
```

## Event Origin Concepts

### Original Publisher vs Immediate Origin

Events track two types of origins:

1. **Original Publisher**: The component that initially created the event
   - Stored in `originalPublisherUid`
   - Never changes during event propagation
   - Used to trace the ultimate source of an event

2. **Immediate Origin**: The component that last modified or forwarded the event
   - Stored in `immediateOrigin`
   - Updated each time the event is forwarded
   - Used to prevent immediate feedback loops

Example:
```java
// Bridge creates an event
HomekitEventMetadata metadata1 = new HomekitEventMetadata(
    "bridge:123",    // Original publisher
    null,            // No correlation ID
    "bridge:123",    // Immediate origin (same as publisher)
    Set.of()         // No peers
);

// HomekitAccessory forwards the event
HomekitEventMetadata metadata2 = new HomekitEventMetadata(
    metadata1,       // Preserve original publisher
    "accessory:456"  // New immediate origin
);
```

## Event Processing

### Event Processing Flow

1. **Event Creation**
   - Define event type
   - Set metadata
   - Initialize event data

2. **Event Publication**
   - Submit to event manager
   - Queue for processing
   - Notify subscribers

3. **Event Processing**
   - Validate event
   - Apply business logic
   - Update system state

4. **Event Forwarding**
   - Create new events if needed
   - Update metadata
   - Propagate to next component

### Event Processing Example

```java
public class HomekitBridge {
    private final String bridgeId;
    private final HomekitEventManager eventManager;

    public void onEvent(HomekitEvent event) {
        // 1. Validate event
        if (!isValidEvent(event)) {
            return;
        }

        // 2. Process event
        try {
            processEvent(event);

            // 3. Create new event if needed
            if (shouldForwardEvent(event)) {
                HomekitEvent newEvent = createNewEvent(event);
                eventManager.publish(newEvent);
            }
        } catch (Exception e) {
            handleProcessingError(e);
        }
    }
}
```

## Event Correlation

Events can be correlated to track related state changes:

```java
// Initial event with correlation ID
String correlationId = UUID.randomUUID().toString();
HomekitEventMetadata metadata = new HomekitEventMetadata(
    "bridge:123",
    correlationId,    // Set correlation ID
    "bridge:123",
    Set.of()
);

// Related event reuses correlation ID
HomekitEventMetadata related = new HomekitEventMetadata(
    "accessory:456",
    correlationId,    // Same correlation ID
    "accessory:456",
    Set.of()
);
```

## Event Subscription

Components can subscribe to specific event types:

```java
// Subscribe to characteristic state changes
eventManager.subscribe(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "accessory:456",
    this::onCharacteristicStateChanged
);

// Subscribe to accessory events
eventManager.subscribe(
    HomekitEventType.ACCESSORY_STATE_CHANGED,
    "bridge:123",
    null,
    this::onAccessoryStateChanged
);
```

## Event Routing Options

The event system supports two routing modes:

1. **Direct Routing**: Events are delivered only to specific subscribers
2. **Broadcast Routing**: Events are delivered to all matching subscribers

### Direct Routing

Direct routing is used when an event should be delivered to a specific subscriber:

```java
// Create an event with a specific destination
HomekitEvent event = new HomekitCharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "accessory:456",  // Set specific subscriber UID
    new HomekitEventMetadata(
        "bridge:123",
        null,
        "bridge:123",
        Set.of()
    )
);
eventManager.publishEvent(event);
```

Key aspects of direct routing:
- Events are delivered only to the specified subscriber
- More efficient than broadcast for targeted updates
- Reduces unnecessary event processing
- Helps prevent loops by limiting event propagation

### Wildcard SubscriberUID

The event system uses "*" as a special subscriberUID to indicate broadcast routing. The wildcard can be specified explicitly or omitted entirely:

```java
// Create a broadcast event using explicit wildcard
HomekitEvent event1 = new HomekitCharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "*",  // Explicit wildcard subscriberUID
    new HomekitEventMetadata(
        "bridge:123",
        null,
        "bridge:123",
        Set.of()
    )
);

// Create a broadcast event by omitting subscriberUID
HomekitEvent event2 = new HomekitCharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    null,  // Omitted subscriberUID = broadcast
    new HomekitEventMetadata(
        "bridge:123",
        null,
        "bridge:123",
        Set.of()
    )
);

// Both events will be delivered to all matching subscribers
eventManager.publishEvent(event1);
eventManager.publishEvent(event2);
```

Key aspects of wildcard subscriberUID:
- "*" indicates that the event should be delivered to all matching subscribers
- `null` subscriberUID is treated the same as "*" for broadcast
- Used for system-wide updates and discovery
- Enables broadcast-style event propagation
- Requires careful loop prevention measures

When processing events with wildcard subscriberUID:
```java
public void onEvent(HomekitEvent event) {
    // Check if event is for all subscribers
    if (event.getSubscriberUID() == null || "*".equals(event.getSubscriberUID())) {
        // Process broadcast event
        processBroadcastEvent(event);
    } else {
        // Process direct event
        processDirectEvent(event);
    }
}
```

Common use cases for wildcard subscriberUID:
1. **System Discovery**: Finding all available components
2. **State Synchronization**: Updating all interested parties
3. **Configuration Changes**: Notifying all affected components
4. **System-wide Events**: Broadcasting important system events

### Broadcast Routing

Broadcast routing is used when an event should be delivered to all matching subscribers:

```java
// Create a broadcast event
HomekitEvent event = new HomekitCharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "*",  // Use wildcard for broadcast
    new HomekitEventMetadata(
        "bridge:123",
        null,
        "bridge:123",
        Set.of()
    )
);
eventManager.publishEvent(event);
```

Key aspects of broadcast routing:
- Events are delivered to all matching subscribers
- Useful for system-wide updates
- Supports discovery and coordination
- Requires careful loop prevention

## References

- [Homekit HomekitAccessory Protocol Specification](https://developer.apple.com/homekit/)
- [OpenHAB Event System](https://www.openhab.org/docs/developer/architecture/events.html) 

## Recommendation

Based on the analysis, the following enhancements are recommended for OpenHAB in order of priority:

1. **Event Context Propagation**: Provides the most value with minimal impact
2. **Event Processing Timeouts**: Improves reliability with low overhead
3. **Event Processing Metrics Aggregation**: Enhances monitoring with minimal impact
4. **Event Processing Circuit Breakers**: Improves system stability
5. **Event Processing Rate Limiting**: Prevents system overload

These enhancements would provide significant benefits while maintaining OpenHAB's performance and reliability requirements.