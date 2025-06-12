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

package org.openhab.io.homekit.library.characteristic;

import javax.json.JsonValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.core.characteristic.HomekitStringCharacteristic;
import org.openhab.io.homekit.event.manager.HomekitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HomeKit Version Characteristic.
 * This characteristic represents the version string for the accessory or service.
 *
 * @author Karel Goderis - Initial Contribution
 * @see <a href="https://developer.apple.com/documentation/HomeKit">HAP Specification</a>
 */
@HomekitCharacteristicType(type = "00000037-0000-1000-8000-0026BB765291", name = "Version", tag = "version", acceptedItemTypes = {
        "String", "Text" })
@NonNullByDefault
public class HomekitVersionCharacteristic extends HomekitStringCharacteristic {
    private static final Logger logger = LoggerFactory.getLogger(HomekitVersionCharacteristic.class);

    public HomekitVersionCharacteristic(HomekitService service, HomekitEventManager eventManager, long instanceId) {
        super(service, eventManager);
        withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(false)
                .withDescription("Version");
        try {
            setValueInternal("1.0.0");
        } catch (Exception e) {
            // This should never happen since we're using setValueInternal
            logger.error("Failed to set initial version value", e);
            throw new IllegalStateException("Failed to initialize version characteristic", e);
        }
    }

    public HomekitVersionCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
    }

    @Override
    public String getValue() {
        return OpenHAB.getVersion();
    }

    public void setVersion(String version) {
        try {
            setValueInternal(version);
        } catch (Exception e) {
            // This should never happen since we're using setValueInternal
            logger.error("Failed to set version value: {}", version, e);
            throw new IllegalStateException("Failed to set version value", e);
        }
    }
}
