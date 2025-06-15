# Event Loop Prevention and Event Processing

This document describes the mechanisms for preventing event loops and managing event processing in the Homekit integration.

## Overview

The event system includes several features to prevent loops and manage event propagation:

1. **Event History Tracking**: Each event maintains a history of its propagation path
2. **Publisher History Tracking**: Each event maintains an ordered list of publishers in its propagation chain
3. **Hop Count Limiting**: Events are limited to a maximum number of hops
4. **Origin Tracking**: Events track their original publisher and immediate creator
5. **Correlation IDs**: Related events can be linked using correlation IDs
6. **Peer Identification**: Components can identify related peers to prevent feedback loops

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

3. **Publisher History**: The ordered list of all publishers in the event's propagation chain
   - Stored in `publisherHistory`
   - Updated each time the event is forwarded
   - Used to detect publisher-based loops and provide publisher traceability

Example:
```java
// Bridge creates an event
HomekitEventMetadata metadata1 = new HomekitEventMetadata(
    "bridge:123",    // Original publisher
    null,            // No correlation ID
    "bridge:123",    // Immediate origin (same as publisher)
    Set.of()         // No peers
);
// publisherHistory = ["bridge:123"]

// HomekitAccessory forwards the event
HomekitEventMetadata metadata2 = new HomekitEventMetadata(
    metadata1,       // Preserve original publisher
    "accessory:456"  // New immediate origin
);
// publisherHistory = ["bridge:123", "accessory:456"]
```

### Peer Identification

Peers are components that should be treated as equivalent for event processing purposes:

1. **Purpose**:
   - Prevent feedback loops between related components
   - Group components that share responsibility
   - Enable coordinated event processing

2. **Usage**:
   - Components can identify their peers when creating events
   - Events from peers can be skipped to prevent loops
   - Useful for components that share state or functionality

Example:
```java
// Bridge with multiple instances
Set<String> bridgePeers = Set.of(
    "bridge:123",
    "bridge:456",
    "bridge:789"
);

// Create event with peer identification
HomekitEventMetadata metadata = new HomekitEventMetadata(
    "bridge:123",    // Original publisher
    null,            // No correlation ID
    "bridge:123",    // Immediate origin
    bridgePeers      // All bridge instances are peers
);

// Another bridge can check if event is from a peer
if (EventOriginChecker.isFromPeer(event)) {
    // Skip processing to prevent feedback loop
    return;
}
```

Common peer scenarios:
1. **Multiple Bridge Instances**: All bridges in a cluster
2. **Redundant Services**: Multiple instances of the same service
3. **Related Components**: Components that share state or functionality
4. **Load-Balanced Components**: Components that distribute work

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

## Event Origin Checking

The `EventOriginChecker` utility class provides methods for checking event origins:

```java
// Basic origin checks
boolean isFromBridge = EventOriginChecker.isFromBridge(event, "bridge123");
boolean isFromAccessory = EventOriginChecker.isFromAccessory(event, "bridge123", "accessory456");
boolean isFromService = EventOriginChecker.isFromService(event, "bridge123", "accessory456", "service789");

// Advanced origin checks
boolean isFromPeer = EventOriginChecker.isFromPeer(event);
boolean isCreatedByMe = EventOriginChecker.isCreatedBy(event, "myComponentId");
boolean isCorrelated = EventOriginChecker.isCorrelated(event1, event2);
```

## Loop Prevention Mechanisms Working Together

The Homekit event system uses multiple complementary mechanisms to prevent event loops. These mechanisms work at different levels and provide overlapping protection:

### 1. Event History and Hop Count
- **Purpose**: Prevent infinite propagation of the same event
- **Implementation**: Each event tracks its history and hop count
- **When Used**: Every time an event is created or forwarded
- **Example**:
```java
// Event with history and hop count
HomekitEventMetadata metadata = new HomekitEventMetadata(
    "bridge:123",    // Original publisher
    null,            // No correlation ID
    "bridge:123",    // Immediate origin
    Set.of()         // No peers
);

// When forwarding, hop count increases
HomekitEventMetadata forwarded = new HomekitEventMetadata(metadata, "accessory:456");
// Hop count is now 1, history includes original event
// Publisher history is ["bridge:123", "accessory:456"]
```

### 2. Publisher History
- **Purpose**: Detect publisher-based loops and provide publisher traceability
- **Implementation**: Each event maintains an ordered list of publishers
- **When Used**: Every time an event is forwarded
- **Example**:
```java
// Check for publisher-based loops
if (event.getMetadata().hasLoop()) {
    // Publisher appears more than once in history
    return;
}

// Check if we've already published this event
if (event.getMetadata().isInPublisherHistory(myPublisherId)) {
    // We're already in the publisher chain
    return;
}
```

### 3. Correlation ID Tracking
- **Purpose**: Link related events and prevent processing the same logical event multiple times
- **Implementation**: Events share correlation IDs, tracked in HomekitEventMetadata
- **When Used**: When events are part of the same logical flow
- **Example**:
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

### 4. Origin and Peer Tracking
- **Purpose**: Prevent feedback loops between related components
- **Implementation**: Track original publisher, immediate origin, and peer identifiers
- **When Used**: When processing events from related components
- **Example**:
```java
// Bridge and its accessories are peers
Set<String> peers = Set.of("bridge:123", "accessory:456", "accessory:789");
HomekitEventMetadata metadata = new HomekitEventMetadata(
    "bridge:123",
    null,
    "bridge:123",
    peers            // Define peer group
);

// Check if event is from a peer
if (metadata.isFromPeer()) {
    // Handle peer event differently
}
```

