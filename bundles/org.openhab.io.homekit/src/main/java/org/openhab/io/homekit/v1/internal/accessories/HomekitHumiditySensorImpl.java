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

import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.io.homekit.v1.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.v1.internal.HomekitTaggedItem;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.HumiditySensor;

/**
 *
 * @author Andy Lintner - Initial contribution
 */
public class HomekitHumiditySensorImpl extends AbstractHomekitAccessoryImpl<NumberItem> implements HumiditySensor {

    public HomekitHumiditySensorImpl(HomekitTaggedItem taggedItem, ItemRegistry itemRegistry,
            HomekitAccessoryUpdater updater) {
        super(taggedItem, itemRegistry, updater, NumberItem.class);
    }

    @Override
    public CompletableFuture<Double> getCurrentRelativeHumidity() {
        DecimalType state = getItem().getStateAs(DecimalType.class);
        if (state == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(state.doubleValue());
    }

    @Override
    public void subscribeCurrentRelativeHumidity(HomekitCharacteristicChangeCallback callback) {
        getUpdater().subscribe(getItem(), callback);
    }

    @Override
    public void unsubscribeCurrentRelativeHumidity() {
        getUpdater().unsubscribe(getItem());
    }
}
