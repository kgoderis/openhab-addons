/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal.handler;

import org.openhab.binding.atsadvanced.internal.StatObjectType;

public interface ObjectStatusListener {
    void onObjectStateChanged(StatObjectType type, int objectNumber);
}
