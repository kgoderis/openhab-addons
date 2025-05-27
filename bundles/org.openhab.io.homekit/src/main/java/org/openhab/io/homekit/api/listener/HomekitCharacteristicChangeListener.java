package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent;

/**
 * Listener interface for HomeKit characteristic events.
 * <p>
 * This interface defines the contract for components that need to be notified of changes
 * in HomeKit characteristics, including characteristic events and state changes.
 * </p>
 * <p>
 * The interface provides:
 * <ul>
 *   <li>Characteristic event notifications</li>
 *   <li>State change tracking</li>
 *   <li>Event handling</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 *   <li>Event-based notification system</li>
 *   <li>Thread-safe event handling</li>
 *   <li>Characteristic state tracking</li>
 *   <li>Event propagation</li>
 * </ul>
 * </p>
 * <p>
 * The interface integrates with:
 * <ul>
 *   <li>{@link org.openhab.io.homekit.event.model.characteristic.HomekitCharacteristicEvent} for characteristic events</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public interface HomekitCharacteristicChangeListener {
    void onCharacteristicEvent(HomekitCharacteristicEvent event);
}
