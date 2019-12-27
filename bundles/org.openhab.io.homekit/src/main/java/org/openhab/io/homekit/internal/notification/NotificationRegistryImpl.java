package org.openhab.io.homekit.internal.notification;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.io.homekit.api.Notification;
import org.openhab.io.homekit.api.NotificationProvider;
import org.openhab.io.homekit.api.NotificationRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Stores the created HomekitNotification
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = NotificationRegistry.class)
public class NotificationRegistryImpl extends AbstractRegistry<Notification, NotificationUID, NotificationProvider>
        implements NotificationRegistry {

    public NotificationRegistryImpl() {
        super(NotificationProvider.class);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedNotificationProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedNotificationProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    @Activate
    protected void activate(final BundleContext context) {
        super.activate(context);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

}
