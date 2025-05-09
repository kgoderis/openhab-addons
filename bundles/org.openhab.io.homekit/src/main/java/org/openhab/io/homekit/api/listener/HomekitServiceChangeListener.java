package org.openhab.io.homekit.api.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.homekit.event.model.service.HomekitServiceEvent;

@NonNullByDefault
public interface HomekitServiceChangeListener {
    void onServiceEvent(HomekitServiceEvent serviceEvent);
}
