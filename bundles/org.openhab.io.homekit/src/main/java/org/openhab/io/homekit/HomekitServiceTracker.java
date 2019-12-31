package org.openhab.io.homekit;

import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class HomekitServiceTracker<T> implements AutoCloseable, Supplier<T> {

    private final ServiceTracker<T, T> serviceTracker;
    private boolean closed = true;

    private HomekitServiceTracker(Class<T> target, Class<?> source) {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        Bundle bundle = FrameworkUtil.getBundle(source);
        BundleContext context = bundle == null ? null : bundle.getBundleContext();
        if (context == null) {
            throw new IllegalArgumentException("Unable to acquire bundle context for " + source.getCanonicalName());
        }
        this.serviceTracker = new ServiceTracker<T, T>(context, target, null);
    }

    public static <T> HomekitServiceTracker<T> supply(Class<T> target, Class<?> source) {
        return new HomekitServiceTracker<>(target, source);
    }

    @Override
    public T get() {
        if (closed) {
            serviceTracker.open();
            closed = false;
        }
        return serviceTracker.getService();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() throws Exception {
        if (serviceTracker != null && !closed) {
            serviceTracker.close();
        }
    }

}
