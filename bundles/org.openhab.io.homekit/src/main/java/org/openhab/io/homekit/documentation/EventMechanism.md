# HomeKit Event Mechanism

This document describes the event mechanism used in the HomeKit integration, including event types, propagation, and processing.

## Overview

The event system in the HomeKit integration provides a robust mechanism for:
- Event creation and propagation
- Event type management
- Event metadata handling
- Event correlation
- Event processing

## Event Types

The system supports several types of events:

1. **Accessory Events**
   - Accessory state changes
   - Accessory addition/removal
   - Accessory configuration updates

2. **Characteristic Events**
   - Characteristic value changes
   - Characteristic metadata updates
   - Characteristic configuration changes

3. **Service Events**
   - Service state changes
   - Service addition/removal
   - Service configuration updates

## Event Metadata

The `EventMetadata` class provides the foundation for event tracking:

```java
// Create new event metadata
EventMetadata metadata = new EventMetadata(
    "publisher:123",          // Original publisher UID
    "correlation:456",        // Optional correlation ID
    "bridge:789",             // Immediate origin (creator)
    Set.of("bridge:789")      // Peer identifiers
);

// Create event metadata from existing metadata
EventMetadata newMetadata = new EventMetadata(originalMetadata, "newCreator");
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
EventMetadata metadata1 = new EventMetadata(
    "bridge:123",    // Original publisher
    null,            // No correlation ID
    "bridge:123",    // Immediate origin (same as publisher)
    Set.of()         // No peers
);

// Accessory forwards the event
EventMetadata metadata2 = new EventMetadata(
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
public class HomeKitBridge {
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
EventMetadata metadata = new EventMetadata(
    "bridge:123",
    correlationId,    // Set correlation ID
    "bridge:123",
    Set.of()
);

// Related event reuses correlation ID
EventMetadata related = new EventMetadata(
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
HomekitEvent event = new CharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "accessory:456",  // Set specific subscriber UID
    new EventMetadata(
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
HomekitEvent event1 = new CharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "*",  // Explicit wildcard subscriberUID
    new EventMetadata(
        "bridge:123",
        null,
        "bridge:123",
        Set.of()
    )
);

// Create a broadcast event by omitting subscriberUID
HomekitEvent event2 = new CharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    null,  // Omitted subscriberUID = broadcast
    new EventMetadata(
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
HomekitEvent event = new CharacteristicEvent(
    HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
    "bridge:123",
    "*",  // Use wildcard for broadcast
    new EventMetadata(
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

- [HomeKit Accessory Protocol Specification](https://developer.apple.com/homekit/)
- [OpenHAB Event System](https://www.openhab.org/docs/developer/architecture/events.html) 

## Recommendation

Based on the analysis, the following enhancements are recommended for OpenHAB in order of priority:

1. **Event Context Propagation**: Provides the most value with minimal impact
2. **Event Processing Timeouts**: Improves reliability with low overhead
3. **Event Processing Metrics Aggregation**: Enhances monitoring with minimal impact
4. **Event Processing Circuit Breakers**: Improves system stability
5. **Event Processing Rate Limiting**: Prevents system overload

These enhancements would provide significant benefits while maintaining OpenHAB's performance and reliability requirements.