# Event Loop Prevention

## Overview

This document describes the mechanisms used to prevent event loops in the Homekit integration.

## Peer Groups vs Original Publisher Check

### Original Publisher Check
```java
if (event.getMetadata().getOriginalPublisher().equals(getUID())) {
    // Skip processing - this event originated from us
    return;
}
```

### Peer Group Check
```java
if (event.getMetadata().isFromPeerGroup(Set.of(
    new HomekitPeerGroupUID("openhab-homekit"),
    getUID(),
    service.getUID()
))) {
    // Skip processing
    return;
}
```

### When to Use Each Approach

1. **Use Original Publisher Check When**:
   - Simple loop prevention is needed
   - Event chain is linear
   - Component relationships are simple
   - Performance is critical

2. **Use Peer Groups When**:
   - Complex system architecture exists
   - Multiple component types interact
   - System boundaries need enforcement
   - Flexible routing rules are needed
   - Security boundaries are important

### Advantages of Peer Groups

1. **Component Relationships**:
   - Can represent hierarchical relationships
   - Can group related components
   - Can represent system-wide boundaries

2. **Event Routing Control**:
   - Can control event flow between component types
   - Can prevent cross-boundary events
   - Can implement domain-specific routing rules

3. **System Architecture**:
   - Better represents system structure
   - Can enforce architectural boundaries
   - Can implement security boundaries

4. **Flexibility**:
   - Can add/remove components from groups
   - Can change routing rules without code changes
   - Can implement complex routing scenarios

### Example Scenarios Where Peer Groups Are Better

1. **System Boundary Control**:
```java
// Prevent events from crossing system boundaries
if (event.getMetadata().isFromPeerGroup(Set.of(new HomekitPeerGroupUID("external-system")))) {
    // Block events from external systems
    return;
}
```

2. **Component Type Control**:
```java
// Control events between component types
if (event.getMetadata().isFromPeerGroup(Set.of(new HomekitPeerGroupUID("homekit-bridge")))) {
    // Handle bridge-specific events
    return;
}
```

3. **Domain-Specific Routing**:
```java
// Implement domain-specific routing
if (event.getMetadata().isFromPeerGroup(Set.of(new HomekitPeerGroupUID("security-domain")))) {
    // Handle security-related events
    return;
}
```

4. **Architectural Boundaries**:
```java
// Enforce architectural boundaries
if (event.getMetadata().isFromPeerGroup(Set.of(new HomekitPeerGroupUID("presentation-layer")))) {
    // Handle presentation layer events
    return;
}
```

### Conclusion

While the original publisher check is simpler, peer groups provide more powerful and flexible control over event routing and system architecture. In a complex system like Homekit integration, peer groups are likely the better choice because they:
1. Better represent the system architecture
2. Provide more control over event routing
3. Can enforce system boundaries
4. Are more flexible for future changes

## Event Chain Analysis 

### 4. Event Graph Analysis
- **Purpose**: Detect potential loops in subscription patterns
- **Implementation**: Graph-based analysis of subscription relationships
- **When Used**: During configuration and subscription changes
- **Example**:
```java
// Subscription pattern that would create a cycle
subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, "bridge:123", "accessory:456");
subscribe(HomekitEventType.CHARACTERISTIC_STATE_CHANGED, "accessory:456", "bridge:123");
// Graph analysis would detect this cycle
```

## Comprehensive Example: Bridge Event Processing

Here's a complete example of how a bridge might process events:

```java
public class HomekitBridge {
    private final String bridgeId;
    private final HomekitEventManager eventManager;
    private final Set<String> processedCorrelationIds = new HashSet<>();

    public void onEvent(HomekitEvent event) {
        // 1. Check if we should process this event
        if (!EventOriginChecker.shouldProcess(event, bridgeId)) {
            logger.debug("Skipping event from peer or self: {}", event.getType());
            return;
        }

        // 2. Check for correlation to prevent duplicate processing
        String correlationId = event.getMetadata().getCorrelationId();
        if (correlationId != null && processedCorrelationIds.contains(correlationId)) {
            logger.debug("Skipping already processed correlated event: {}", correlationId);
            return;
        }

        // 3. Process the event
        try {
            // Your event processing logic here
            processEvent(event);

            // 4. Create a new event if needed
            if (shouldForwardEvent(event)) {
                String newCorrelationId = correlationId != null ? 
                    correlationId : UUID.randomUUID().toString();
                
                HomekitEvent newEvent = new MyEvent(
                    event.getType(),
                    "bridge:" + bridgeId,
                    new HomekitEventMetadata(
                        event.getMetadata(),
                        bridgeId,
                        newCorrelationId,
                        Set.of(bridgeId) // Mark this bridge as a peer
                    )
                );

                // 5. Track the correlation ID
                processedCorrelationIds.add(newCorrelationId);
                
                // 6. Publish the new event
                eventManager.publishEvent(newEvent);
            }
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getType(), e);
        }
    }

    private void processEvent(HomekitEvent event) {
        // Your event processing implementation
    }

    private boolean shouldForwardEvent(HomekitEvent event) {
        // Your logic to determine if the event should be forwarded
        return true;
    }
}
```

## Comprehensive Example: HomekitAccessory Event Processing

Here's how an accessory might process events:

