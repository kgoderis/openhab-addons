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
package org.openhab.io.homekit.internal.client;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link HomekitBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class HomekitBindingConstants {

    public static final String BINDING_ID = "homekit";

    public static final String DEVICE_ID = "id";
    public static final String CONFIGURATION_NUMBER_SHARP = "c#";

    public static final String CONFIGURATION_URI = "binding:homekit:bridge";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_ACCESSORY = new ThingTypeUID(BINDING_ID, "accessory");
    public static final ThingTypeUID THING_TYPE_STANDALONE_ACCESSORY = new ThingTypeUID(BINDING_ID, "standalone");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
}
