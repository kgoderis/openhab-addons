/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.io.homekit.v1.internal.accessories;

import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.io.homekit.v1.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.v1.internal.HomekitTaggedItem;

/**
 * Implements Lightbulb using an Item that provides an On/Off state
 *
 * @author Andy Lintner - Initial contribution
 */
class HomekitLightbulbImpl extends AbstractHomekitLightbulbImpl<SwitchItem> {

    public HomekitLightbulbImpl(HomekitTaggedItem taggedItem, ItemRegistry itemRegistry,
            HomekitAccessoryUpdater updater) {
        super(taggedItem, itemRegistry, updater, SwitchItem.class);
    }
}