```java
public class HomekitAccessory {
    private final String accessoryId;
    private final String bridgeId;
    private final HomekitEventManager eventManager;

    public void onEvent(HomekitEvent event) {
        // 1. Check if the event is relevant to this accessory
        if (!EventOriginChecker.isFromBridge(event, bridgeId)) {
            return;
        }

        // 2. Check if we created this event
        if (EventOriginChecker.isCreatedBy(event, accessoryId)) {
            return;
        }

        // 3. Process the event
        try {
            // Your event processing logic here
            processEvent(event);

            // 4. Create a response event if needed
            if (shouldRespond(event)) {
                HomekitEvent responseEvent = new ResponseEvent(
                    "response:" + event.getType(),
                    "accessory:" + accessoryId,
                    new HomekitEventMetadata(
                        event.getMetadata(),
                        accessoryId,
                        event.getMetadata().getCorrelationId(),
                        Set.of(accessoryId)
                    )
                );

                eventManager.publishEvent(responseEvent);
            }
        } catch (Exception e) {
            logger.error("Error processing event in accessory {}: {}", 
                accessoryId, event.getType(), e);
        }
    }

    private void processEvent(HomekitEvent event) {
        // Your event processing implementation
    }

    private boolean shouldRespond(HomekitEvent event) {
        // Your logic to determine if a response is needed
        return true;
    }
}
```

## Best Practices

1. **Event Processing**:
   - Always check event origins before processing
   - Use correlation IDs for related events
   - Implement proper error handling
   - Log important event processing steps

2. **Event Creation**:
   - Set appropriate correlation IDs
   - Include all relevant peer identifiers
   - Maintain event history
   - Set proper immediate origin

3. **Performance**:
   - Keep event history size reasonable
   - Implement efficient origin checking
   - Use appropriate logging levels
   - Clean up processed correlation IDs

4. **Debugging**:
   - Enable debug mode for detailed event history
   - Log event propagation paths
   - Track correlation chains
   - Monitor event processing metrics

## Troubleshooting

1. **Event Loops**:
   - Check event history for circular paths
   - Verify peer identification
   - Review correlation ID usage
   - Monitor hop counts

2. **Missing Events**:
   - Verify origin checking logic
   - Check correlation ID tracking
   - Review peer identification
   - Monitor event processing logs

3. **Performance Issues**:
   - Review event history size
   - Check origin checking efficiency
   - Monitor correlation ID tracking
   - Review logging levels

## Metrics and Monitoring

The system tracks several metrics for monitoring event processing:

1. **Event Processing**:
   - Total events processed
   - Events skipped due to origin
   - Events skipped due to correlation
   - Processing errors

2. **Event Propagation**:
   - Average hop count
   - Maximum hop count reached
   - Events dropped due to max hops
   - Events in loops detected

3. **Performance**:
   - Processing time per event
   - Memory usage for event history
   - Correlation ID tracking size
   - Origin checking time

Use these metrics to monitor system health and identify potential issues.

## Potential Enhancements Analysis

This section analyzes potential enhancements to the event system, considering their impact on OpenHAB and the current solution.

### 1. Event Context Propagation

**Pros:**
- Enables flexible event enrichment without modifying event structure
- Supports dynamic routing based on context
- Facilitates debugging with additional context information
- Maintains backward compatibility with existing events

**Cons:**
- Increases memory usage for each event
- May complicate event serialization
- Requires careful management of context size
- Could lead to context pollution if not properly managed

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead if properly implemented
- Good fit for OpenHAB's modular architecture
- Useful for debugging and monitoring

### 2. Event Processing Policies

**Pros:**
- Provides centralized control over event routing
- Enables dynamic updates to processing rules
- Supports complex routing scenarios
- Facilitates system-wide policy management

**Cons:**
- Adds complexity to event processing
- May impact performance with complex policies
- Requires careful policy validation
- Could lead to policy conflicts

**OpenHAB Impact:**
- High implementation effort
- Moderate runtime overhead
- Good fit for OpenHAB's rule engine
- Useful for complex automation scenarios

### 3. Event Processing Graph

**Pros:**
- Provides visual representation of event flows
- Enables proactive loop detection
- Supports complex routing decisions
- Facilitates system understanding

**Cons:**
- High memory usage for graph storage
- Complex implementation
- May impact performance
- Requires graph maintenance

**OpenHAB Impact:**
- High implementation effort
- High runtime overhead
- May be overkill for most OpenHAB use cases
- Better suited for complex enterprise systems

### 4. Event Processing Timeouts

**Pros:**
- Prevents event processing stalls
- Enables automatic cleanup of stuck events
- Improves system reliability
- Supports monitoring of processing times

**Cons:**
- Adds complexity to event tracking
- May lead to premature event termination
- Requires careful timeout configuration
- Could impact system performance

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead
- Good fit for OpenHAB's reliability requirements
- Useful for preventing system hangs

### 5. Event Processing Dependencies

**Pros:**
- Enables proper ordering of event processing
- Supports complex event relationships
- Improves system reliability
- Facilitates debugging

**Cons:**
- Adds complexity to event processing
- May impact performance
- Requires careful dependency management
- Could lead to dependency cycles

**OpenHAB Impact:**
- High implementation effort
- Moderate runtime overhead
- Good fit for OpenHAB's automation scenarios
- Useful for complex rule execution

### 6. Event Processing State Machine

**Pros:**
- Provides clear event processing states
- Enables state-based routing
- Improves system reliability
- Facilitates debugging

**Cons:**
- Adds complexity to event processing
- May impact performance
- Requires careful state management
- Could lead to state explosion

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead
- Good fit for OpenHAB's state management
- Useful for complex device interactions

