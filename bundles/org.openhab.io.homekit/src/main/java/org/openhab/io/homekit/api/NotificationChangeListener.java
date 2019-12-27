package org.openhab.io.homekit.api;

import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * {@link NotificationChangeListener} can be implemented to listen for HomekitNotification being added or removed. The
 * listener must be added and removed via
 * {@link NotificationRegistry#addRegistryChangeListener(NotificationChangeListener)} and
 * {@link NotificationRegistry#removeRegistryChangeListener(NotificationChangeListener)}.
 *
 * @author Karel Goderis - Initial contribution
 *
 * @see NotificationRegistry
 */
public interface NotificationChangeListener extends RegistryChangeListener<Notification> {

}
