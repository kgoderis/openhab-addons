package org.openhab.io.homekit.internal.notification;

import java.util.Collection;
import java.util.HashMap;

import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationProvider;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedNotificationProvider} is an OSGi service, that allows to add or remove
 * notifications at runtime by calling {@link ManagedNotificationProvider#addAccessory(Notification)} or
 * {@link ManagedNotificationProvider#removeAccessory(Notification)}
 * . An added HomekitNotification is automatically
 * exposed to the {@link NotificationRegistry}.
 *
 **/
@Component(immediate = true, service = { ManagedNotificationProvider.class, NotificationProvider.class })
public class ManagedNotificationProvider extends AbstractProvider<Notification>
        implements ManagedProvider<Notification, NotificationUID>, NotificationProvider {

    private HashMap<String, Notification> storage = new HashMap<String, Notification>();

    protected static final Logger logger = LoggerFactory.getLogger(ManagedNotificationProvider.class);

    // TODO 6.8.2.2 Event Policy

    @Override
    public void add(Notification element) {
        if (storage.get(element.getUID().toString()) != null) {
            throw new IllegalArgumentException("Cannot add element, because an element with same UID ("
                    + element.getUID().toString() + ") already exists.");
        }

        storage.put(element.getUID().toString(), element);
        notifyListenersAboutAddedElement(element);
    }

    @Override
    public Collection<Notification> getAll() {
        return storage.values();
    }

    @Override
    public Notification get(NotificationUID key) {
        if (key == null) {
            throw new IllegalArgumentException("Cannot get null element");
        }

        return storage.get(key.toString());
    }

    @Override
    public Notification remove(NotificationUID key) {
        Notification element = storage.remove(key.toString());
        if (element != null) {
            notifyListenersAboutRemovedElement(element);
            logger.debug("Removed element {} from {}.", key.toString(), this.getClass().getSimpleName());
            return element;
        }

        return null;
    }

    @Override
    public Notification update(Notification element) {
        if (storage.get(element.getUID().toString()) != null) {
            Notification oldElement = storage.put(element.getUID().toString(), element);
            if (oldElement != null) {
                notifyListenersAboutUpdatedElement(oldElement, element);
                logger.debug("Updated element {} in {}.", element.getUID().toString(), this.getClass().getSimpleName());
                return oldElement;
            }
        } else {
            logger.warn("Could not update element with key {} in {}, because it does not exists.",
                    element.getUID().toString(), this.getClass().getSimpleName());
        }

        return null;
    }
}