### 7. Event Processing Metrics Aggregation

**Pros:**
- Provides system-wide visibility
- Enables performance optimization
- Supports capacity planning
- Facilitates troubleshooting

**Cons:**
- Adds overhead to event processing
- Requires careful metric selection
- May impact system performance
- Could lead to metric overload

**OpenHAB Impact:**
- Low implementation effort
- Low runtime overhead
- Good fit for OpenHAB's monitoring needs
- Useful for system optimization

### 8. Event Processing Circuit Breakers

**Pros:**
- Prevents cascading failures
- Improves system reliability
- Enables graceful degradation
- Supports automatic recovery

**Cons:**
- Adds complexity to event processing
- Requires careful configuration
- May impact system responsiveness
- Could lead to false positives

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead
- Good fit for OpenHAB's reliability requirements
- Useful for preventing system overload

### 9. Event Processing Rate Limiting

**Pros:**
- Prevents system overload
- Enables fair resource allocation
- Improves system stability
- Supports quality of service

**Cons:**
- Adds complexity to event processing
- May impact system responsiveness
- Requires careful configuration
- Could lead to event loss

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead
- Good fit for OpenHAB's resource management
- Useful for preventing system overload

### 10. Event Processing Priority Queues

**Pros:**
- Enables priority-based processing
- Improves system responsiveness
- Supports quality of service
- Facilitates critical event handling

**Cons:**
- Adds complexity to event processing
- May impact system performance
- Requires careful priority assignment
- Could lead to priority inversion

**OpenHAB Impact:**
- Moderate implementation effort
- Low runtime overhead
- Good fit for OpenHAB's automation needs
- Useful for handling critical events

## Recommendation

Based on the analysis, the following enhancements are recommended for OpenHAB in order of priority:

1. **Event Context Propagation**: Provides the most value with minimal impact
2. **Event Processing Timeouts**: Improves reliability with low overhead
3. **Event Processing Metrics Aggregation**: Enhances monitoring with minimal impact
4. **Event Processing Circuit Breakers**: Improves system stability
5. **Event Processing Rate Limiting**: Prevents system overload

These enhancements would provide significant benefits while maintaining OpenHAB's performance and reliability requirements.

## Common Loop Scenarios and Prevention

### 1. Bridge-HomekitAccessory Feedback Loop
**Scenario**: Bridge updates accessory, accessory updates bridge, creating a loop.

**Prevention**:
```java
// In bridge event handler
public void onEvent(HomekitEvent event) {
    // Check if event originated from this bridge
    if (event.getMetadata().isCreatedBy(myBridgeId)) {
        return; // Ignore self-created events
    }
    
    // Check if event is from a peer accessory
    if (event.getMetadata().isFromPeer()) {
        return; // Ignore peer events
    }
    
    // Process event and create new event with correlation
    String correlationId = event.getMetadata().getCorrelationId();
    if (correlationId == null) {
        correlationId = UUID.randomUUID().toString();
    }
    
    HomekitEvent newEvent = new HomekitCharacteristicEvent(
        HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
        myBridgeId,
        new HomekitEventMetadata(
            myBridgeId,
            correlationId,
            myBridgeId,
            myPeerIdentifiers
        )
    );
}
```

### 2. Multi-Hop Event Propagation
**Scenario**: Event propagates through multiple components, potentially creating loops.

**Prevention**:
```java
// In component event handler
public void onEvent(HomekitEvent event) {
    // Check hop count
    if (event.getMetadata().hasExceededMaxHops()) {
        logger.warn("Event exceeded max hops, dropping");
        return;
    }
    
    // Check event history
    if (event.getMetadata().isInHistory(event.getMetadata().getEventId())) {
        logger.warn("Event loop detected, dropping");
        return;
    }
    
    // Process event and forward with increased hop count
    HomekitEventMetadata newMetadata = new HomekitEventMetadata(event.getMetadata(), myComponentId);
    HomekitEvent newEvent = new HomekitCharacteristicEvent(
        event.getType(),
        myComponentId,
        newMetadata
    );
}
```

### 3. Subscription-Based Loops
**Scenario**: Components subscribe to each other's events, creating potential loops.

**Prevention**:
```java
// When adding subscriptions
public void addSubscription(HomekitEventType type, String publisher, String subscriber) {
    // Check for potential cycles in subscription graph
    if (eventGraph.hasCycle()) {
        logger.warn("Subscription would create cycle: {} -> {}", publisher, subscriber);
        // Optionally reject subscription or just warn
    }
    
    // Add subscription with peer awareness
    HomekitEventMetadata metadata = new HomekitEventMetadata(
        publisher,
        null,
        publisher,
        getPeerGroup(publisher, subscriber)
    );
    
    subscribe(type, publisher, subscriber, metadata);
}
```

## Best Practices

1. **Always Check Event Validity**:
   ```java
   if (!event.isValid()) {
       return; // Event is invalid (loop or max hops exceeded)
   }
   ```

