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
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.io.homekit.v1.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.v1.internal.HomekitTaggedItem;

import  org.openhab.io.homekit.hap.HomekitCharacteristicChangeCallback;
import  org.openhab.io.homekit.hap.accessories.Switch;

/**
 * Implements Switch using an Item that provides an On/Off state.
 *
 * @author Andy Lintner - Initial contribution
 */
public class HomekitSwitchImpl extends AbstractHomekitAccessoryImpl<SwitchItem> implements Switch {

    public HomekitSwitchImpl(HomekitTaggedItem taggedItem, ItemRegistry itemRegistry, HomekitAccessoryUpdater updater) {
        super(taggedItem, itemRegistry, updater, SwitchItem.class);
    }

    @Override
    public CompletableFuture<Boolean> getSwitchState() {
        OnOffType state = getItem().getStateAs(OnOffType.class);
        return CompletableFuture.completedFuture(state == OnOffType.ON);
    }

    @Override
    public CompletableFuture<Void> setSwitchState(boolean state) throws Exception {
        GenericItem item = getItem();
        if (item instanceof SwitchItem) {
            ((SwitchItem) item).send(state ? OnOffType.ON : OnOffType.OFF);
        } else if (item instanceof GroupItem) {
            ((GroupItem) item).send(state ? OnOffType.ON : OnOffType.OFF);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeSwitchState(HomekitCharacteristicChangeCallback callback) {
        getUpdater().subscribe(getItem(), callback);
    }

    @Override
    public void unsubscribeSwitchState() {
        getUpdater().unsubscribe(getItem());
    }
}
