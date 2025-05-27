package org.openhab.io.homekit.util;

import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for tracking OSGi services in the HomeKit integration.
 *
 * <p>
 * This class provides a convenient way to track and access OSGi services within
 * the HomeKit integration. It implements both {@link AutoCloseable} and {@link Supplier}
 * interfaces to provide automatic resource management and service access.
 * </p>
 *
 * <p>
 * The class integrates with several key components:
 * </p>
 * <ul>
 *   <li>{@link org.osgi.util.tracker.ServiceTracker} for OSGi service tracking</li>
 *   <li>{@link org.osgi.framework.BundleContext} for service registration</li>
 *   <li>{@link java.util.function.Supplier} for service access</li>
 *   <li>{@link java.lang.AutoCloseable} for resource management</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li>Automatic service tracking and lifecycle management</li>
 *   <li>Type-safe service access</li>
 *   <li>Resource cleanup through AutoCloseable</li>
 *   <li>Bundle context validation</li>
 *   <li>Null safety checks</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
public class HomekitServiceTracker<T> implements AutoCloseable, Supplier<T> {
    // ========== Log Message Prefixes ==========
    protected static final String LOG_PREFIX = "Homekit ServiceTracker: ";
    protected static final String LOG_INIT = LOG_PREFIX + "Init - ";
    protected static final String LOG_SERVICE = LOG_PREFIX + "Service - ";
    protected static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private static final Logger logger = LoggerFactory.getLogger(HomekitServiceTracker.class);
    private final ServiceTracker<T, T> serviceTracker;
    private boolean closed = true;

    /**
     * Creates a new service tracker for the specified service type.
     *
     * <p>
     * This constructor initializes a service tracker for the given service type
     * and source class. It validates the bundle context and sets up service tracking.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Validates input parameters</li>
     *   <li>Acquires bundle context</li>
     *   <li>Initializes service tracker</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param target The service type to track
     * @param source The source class for bundle context
     * @throws IllegalArgumentException if target or source is null, or if bundle context cannot be acquired
     */
    private HomekitServiceTracker(Class<T> target, Class<?> source) {
        if (target == null) {
            logger.error("{}Target class cannot be null", LOG_ERROR);
            throw new IllegalArgumentException("Target cannot be null");
        }
        if (source == null) {
            logger.error("{}Source class cannot be null", LOG_ERROR);
            throw new IllegalArgumentException("Source cannot be null");
        }
        Bundle bundle = FrameworkUtil.getBundle(source);
        BundleContext context = bundle == null ? null : bundle.getBundleContext();
        if (context == null) {
            logger.error("{}Unable to acquire bundle context for {}", LOG_ERROR, source.getCanonicalName());
            throw new IllegalArgumentException("Unable to acquire bundle context for " + source.getCanonicalName());
        }
        this.serviceTracker = new ServiceTracker<T, T>(context, target, null);
        logger.trace("{}Created service tracker for {} in bundle {}", LOG_INIT, target.getSimpleName(), bundle.getSymbolicName());
    }

    /**
     * Creates a new service tracker for the specified service type.
     *
     * <p>
     * This factory method provides a convenient way to create service trackers
     * with proper type inference.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Type-safe creation</li>
     *   <li>Parameter validation</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @param <T> The service type to track
     * @param target The service class to track
     * @param source The source class for bundle context
     * @return A new service tracker instance
     * @throws IllegalArgumentException if target or source is null
     */
    public static <T> HomekitServiceTracker<T> supply(Class<T> target, Class<?> source) {
        logger.trace("{}Creating service tracker for {}", LOG_INIT, target.getSimpleName());
        return new HomekitServiceTracker<>(target, source);
    }

    /**
     * Gets the tracked service instance.
     *
     * <p>
     * This method ensures the service tracker is open and returns the current
     * service instance. If the tracker is closed, it will be opened automatically.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Auto-opens tracker if closed</li>
     *   <li>Returns current service</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @return The current service instance, or null if not available
     */
    @Override
    public T get() {
        if (closed) {
            logger.trace("{}Opening service tracker", LOG_SERVICE);
            serviceTracker.open();
            closed = false;
        }
        T service = serviceTracker.getService();
        logger.trace("{}Retrieved service: {}", LOG_SERVICE, service != null ? service.getClass().getSimpleName() : "null");
        return service;
    }

    /**
     * Ensures proper cleanup of resources.
     *
     * <p>
     * This method is called by the garbage collector before the object is
     * collected. It ensures that the service tracker is properly closed.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Calls close() method</li>
     *   <li>Ensures resource cleanup</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @throws Throwable if an error occurs during cleanup
     */
    @Override
    protected void finalize() throws Throwable {
        logger.trace("{}Finalizing service tracker", LOG_SERVICE);
        close();
        super.finalize();
    }

    /**
     * Closes the service tracker and releases resources.
     *
     * <p>
     * This method ensures that the service tracker is properly closed and
     * all resources are released. It can be called multiple times safely.
     * </p>
     *
     * <p>
     * Key implementation details:
     * </p>
     * <ul>
     *   <li>Closes service tracker</li>
     *   <li>Updates closed state</li>
     *   <li>Provides trace-level logging</li>
     * </ul>
     *
     * @throws Exception if an error occurs during closure
     */
    @Override
    public void close() throws Exception {
        if (serviceTracker != null && !closed) {
            logger.trace("{}Closing service tracker", LOG_SERVICE);
            serviceTracker.close();
            closed = true;
        }
    }
}