2. **Use Correlation IDs for Related Events**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   ```

3. **Define Appropriate Peer Groups**:
   ```java
   Set<String> peers = Set.of("bridge:123", "accessory:456");
   HomekitEventMetadata metadata = new HomekitEventMetadata(publisher, null, publisher, peers);
   ```

4. **Monitor Event Metrics**:
   ```java
   Map<String, Integer> metrics = eventManager.getEventMetrics();
   logger.info("Dropped events due to loops: {}", metrics.get("droppedEventsDueToLoops"));
   logger.info("Dropped events due to hops: {}", metrics.get("droppedEventsDueToHops"));
   logger.info("Dropped events due to correlation: {}", metrics.get("droppedEventsDueToCorrelation"));
   ```

## Troubleshooting Event Loops

1. **Check Event History**:
   ```java
   String history = event.getMetadata().getEventHistoryAsString();
   logger.debug("Event history: {}", history);
   ```

2. **Check Publisher History**:
   ```java
   String publisherHistory = event.getMetadata().getPublisherHistoryAsString();
   logger.debug("Publisher history: {}", publisherHistory);
   ```

3. **Monitor Correlation IDs**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId != null) {
       logger.debug("Processing event with correlation ID: {}", correlationId);
   }
   ```

4. **Verify Peer Relationships**:
   ```java
   if (event.getMetadata().isFromPeer()) {
       logger.debug("Event from peer: {}", event.getMetadata().getImmediateOrigin());
   }
   ```

5. **Analyze Subscription Graph**:
   ```java
   if (eventGraph.hasCycle()) {
       String cyclePath = eventGraph.getCyclePath();
       logger.warn("Cycle detected in subscription graph: {}", cyclePath);
   }
   ```

## Correlation ID Management

Correlation IDs are a powerful mechanism for tracking related events and preventing loops. This section provides detailed information about correlation ID management.

### Correlation ID Lifecycle

1. **Creation**:
   ```java
   // When creating a root event (not in response to another event)
   String correlationId = UUID.randomUUID().toString();
   HomekitEventMetadata metadata = new HomekitEventMetadata(
       "bridge:123",
       correlationId,    // New correlation ID
       "bridge:123",
       Set.of()
   );

   // When creating a response event
   String correlationId = originalEvent.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   HomekitEventMetadata metadata = new HomekitEventMetadata(
       "accessory:456",
       correlationId,    // Reuse correlation ID
       "accessory:456",
       Set.of()
   );
   ```

2. **Propagation**:
   ```java
   // In event handler
   public void onEvent(HomekitEvent event) {
       // Get correlation ID from original event
       String correlationId = event.getMetadata().getCorrelationId();
       
       // Process event
       processEvent(event);
       
       // Create new event with same correlation ID
       HomekitEvent newEvent = new HomekitCharacteristicEvent(
           event.getType(),
           "bridge:123",
           new HomekitEventMetadata(
               "bridge:123",
               correlationId,    // Propagate correlation ID
               "bridge:123",
               Set.of()
           )
       );
   }
   ```

3. **Expiration**:
   ```java
   // Check if correlation ID has expired
   if (event.getMetadata().hasCorrelationExpired()) {
       logger.debug("Correlation ID expired, creating new one");
       String newCorrelationId = UUID.randomUUID().toString();
       // Create new event with new correlation ID
   }
   ```

### Correlation ID Tracking

1. **Component-Level Tracking**:
   ```java
   public class HomekitComponent {
       private final Set<String> processedCorrelationIds = new HashSet<>();
       
       public void onEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               if (processedCorrelationIds.contains(correlationId)) {
                   logger.debug("Already processed correlation ID: {}", correlationId);
                   return;
               }
               processedCorrelationIds.add(correlationId);
           }
           
           // Process event
       }
   }
   ```

2. **System-Level Tracking**:
   ```java
   public class HomekitEventManager {
       private final Map<String, Long> correlationTimestamps = new ConcurrentHashMap<>();
       
       public void publishEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               Long timestamp = correlationTimestamps.get(correlationId);
               if (timestamp != null && 
                   System.currentTimeMillis() - timestamp < HomekitEventMetadata.CORRELATION_ID_EXPIRY_MS) {
                   logger.debug("Event with correlation ID {} already processed", correlationId);
                   return;
               }
               correlationTimestamps.put(correlationId, System.currentTimeMillis());
           }
           
           // Publish event
       }
   }
   ```

### Advanced Correlation Patterns

1. **Correlation Chains**:
   ```java
   // Track correlation chain
   public class CorrelationChain {
       private final String rootCorrelationId;
       private final List<String> chain = new ArrayList<>();
       
       public CorrelationChain(String rootCorrelationId) {
           this.rootCorrelationId = rootCorrelationId;
           this.chain.add(rootCorrelationId);
       }
       
       public void addLink(String correlationId) {
           chain.add(correlationId);
       }
       
       public boolean isInChain(String correlationId) {
           return chain.contains(correlationId);
       }
   }
   ```

2. **Correlation Groups**:
   ```java
   // Group related events
   public class CorrelationGroup {
       private final String groupId;
       private final Set<String> correlationIds = new HashSet<>();
       
       public void addEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               correlationIds.add(correlationId);
           }
       }
       
       public boolean isGroupEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           return correlationId != null && correlationIds.contains(correlationId);
       }
   }
   ```

### Best Practices for Correlation IDs

