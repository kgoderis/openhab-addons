package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;

/**
 * Listener interface for HomeKit service events.
 * <p>
 * This interface defines the contract for components that need to be notified of changes
 * in HomeKit services, including service events and state changes.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 * <li>Service event notifications</li>
 * <li>State change tracking</li>
 * <li>Event handling</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 * <li>Event-based notification system</li>
 * <li>Thread-safe event handling</li>
 * <li>Service state tracking</li>
 * <li>Event propagation</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.event.model.service.HomekitServiceEvent} for service events</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitServiceChangeListener {
    void onServiceEvent(HomekitServiceEvent serviceEvent);
}
