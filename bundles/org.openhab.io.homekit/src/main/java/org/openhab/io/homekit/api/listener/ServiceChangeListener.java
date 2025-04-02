package org.openhab.io.homekit.api.listener;

import org.openhab.io.homekit.internal.events.ServiceEvent;

public interface ServiceChangeListener {
    void onServiceEvent(ServiceEvent event);
}