1. **Always Check for Existing Correlation IDs**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   ```

2. **Handle Correlation ID Expiration**:
   ```java
   if (event.getMetadata().hasCorrelationExpired()) {
       // Create new correlation ID or handle expired case
   }
   ```

3. **Track Correlation Metrics**:
   ```java
   public class CorrelationMetrics {
       private final AtomicInteger totalCorrelatedEvents = new AtomicInteger();
       private final AtomicInteger droppedCorrelatedEvents = new AtomicInteger();
       private final AtomicInteger expiredCorrelationIds = new AtomicInteger();
       
       public void logMetrics() {
           logger.info("Total correlated events: {}", totalCorrelatedEvents.get());
           logger.info("Dropped correlated events: {}", droppedCorrelatedEvents.get());
           logger.info("Expired correlation IDs: {}", expiredCorrelationIds.get());
       }
   }
   ```

4. **Use Correlation IDs for Debugging**:
   ```java
   public void debugEventFlow(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       if (correlationId != null) {
           logger.debug("Event flow for correlation ID {}: {}", correlationId, 
               event.getMetadata().getEventHistoryAsString());
       }
   }
   ```

### Common Correlation ID Scenarios

1. **Bridge-HomekitAccessory Communication**:
   ```java
   // Bridge sends event to accessory
   String correlationId = UUID.randomUUID().toString();
   HomekitEvent bridgeEvent = new HomekitCharacteristicEvent(
       HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
       "bridge:123",
       new HomekitEventMetadata(
           "bridge:123",
           correlationId,
           "bridge:123",
           Set.of()
       )
   );
   
   // HomekitAccessory processes event and responds
   public void onEvent(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       // Process event
       HomekitEvent response = new HomekitCharacteristicEvent(
           event.getType(),
           "accessory:456",
           new HomekitEventMetadata(
               "accessory:456",
               correlationId,    // Use same correlation ID
               "accessory:456",
               Set.of()
           )
       );
   }
   ```

2. **Multi-Step Event Processing**:
   ```java
   // Initial event
   String correlationId = UUID.randomUUID().toString();
   HomekitEvent initialEvent = new HomekitCharacteristicEvent(
       HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
       "bridge:123",
       new HomekitEventMetadata(
           "bridge:123",
           correlationId,
           "bridge:123",
           Set.of()
       )
   );
   
   // Each step in processing
   public void processStep(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       // Process step
       HomekitEvent nextStep = new HomekitCharacteristicEvent(
           event.getType(),
           "processor:789",
           new HomekitEventMetadata(
               "processor:789",
               correlationId,    // Maintain correlation ID
               "processor:789",
               Set.of()
           )
       );
   }
   ```

3. **Event Aggregation**:
   ```java
   // Aggregate related events
   public class EventAggregator {
       private final Map<String, List<HomekitEvent>> correlatedEvents = new HashMap<>();
       
       public void addEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               correlatedEvents.computeIfAbsent(correlationId, k -> new ArrayList<>())
                   .add(event);
           }
       }
       
       public List<HomekitEvent> getCorrelatedEvents(String correlationId) {
           return correlatedEvents.getOrDefault(correlationId, List.of());
       }
   }
   ```

### Loop Prevention with Direct Routing

Direct routing provides additional loop prevention benefits:

1. **Targeted Delivery**:
   ```java
   // Event is delivered only to the specified subscriber
   HomekitEvent event = new HomekitCharacteristicEvent(
       eventType,
       publisherUID,
       "specific-subscriber",  // Set specific subscriber UID
       metadata
   );
   ```

2. **Reduced Propagation**:
   ```java
   // Events don't propagate to unnecessary subscribers
   if (event.getSubscriberUID() != null && !event.getSubscriberUID().equals("*")) {
       // Only process if we're the intended recipient
       if (!event.getSubscriberUID().equals(myId)) {
           return;
       }
   }
   ```

3. **Explicit Path Control**:
   ```java
   // Control the event propagation path
   HomekitEvent newEvent = new HomekitCharacteristicEvent(
       event.getType(),
       event.getPublisherUID(),
       "next-hop",  // Set next hop as subscriber
       event.getMetadata()
   );
   eventManager.publishEvent(newEvent);
   ```

### Loop Prevention with Broadcast Routing

Broadcast routing requires additional loop prevention measures:

1. **Origin Checking**:
   ```java
   // Check if we created this event
   if (event.getMetadata().isCreatedBy(myId)) {
       return; // Prevent immediate feedback
   }
   ```

2. **Hop Count Limiting**:
   ```java
   // Limit broadcast propagation
   if (event.getMetadata().hasExceededMaxHops()) {
       return; // Stop propagation
   }
   ```

3. **Event History Tracking**:
   ```java
   // Track event history for broadcast events
   if (event.getMetadata().isInHistory(event.getMetadata().getEventId())) {
       return; // Prevent loops
   }
   ```

### Best Practices for Event Routing

1. **Default to Direct Routing**:
   - Use direct routing unless broadcast is specifically needed
   - Improves performance and reduces loop risk

2. **Use Broadcast Sparingly**:
   - Reserve broadcast for system-wide events
   - Consider using direct routing with multiple targets instead

3. **Combine with Other Prevention**:
   - Use direct routing with correlation IDs
   - Combine with hop count limits
   - Track event history regardless of routing mode

4. **Monitor Routing Patterns**:
   - Track direct vs broadcast event counts
   - Monitor event delivery paths
   - Watch for routing-related loops

## Common Loop Scenarios and Prevention

### 1. Bridge-HomekitAccessory Feedback Loop
**Scenario**: Bridge updates accessory, accessory updates bridge, creating a loop.

**Prevention**:
```java
// In bridge event handler
public void onEvent(HomekitEvent event) {
    // Check if event originated from this bridge
    if (event.getMetadata().isCreatedBy(myBridgeId)) {
        return; // Ignore self-created events
    }
    
    // Check if event is from a peer accessory
    if (event.getMetadata().isFromPeer()) {
        return; // Ignore peer events
    }
    
    // Process event and create new event with correlation
    String correlationId = event.getMetadata().getCorrelationId();
    if (correlationId == null) {
        correlationId = UUID.randomUUID().toString();
    }
    
    HomekitEvent newEvent = new HomekitCharacteristicEvent(
        HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
        myBridgeId,
        new HomekitEventMetadata(
            myBridgeId,
            correlationId,
            myBridgeId,
            myPeerIdentifiers
        )
    );
}
```

### 2. Multi-Hop Event Propagation
**Scenario**: Event propagates through multiple components, potentially creating loops.

**Prevention**:
```java
// In component event handler
public void onEvent(HomekitEvent event) {
    // Check hop count
    if (event.getMetadata().hasExceededMaxHops()) {
        logger.warn("Event exceeded max hops, dropping");
        return;
    }
    
    // Check event history
    if (event.getMetadata().isInHistory(event.getMetadata().getEventId())) {
        logger.warn("Event loop detected, dropping");
        return;
    }
    
    // Process event and forward with increased hop count
    HomekitEventMetadata newMetadata = new HomekitEventMetadata(event.getMetadata(), myComponentId);
    HomekitEvent newEvent = new HomekitCharacteristicEvent(
        event.getType(),
        myComponentId,
        newMetadata
    );
}
```

### 3. Subscription-Based Loops
**Scenario**: Components subscribe to each other's events, creating potential loops.

**Prevention**:
```java
// When adding subscriptions
public void addSubscription(HomekitEventType type, String publisher, String subscriber) {
    // Check for potential cycles in subscription graph
    if (eventGraph.hasCycle()) {
        logger.warn("Subscription would create cycle: {} -> {}", publisher, subscriber);
        // Optionally reject subscription or just warn
    }
    
    // Add subscription with peer awareness
    HomekitEventMetadata metadata = new HomekitEventMetadata(
        publisher,
        null,
        publisher,
        getPeerGroup(publisher, subscriber)
    );
    
    subscribe(type, publisher, subscriber, metadata);
}
```

## Best Practices

1. **Always Check Event Validity**:
   ```java
   if (!event.isValid()) {
       return; // Event is invalid (loop or max hops exceeded)
   }
   ```

2. **Use Correlation IDs for Related Events**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   ```

