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
package org.openhab.io.homekit.internal.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.HomekitUID;

/**
 * {@link HomekitAccessoryServerUID} represents a unique identifier for accessory servers.
 * The UID format is: homekit:server:{pairingId}
 *
 * @author Your Name - Initial contribution
 */
@NonNullByDefault
public class HomekitAccessoryServerUID extends HomekitUID {

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    HomekitAccessoryServerUID() {
        super("server", "homekit:server:");
    }

    /**
     * Instantiates a new accessory server UID.
     *
     * @param pairingId the pairing ID of the accessory server
     */
    public HomekitAccessoryServerUID(String pairingId) {
        super("server", "homekit:server:" + pairingId);
    }

    /**
     * Gets the pairing ID.
     *
     * @return the pairing ID
     */
    public String getPairingId() {
        return getSegment(2);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:server:pairingId
    }
}
