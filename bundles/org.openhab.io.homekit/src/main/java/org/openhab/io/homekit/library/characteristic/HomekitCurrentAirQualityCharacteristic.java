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
// import org.openhab.io.homekit.core.characteristic.HomekitEnumCharacteristic;
// import org.openhab.io.homekit.event.manager.HomekitEventManager;

// /**
//  * HomeKit Current Air Quality Characteristic.
//  * This characteristic represents the current air quality level.
//  *
//  * @see <a href="https://developers.homebridge.io/#/characteristic/AirQuality">HomeKit Documentation</a>
//  * @author Karel Goderis - Initial contribution
//  */
// @HomekitCharacteristicType(type = "00000095-0000-1000-8000-0026BB765291", name = "Air Quality", tag = "airQuality", acceptedItemTypes = {
//         "Number", "String" })
// @NonNullByDefault
// public class HomekitCurrentAirQualityCharacteristic extends HomekitEnumCharacteristic {
//     /**
//      * Enum representing the possible air quality levels.
//      * Each level has a corresponding integer code used in the HomeKit protocol.
//      */
//     public enum AirQuality {
//         /** Air quality level is unknown */
//         UNKNOWN(0),
//         /** Air quality is excellent */
//         EXCELLENT(1),
//         /** Air quality is good */
//         GOOD(2),
//         /** Air quality is fair */
//         FAIR(3),
//         /** Air quality is inferior */
//         INFERIOR(4),
//         /** Air quality is poor */
//         POOR(5);

//         private final int code;

//         AirQuality(int code) {
//             this.code = code;
//         }

//         /**
//          * Gets the integer code for this air quality level.
//          *
//          * @return the integer code
//          */
//         public int getCode() {
//             return code;
//         }

//         /**
//          * Converts an integer code to the corresponding AirQuality.
//          * If no matching level is found, returns UNKNOWN as default.
//          *
//          * @param code the integer code to convert
//          * @return the corresponding AirQuality, or UNKNOWN if not found
//          */
//         public static AirQuality fromCode(int code) {
//             for (AirQuality s : values()) {
//                 if (s.code == code) {
//                     return s;
//                 }
//             }
//             return UNKNOWN;
//         }
//     }

//     /**
//      * Creates a new Current Air Quality characteristic.
//      *
//      * @param service The HomeKit service this characteristic belongs to
//      * @param eventManager The event manager for handling HomeKit events
//      * @param instanceId The instance ID for this characteristic
//      */
//     public HomekitCurrentAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
//             long instanceId) {
//         super(service, eventManager, AirQuality.values().length);
//         withInstanceId(instanceId).withPairedWrite(false).withPairedRead(true).withEvents(true)
//                 .withDescription("Current Air Quality");
//     }

//     /**
//      * Creates a new Current Air Quality characteristic from a JSON value.
//      *
//      * @param service The HomeKit service this characteristic belongs to
//      * @param eventManager The event manager for handling HomeKit events
//      * @param value The JSON value to initialize the characteristic with
//      */
//     public HomekitCurrentAirQualityCharacteristic(HomekitService service, HomekitEventManager eventManager,
//             JsonValue value) {
//         super(service, eventManager, value);
//     }

//     /**
//      * Checks if the given value is a valid air quality level.
//      *
//      * @param value the integer value to check
//      * @return true if the value corresponds to a valid air quality level, false otherwise
//      */
//     @Override
//     public boolean isAllowedValue(@Nullable Integer value) {
//         return value != null && value >= 0 && value < AirQuality.values().length;
//     }

//     /**
//      * Gets the set of all valid air quality level values.
//      *
//      * @return a set containing all valid air quality level codes
//      */
//     @Override
//     public java.util.Set<Integer> getAllowedValues() {
//         return java.util.Set.of(AirQuality.UNKNOWN.getCode(), AirQuality.EXCELLENT.getCode(), AirQuality.GOOD.getCode(),
//                 AirQuality.FAIR.getCode(), AirQuality.INFERIOR.getCode(), AirQuality.POOR.getCode());
//     }
// }
