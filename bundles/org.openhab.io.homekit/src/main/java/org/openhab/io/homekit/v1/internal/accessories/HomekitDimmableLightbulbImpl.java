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

import java.util.concurrent.CompletableFuture;

import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.v1.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.v1.internal.HomekitTaggedItem;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.DimmableLightbulb;

/**
 * Implements DimmableLightBulb using an Item that provides a On/Off and Percent state.
 *
 * @author Andy Lintner - Initial contribution
 */
class HomekitDimmableLightbulbImpl extends AbstractHomekitLightbulbImpl<DimmerItem> implements DimmableLightbulb {

    public HomekitDimmableLightbulbImpl(HomekitTaggedItem taggedItem, ItemRegistry itemRegistry,
            HomekitAccessoryUpdater updater) {
        super(taggedItem, itemRegistry, updater, DimmerItem.class);
    }

    @Override
    public CompletableFuture<Integer> getBrightness() {
        State state = getItem().getStateAs(PercentType.class);
        if (state instanceof PercentType) {
            PercentType brightness = (PercentType) state;
            return CompletableFuture.completedFuture(brightness.intValue());
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> setBrightness(Integer value) throws Exception {
        GenericItem item = getItem();
        if (item instanceof DimmerItem) {
            ((DimmerItem) item).send(new PercentType(value));
        } else if (item instanceof GroupItem) {
            ((GroupItem) item).send(new PercentType(value));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeBrightness(HomekitCharacteristicChangeCallback callback) {
        getUpdater().subscribe(getItem(), "brightness", callback);
    }

    @Override
    public void unsubscribeBrightness() {
        getUpdater().unsubscribe(getItem(), "brightness");
    }
}
