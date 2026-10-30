/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal.handler;

public interface RasStatusListener {
    void onRasStateChanged(int rasNumber);
}