3. **Define Appropriate Peer Groups**:
   ```java
   Set<String> peers = Set.of("bridge:123", "accessory:456");
   HomekitEventMetadata metadata = new HomekitEventMetadata(publisher, null, publisher, peers);
   ```

4. **Monitor Event Metrics**:
   ```java
   Map<String, Integer> metrics = eventManager.getEventMetrics();
   logger.info("Dropped events due to loops: {}", metrics.get("droppedEventsDueToLoops"));
   logger.info("Dropped events due to hops: {}", metrics.get("droppedEventsDueToHops"));
   logger.info("Dropped events due to correlation: {}", metrics.get("droppedEventsDueToCorrelation"));
   ```

## Troubleshooting Event Loops

1. **Check Event History**:
   ```java
   String history = event.getMetadata().getEventHistoryAsString();
   logger.debug("Event history: {}", history);
   ```

2. **Check Publisher History**:
   ```java
   String publisherHistory = event.getMetadata().getPublisherHistoryAsString();
   logger.debug("Publisher history: {}", publisherHistory);
   ```

3. **Monitor Correlation IDs**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId != null) {
       logger.debug("Processing event with correlation ID: {}", correlationId);
   }
   ```

4. **Verify Peer Relationships**:
   ```java
   if (event.getMetadata().isFromPeer()) {
       logger.debug("Event from peer: {}", event.getMetadata().getImmediateOrigin());
   }
   ```

5. **Analyze Subscription Graph**:
   ```java
   if (eventGraph.hasCycle()) {
       String cyclePath = eventGraph.getCyclePath();
       logger.warn("Cycle detected in subscription graph: {}", cyclePath);
   }
   ```

## Correlation ID Management

Correlation IDs are a powerful mechanism for tracking related events and preventing loops. This section provides detailed information about correlation ID management.

### Correlation ID Lifecycle

1. **Creation**:
   ```java
   // When creating a root event (not in response to another event)
   String correlationId = UUID.randomUUID().toString();
   HomekitEventMetadata metadata = new HomekitEventMetadata(
       "bridge:123",
       correlationId,    // New correlation ID
       "bridge:123",
       Set.of()
   );

   // When creating a response event
   String correlationId = originalEvent.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   HomekitEventMetadata metadata = new HomekitEventMetadata(
       "accessory:456",
       correlationId,    // Reuse correlation ID
       "accessory:456",
       Set.of()
   );
   ```

2. **Propagation**:
   ```java
   // In event handler
   public void onEvent(HomekitEvent event) {
       // Get correlation ID from original event
       String correlationId = event.getMetadata().getCorrelationId();
       
       // Process event
       processEvent(event);
       
       // Create new event with same correlation ID
       HomekitEvent newEvent = new HomekitCharacteristicEvent(
           event.getType(),
           "bridge:123",
           new HomekitEventMetadata(
               "bridge:123",
               correlationId,    // Propagate correlation ID
               "bridge:123",
               Set.of()
           )
       );
   }
   ```

3. **Expiration**:
   ```java
   // Check if correlation ID has expired
   if (event.getMetadata().hasCorrelationExpired()) {
       logger.debug("Correlation ID expired, creating new one");
       String newCorrelationId = UUID.randomUUID().toString();
       // Create new event with new correlation ID
   }
   ```

### Correlation ID Tracking

1. **Component-Level Tracking**:
   ```java
   public class HomekitComponent {
       private final Set<String> processedCorrelationIds = new HashSet<>();
       
       public void onEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               if (processedCorrelationIds.contains(correlationId)) {
                   logger.debug("Already processed correlation ID: {}", correlationId);
                   return;
               }
               processedCorrelationIds.add(correlationId);
           }
           
           // Process event
       }
   }
   ```

2. **System-Level Tracking**:
   ```java
   public class HomekitEventManager {
       private final Map<String, Long> correlationTimestamps = new ConcurrentHashMap<>();
       
       public void publishEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               Long timestamp = correlationTimestamps.get(correlationId);
               if (timestamp != null && 
                   System.currentTimeMillis() - timestamp < HomekitEventMetadata.CORRELATION_ID_EXPIRY_MS) {
                   logger.debug("Event with correlation ID {} already processed", correlationId);
                   return;
               }
               correlationTimestamps.put(correlationId, System.currentTimeMillis());
           }
           
           // Publish event
       }
   }
   ```

### Advanced Correlation Patterns

1. **Correlation Chains**:
   ```java
   // Track correlation chain
   public class CorrelationChain {
       private final String rootCorrelationId;
       private final List<String> chain = new ArrayList<>();
       
       public CorrelationChain(String rootCorrelationId) {
           this.rootCorrelationId = rootCorrelationId;
           this.chain.add(rootCorrelationId);
       }
       
       public void addLink(String correlationId) {
           chain.add(correlationId);
       }
       
       public boolean isInChain(String correlationId) {
           return chain.contains(correlationId);
       }
   }
   ```

2. **Correlation Groups**:
   ```java
   // Group related events
   public class CorrelationGroup {
       private final String groupId;
       private final Set<String> correlationIds = new HashSet<>();
       
       public void addEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               correlationIds.add(correlationId);
           }
       }
       
       public boolean isGroupEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           return correlationId != null && correlationIds.contains(correlationId);
       }
   }
   ```

### Best Practices for Correlation IDs

1. **Always Check for Existing Correlation IDs**:
   ```java
   String correlationId = event.getMetadata().getCorrelationId();
   if (correlationId == null) {
       correlationId = UUID.randomUUID().toString();
   }
   ```

2. **Handle Correlation ID Expiration**:
   ```java
   if (event.getMetadata().hasCorrelationExpired()) {
       // Create new correlation ID or handle expired case
   }
   ```

3. **Track Correlation Metrics**:
   ```java
   public class CorrelationMetrics {
       private final AtomicInteger totalCorrelatedEvents = new AtomicInteger();
       private final AtomicInteger droppedCorrelatedEvents = new AtomicInteger();
       private final AtomicInteger expiredCorrelationIds = new AtomicInteger();
       
       public void logMetrics() {
           logger.info("Total correlated events: {}", totalCorrelatedEvents.get());
           logger.info("Dropped correlated events: {}", droppedCorrelatedEvents.get());
           logger.info("Expired correlation IDs: {}", expiredCorrelationIds.get());
       }
   }
   ```

4. **Use Correlation IDs for Debugging**:
   ```java
   public void debugEventFlow(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       if (correlationId != null) {
           logger.debug("Event flow for correlation ID {}: {}", correlationId, 
               event.getMetadata().getEventHistoryAsString());
       }
   }
   ```

### Common Correlation ID Scenarios

1. **Bridge-HomekitAccessory Communication**:
   ```java
   // Bridge sends event to accessory
   String correlationId = UUID.randomUUID().toString();
   HomekitEvent bridgeEvent = new HomekitCharacteristicEvent(
       HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
       "bridge:123",
       new HomekitEventMetadata(
           "bridge:123",
           correlationId,
           "bridge:123",
           Set.of()
       )
   );
   
   // HomekitAccessory processes event and responds
   public void onEvent(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       // Process event
       HomekitEvent response = new HomekitCharacteristicEvent(
           event.getType(),
           "accessory:456",
           new HomekitEventMetadata(
               "accessory:456",
               correlationId,    // Use same correlation ID
               "accessory:456",
               Set.of()
           )
       );
   }
   ```

2. **Multi-Step Event Processing**:
   ```java
   // Initial event
   String correlationId = UUID.randomUUID().toString();
   HomekitEvent initialEvent = new HomekitCharacteristicEvent(
       HomekitEventType.CHARACTERISTIC_STATE_CHANGED,
       "bridge:123",
       new HomekitEventMetadata(
           "bridge:123",
           correlationId,
           "bridge:123",
           Set.of()
       )
   );
   
   // Each step in processing
   public void processStep(HomekitEvent event) {
       String correlationId = event.getMetadata().getCorrelationId();
       // Process step
       HomekitEvent nextStep = new HomekitCharacteristicEvent(
           event.getType(),
           "processor:789",
           new HomekitEventMetadata(
               "processor:789",
               correlationId,    // Maintain correlation ID
               "processor:789",
               Set.of()
           )
       );
   }
   ```

3. **Event Aggregation**:
   ```java
   // Aggregate related events
   public class EventAggregator {
       private final Map<String, List<HomekitEvent>> correlatedEvents = new HashMap<>();
       
       public void addEvent(HomekitEvent event) {
           String correlationId = event.getMetadata().getCorrelationId();
           if (correlationId != null) {
               correlatedEvents.computeIfAbsent(correlationId, k -> new ArrayList<>())
                   .add(event);
           }
       }
       
       public List<HomekitEvent> getCorrelatedEvents(String correlationId) {
           return correlatedEvents.getOrDefault(correlationId, List.of());
       }
   }
   ``` 
