package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.ServiceEvent;

@NonNullByDefault
public interface ServiceChangeListener {
    void onServiceEvent(ServiceEvent serviceEvent);
}
