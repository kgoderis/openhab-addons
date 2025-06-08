/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.io.homekit.util;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A high-performance debouncer implementation for HomeKit integration that provides efficient
 * rate limiting and event coalescing capabilities.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Lock-free synchronization using atomic operations</li>
 * <li>Minimal memory allocation during operation</li>
 * <li>Configurable debounce delay</li>
 * <li>Thread-safe operation</li>
 * <li>Call counting and folding</li>
 * </ul>
 *
 * <p>
 * Integration points:
 * <ul>
 * <li>HomeKit event handling</li>
 * <li>State change notifications</li>
 * <li>Rate-limited operations</li>
 * </ul>
 *
 * <p>
 * Implementation details:
 * <ul>
 * <li>Uses AtomicBoolean for thread-safe state transitions</li>
 * <li>Employs AtomicInteger for call counting</li>
 * <li>Leverages ScheduledExecutorService for timing</li>
 * <li>Supports configurable clock source for testing</li>
 * </ul>
 *
 * @author Tim Harper - Initial contribution
 * @version 1.0
 */
@NonNullByDefault
public class HomekitDebouncer {

    private static final Logger logger = LoggerFactory.getLogger(HomekitDebouncer.class);
    private static final String LOG_PREFIX = "Homekit Debouncer: ";
    private static final String LOG_INIT = LOG_PREFIX + "Init - ";
    private static final String LOG_ACTION = LOG_PREFIX + "Action - ";
    private static final String LOG_ERROR = LOG_PREFIX + "Error - ";

    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final Long delayMs;
    private final Runnable action;
    private final AtomicBoolean pending = new AtomicBoolean(false);
    private final AtomicInteger calls = new AtomicInteger(0);
    private final String name;

    private volatile Long lastCallAttempt;

    /**
     * Creates a new HomekitDebouncer with the specified configuration.
     *
     * <p>
     * Note: Debounced calls are filtered synchronously, in the caller thread, without the need for locks,
     * context switches, or heap allocations. We use AtomicBoolean to resolve concurrent races; the probability
     * of contending on an AtomicBoolean transition is very low.
     *
     * @param name The name of this debouncer for logging and identification
     * @param scheduler The scheduler implementation to use for timing operations
     * @param delay The time after which to invoke action; each time {@link #call()} is invoked, this delay is reset
     * @param clock The source from which we get the current time. This input should use the same source.
     *            Specified for testing purposes
     * @param action The action to invoke after the debounce period
     * @throws IllegalArgumentException if any parameter is null
     */
    public HomekitDebouncer(String name, ScheduledExecutorService scheduler, Duration delay, Clock clock,
            Runnable action) {
        if (name == null || scheduler == null || delay == null || clock == null || action == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }

        logger.trace("{}Creating debouncer '{}' with delay {}ms", LOG_INIT, name, delay.toMillis());

        this.name = name;
        this.scheduler = scheduler;
        this.action = action;
        this.delayMs = delay.toMillis();
        this.clock = clock;
        this.lastCallAttempt = clock.millis();
    }

    /**
     * Registers that the provided action should be called according to the debounce logic.
     * This method is thread-safe and can be called from multiple threads concurrently.
     */
    public void call() {
        lastCallAttempt = clock.millis();
        int currentCalls = calls.incrementAndGet();
        logger.trace("{}Call registered for '{}' (total calls: {})", LOG_ACTION, name, currentCalls);

        if (pending.compareAndSet(false, true)) {
            logger.trace("{}Scheduling action for '{}' in {}ms", LOG_ACTION, name, delayMs);
            scheduler.schedule(this::tryActionOrPostpone, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Attempts to execute the action if the debounce delay has been surpassed,
     * otherwise reschedules the attempt.
     */
    private void tryActionOrPostpone() {
        long now = clock.millis();
        boolean delaySurpassed = ((now - lastCallAttempt) >= delayMs);

        if (delaySurpassed) {
            if (pending.compareAndSet(true, false)) {
                int foldedCalls = calls.getAndSet(0);
                logger.debug("{}Action '{}' invoked after delay {}ms ({} calls folded)", LOG_ACTION, name, delayMs,
                        foldedCalls);
                try {
                    action.run();
                } catch (Exception e) {
                    logger.warn("{}Action '{}' resulted in error: {}", LOG_ERROR, name, e.getMessage());
                }
            } else {
                logger.warn("{}Invalid state detected in debouncer '{}'", LOG_ERROR, name);
            }
        } else {
            long delay = Math.max(1, lastCallAttempt - now + delayMs);
            logger.trace("{}Rescheduling action '{}' in {}ms", LOG_ACTION, name, delay);
            scheduler.schedule(this::tryActionOrPostpone, delay, TimeUnit.MILLISECONDS);
        }
    }
}
