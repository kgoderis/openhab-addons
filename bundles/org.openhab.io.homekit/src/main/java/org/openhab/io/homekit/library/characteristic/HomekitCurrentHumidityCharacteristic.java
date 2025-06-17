// /*
//  * Copyright (c) 2010-2025 Contributors to the openHAB project
//  *
//  * See the NOTICE file(s) distributed with this work for additional
//  * information.
//  *
//  * This program and the accompanying materials are made available under the
//  * terms of the Eclipse Public License 2.0 which is available at
//  * http://www.eclipse.org/legal/epl-2.0
//  *
//  * SPDX-License-Identifier: EPL-2.0
//  */

// package org.openhab.io.homekit.library.characteristic;

// import javax.json.JsonValue;

// import org.eclipse.jdt.annotation.NonNullByDefault;
// import org.eclipse.jdt.annotation.Nullable;
// import org.openhab.io.homekit.api.characteristic.HomekitCharacteristicType;
// import org.openhab.io.homekit.api.service.HomekitService;
// import org.openhab.io.homekit.core.characteristic.HomekitFloatCharacteristic;
// import org.openhab.io.homekit.event.manager.HomekitEventManager;

// /**
//  * HomeKit Current Humidity Characteristic.
//  * This characteristic represents the current relative humidity level as a percentage between 0 and 100.
//  *
//  * @see <a href=\"https://developer.apple.com/documentation/HomeKit\">HAP Specification</a>
//  * @author Karel Goderis - Initial contribution
//  */
// @HomekitCharacteristicType(type = "00000010-0000-1000-8000-0026BB765291", name = "Current Relative Humidity", tag = "currentRelativeHumidity", acceptedItemTypes = {
//         "Number" })
// @NonNullByDefault
// public class HomekitCurrentHumidityCharacteristic extends HomekitFloatCharacteristic {

//     public HomekitCurrentHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
//             long instanceId) {
//         super(service, eventManager, 0.0, 100.0, 1.0, "percentage");
//         withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
//                 .withDescription("Current Relative Humidity");
//     }

//     public HomekitCurrentHumidityCharacteristic(HomekitService service, HomekitEventManager eventManager,
//             JsonValue value) {
//         super(service, eventManager, value);
//     }

//     /**
//      * Checks if the given value is a valid humidity percentage.
//      *
//      * @param value the value to check
//      * @return true if the value is between 0 and 100, false otherwise
//      */
//     @Override
//     public boolean isAllowedValue(@Nullable Double value) {
//         if (value == null) {
//             return false;
//         }
//         return value >= 0.0 && value <= 100.0;
//     }
// }
