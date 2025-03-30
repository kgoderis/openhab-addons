package org.openhab.io.homekit.internal.listeners;

import org.openhab.io.homekit.internal.events.ServiceEvent;

public interface ServiceChangeListener {
    void onServiceEvent(ServiceEvent event);
}