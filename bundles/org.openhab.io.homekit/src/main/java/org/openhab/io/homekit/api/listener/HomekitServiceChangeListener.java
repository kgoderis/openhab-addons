package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.internal.events.HomekitServiceEvent;

@NonNullByDefault
public interface HomekitServiceChangeListener {
    void onServiceEvent(HomekitServiceEvent serviceEvent);
}
