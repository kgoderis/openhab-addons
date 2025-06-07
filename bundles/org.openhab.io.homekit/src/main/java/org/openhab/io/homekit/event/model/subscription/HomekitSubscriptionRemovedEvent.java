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

package org.openhab.io.homekit.event.model.subscription;

import java.util.Collections;

import org.openhab.core.thing.UID;
import org.openhab.io.homekit.api.event.HomekitEventType;
import org.openhab.io.homekit.event.core.AbstractHomekitEvent;
import org.openhab.io.homekit.event.core.HomekitEventMetadata;
import org.openhab.io.homekit.event.core.HomekitEventSubscription;

/**
 * Event class representing the removal of a HomeKit subscription.
 * This event is fired when a client unsubscribes from updates of a HomeKit characteristic.
 *
 * <p>
 * The class integrates with:
 * </p>
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEvent} for base event functionality</li>
 * <li>{@link org.openhab.io.homekit.api.event.HomekitEventType} for event type identification</li>
 * <li>{@link org.openhab.core.thing.UID} for component identification</li>
 * <li>{@link org.openhab.io.homekit.event.core.HomekitEventSubscription} for subscription details</li>
 * </ul>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Subscription lifecycle management</li>
 * <li>Publisher identification for cleanup</li>
 * <li>Timestamp tracking for state management</li>
 * <li>Wildcard subscriber support</li>
 * </ul>
 *
 * <p>
 * <b>Usage Patterns:</b>
 * </p>
 * <ul>
 * <li>Cleanup of subscription resources</li>
 * <li>State management in publishers</li>
 * <li>Subscription tracking and monitoring</li>
 * <li>System diagnostics and logging</li>
 * </ul>
 *
 * <p>
 * <b>Lifecycle Management:</b>
 * </p>
 * <ul>
 * <li>Triggers cleanup of subscription resources</li>
 * <li>Updates publisher state</li>
 * <li>Maintains subscription history</li>
 * <li>Supports system diagnostics</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 3.x
 */
public class HomekitSubscriptionRemovedEvent extends AbstractHomekitEvent {
    private final HomekitEventSubscription subscription;

    /**
     * Creates a new subscription removed event.
     *
     * @param subscription the subscription being removed
     */
    public HomekitSubscriptionRemovedEvent(HomekitEventSubscription subscription) {
        super(HomekitEventType.SUBSCRIPTION_REMOVED, subscription.getPublisherUID(), WILDCARD_UID,
                new HomekitEventMetadata(subscription.getPublisherUID(), null, null, Collections.emptySet()));
        this.subscription = subscription;
    }

    /**
     * Returns the subscription being removed.
     *
     * @return the subscription
     */
    public HomekitEventSubscription getSubscription() {
        return subscription;
    }

    @Override
    public UID getPublisherUID() {
        return subscription.getPublisherUID();
    }

    @Override
    public String toString() {
        return "HomekitSubscriptionRemovedEvent{" + "subscription=" + subscription + ", type=" + getType()
                + ", timestamp=" + getTimestamp() + '}';
    }
}
