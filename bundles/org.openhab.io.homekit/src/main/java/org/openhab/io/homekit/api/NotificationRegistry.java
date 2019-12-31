package org.openhab.io.homekit.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;
import org.openhab.io.homekit.internal.notification.NotificationUID;

/**
 * {@link NotificationRegistry} tracks all {@link Notification}s from different {@link NotificationProvider}s and
 * provides access to them. The {@link NotificationRegistry} supports adding of listeners (see
 * {@link HomekitNotificationhangeListener})
 *
 * @author Karel Goderis - Initial contribution
 */

@NonNullByDefault
public interface NotificationRegistry extends Registry<Notification, NotificationUID> {

}
